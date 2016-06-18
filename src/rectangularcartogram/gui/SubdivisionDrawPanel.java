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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.CompositeFace;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.LowDegreeVertexException;

public class SubdivisionDrawPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final double HIT_PRECISION = 7; // How close you must click to a vertex or edge in order to select it. Higher values mean you can be further away. Note that this makes it harder to select the right vertex when several are very close.
    private final int VERTEX_SIZE = 5; // Radius in pixels of the vertices
    private Subdivision subdivision; // The current subdivision
    private double zoomfactor = 1;
    private int panX = 0;
    private int panY = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private Collection<SelectionListener> listeners;
    private SubdivisionFace selectedFace = null; // The currently selected face
    private Edge selectedEdge = null; // The currently selected edge
    private boolean zoomOnPaint = false; // Whether to zoom to the graph on the next repaint, used to defer zooming until the bounds have been set correctly
    private boolean drawLabels = true;
    private boolean drawDualGraph = true;
    private boolean showError = true; // Whether to color the regions based on their cartographic error

    public SubdivisionDrawPanel() {
        initialize();
    }

    private void initialize() {
        setFocusable(true);
        setOpaque(true);
        setBackground(Color.white);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);

        try {
            subdivision = new Subdivision(new Graph());
        } catch (LowDegreeVertexException ex) {
            // won't happen
        }
        
        listeners = new ArrayList<SelectionListener>();
    }

    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    public Subdivision getSubdivision() {
        return subdivision;
    }

    public void setSubdivision(Subdivision subdivision) {
        this.subdivision = subdivision;
        setSelectedFace(null);
        zoomOnPaint = true; // This panel might not have correct bounds yet, so we defer the actual zooming until the next repaint
        repaint();
    }

    public SubdivisionFace getSelectedFace() {
        return selectedFace;
    }

    public void setSelectedFace(SubdivisionFace f) {
        // First deselect the current selected edge
        if (selectedEdge != null) {
            selectedEdge = null;

            for (SelectionListener list : listeners) {
                list.edgeSelected(this, null);
            }
        }

        if (f != selectedFace) {
            selectedFace = f;

            for (SelectionListener list : listeners) {
                list.faceSelected(this, f);
            }

            requestFocus();
        }
    }

    public Edge getSelectedEdge() {
        return selectedEdge;
    }

    public void setSelectedEdge(Edge e) {
        // First deselect the current selected face
        if (selectedFace != null) {
            selectedFace = null;

            for (SelectionListener list : listeners) {
                list.faceSelected(this, null);
            }
        }

        if (e != selectedEdge) {
            selectedEdge = e;

            for (SelectionListener list : listeners) {
                list.edgeSelected(this, e);
            }

            requestFocus();
        }
    }

    public boolean isDrawDualGraph() {
        return drawDualGraph;
    }

    public void setDrawDualGraph(boolean drawDualGraph) {
        this.drawDualGraph = drawDualGraph;
    }

    public boolean isDrawLabels() {
        return drawLabels;
    }

    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
    }

    public boolean isShowError() {
        return showError;
    }

    public void setShowError(boolean showError) {
        this.showError = showError;
    }

    public void zoomToGraph() {
        if (!subdivision.getFaces().isEmpty()) {
            int margin = 20;

            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY,
                    maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

            for (SubdivisionFace f : subdivision.getFaces()) {
                for (Vertex vertex : f.getVertices()) {
                    minX = Math.min(minX, vertex.getX());
                    minY = Math.min(minY, vertex.getY());
                    maxX = Math.max(maxX, vertex.getX());
                    maxY = Math.max(maxY, vertex.getY());
                }

                minX = Math.min(minX, f.getCorrespondingVertex().getX());
                minY = Math.min(minY, f.getCorrespondingVertex().getY());
                maxX = Math.max(maxX, f.getCorrespondingVertex().getX());
                maxY = Math.max(maxY, f.getCorrespondingVertex().getY());
            }

            double zoomfactorX = (maxX - minX) / (getWidth() - 2 * margin);
            double zoomfactorY = (maxY - minY) / (getHeight() - 2 * margin);

            if (zoomfactorY > zoomfactorX) {
                zoomfactor = zoomfactorY;
                panX = (int) Math.round((maxX + minX) / (2 * zoomfactor)) - getWidth() / 2;
                panY = (int) Math.round(maxY / zoomfactor) - getHeight() + margin;
            } else {
                zoomfactor = zoomfactorX;
                panX = (int) Math.round(minX / zoomfactor) - margin;
                panY = (int) Math.round((maxY + minY) / (2 * zoomfactor)) - getHeight() / 2;
            }
        }

        repaint();
    }

    private double xScreenToWorld(int x) {
        return (x + panX) * zoomfactor;
    }

    private double yScreenToWorld(int y) {
        return (getHeight() - y + panY) * zoomfactor;
    }

    private int xWorldToScreen(double x) {
        return (int) Math.round((x / zoomfactor) - panX);
    }

    private int yWorldToScreen(double y) {
        return getHeight() - (int) Math.round((y / zoomfactor) - panY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (zoomOnPaint) {
            zoomToGraph();
            zoomOnPaint = false;
        }

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(new BasicStroke());

        for (SubdivisionFace f : subdivision.getFaces()) {
            drawFace(g2, f, true);
        }

        // Draw the selected face again so it comes out on top
        if (selectedFace != null) {
            int[] xpoints = new int[selectedFace.getVertices().size()];
            int[] ypoints = new int[selectedFace.getVertices().size()];

            for (int i = 0; i < selectedFace.getVertices().size(); i++) {
                Vertex v = selectedFace.getVertices().get(i);

                xpoints[i] = xWorldToScreen(v.getX());
                ypoints[i] = yWorldToScreen(v.getY());
            }

            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(2));
            g2.drawPolygon(xpoints, ypoints, selectedFace.getVertices().size());
        }

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke());

        if (drawDualGraph) {
            for (Edge e : subdivision.getDualGraph().getEdges()) {
                if (e.isVisible()) {
                    if (subdivision.getDualGraph().getEdgeLabel(e) == Labeling.NONE) {
                        if (e == selectedEdge) {
                            g2.setColor(Color.GREEN);
                            g2.setStroke(new BasicStroke(2));
                        } else {
                            g2.setColor(Color.BLACK);
                            g2.setStroke(new BasicStroke());
                        }
                    } else {
                        if (e == selectedEdge) {
                            g2.setColor(Color.GREEN);
                        } else if (subdivision.getDualGraph().getEdgeLabel(e) == Labeling.RED) {
                            g2.setColor(Color.RED);
                        } else if (subdivision.getDualGraph().getEdgeLabel(e) == Labeling.BLUE) {
                            g2.setColor(Color.BLUE);
                        } else {
                            g2.setColor(Color.BLACK);
                        }

                        g2.setStroke(new BasicStroke(2));
                    }

                    drawEdge(g2, e);
                }
            }

            for (Vertex v : subdivision.getDualGraph().getVertices()) {
                if (v.isVisible()) {
                    g2.setColor(Color.BLUE);
                    g2.fillOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);

                    if (selectedFace != null && v == selectedFace.getCorrespondingVertex()) {
                        g2.setColor(Color.GREEN);
                        g2.setStroke(new BasicStroke(2));
                    } else {
                        g2.setColor(Color.BLACK);
                        g2.setStroke(new BasicStroke());
                    }

                    g2.drawOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);

                    if (drawLabels) {
                        // Draw the name of the face this vertex belongs to
                        String name = subdivision.getFace(v).getName();

                        g2.setColor(Color.BLACK);
                        g2.drawString(name, xWorldToScreen(v.getX()) + (VERTEX_SIZE + 2), yWorldToScreen(v.getY()) - (VERTEX_SIZE + 2));
                    }
                }
            }
        } else if (drawLabels) {
            for (Vertex v : subdivision.getDualGraph().getVertices()) {
                // Draw the name of the face this vertex belongs to, centered on the vertex position
                String name = subdivision.getFace(v).getName();
                FontMetrics fm = g.getFontMetrics(g.getFont());
                java.awt.geom.Rectangle2D rect = fm.getStringBounds(name, g);

                int textHeight = (int) (rect.getHeight());
                int textWidth = (int) (rect.getWidth());

                g2.setColor(Color.BLACK);
                g2.drawString(name, xWorldToScreen(v.getX()) - textWidth / 2, yWorldToScreen(v.getY()) + textHeight / 2);
            }
        }
    }

    private void drawFace(Graphics2D g2, SubdivisionFace f, boolean topLevel) {
        if (!f.getVertices().isEmpty()) {
            boolean composite = f instanceof CompositeFace;
            int[] xpoints = null, ypoints = null;

            // Fetch the screen coordinates of all vertices
            if (topLevel || !composite) {
                xpoints = new int[f.getVertices().size()];
                ypoints = new int[f.getVertices().size()];

                for (int i = 0; i < f.getVertices().size(); i++) {
                    Vertex v = f.getVertices().get(i);

                    xpoints[i] = xWorldToScreen(v.getX());
                    ypoints[i] = yWorldToScreen(v.getY());
                }
            }

            // Fill the polygon with the correct colour
            if (topLevel) {
                if (showError && !f.isSea()) {
                    g2.setColor(getErrorColor(f.getCartographicError(true)));
                } else {
                    g2.setColor(f.getColor());
                }
                g2.fillPolygon(xpoints, ypoints, f.getVertices().size());
            }

            // Recursively draw subregions of a composite face
            if (f instanceof CompositeFace) {
                drawFace(g2, ((CompositeFace) f).getFace1(), false);
                drawFace(g2, ((CompositeFace) f).getFace2(), false);
            }

            // Draw the region outline
            if (topLevel || !composite) {
                g2.setColor(topLevel ? Color.BLACK : Color.LIGHT_GRAY);
                g2.drawPolygon(xpoints, ypoints, f.getVertices().size());
            }
        }
    }

    private void drawEdge(Graphics2D g2, Edge e) {
        Vertex vA = e.getVA();
        Vertex vB = e.getVB();

        // Draw the edge
        g2.drawLine(xWorldToScreen(vA.getX()), yWorldToScreen(vA.getY()), xWorldToScreen(vB.getX()), yWorldToScreen(vB.getY()));

        if (e.isDirected()) {
            // Draw an arrowhead in the middle
            drawHead(g2, xWorldToScreen(e.getOrigin().getX()), yWorldToScreen(e.getOrigin().getY()), xWorldToScreen(0.5 * (e.getOrigin().getX() + e.getDestination().getX())), yWorldToScreen(0.5 * (e.getOrigin().getY() + e.getDestination().getY())));
        }
    }

    private void drawHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int ARROW_HEAD_WIDTH = 8;
        int ARROW_HEAD_LENGTH = 8;

        // Calculate the vector
        double vx = x2 - x1;
        double vy = y2 - y1;

        // Normalize it
        double length = Math.sqrt(vx * vx + vy * vy);

        vx = vx / length;
        vy = vy / length;

        // Get the orthogonal vector to the right
        double ox = vy;
        double oy = -vx;

        // Calculate the two points
        double leftPointX = x2 - ARROW_HEAD_LENGTH * vx - ARROW_HEAD_WIDTH * ox;
        double leftPointY = y2 - ARROW_HEAD_LENGTH * vy - ARROW_HEAD_WIDTH * oy;
        double rightPointX = x2 - ARROW_HEAD_LENGTH * vx + ARROW_HEAD_WIDTH * ox;
        double rightPointY = y2 - ARROW_HEAD_LENGTH * vy + ARROW_HEAD_WIDTH * oy;

        // Create a GeneralPath for the arrowhead
        GeneralPath arrowHead = new GeneralPath();
        arrowHead.moveTo(leftPointX, leftPointY);
        arrowHead.lineTo(x2, y2);
        arrowHead.lineTo(rightPointX, rightPointY);

        // Draw this path
        g2.draw(arrowHead);
    }

    /**
     * Public so the IPEExporter can use this too to define a region's color.
     * @param signedCartographicError
     * @return
     */
    public static Color getErrorColor(double signedCartographicError) {
        // If the region is too small, make it red. If it's too large, make it blue.
        Color tooSmallErrorColor = new Color(150, 25, 50);
        Color tooLargeErrorColor = new Color(50, 50, 175);

        double absError = Math.abs(signedCartographicError);
        double colorVal;

        // if the error is above 30% make it 100%, if it is below 5% make it 0%,
        // colorVal == 0 => darkest color, colorVal == 1 => white
        if (absError <= 0.05) {
            colorVal = 1.0;
        } else if (absError <= 0.1) {
            colorVal = 0.75;
        } else if (absError <= 0.2) {
            colorVal = 0.5;
        } else if (absError <= 0.3) {
            colorVal = 0.2;
        } else {
            colorVal = 0.0;
        }

        if (signedCartographicError < 0) {
            return new Color(tooSmallErrorColor.getRed() + (int) (colorVal * (255 - tooSmallErrorColor.getRed())),
                    tooSmallErrorColor.getGreen() + (int) (colorVal * (255 - tooSmallErrorColor.getGreen())),
                    tooSmallErrorColor.getBlue() + (int) (colorVal * (255 - tooSmallErrorColor.getBlue())));
        } else {
            return new Color(tooLargeErrorColor.getRed() + (int) (colorVal * (255 - tooLargeErrorColor.getRed())),
                    tooLargeErrorColor.getGreen() + (int) (colorVal * (255 - tooLargeErrorColor.getGreen())),
                    tooLargeErrorColor.getBlue() + (int) (colorVal * (255 - tooLargeErrorColor.getBlue())));
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            double wX = xScreenToWorld(e.getX());
            double wY = yScreenToWorld(e.getY());

            Graph graph = subdivision.getDualGraph();
            Vertex v = graph.getVertexAt(wX, wY, zoomfactor * HIT_PRECISION);

            if (v == null) {
                // Check if we selected an edge, if we didn't, getEdgeAt will return null and we clear the selection
                Edge edge = graph.getEdgeAt(wX, wY, zoomfactor * HIT_PRECISION);

                setSelectedEdge(edge);
            } else {
                setSelectedFace(subdivision.getFace(v));
            }

            repaint();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // start panning, store the current mouse position
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
            // pan
            panX += mouseX - e.getX();
            panY += e.getY() - mouseY;

            mouseX = e.getX();
            mouseY = e.getY();

            repaint();
        } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK && selectedFace != null) {
            // Only allow dragging the corresponding vertices with Control pressed, to avoid accidentally moving the vertices when selecting them.
            selectedFace.getCorrespondingVertex().setX(xScreenToWorld(e.getX()));
            selectedFace.getCorrespondingVertex().setY(yScreenToWorld(e.getY()));

            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = 1;

        if (e.getWheelRotation() < 0) {
            factor = (10.0 / 11.0);
        } else {
            factor = (11.0 / 10.0);
        }

        zoomfactor *= factor;

        int centerX = e.getX();
        int centerY = getHeight() - e.getY();
        panX = (int) Math.round((centerX + panX) / factor - centerX);
        panY = (int) Math.round((centerY + panY) / factor - centerY);

        repaint();
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            zoomToGraph();
        }
    }

    public void keyReleased(KeyEvent e) {
    }
}
