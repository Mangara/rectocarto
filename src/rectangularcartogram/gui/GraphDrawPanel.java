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
import java.util.HashMap;
import javax.swing.JPanel;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;

public class GraphDrawPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private final double HIT_PRECISION = 7; // How close you must click to a vertex or edge in order to select it. Higher values mean you can be further away. Note that this makes it harder to select the right vertex when several are very close.
    private final int VERTEX_SIZE = 5; // Radius in pixels of the vertices
    private Graph graph; // The current graph
    private Vertex selectedVertex = null; // The currently selected vertex
    private Edge selectedEdge = null; // The currently selected edge. Invariant: (selectedVertex == null) || (selectedEdge == null), meaning that you can't select both a vertex and and edge.
    private double zoomfactor = 1;
    private int panX = 0;
    private int panY = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private Collection<SelectionListener> listeners;

    public GraphDrawPanel() {
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

        graph = new Graph();
        listeners = new ArrayList<SelectionListener>();
    }

    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        setSelectedVertex(null);
        zoomToGraph();
    }

    public Edge getSelectedEdge() {
        return selectedEdge;
    }

    public Vertex getSelectedVertex() {
        return selectedVertex;
    }

    public void zoomToGraph() {
        if (!graph.getVertices().isEmpty()) {
            int margin = 20;

            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY,
                    maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

            for (Vertex vertex : graph.getVertices()) {
                minX = Math.min(minX, vertex.getX());
                minY = Math.min(minY, vertex.getY());
                maxX = Math.max(maxX, vertex.getX());
                maxY = Math.max(maxY, vertex.getY());
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
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Edge e : graph.getEdges()) {
            if (e.isVisible()) {
                if (graph.getEdgeLabel(e) == Labeling.NONE) {
                    if (e == selectedEdge) {
                        g2.setColor(Color.GREEN);
                    } else {
                        g2.setColor(Color.BLACK);
                    }

                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                } else {
                    if (e == selectedEdge) {
                        g2.setColor(Color.GREEN);
                    } else if (graph.getEdgeLabel(e) == Labeling.RED) {
                        g2.setColor(Color.RED);
                    } else if (graph.getEdgeLabel(e) == Labeling.BLUE) {
                        g2.setColor(Color.BLUE);
                    } else {
                        g2.setColor(Color.BLACK);
                    }

                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }

                drawEdge(g2, e);
            }
        }

        for (Vertex v : graph.getVertices()) {
            if (v.isVisible()) {
                g2.setColor(Color.blue);
                g2.fillOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);

                if (v == selectedVertex) {
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke());
                }

                g2.drawOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);
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

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.isControlDown()) {
                if (selectedVertex != null) {
                    double wX = xScreenToWorld(e.getX());
                    double wY = yScreenToWorld(e.getY());

                    Vertex v = graph.getVertexAt(wX, wY, zoomfactor * HIT_PRECISION);

                    if (v != null) {
                        graph.addEdge(selectedVertex, v);
                        repaint();
                    }
                }
            } else {
                double wX = xScreenToWorld(e.getX());
                double wY = yScreenToWorld(e.getY());

                Vertex v = graph.getVertexAt(wX, wY, zoomfactor * HIT_PRECISION);

                if (v == null) {
                    // Check if we selected an edge
                    Edge edge = graph.getEdgeAt(wX, wY, zoomfactor * HIT_PRECISION);

                    if (edge == null) {
                        Vertex newVertex = new Vertex(wX, wY);
                        graph.addVertex(newVertex);

                        if (e.isAltDown() && selectedVertex != null) {
                            graph.addEdge(newVertex, selectedVertex);
                        }

                        setSelectedVertex(newVertex);
                    } else {
                        setSelectedEdge(edge);
                    }
                } else {
                    if (e.isAltDown() && selectedVertex != null) {
                        graph.addEdge(v, selectedVertex);
                    }

                    setSelectedVertex(v);
                }

                repaint();
            }
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
        } else if (!e.isControlDown() && selectedVertex != null) {
            selectedVertex.setX(xScreenToWorld(e.getX()));
            selectedVertex.setY(yScreenToWorld(e.getY()));

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
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if (selectedVertex != null) {
                graph.removeVertex(selectedVertex);
                setSelectedVertex(null);
            } else if (selectedEdge != null) {
                graph.removeEdge(selectedEdge);
                setSelectedEdge(null);
            }

            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            zoomToGraph();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void setSelectedVertex(Vertex v) {
        // Deselect the current selected edge
        if (selectedEdge != null) {
            selectedEdge = null;

            for (SelectionListener list : listeners) {
                list.edgeSelected(this, null);
            }
        }

        if (v != selectedVertex) {
            selectedVertex = v;

            for (SelectionListener list : listeners) {
                list.vertexSelected(this, v);
            }

            requestFocus();
        }
    }

    public void setSelectedEdge(Edge e) {
        // Deselect the current selected vertex
        if (selectedVertex != null) {
            selectedVertex = null;

            for (SelectionListener list : listeners) {
                list.edgeSelected(this, null);
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
}
