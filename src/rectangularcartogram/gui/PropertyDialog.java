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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.JOptionPane;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.subdivision.CompositeFace;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.webimport.WFBImporter;

public class PropertyDialog extends javax.swing.JDialog {

    public enum Property {

        WEIGHT, COLOUR;
    }
    private Subdivision sub;
    private Property property;

    /** Creates new form WeightsDialog */
    public PropertyDialog(java.awt.Frame parent, boolean modal, Subdivision sub, Property property) {
        super(parent, modal);

        this.property = property;
        this.sub = sub;

        initComponents();

        if (property == Property.COLOUR) {
            importButton.setEnabled(false);
        } else if (property == Property.WEIGHT) {
            importButton.setEnabled(true);
        }

        initializeText();
    }

    private void initializeText() {
        if (property == Property.WEIGHT) {
            // Make a list of all non-composite non-sea areas and their current weight
            List<Pair<String, Double>> weights = new ArrayList<Pair<String, Double>>();

            for (SubdivisionFace f : sub.getFaces()) {
                if (!f.isSea() && !(f instanceof CompositeFace)) {
                    weights.add(new Pair<String, Double>(f.getName(), f.getWeight()));
                }
            }

            // Sort this list by the area name
            Collections.sort(weights, new Comparator<Pair<String, Double>>() {

                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                    return o1.getFirst().compareTo(o2.getFirst());
                }
            });

            // Put it in the text area
            textArea.setText("");

            for (Pair<String, Double> pair : weights) {
                textArea.append(pair.getFirst() + ',' + pair.getSecond() + '\n');
            }
        } else if (property == Property.COLOUR) {
            // Make a list of all non-composite non-sea areas and their current weight
            List<Pair<String, Color>> weights = new ArrayList<Pair<String, Color>>();

            for (SubdivisionFace f : sub.getFaces()) {
                if (!f.isSea() && !(f instanceof CompositeFace)) {
                    weights.add(new Pair<String, Color>(f.getName(), f.getColor()));
                }
            }

            // Sort this list by the area name
            Collections.sort(weights, new Comparator<Pair<String, Color>>() {

                public int compare(Pair<String, Color> o1, Pair<String, Color> o2) {
                    return o1.getFirst().compareTo(o2.getFirst());
                }
            });

            // Put it in the text area
            textArea.setText("");

            for (Pair<String, Color> pair : weights) {
                textArea.append(String.format("%s,%d-%d-%d\n", pair.getFirst(), pair.getSecond().getRed(), pair.getSecond().getGreen(), pair.getSecond().getBlue()));
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        importButton.setText("Import from web");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(importButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 167, Short.MAX_VALUE)
                .addComponent(cancelButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton)
                    .addComponent(importButton))
                .addContainerGap())
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

        textArea.setColumns(20);
        textArea.setRows(5);
        jScrollPane1.setViewportView(textArea);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            // For each line of the text area:
            // - decompose it into name,value
            // - find a subdivisionface with matching name
            // - replace its value
            String[] lines = textArea.getText().split("\n");
            HashSet<SubdivisionFace> unassigned = new HashSet<SubdivisionFace>(sub.getFaces());

            Iterator<SubdivisionFace> it = unassigned.iterator();

            while (it.hasNext()) {
                SubdivisionFace f = it.next();

                if (f.isSea() || f instanceof CompositeFace) {
                    it.remove();
                }
            }

            for (String line : lines) {
                int split = line.lastIndexOf(',');
                String name = line.substring(0, split);

                if (property == Property.WEIGHT) {
                    double value = Double.parseDouble(line.substring(split + 1));
                    SubdivisionFace face = null;

                    for (SubdivisionFace f : unassigned) {
                        if (f.getName().equals(name)) {
                            face = f;
                            break;
                        }
                    }

                    if (face == null) {
                        System.err.println("No matching region found for \"" + name + "\"");
                    } else {
                        face.setWeight(value);
                        unassigned.remove(face);
                    }
                } else if (property == Property.COLOUR) {
                    String[] components = line.substring(split + 1).split("-");
                    Color color = new Color(Integer.parseInt(components[0]), Integer.parseInt(components[1]), Integer.parseInt(components[2]));

                    SubdivisionFace face = null;

                    for (SubdivisionFace f : unassigned) {
                        String faceName = f.getName();

                        if (faceName.contains("+")) {
                            faceName = faceName.substring(0, faceName.indexOf(" +"));
                        }

                        if (faceName.equals(name)) {
                            face = f;
                            break;
                        }
                    }

                    if (face == null) {
                        System.err.println("No matching region found for \"" + name + "\"");
                    } else {
                        face.setColor(color);
                        unassigned.remove(face);
                    }
                }
            }

            if (!unassigned.isEmpty()) {
                System.err.println("The following regions were not assigned a new value:");

                for (SubdivisionFace f : unassigned) {
                    System.err.println(f.getName());
                }
            }

            sub.updateCompositeFaces();

            dispose();
        } catch (Exception e) {
            // Nice error
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "An error occurred while parsing the data:\n"
                    + e.getMessage(),
                    "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        String number = JOptionPane.showInputDialog("CIA World Fact Book statistic id:");

        if (number != null && !number.isEmpty()) {
            try {
                Map<SubdivisionFace, Double> faceValues = WFBImporter.getWeightsFromWFB(Integer.parseInt(number), sub);

                // Format the stuff as required and add it to the text area
                textArea.setText("");

                for (Map.Entry<SubdivisionFace, Double> entry : faceValues.entrySet()) {
                    textArea.append(entry.getKey().getName() + ',' + entry.getValue() + '\n');
                }
            } catch (SSLHandshakeException ex) {
                // Nice error
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while importing the data:\n"
                        + ex.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                // Nice error
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "An error occurred while importing the data:\n"
                        + ex.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_importButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton importButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
