/*
 * Copyright 2010-2016 Wouter Meulemans and Sander Verdonschot <sander.verdonschot at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rectangularcartogram.gui;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import rectangularcartogram.algos.CartogramMaker;
import rectangularcartogram.algos.DiameterCounter;
import rectangularcartogram.algos.FalseSeaAdjacenciesTest;
import rectangularcartogram.algos.GoodLabelingFinder;
import rectangularcartogram.algos.GraphChecker;
import rectangularcartogram.algos.LabelingCountEstimator;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.algos.LabelingCounter;
import rectangularcartogram.algos.RELExtracter;
import rectangularcartogram.algos.RELFusy;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.algos.SimulatedAnnealingTraverser;
import rectangularcartogram.algos.UpperboundCounter;
import rectangularcartogram.algos.ga.LabelingGA;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.embedded.EmbeddedGraph;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.ipe.IPEExporter;
import rectangularcartogram.ipe.IPEImporter;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectDirectionException;
import rectangularcartogram.exceptions.IntersectingEdgesException;
import rectangularcartogram.exceptions.LowDegreeVertexException;
import rectangularcartogram.exceptions.SeparatingTriangleException;
import rectangularcartogram.ipe.IPESubdivisionUpdater;
import rectangularcartogram.measures.AngleDeviationMeasure;
import rectangularcartogram.measures.BinaryAngleDeviationMeasure;
import rectangularcartogram.measures.BinaryBoundingBoxSeparationMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.CombinedMeasure;
import rectangularcartogram.measures.IncorrectDirectionsMeasure;
import rectangularcartogram.measures.QualityMeasure;
import rectangularcartogram.measures.QualityMeasure.Fold;
//import twistedcylinder.TriangulatedGridComputer;

public class MainFrame extends javax.swing.JFrame {

    private GraphDrawPanel drawPanel;
    private SubdivisionDrawPanel subDrawPanel;
    private FacePropertiesFrame faceProperties;
    private JFileChooser openFileChooser;
    private JFileChooser saveFileChooser;
    private JFileChooser exportFileChooser;
    private String myExtension = "grp";
    private FileNameExtensionFilter myFilter = new FileNameExtensionFilter("Graphs", myExtension);
    private String mySubExtension = "sub";
    private FileNameExtensionFilter mySubFilter = new FileNameExtensionFilter("Subdivisions", mySubExtension);
    private String ipeExtension = "ipe";
    private FileNameExtensionFilter ipeFilter = new FileNameExtensionFilter("IPE Subdivisions", ipeExtension);
    private IPEExporter ipeExporter = new IPEExporter();

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();

        drawPanel = new GraphDrawPanel();
        drawPanel.setPreferredSize(new Dimension(1200, 600));
        tabbedPane.add(drawPanel, "Graph");

        faceProperties = new FacePropertiesFrame(this);
        sidePanel.add(faceProperties, "faceProperties");

        subDrawPanel = new SubdivisionDrawPanel();
        subDrawPanel.addSelectionListener(faceProperties);
        tabbedPane.add(subDrawPanel, "Subdivision");

        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                convertMenuItem.setEnabled(false);
                subdivisionMenu.setEnabled(false);

                if (tabbedPane.getSelectedComponent() instanceof GraphDrawPanel) {
                    convertMenuItem.setEnabled(true);
                } else if (tabbedPane.getSelectedComponent() instanceof SubdivisionDrawPanel) {
                    subdivisionMenu.setEnabled(true);
                    faceProperties.setSubdrawPanel((SubdivisionDrawPanel) tabbedPane.getSelectedComponent());
                }
            }
        });

        // Initialize the file choosers
        openFileChooser = new JFileChooser(System.getProperty("user.dir"));
        openFileChooser.addChoosableFileFilter(myFilter);
        openFileChooser.addChoosableFileFilter(ipeFilter);
        openFileChooser.addChoosableFileFilter(mySubFilter);
        openFileChooser.setFileFilter(mySubFilter);
        saveFileChooser = new JFileChooser(System.getProperty("user.dir"));
        saveFileChooser.addChoosableFileFilter(myFilter);
        saveFileChooser.addChoosableFileFilter(mySubFilter);
        saveFileChooser.setFileFilter(mySubFilter);
        exportFileChooser = new JFileChooser(System.getProperty("user.dir"));
        exportFileChooser.addChoosableFileFilter(ipeFilter);
        exportFileChooser.setFileFilter(ipeFilter);
    }

    public void repaintActiveTab() {
        tabbedPane.getSelectedComponent().repaint();
    }

    private Graph loadGraph(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Graph.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private Subdivision loadSubdivision(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Subdivision.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void saveGraph(Graph graph, File file) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            // Write the data
            graph.save(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void saveSubdivision(Subdivision sub, File file) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            // Write the data
            sub.save(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupDrawVertices = new javax.swing.ButtonGroup();
        buttonGroupDrawSubs = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        sidePanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        graphMenu = new javax.swing.JMenu();
        convertMenuItem = new javax.swing.JMenuItem();
        checkMenuItem = new javax.swing.JMenuItem();
        fusyMenuItem = new javax.swing.JMenuItem();
        countLabelingsMenuItem = new javax.swing.JMenuItem();
        countDiameterMenuItem = new javax.swing.JMenuItem();
        upperboundMenuItem = new javax.swing.JMenuItem();
        estimateLabelingsMenuItem = new javax.swing.JMenuItem();
        optimizeMenuItem = new javax.swing.JMenuItem();
        worsenizeMenuItem = new javax.swing.JMenuItem();
        simAnnealMenuItem = new javax.swing.JMenuItem();
        simAnnealBadMenuItem = new javax.swing.JMenuItem();
        gaMenuItem = new javax.swing.JMenuItem();
        subdivisionMenu = new javax.swing.JMenu();
        rectilinearizeMenuItem = new javax.swing.JMenuItem();
        cartogramMenuItem = new javax.swing.JMenuItem();
        setWeightsMenuItem = new javax.swing.JMenuItem();
        copyWeightsMenuItem = new javax.swing.JMenuItem();
        setColorsMenuItem = new javax.swing.JMenuItem();
        testMenu = new javax.swing.JMenu();
        triangulatedGridMenuItem = new javax.swing.JMenuItem();
        embedMenuItem = new javax.swing.JMenuItem();
        dualMenuItem = new javax.swing.JMenuItem();
        testFalseSeaAdjacenciesMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Rectangular Cartographer");

        jSplitPane1.setDividerLocation(230);

        sidePanel.setPreferredSize(new java.awt.Dimension(800, 600));
        sidePanel.setLayout(new java.awt.CardLayout());
        jSplitPane1.setLeftComponent(sidePanel);
        jSplitPane1.setRightComponent(tabbedPane);

        fileMenu.setText("File");

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setText("New");
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText("Open...");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText("Save...");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        exportMenuItem.setText("Export to IPE");
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportMenuItem);

        menuBar.add(fileMenu);

        graphMenu.setText("Graph");

        convertMenuItem.setText("Convert to Subdivision");
        convertMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                convertMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(convertMenuItem);

        checkMenuItem.setText("Check Graph");
        checkMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(checkMenuItem);

        fusyMenuItem.setText("Label with Fusy");
        fusyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fusyMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(fusyMenuItem);

        countLabelingsMenuItem.setText("Count Labelings");
        countLabelingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countLabelingsMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(countLabelingsMenuItem);

        countDiameterMenuItem.setText("Count Diameter");
        countDiameterMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countDiameterMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(countDiameterMenuItem);

        upperboundMenuItem.setText("Compute Upper Bound");
        upperboundMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upperboundMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(upperboundMenuItem);

        estimateLabelingsMenuItem.setText("Estimate Labelings");
        estimateLabelingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                estimateLabelingsMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(estimateLabelingsMenuItem);

        optimizeMenuItem.setText("Find Optimal Labeling");
        optimizeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optimizeMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(optimizeMenuItem);

        worsenizeMenuItem.setText("Find Worst Labeling");
        worsenizeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                worsenizeMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(worsenizeMenuItem);

        simAnnealMenuItem.setText("Simulated Annealing");
        simAnnealMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simAnnealMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(simAnnealMenuItem);

        simAnnealBadMenuItem.setText("Simulated Annealing Bad");
        simAnnealBadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simAnnealBadMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(simAnnealBadMenuItem);

        gaMenuItem.setText("Genetic Algorithm");
        gaMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gaMenuItemActionPerformed(evt);
            }
        });
        graphMenu.add(gaMenuItem);

        menuBar.add(graphMenu);

        subdivisionMenu.setText("Subdivision");
        subdivisionMenu.setEnabled(false);

        rectilinearizeMenuItem.setText("Rectilinearize");
        rectilinearizeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rectilinearizeMenuItemActionPerformed(evt);
            }
        });
        subdivisionMenu.add(rectilinearizeMenuItem);

        cartogramMenuItem.setText("Make Cartogram");
        cartogramMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cartogramMenuItemActionPerformed(evt);
            }
        });
        subdivisionMenu.add(cartogramMenuItem);

        setWeightsMenuItem.setText("Set Weights");
        setWeightsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setWeightsMenuItemActionPerformed(evt);
            }
        });
        subdivisionMenu.add(setWeightsMenuItem);

        copyWeightsMenuItem.setText("Copy Weights...");
        copyWeightsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyWeightsMenuItemActionPerformed(evt);
            }
        });
        subdivisionMenu.add(copyWeightsMenuItem);

        setColorsMenuItem.setText("Set Colors");
        setColorsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setColorsMenuItemActionPerformed(evt);
            }
        });
        subdivisionMenu.add(setColorsMenuItem);

        menuBar.add(subdivisionMenu);

        testMenu.setText("Test");

        triangulatedGridMenuItem.setText("Triangulated Grid");
        triangulatedGridMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triangulatedGridMenuItemActionPerformed(evt);
            }
        });
        testMenu.add(triangulatedGridMenuItem);

        embedMenuItem.setText("Embed");
        embedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                embedMenuItemActionPerformed(evt);
            }
        });
        testMenu.add(embedMenuItem);

        dualMenuItem.setText("Dual Graph");
        dualMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dualMenuItemActionPerformed(evt);
            }
        });
        testMenu.add(dualMenuItem);

        testFalseSeaAdjacenciesMenuItem.setText("Test False Sea Adjacencies");
        testFalseSeaAdjacenciesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testFalseSeaAdjacenciesMenuItemActionPerformed(evt);
            }
        });
        testMenu.add(testFalseSeaAdjacenciesMenuItem);

        jMenuItem1.setText("Compute REL of Rectangular");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        testMenu.add(jMenuItem1);

        menuBar.add(testMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 865, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        int opened = openFileChooser.showOpenDialog(this);

        if (opened == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = openFileChooser.getSelectedFile();

                String type = null;

                if (openFileChooser.getFileFilter() == myFilter) {
                    type = "Graph";
                } else if (openFileChooser.getFileFilter() == mySubFilter) {
                    type = "Subdivision";
                } else if (openFileChooser.getFileFilter() == ipeFilter) {
                    type = "Ipe";
                } else {
                    if (selectedFile.getName().contains("." + myExtension)) {
                        type = "Graph";
                    } else if (selectedFile.getName().contains("." + mySubExtension)) {
                        type = "Subdivision";
                    } else if (selectedFile.getName().contains("." + ipeExtension)) {
                        type = "Ipe";
                    } else {
                        throw new IOException("Unknown file type.");
                    }
                }


                if (type.equals("Graph")) {
                    Graph graph = loadGraph(selectedFile);

                    if (graph != null) {
                        drawPanel.setGraph(graph);

                        try {
                            Subdivision sub = new Subdivision(graph);

                            subDrawPanel.setSubdivision(sub);

                            tabbedPane.setSelectedComponent(subDrawPanel);
                            faceProperties.setSubdrawPanel(subDrawPanel);

                            CardLayout cl = (CardLayout) sidePanel.getLayout();
                            cl.show(sidePanel, "faceProperties");
                        } catch (LowDegreeVertexException ex) {
                            handleIncorrectGraphException(ex);
                        }
                    }
                } else if (type.equals("Subdivision")) {
                    Subdivision sub = loadSubdivision(selectedFile);

                    if (sub != null) {
                        subDrawPanel.setSubdivision(sub);

                        tabbedPane.setSelectedComponent(subDrawPanel);
                        faceProperties.setSubdrawPanel(subDrawPanel);

                        CardLayout cl = (CardLayout) sidePanel.getLayout();
                        cl.show(sidePanel, "faceProperties");

                        subDrawPanel.repaint();
                    }
                } else if (type.equals("Ipe")) {
                    System.out.println("Importing graph...");
                    Graph graph = IPEImporter.importGraph(selectedFile);

                    if (graph != null) {
                        System.out.println("Correcting visibility...");
                        for (Vertex vertex : graph.getVertices()) {
                            vertex.setVisible(false);
                        }

                        drawPanel.setGraph(graph);

                        System.out.println("Converting to subdivision...");

                        for (Vertex vertex : graph.getVertices()) {
                            if (vertex.getDegree() < 2) {
                                System.out.println("Vertex of small degree: " + vertex);
                            }
                        }

                        try {
                            Subdivision sub = new Subdivision(graph);
                            IPESubdivisionUpdater.updateSubdivision(sub, selectedFile);

                            subDrawPanel.setSubdivision(sub);

                            tabbedPane.setSelectedComponent(subDrawPanel);
                            faceProperties.setSubdrawPanel(subDrawPanel);

                            CardLayout cl = (CardLayout) sidePanel.getLayout();
                            cl.show(sidePanel, "faceProperties");
                            System.out.println("Done.");
                        } catch (LowDegreeVertexException ex) {
                            System.out.println("Exception.");
                            handleIncorrectGraphException(ex);
                        }
                    }
                }

                saveFileChooser.setCurrentDirectory(selectedFile);
            } catch (IOException ioe) {
                // Nice error
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while loading the data:\n"
                        + ioe.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        int saved = saveFileChooser.showSaveDialog(this);

        if (saved == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = saveFileChooser.getSelectedFile();

                // Add an extension if that wasn't done already
                if (saveFileChooser.getFileFilter() == myFilter) {
                    if (!selectedFile.getName().contains("." + myExtension)) {
                        selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + "." + myExtension);
                    }

                    saveGraph(drawPanel.getGraph(), selectedFile);
                } else if (saveFileChooser.getFileFilter() == mySubFilter) {
                    if (!selectedFile.getName().contains("." + mySubExtension)) {
                        selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + "." + mySubExtension);
                    }

                    saveSubdivision(getCurrentSubdivision(), selectedFile);
                } else {
                    if (!selectedFile.getName().contains(".")) {
                        selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + "." + myExtension);
                    }

                    saveGraph(drawPanel.getGraph(), selectedFile);
                }

                openFileChooser.setCurrentDirectory(selectedFile);

            } catch (IOException ioe) {
                // Nice error
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while saving the data:\n"
                        + ioe.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        drawPanel.setGraph(new Graph());
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void convertMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_convertMenuItemActionPerformed
        try {
            Subdivision sub = new Subdivision(drawPanel.getGraph());

            subDrawPanel.setSubdivision(sub);

            tabbedPane.setSelectedComponent(subDrawPanel);

            CardLayout cl = (CardLayout) sidePanel.getLayout();
            cl.show(sidePanel, "faceProperties");
        } catch (LowDegreeVertexException ex) {
            handleIncorrectGraphException(ex);
        }
    }//GEN-LAST:event_convertMenuItemActionPerformed

    private void fusyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fusyMenuItemActionPerformed
        try {
            RELFusy fusy = new RELFusy();
            fusy.computeREL(getCurrentGraph());

            repaintActiveTab();
        } catch (IncorrectGraphException ex) {
            handleIncorrectGraphException(ex);
        }
    }//GEN-LAST:event_fusyMenuItemActionPerformed

    private void dualMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dualMenuItemActionPerformed
        GraphDrawPanel dual = new GraphDrawPanel();

        dual.setGraph(getCurrentGraph().getDualGraph());

        tabbedPane.add(dual, "Dual Graph");
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }//GEN-LAST:event_dualMenuItemActionPerformed

    private void rectilinearizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rectilinearizeMenuItemActionPerformed
        /*
         * GraphDrawPanel st = new GraphDrawPanel();
         *
         * RectangularDualDrawer drawer = new RectangularDualDrawer(); Graph
         * blue = drawer.drawGraph(drawPanel.getGraph()); st.setGraph(blue);
         *
         * tabbedPane.add(st, "Blue st-graph");
         * tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
         *
         * GraphDrawPanel dual = new GraphDrawPanel();
         *
         * dual.setGraph(drawer.constructDirectedDualGraph(new
         * EmbeddedGraph(blue)).getFirst());
         *
         * tabbedPane.add(dual, "Dual Graph");
         * tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
         */

        Subdivision map = getCurrentSubdivision();

        int width = map.getCartogramWidth();
        int height = map.getCartogramHeight();

        String userWidth = JOptionPane.showInputDialog(this, "Choose a width for the cartogram.", width);
        String userHeight = JOptionPane.showInputDialog(this, "Choose a height for the cartogram.", height);

        try {
            width = Integer.parseInt(userWidth);
            height = Integer.parseInt(userHeight);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid input. Using the original values.",
                    "Error!", JOptionPane.ERROR_MESSAGE);
        }

        map.setCartogramWidth(width);
        map.setCartogramHeight(height);

        try {
            RectangularDualDrawer drawer = new RectangularDualDrawer();
            Subdivision sub = drawer.drawSubdivision(getCurrentSubdivision(), true).getFirst();

            SubdivisionDrawPanel subDraw = new SubdivisionDrawPanel();
            subDraw.addSelectionListener(faceProperties);
            subDraw.setSubdivision(sub);

            tabbedPane.add(subDraw, "Rectangular Dual");
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        } catch (IncorrectGraphException e) {
            handleIncorrectGraphException(e);
        }
    }//GEN-LAST:event_rectilinearizeMenuItemActionPerformed

    private void triangulatedGridMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triangulatedGridMenuItemActionPerformed

        /*
         * System.out.println(); System.out.println(); System.out.println();
         *
         * TriangulatedGridComputer tg = new TriangulatedGridComputer(1);
         * System.out.println(); System.out.println();
        System.out.println();
         */

        /*TriangulatedGridComputer tg2 = new TriangulatedGridComputer(2);
        System.out.println();
        System.out.println();
        System.out.println();*/

        /*
         * TriangulatedGridComputer tg3 = new TriangulatedGridComputer(3);
         * System.out.println(); System.out.println();
        System.out.println();
         */

        /*
         * TriangulatedGridComputer tg4 = new TriangulatedGridComputer(4);
         * System.out.println(); System.out.println(); System.out.println();
         *
         * TriangulatedGridComputer tg5 = new TriangulatedGridComputer(5);
         * System.out.println(); System.out.println();
        System.out.println();
         */

        /*
         * TriangulatedGridComputer tg6 = new TriangulatedGridComputer(6);
         * System.out.println(); System.out.println();
        System.out.println();
         */

        //TriangulatedGridComputer tg7 = new TriangulatedGridComputer(7);

        //tg.applyFromLabeling("R11");
        //System.out.println(tg.applyToLabeling("R00"));

        //drawPanel.setGraph(tg2.getGraph());
    }//GEN-LAST:event_triangulatedGridMenuItemActionPerformed

    private void countLabelingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countLabelingsMenuItemActionPerformed
        try {
            LabelingCounter count = new LabelingCounter(getCurrentGraph());
            count.countLabelings();
            repaintActiveTab();
            JOptionPane.showMessageDialog(this, "I counted " + count.getnLabelings() + " labelings.", "Number of labelings", JOptionPane.INFORMATION_MESSAGE);
        } catch (IncorrectGraphException ex) {
            handleIncorrectGraphException(ex);
        }
    }//GEN-LAST:event_countLabelingsMenuItemActionPerformed

    private void optimizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeMenuItemActionPerformed
        Subdivision sub = getCurrentSubdivision();
        QualityMeasure measure = selectQualityMeasure(sub);

        if (measure != null) {
            try {
                GoodLabelingFinder glf = new GoodLabelingFinder(sub.getDualGraph(), measure);
                glf.findBestLabeling();
                sub.getDualGraph().setRegularEdgeLabeling(glf.getBestLabeling());
                repaintActiveTab();
            } catch (IncorrectGraphException ex) {
                handleIncorrectGraphException(ex);
            }
        }
    }//GEN-LAST:event_optimizeMenuItemActionPerformed

    private void simAnnealMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simAnnealMenuItemActionPerformed
        Subdivision sub = getCurrentSubdivision();
        QualityMeasure measure = selectQualityMeasure(sub);

        if (measure != null) {
            try {
                SimulatedAnnealingTraverser sim = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, 1000000);
                sim.findBestLabeling();
                sub.getDualGraph().setRegularEdgeLabeling(sim.getBestLabeling());
                repaintActiveTab();
            } catch (IncorrectGraphException ex) {
                handleIncorrectGraphException(ex);
            }
        }
    }//GEN-LAST:event_simAnnealMenuItemActionPerformed

    private void worsenizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_worsenizeMenuItemActionPerformed
        Subdivision sub = getCurrentSubdivision();
        QualityMeasure measure = selectQualityMeasure(sub);

        if (measure != null) {
            try {
                measure.setHigherIsBetter(!measure.higherIsBetter()); // Reverse the quality measure

                GoodLabelingFinder glf = new GoodLabelingFinder(sub.getDualGraph(), measure);
                glf.findBestLabeling();
                sub.getDualGraph().setRegularEdgeLabeling(glf.getBestLabeling());
                repaintActiveTab();
            } catch (IncorrectGraphException ex) {
                handleIncorrectGraphException(ex);
            }
        }
    }//GEN-LAST:event_worsenizeMenuItemActionPerformed

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMenuItemActionPerformed
        int saved = exportFileChooser.showSaveDialog(this);

        if (saved == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = exportFileChooser.getSelectedFile();

                // Add an extension if that wasn't done already
                if (exportFileChooser.getFileFilter() == ipeFilter) {
                    if (!selectedFile.getName().contains("." + ipeExtension)) {
                        selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + "." + ipeExtension);
                    }
                } else {
                    if (!selectedFile.getName().contains(".")) {
                        selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + "." + ipeExtension);
                    }
                }

                Component panel = tabbedPane.getSelectedComponent();

                if (panel instanceof GraphDrawPanel) {
                    ipeExporter.exportIPEFile(selectedFile, ((GraphDrawPanel) panel).getGraph(), false);
                } else if (panel instanceof SubdivisionDrawPanel) {
                    ipeExporter.exportIPEFile(selectedFile, ((SubdivisionDrawPanel) panel).getSubdivision(), false);
                }
            } catch (IOException ioe) {
                // Nice error
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while exporting the data:\n"
                        + ioe.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    private void simAnnealBadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simAnnealBadMenuItemActionPerformed
        Subdivision sub = getCurrentSubdivision();
        QualityMeasure measure = selectQualityMeasure(sub);

        if (measure != null) {
            try {
                measure.setHigherIsBetter(!measure.higherIsBetter()); // Reverse the quality measure

                SimulatedAnnealingTraverser sim = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, 10000);
                sim.findBestLabeling();
                sub.getDualGraph().setRegularEdgeLabeling(sim.getBestLabeling());
                repaintActiveTab();
            } catch (IncorrectGraphException ex) {
                handleIncorrectGraphException(ex);
            }
        }
    }//GEN-LAST:event_simAnnealBadMenuItemActionPerformed

    private void cartogramMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cartogramMenuItemActionPerformed
        IloCplex cplex = null;

        try {
            cplex = new IloCplex();
            CartogramMaker cart = new CartogramMaker(((SubdivisionDrawPanel) tabbedPane.getSelectedComponent()).getSubdivision(), cplex);

            for (int i = 0; i < 50; i++) {
                cart.iterate();
                cart.iterate();
            }

            SubdivisionDrawPanel cartPanel = new SubdivisionDrawPanel();

            cartPanel.setSubdivision(cart.getCartogram());

            tabbedPane.add(cartPanel, "Cartogram");
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        } catch (IncorrectGraphException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IloException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (cplex != null) {
                cplex.end();
            }
        }
    }//GEN-LAST:event_cartogramMenuItemActionPerformed

    private void testFalseSeaAdjacenciesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testFalseSeaAdjacenciesMenuItemActionPerformed
        FalseSeaAdjacenciesTest test = new FalseSeaAdjacenciesTest();
        test.labelHorizontalConstraintEdges(((SubdivisionDrawPanel) tabbedPane.getSelectedComponent()).getSubdivision()); // TODO: change to selected subdrawpanel
    }//GEN-LAST:event_testFalseSeaAdjacenciesMenuItemActionPerformed

    private void countDiameterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countDiameterMenuItemActionPerformed
        try {
            DiameterCounter count = new DiameterCounter(getCurrentGraph());
            count.countDiameter();
            repaintActiveTab();
            JOptionPane.showMessageDialog(this, "I counted a diameter of " + count.getCount() + " labelings.", "Diameter", JOptionPane.INFORMATION_MESSAGE);
        } catch (IncorrectGraphException ex) {
            handleIncorrectGraphException(ex);
        }
    }//GEN-LAST:event_countDiameterMenuItemActionPerformed

    private void setWeightsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setWeightsMenuItemActionPerformed
        PropertyDialog wd = new PropertyDialog(this, false, getCurrentSubdivision(), PropertyDialog.Property.WEIGHT);
        wd.setVisible(true);
    }//GEN-LAST:event_setWeightsMenuItemActionPerformed

    private void upperboundMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upperboundMenuItemActionPerformed
        System.out.println("Upper bound for current graph: " + UpperboundCounter.computeUpperBoundForGraph(getCurrentGraph()));
    }//GEN-LAST:event_upperboundMenuItemActionPerformed

    private void gaMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gaMenuItemActionPerformed
        Subdivision sub = getCurrentSubdivision();
        QualityMeasure measure = selectQualityMeasure(sub);

        if (measure != null) {
            try {
                LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);

                ga.setElitistFraction(0.04);
                ga.setCrossoverChance(0.05);
                ga.setMutationChance(0.9);
                ga.setSelection(new RankSelection(0.9));

                ga.initialize(50);
                Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(100);

                sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

                repaintActiveTab();
            } catch (IncorrectGraphException ex) {
                handleIncorrectGraphException(ex);
            }
        }
    }//GEN-LAST:event_gaMenuItemActionPerformed

    private void estimateLabelingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_estimateLabelingsMenuItemActionPerformed
        try {
            LabelingCountEstimator estimator = new LabelingCountEstimator(getCurrentGraph());
            long estimate = estimator.estimateLabelingCount();
            JOptionPane.showMessageDialog(this, "I estimate " + estimate + " labelings.", "Number of labelings", JOptionPane.INFORMATION_MESSAGE);
        } catch (IncorrectGraphException ex) {
            handleIncorrectGraphException(ex);
        }
    }//GEN-LAST:event_estimateLabelingsMenuItemActionPerformed

    private void copyWeightsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyWeightsMenuItemActionPerformed
        openFileChooser.setFileFilter(mySubFilter);
        int opened = openFileChooser.showOpenDialog(this);

        if (opened == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = openFileChooser.getSelectedFile();

                boolean isSubdivision = openFileChooser.getFileFilter() == mySubFilter || selectedFile.getName().contains("." + mySubExtension);

                if (isSubdivision) {
                    Subdivision sub = loadSubdivision(selectedFile);
                    Subdivision target = getCurrentSubdivision();

                    if (sub != null && target != null) {
                        Set<String> names = new HashSet<String>();
                        Map<String, Double> weights = new HashMap<String, Double>();

                        for (SubdivisionFace f : sub.getFaces()) {
                            if (names.add(f.getName())) {
                                // the name was not present yet
                                weights.put(f.getName(), f.getWeight());
                            } else {
                                // the name was already present
                                weights.remove(f.getName());
                            }
                        }

                        for (SubdivisionFace f : target.getFaces()) {
                            if (!f.isSea() && weights.containsKey(f.getName())) {
                                f.setWeight(weights.get(f.getName()));
                            }
                        }
                    }
                } else {
                    throw new IOException("Can only copy weights from subdivisions.");
                }

                saveFileChooser.setCurrentDirectory(selectedFile);
            } catch (IOException ioe) {
                // Nice error
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while loading the data:\n"
                        + ioe.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_copyWeightsMenuItemActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        getCurrentSubdivision().getDualGraph().setRegularEdgeLabeling(RELExtracter.findRegularEdgeLabeling(getCurrentSubdivision()));
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void embedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_embedMenuItemActionPerformed
        GraphDrawPanel embedded = new GraphDrawPanel();

        embedded.setGraph((new EmbeddedGraph(getCurrentGraph()).toGraph()));

        tabbedPane.add(embedded, "Embedded Graph");
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }//GEN-LAST:event_embedMenuItemActionPerformed

    private void checkMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkMenuItemActionPerformed
        Graph g = getCurrentGraph();

        Vertex lowDegree = GraphChecker.findLowDegreeVertex(g);

        if (lowDegree != null) {
            handleIncorrectGraphException(new LowDegreeVertexException(lowDegree));
        } else {
            boolean planar = GraphChecker.markAllIntersections(g);
            repaintActiveTab();

            if (planar) {
                List<List<Edge>> sepTriangs = GraphChecker.listSeparatingTriangles(g);

                if (sepTriangs.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Graph is okay.", "Message", JOptionPane.PLAIN_MESSAGE);
                } else {
                    handleIncorrectGraphException(new SeparatingTriangleException(sepTriangs));
                }
            } else {
                JOptionPane.showMessageDialog(this, "Graph is not planar.", "Message", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_checkMenuItemActionPerformed

    private void setColorsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setColorsMenuItemActionPerformed
        PropertyDialog wd = new PropertyDialog(this, false, getCurrentSubdivision(), PropertyDialog.Property.COLOUR);
        wd.setVisible(true);
    }//GEN-LAST:event_setColorsMenuItemActionPerformed

    private Graph getCurrentGraph() {
        Component panel = tabbedPane.getSelectedComponent();

        if (panel instanceof GraphDrawPanel) {
            return ((GraphDrawPanel) panel).getGraph();
        } else if (panel instanceof SubdivisionDrawPanel) {
            return ((SubdivisionDrawPanel) panel).getSubdivision().getDualGraph();
        } else {
            return null;
        }
    }

    private Subdivision getCurrentSubdivision() {
        Component panel = tabbedPane.getSelectedComponent();

        if (panel instanceof SubdivisionDrawPanel) {
            return ((SubdivisionDrawPanel) panel).getSubdivision();
        } else {
            return null;
        }
    }

    private QualityMeasure selectQualityMeasure(Subdivision sub) {
        Object[] possibilities = {"Angle Deviation", "Maximum Angle Deviation", "Angular Snap Correct Count", "Bounding Box Separation", "Maximum Bounding Box Separation", "Bounding Box Correct Count", "Incorrect Directions", "Combined"};
        String s = (String) JOptionPane.showInputDialog(
                this,
                "Choose a quality measure:",
                "Quality Measure",
                JOptionPane.QUESTION_MESSAGE,
                null,
                possibilities,
                possibilities[0]);

        if ((s != null) && (s.length() > 0)) {
            if ("Angle Deviation".equals(s)) {
                return new AngleDeviationMeasure(sub, Fold.AVERAGE_SQUARED);
            } else if ("Maximum Angle Deviation".equals(s)) {
                return new AngleDeviationMeasure(sub, Fold.MAXIMUM);
            } else if ("Angular Snap Correct Count".equals(s)) {
                return new BinaryAngleDeviationMeasure(new CycleGraph(sub.getDualGraph()));
            } else if ("Bounding Box Separation".equals(s)) {
                return new BoundingBoxSeparationMeasure(sub, Fold.AVERAGE_SQUARED);
            } else if ("Maximum Bounding Box Separation".equals(s)) {
                return new BoundingBoxSeparationMeasure(sub, Fold.MAXIMUM);
            } else if ("Bounding Box Correct Count".equals(s)) {
                return new BinaryBoundingBoxSeparationMeasure(sub, new CycleGraph(sub.getDualGraph()));
            } else if ("Incorrect Directions".equals(s)) {
                return new IncorrectDirectionsMeasure(sub.getDualGraph());
            } else if ("Combined".equals(s)) {
                CombinedMeasure m = new CombinedMeasure();
                m.addMeasure(new AngleDeviationMeasure(sub, Fold.AVERAGE_SQUARED), 0.5);
                m.addMeasure(new BoundingBoxSeparationMeasure(sub, Fold.AVERAGE_SQUARED), 0.5);
                return m;
            }
        }

        return null;
    }

    private void handleIncorrectGraphException(IncorrectGraphException ex) {
        if (ex instanceof LowDegreeVertexException) {
            LowDegreeVertexException ldvx = (LowDegreeVertexException) ex;

            if (!ldvx.getVertex().isVisible()) {
                ldvx.getVertex().setVisible(true);
            }

            Component panel = tabbedPane.getSelectedComponent();

            if (panel instanceof GraphDrawPanel) {
                GraphDrawPanel gdp = (GraphDrawPanel) panel;
                gdp.setSelectedVertex(ldvx.getVertex());
                gdp.repaint();
            } else if (panel instanceof SubdivisionDrawPanel) {
                SubdivisionDrawPanel sdp = (SubdivisionDrawPanel) panel;
                sdp.setSelectedFace(sdp.getSubdivision().getFace(ldvx.getVertex()));
                sdp.repaint();
            }
        } else if (ex instanceof IntersectingEdgesException) {
            IntersectingEdgesException iee = (IntersectingEdgesException) ex;

            Component panel = tabbedPane.getSelectedComponent();

            if (panel instanceof GraphDrawPanel) {
                GraphDrawPanel gdp = (GraphDrawPanel) panel;

                for (Edge edge : gdp.getGraph().getEdges()) {
                    gdp.getGraph().setLabel(edge, Graph.Labeling.NONE);
                }

                gdp.getGraph().setLabel(iee.getEdge1(), Graph.Labeling.RED);
                gdp.getGraph().setLabel(iee.getEdge2(), Graph.Labeling.RED);
                gdp.repaint();
            } else if (panel instanceof SubdivisionDrawPanel) {
                SubdivisionDrawPanel sdp = (SubdivisionDrawPanel) panel;

                for (Edge edge : sdp.getSubdivision().getDualGraph().getEdges()) {
                    sdp.getSubdivision().getDualGraph().setLabel(edge, Graph.Labeling.NONE);
                }

                sdp.getSubdivision().getDualGraph().setLabel(iee.getEdge1(), Graph.Labeling.RED);
                sdp.getSubdivision().getDualGraph().setLabel(iee.getEdge2(), Graph.Labeling.RED);
                sdp.repaint();
            }
        } else if (ex instanceof IncorrectDirectionException) {
            IncorrectDirectionException ide = (IncorrectDirectionException) ex;

            Component panel = tabbedPane.getSelectedComponent();

            if (panel instanceof GraphDrawPanel) {
                GraphDrawPanel gdp = (GraphDrawPanel) panel;
                gdp.setSelectedVertex(ide.getV());
                gdp.repaint();
            } else if (panel instanceof SubdivisionDrawPanel) {
                SubdivisionDrawPanel sdp = (SubdivisionDrawPanel) panel;
                sdp.setSelectedFace(sdp.getSubdivision().getFace(ide.getV()));
                sdp.repaint();
            }
        } else if (ex instanceof SeparatingTriangleException) {
            SeparatingTriangleException ste = (SeparatingTriangleException) ex;

            Component panel = tabbedPane.getSelectedComponent();

            if (panel instanceof GraphDrawPanel) {
                GraphDrawPanel gdp = (GraphDrawPanel) panel;

                for (Edge edge : gdp.getGraph().getEdges()) {
                    gdp.getGraph().setLabel(edge, Graph.Labeling.NONE);
                }

                for (List<Edge> triangle : ste.getTriangles()) {
                    for (Edge edge : triangle) {
                        gdp.getGraph().setLabel(edge, Graph.Labeling.RED);
                    }
                }

                gdp.repaint();
            } else if (panel instanceof SubdivisionDrawPanel) {
                SubdivisionDrawPanel sdp = (SubdivisionDrawPanel) panel;

                for (Edge edge : sdp.getSubdivision().getDualGraph().getEdges()) {
                    sdp.getSubdivision().getDualGraph().setLabel(edge, Graph.Labeling.NONE);
                }

                for (List<Edge> triangle : ste.getTriangles()) {
                    for (Edge edge : triangle) {
                        sdp.getSubdivision().getDualGraph().setLabel(edge, Graph.Labeling.RED);
                    }
                }

                sdp.repaint();
            }
        }

        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Incorrect graph:\n"
                + ex.getMessage(),
                "Error!", JOptionPane.ERROR_MESSAGE);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupDrawSubs;
    private javax.swing.ButtonGroup buttonGroupDrawVertices;
    private javax.swing.JMenuItem cartogramMenuItem;
    private javax.swing.JMenuItem checkMenuItem;
    private javax.swing.JMenuItem convertMenuItem;
    private javax.swing.JMenuItem copyWeightsMenuItem;
    private javax.swing.JMenuItem countDiameterMenuItem;
    private javax.swing.JMenuItem countLabelingsMenuItem;
    private javax.swing.JMenuItem dualMenuItem;
    private javax.swing.JMenuItem embedMenuItem;
    private javax.swing.JMenuItem estimateLabelingsMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem fusyMenuItem;
    private javax.swing.JMenuItem gaMenuItem;
    private javax.swing.JMenu graphMenu;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem optimizeMenuItem;
    private javax.swing.JMenuItem rectilinearizeMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem setColorsMenuItem;
    private javax.swing.JMenuItem setWeightsMenuItem;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JMenuItem simAnnealBadMenuItem;
    private javax.swing.JMenuItem simAnnealMenuItem;
    private javax.swing.JMenu subdivisionMenu;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JMenuItem testFalseSeaAdjacenciesMenuItem;
    private javax.swing.JMenu testMenu;
    private javax.swing.JMenuItem triangulatedGridMenuItem;
    private javax.swing.JMenuItem upperboundMenuItem;
    private javax.swing.JMenuItem worsenizeMenuItem;
    // End of variables declaration//GEN-END:variables
}
