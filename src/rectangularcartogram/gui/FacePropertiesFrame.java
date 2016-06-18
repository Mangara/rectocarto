/*
 * Copyright 2010-2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.CompositeFace;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class FacePropertiesFrame extends javax.swing.JPanel implements SelectionListener, DocumentListener {

    private MainFrame mainFrame;
    private SubdivisionDrawPanel subdrawPanel;
    private SubdivisionFace face;
    private boolean textIsStable = true;

    /** Creates new form FacePropertiesFrame */
    public FacePropertiesFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        initComponents();
    }

    public void setSubdrawPanel(SubdivisionDrawPanel subdrawPanel) {
        this.subdrawPanel = subdrawPanel;

        if (subdrawPanel.getSubdivision() == null) {
            avgErrorTextField.setText("");
            maxErrorTextField.setText("");
        } else {
            subdrawPanel.setDrawDualGraph(drawDualCheckBox.isSelected());
            subdrawPanel.setDrawLabels(drawLabelsCheckBox.isSelected());

            avgErrorTextField.setText(String.format("%.5f", subdrawPanel.getSubdivision().getAverageCartographicError()));
            maxErrorTextField.setText(String.format("%.5f", subdrawPanel.getSubdivision().getMaximumCartographicError()));
        }
    }

    public SubdivisionFace getFace() {
        return face;
    }

    public void setFace(SubdivisionFace face) {
        this.face = face;

        textIsStable = false;
        if (face == null) {
            nameTextField.setText("");
            weightTextField.setText("");
            colorFieldLabel.setBackground(Color.WHITE);
            seaCheckBox.setSelected(false);

            colorButton.setEnabled(false);
            seaCheckBox.setEnabled(false);
            nameTextField.setEnabled(false);
            weightTextField.setEnabled(false);

            weightTextField.setBackground(Color.WHITE);
        } else {
            nameTextField.setText(face.getName());
            weightTextField.setText(Double.toString(face.getWeight()));
            colorFieldLabel.setBackground(face.getColor());
            seaCheckBox.setSelected(face.isSea());

            if (face.isSea()) {
                colorButton.setEnabled(false);
                weightTextField.setEnabled(false);
            } else {
                colorButton.setEnabled(true);
                weightTextField.setEnabled(true);
            }

            seaCheckBox.setEnabled(true);
            nameTextField.setEnabled(true);

            weightTextField.setBackground(Color.WHITE);
        }
        textIsStable = true;

        // Determine whether to enable the merge button: a valid non-sea face with non-sea neighbours
        boolean enableMerge = false;
        boolean enableUnmerge = false;

        if (face != null && !face.isSea()) {
            enableMerge = !getMergeCandidates().isEmpty();
            enableUnmerge = face instanceof CompositeFace;
        }

        mergeButton.setEnabled(enableMerge);
        undoMergeButton.setEnabled(enableUnmerge);
    }

    private void textChanged() {
        if (textIsStable && face != null) {
            face.setName(nameTextField.getText());

            double weight = 0;

            try {
                weight = Double.parseDouble(weightTextField.getText());
                weightTextField.setBackground(Color.WHITE);
            } catch (NumberFormatException ex) {
                // Do nothing, weight remains 0
                weightTextField.setBackground(Color.PINK);
            }

            face.setWeight(weight);

            mainFrame.repaintActiveTab();
        }
    }

    private static boolean DEBUG = false;

    private List<SubdivisionFace> getMergeCandidates() {
        if (DEBUG) System.out.println("Finding merge candidates for " + face);

        List<SubdivisionFace> candidates = subdrawPanel.getSubdivision().getTopLevelNeighbours(face, false);
        List<SubdivisionFace> neighbours = subdrawPanel.getSubdivision().getTopLevelNeighbours(face, true);
        if (DEBUG) System.out.println("All neighbours of current face: " + neighbours);
        if (DEBUG) System.out.println("Current candidates: " + candidates);

        // All other neighbours of this face should also be neighbours of the face into which we are merging
        for (SubdivisionFace n : neighbours) {
            if (!n.isSea()) {
                List<SubdivisionFace> neighbours2 = subdrawPanel.getSubdivision().getTopLevelNeighbours(n, true);

                for (SubdivisionFace nn : neighbours) {
                    if (nn != n && !neighbours2.contains(nn)) {
                        if (DEBUG) System.out.println(nn + " is a neighbour of this face, but not of " + n);
                        candidates.remove(n);
                        break;
                    }
                }
            }
        }

        if (DEBUG) System.out.println("Final list of candidates: " + candidates);
        if (DEBUG) System.out.println();

        return candidates;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        facePropertiesPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        weightLabel = new javax.swing.JLabel();
        weightTextField = new javax.swing.JTextField();
        colorLabel = new javax.swing.JLabel();
        colorButton = new javax.swing.JButton();
        colorFieldLabel = new javax.swing.JLabel();
        seaCheckBox = new javax.swing.JCheckBox();
        drawingOptionsPanel = new javax.swing.JPanel();
        drawDualCheckBox = new javax.swing.JCheckBox();
        drawLabelsCheckBox = new javax.swing.JCheckBox();
        errorColoursCheckBox = new javax.swing.JCheckBox();
        cartoErrorsPanel = new javax.swing.JPanel();
        avgErrorLabel = new javax.swing.JLabel();
        maxErrorLabel = new javax.swing.JLabel();
        avgErrorTextField = new javax.swing.JTextField();
        maxErrorTextField = new javax.swing.JTextField();
        mergeButton = new javax.swing.JButton();
        undoMergeButton = new javax.swing.JButton();

        facePropertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Face Properties"));
        facePropertiesPanel.setName("facePropertiesPanel"); // NOI18N

        nameLabel.setText("Name");
        nameLabel.setName("nameLabel"); // NOI18N

        nameTextField.setEnabled(false);
        nameTextField.setName("nameTextField"); // NOI18N
        nameTextField.getDocument().addDocumentListener(this);

        weightLabel.setText("Weight");
        weightLabel.setName("weightLabel"); // NOI18N

        weightTextField.setEnabled(false);
        weightTextField.setName("weightTextField"); // NOI18N
        weightTextField.getDocument().addDocumentListener(this);

        colorLabel.setText("Colour");
        colorLabel.setName("colorLabel"); // NOI18N

        colorButton.setText("Change...");
        colorButton.setEnabled(false);
        colorButton.setName("colorButton"); // NOI18N
        colorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorButtonActionPerformed(evt);
            }
        });

        colorFieldLabel.setBackground(new java.awt.Color(255, 255, 255));
        colorFieldLabel.setName("colorFieldLabel"); // NOI18N
        colorFieldLabel.setOpaque(true);

        seaCheckBox.setText("Sea");
        seaCheckBox.setEnabled(false);
        seaCheckBox.setName("seaCheckBox"); // NOI18N
        seaCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seaCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout facePropertiesPanelLayout = new javax.swing.GroupLayout(facePropertiesPanel);
        facePropertiesPanel.setLayout(facePropertiesPanelLayout);
        facePropertiesPanelLayout.setHorizontalGroup(
            facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 334, Short.MAX_VALUE)
            .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(facePropertiesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(nameLabel)
                        .addComponent(weightLabel)
                        .addComponent(colorLabel))
                    .addGap(30, 30, 30)
                    .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(weightTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                        .addComponent(nameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                        .addComponent(seaCheckBox)
                        .addGroup(facePropertiesPanelLayout.createSequentialGroup()
                            .addComponent(colorFieldLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(colorButton, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)))
                    .addContainerGap()))
        );
        facePropertiesPanelLayout.setVerticalGroup(
            facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 176, Short.MAX_VALUE)
            .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(facePropertiesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(nameLabel)
                        .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(weightLabel)
                        .addComponent(weightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(facePropertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(colorLabel)
                            .addComponent(colorButton))
                        .addComponent(colorFieldLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(seaCheckBox)
                    .addContainerGap(35, Short.MAX_VALUE)))
        );

        drawingOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Drawing Options"));
        drawingOptionsPanel.setName("drawingOptionsPanel"); // NOI18N

        drawDualCheckBox.setSelected(true);
        drawDualCheckBox.setText("Draw Dual Graph");
        drawDualCheckBox.setName("drawDualCheckBox"); // NOI18N
        drawDualCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawDualCheckBoxItemStateChanged(evt);
            }
        });

        drawLabelsCheckBox.setSelected(true);
        drawLabelsCheckBox.setText("Draw Region Labels");
        drawLabelsCheckBox.setName("drawLabelsCheckBox"); // NOI18N
        drawLabelsCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawLabelsCheckBoxItemStateChanged(evt);
            }
        });

        errorColoursCheckBox.setSelected(true);
        errorColoursCheckBox.setText("Colour Regions by Error");
        errorColoursCheckBox.setName("errorColoursCheckBox"); // NOI18N
        errorColoursCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                errorColoursCheckBoxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout drawingOptionsPanelLayout = new javax.swing.GroupLayout(drawingOptionsPanel);
        drawingOptionsPanel.setLayout(drawingOptionsPanelLayout);
        drawingOptionsPanelLayout.setHorizontalGroup(
            drawingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(drawingOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(drawingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(drawLabelsCheckBox)
                    .addComponent(drawDualCheckBox)
                    .addComponent(errorColoursCheckBox))
                .addContainerGap(149, Short.MAX_VALUE))
        );
        drawingOptionsPanelLayout.setVerticalGroup(
            drawingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(drawingOptionsPanelLayout.createSequentialGroup()
                .addComponent(drawDualCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(drawLabelsCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(errorColoursCheckBox))
        );

        cartoErrorsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Cartographic Errors"));
        cartoErrorsPanel.setName("cartoErrorsPanel"); // NOI18N

        avgErrorLabel.setText("Average Error");
        avgErrorLabel.setName("avgErrorLabel"); // NOI18N

        maxErrorLabel.setText("Maximum Error");
        maxErrorLabel.setName("maxErrorLabel"); // NOI18N

        avgErrorTextField.setEditable(false);
        avgErrorTextField.setName("avgErrorTextField"); // NOI18N

        maxErrorTextField.setEditable(false);
        maxErrorTextField.setName("maxErrorTextField"); // NOI18N

        javax.swing.GroupLayout cartoErrorsPanelLayout = new javax.swing.GroupLayout(cartoErrorsPanel);
        cartoErrorsPanel.setLayout(cartoErrorsPanelLayout);
        cartoErrorsPanelLayout.setHorizontalGroup(
            cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cartoErrorsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(avgErrorLabel)
                    .addComponent(maxErrorLabel))
                .addGap(15, 15, 15)
                .addGroup(cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(maxErrorTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                    .addComponent(avgErrorTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))
                .addContainerGap())
        );
        cartoErrorsPanelLayout.setVerticalGroup(
            cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cartoErrorsPanelLayout.createSequentialGroup()
                .addGroup(cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(avgErrorLabel)
                    .addComponent(avgErrorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(cartoErrorsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxErrorLabel)
                    .addComponent(maxErrorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mergeButton.setText("Merge with neighbour...");
        mergeButton.setEnabled(false);
        mergeButton.setName("mergeButton"); // NOI18N
        mergeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeButtonActionPerformed(evt);
            }
        });

        undoMergeButton.setText("Undo Merge");
        undoMergeButton.setEnabled(false);
        undoMergeButton.setName("undoMergeButton"); // NOI18N
        undoMergeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoMergeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(drawingOptionsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cartoErrorsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(facePropertiesPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(undoMergeButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(mergeButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(facePropertiesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(drawingOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cartoErrorsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mergeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(undoMergeButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void colorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorButtonActionPerformed
        Color newColour = JColorChooser.showDialog(this, "Choose a new color", face.getColor());
        face.setColor(newColour);
        colorFieldLabel.setBackground(newColour);

        mainFrame.repaintActiveTab();
    }//GEN-LAST:event_colorButtonActionPerformed

    private void seaCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seaCheckBoxActionPerformed
        if (seaCheckBox.isSelected()) {
            textIsStable = false;

            // Disable the gui elements
            weightTextField.setEnabled(false);
            colorButton.setEnabled(false);

            // Set the correct sea settings for the face
            face.setSea(true);
            face.setWeight(SubdivisionFace.SEA_WEIGHT);
            face.setColor(SubdivisionFace.SEA_COLOR);

            // Show the correct sea settings in the gui
            weightTextField.setText(Double.toString(SubdivisionFace.SEA_WEIGHT));
            colorFieldLabel.setBackground(SubdivisionFace.SEA_COLOR);

            mainFrame.repaintActiveTab();
            textIsStable = true;
        } else {
            // Enable the other gui elements
            weightTextField.setEnabled(true);
            colorButton.setEnabled(true);

            // The face isn't a sea any more
            face.setSea(false);
        }
    }//GEN-LAST:event_seaCheckBoxActionPerformed

    private void drawDualCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawDualCheckBoxItemStateChanged
        if (subdrawPanel != null) {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                subdrawPanel.setDrawDualGraph(true);
            } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                subdrawPanel.setDrawDualGraph(false);
            }

            subdrawPanel.repaint();
        }
    }//GEN-LAST:event_drawDualCheckBoxItemStateChanged

    private void drawLabelsCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawLabelsCheckBoxItemStateChanged
        if (subdrawPanel != null) {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                subdrawPanel.setDrawLabels(true);
            } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                subdrawPanel.setDrawLabels(false);
            }

            subdrawPanel.repaint();
        }
    }//GEN-LAST:event_drawLabelsCheckBoxItemStateChanged

    private void mergeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeButtonActionPerformed
        SubdivisionFace mergeWith = (SubdivisionFace) JOptionPane.showInputDialog(
                mainFrame,
                "Select a neighbour:",
                "Merge",
                JOptionPane.QUESTION_MESSAGE,
                null,
                getMergeCandidates().toArray(),
                null);

        if (mergeWith != null) {
            subdrawPanel.setSelectedFace(subdrawPanel.getSubdivision().merge(face, mergeWith));
            subdrawPanel.repaint();
        }
    }//GEN-LAST:event_mergeButtonActionPerformed

    private void undoMergeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoMergeButtonActionPerformed
        subdrawPanel.setSelectedFace(subdrawPanel.getSubdivision().unmerge((CompositeFace) face));
        subdrawPanel.repaint();
    }//GEN-LAST:event_undoMergeButtonActionPerformed

    private void errorColoursCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_errorColoursCheckBoxItemStateChanged
        if (subdrawPanel != null) {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                subdrawPanel.setShowError(true);
            } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                subdrawPanel.setShowError(false);
            }

            subdrawPanel.repaint();
        }
    }//GEN-LAST:event_errorColoursCheckBoxItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel avgErrorLabel;
    private javax.swing.JTextField avgErrorTextField;
    private javax.swing.JPanel cartoErrorsPanel;
    private javax.swing.JButton colorButton;
    private javax.swing.JLabel colorFieldLabel;
    private javax.swing.JLabel colorLabel;
    private javax.swing.JCheckBox drawDualCheckBox;
    private javax.swing.JCheckBox drawLabelsCheckBox;
    private javax.swing.JPanel drawingOptionsPanel;
    private javax.swing.JCheckBox errorColoursCheckBox;
    private javax.swing.JPanel facePropertiesPanel;
    private javax.swing.JLabel maxErrorLabel;
    private javax.swing.JTextField maxErrorTextField;
    private javax.swing.JButton mergeButton;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JCheckBox seaCheckBox;
    private javax.swing.JButton undoMergeButton;
    private javax.swing.JLabel weightLabel;
    private javax.swing.JTextField weightTextField;
    // End of variables declaration//GEN-END:variables

    public void edgeSelected(JPanel source, Edge edge) {
    }

    public void vertexSelected(JPanel source, Vertex vertex) {
    }

    public void faceSelected(SubdivisionDrawPanel source, SubdivisionFace face) {
        setFace(face);
    }

    public void insertUpdate(DocumentEvent e) {
        textChanged();
    }

    public void removeUpdate(DocumentEvent e) {
        textChanged();
    }

    public void changedUpdate(DocumentEvent e) {
    }
}
