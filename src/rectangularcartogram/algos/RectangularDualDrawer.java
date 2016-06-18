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
package rectangularcartogram.algos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.embedded.EmbeddedGraph;
import rectangularcartogram.data.embedded.EmbeddedVertex;
import rectangularcartogram.data.embedded.Face;
import rectangularcartogram.data.embedded.HalfEdge;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class RectangularDualDrawer {

    // ST-graphs
    private Graph red; // Red st-graph
    private Graph blue; // Blue st-graph
    private HashMap<Vertex, Vertex> originalToRed; // Mapping between vertices in the original graph and in the red st-graph
    private HashMap<Vertex, Vertex> originalToBlue; // Mapping between vertices in the original graph and in the blue st-graph
    private EmbeddedGraph redDCEL; // DCEL of the red st-graph
    private EmbeddedGraph blueDCEL; // DCEL of the blue st-graph
    private HashMap<Vertex, EmbeddedVertex> redToDCEL; // Mapping between vertices in the normal version and in the DCEL version of the red st-graph
    private HashMap<Vertex, EmbeddedVertex> blueToDCEL; // Mapping between vertices in the normal version and in the DCEL version of the blue st-graph
    // st-like numberings for the faces of the red and blue graph
    private HashMap<Face, Integer> redFaceNumbering;
    private HashMap<Face, Integer> blueFaceNumbering;
    // Rectangles of the resulting rectangular dual, corresponding to the vertices in the input graph
    private HashMap<Vertex, Rectangle> rectangles;

    /**
     *
     * @param subdivision
     * @param drawCompact - If this is true, multiple segments can use the same coordinates. This makes better pictures, but prevents segment identification by coordinate.
     * @return
     */
    public Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> drawSubdivision(Subdivision subdivision, boolean drawCompact) throws IncorrectGraphException {
        GraphChecker.checkGraph(subdivision.getDualGraph());
        
        //System.out.println("Constructing st-graphs");
        constructStGraphs(subdivision.getDualGraph());
        //System.out.println("Constructing face numbering");
        computeFaceNumbering(drawCompact);
        //System.out.println("Constructing rectangles");
        computeRectangles(subdivision.getDualGraph());

        //System.out.println("Constructing result");
        Subdivision result = new Subdivision();

        // Calculate the correct scale to match the specified cartogram dimensions
        result.setCartogramWidth(subdivision.getCartogramWidth());
        result.setCartogramHeight(subdivision.getCartogramHeight());

        int maxX = Collections.max(redFaceNumbering.values()) + 1;
        int maxY = Collections.max(blueFaceNumbering.values()) + 1;

        double xScale = subdivision.getCartogramWidth() / (double) maxX;
        double yScale = subdivision.getCartogramHeight() / (double) maxY;

        HashMap<Vertex, Vertex> vertexMap = new HashMap<Vertex, Vertex>(2 * subdivision.getFaces().size());
        HashMap<SubdivisionFace, SubdivisionFace> faceMap = new HashMap<SubdivisionFace, SubdivisionFace>(2 * subdivision.getFaces().size());

        for (SubdivisionFace face : subdivision.getTopLevelFaces()) {
            Rectangle rect = rectangles.get(face.getCorrespondingVertex());

            ArrayList<Vertex> corners = new ArrayList<Vertex>(4);
            corners.add(new Vertex(xScale * rect.xLeft, yScale * rect.yLow));
            corners.add(new Vertex(xScale * rect.xRight, yScale * rect.yLow));
            corners.add(new Vertex(xScale * rect.xRight, yScale * rect.yHigh));
            corners.add(new Vertex(xScale * rect.xLeft, yScale * rect.yHigh));

            boolean boundaryRegion = subdivision.getDualGraph().getExteriorVertices().contains(face.getCorrespondingVertex());
            SubdivisionFace newFace = new SubdivisionFace(corners, face.getColor(), face.getName(), face.getWeight(), face.isSea(), boundaryRegion);
            result.addFace(newFace);

            faceMap.put(face, newFace);

            result.getFaceMap().put(newFace.getCorrespondingVertex(), newFace);
            vertexMap.put(face.getCorrespondingVertex(), newFace.getCorrespondingVertex());
        }

        Graph dual = new Graph();

        for (Vertex v : subdivision.getDualGraph().getVertices()) {
            dual.addVertex(vertexMap.get(v));
        }

        Map<Edge, Edge> edgeMap = new HashMap<Edge, Edge>(subdivision.getDualGraph().getEdges().size() * 2);
        
        for (Edge e : subdivision.getDualGraph().getEdges()) {
            Edge newEdge = dual.addEdge(vertexMap.get(e.getVA()), vertexMap.get(e.getVB()));
            newEdge.setDirection(e.getDirection());
            edgeMap.put(e, newEdge);
        }
        
        for (Edge e : subdivision.getDualGraph().getEdges()) {
            dual.setLabel(edgeMap.get(e), subdivision.getDualGraph().getEdgeLabel(e));
        }

        result.setDualGraph(dual);

        return new Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>>(result, faceMap);
    }

    /**
     * Input: a triangulated graph with quadrangular outer face and a regular edge labeling
     * @param graph
     * @param drawCompact - If this is true, multiple segments can use the same coordinates. This makes better pictures, but prevents segment identification by coordinate.
     * @return
     */
    public Graph drawGraph(Graph graph, boolean drawCompact) {
        constructStGraphs(graph);
        computeFaceNumbering(drawCompact);
        computeRectangles(graph);

        // Lelijke hack voor visualisatie
        Graph result = new Graph();

        for (Rectangle rect : rectangles.values()) {
            Vertex bottomLeft = new Vertex(rect.xLeft, rect.yLow);
            Vertex bottomRight = new Vertex(rect.xRight, rect.yLow);
            Vertex topRight = new Vertex(rect.xRight, rect.yHigh);
            Vertex topLeft = new Vertex(rect.xLeft, rect.yHigh);

            result.addVertex(bottomLeft);
            result.addVertex(bottomRight);
            result.addVertex(topRight);
            result.addVertex(topLeft);

            result.addEdge(bottomLeft, bottomRight);
            result.addEdge(bottomRight, topRight);
            result.addEdge(topRight, topLeft);
            result.addEdge(topLeft, bottomLeft);
        }

        return result;
    }

    private void constructStGraphs(Graph graph) {
        RegularEdgeLabeling labeling = graph.getRegularEdgeLabeling();

        // Construct the blue and red st-graphs
        red = new Graph();
        blue = new Graph();

        originalToRed = new HashMap<Vertex, Vertex>(2 * graph.getVertices().size());
        originalToBlue = new HashMap<Vertex, Vertex>(2 * graph.getVertices().size());

        for (Vertex v : graph.getVertices()) {
            Vertex blueVertex = new Vertex(v.getX(), v.getY());
            Vertex redVertex = new Vertex(v.getX(), v.getY());

            red.addVertex(blueVertex);
            blue.addVertex(redVertex);

            originalToRed.put(v, blueVertex);
            originalToBlue.put(v, redVertex);
        }

        for (Edge e : graph.getEdges()) {
            if (labeling.get(e).getFirst() == Labeling.RED) {
                Edge newEdge = red.addEdge(originalToRed.get(e.getVA()), originalToRed.get(e.getVB()));
                newEdge.setDirection(e.getDirection());
            } else if (labeling.get(e).getFirst() == Labeling.BLUE) {
                Edge newEdge = blue.addEdge(originalToBlue.get(e.getVA()), originalToBlue.get(e.getVB()));
                newEdge.setDirection(e.getDirection());
            }
        }

        // Red exterior edges: S -> W, W -> N, S -> E, E -> N, (S -> N)
        Edge newEdge = red.addEdge(originalToRed.get(graph.getVS()), originalToRed.get(graph.getVW()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = red.addEdge(originalToRed.get(graph.getVW()), originalToRed.get(graph.getVN()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = red.addEdge(originalToRed.get(graph.getVS()), originalToRed.get(graph.getVE()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = red.addEdge(originalToRed.get(graph.getVE()), originalToRed.get(graph.getVN()));
        newEdge.setDirection(Edge.Direction.AB);
        //newEdge = blue.addEdge(originalToBlue.get(graph.getVS()), originalToBlue.get(graph.getVN()));
        //newEdge.setDirection(Edge.Direction.AB);

        // Blue exterior edges: W -> S, S -> E, W -> N, N -> E, (W -> E)
        newEdge = blue.addEdge(originalToBlue.get(graph.getVW()), originalToBlue.get(graph.getVS()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = blue.addEdge(originalToBlue.get(graph.getVS()), originalToBlue.get(graph.getVE()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = blue.addEdge(originalToBlue.get(graph.getVW()), originalToBlue.get(graph.getVN()));
        newEdge.setDirection(Edge.Direction.AB);
        newEdge = blue.addEdge(originalToBlue.get(graph.getVN()), originalToBlue.get(graph.getVE()));
        newEdge.setDirection(Edge.Direction.AB);
        //newEdge = red.addEdge(originalToRed.get(graph.getVW()), originalToRed.get(graph.getVE()));
        //newEdge.setDirection(Edge.Direction.AB);

        redDCEL = new EmbeddedGraph(red);
        redToDCEL = redDCEL.getVertexMap();
        blueDCEL = new EmbeddedGraph(blue);
        blueToDCEL = blueDCEL.getVertexMap();
    }

    private void computeFaceNumbering(boolean drawCompact) {
        // Build dual graphs
        Pair<Graph, HashMap<Face, Vertex>> redDualPair = constructDirectedDualGraph(redDCEL);
        Graph redDual = redDualPair.getFirst(); // The dual graph of the red st-graph
        HashMap<Face, Vertex> redToDual = redDualPair.getSecond(); // Mapping between the faces in the red st-graph and the vertices in its dual

        Pair<Graph, HashMap<Face, Vertex>> blueDualPair = constructDirectedDualGraph(blueDCEL);
        Graph blueDual = blueDualPair.getFirst(); // The dual graph of the blue st-graph
        HashMap<Face, Vertex> blueToDual = blueDualPair.getSecond(); // Mapping between the faces in the blue st-graph and the vertices in its dual

        // Compute the numberings.
        // Longest path allows multiple sements to use the same coordinates as long as they don't touch, while topological sort does not.
        // So longest path makes better pictures, while topological sort allows for segment identification by coordinate.
        HashMap<Vertex, Integer> redDualNumbering;
        HashMap<Vertex, Integer> blueDualNumbering;

        if (drawCompact) {
            redDualNumbering = computeLongestPathFromSource(redDual);
            blueDualNumbering = computeLongestPathFromSource(blueDual);
        } else {
            redDualNumbering = topologicalSort(redDual);
            blueDualNumbering = topologicalSort(blueDual);
        }

        // Translate it to face numberings
        redFaceNumbering = new HashMap<Face, Integer>(2 * redDCEL.getFaces().size());
        blueFaceNumbering = new HashMap<Face, Integer>(2 * blueDCEL.getFaces().size());

        for (Face face : redDCEL.getFaces()) {
            if (!face.isOuterFace()) {
                redFaceNumbering.put(face, redDualNumbering.get(redToDual.get(face)));
            }
        }

        for (Face face : blueDCEL.getFaces()) {
            if (!face.isOuterFace()) {
                blueFaceNumbering.put(face, blueDualNumbering.get(blueToDual.get(face)));
            }
        }
    }

    private void computeRectangles(Graph graph) {
        rectangles = new HashMap<Vertex, Rectangle>(graph.getVertices().size() * 2);

        int d1 = Collections.max(redFaceNumbering.values()) + 1; // maxX
        int d2 = Collections.max(blueFaceNumbering.values()) + 1; // maxY

        // Inner vertices
        for (Vertex v : graph.getVertices()) {
            if (!graph.getExteriorVertices().contains(v)) {
                int xLeft = redFaceNumbering.get(getLeftFace(redToDCEL.get(originalToRed.get(v))));
                int xRight = redFaceNumbering.get(getRightFace(redToDCEL.get(originalToRed.get(v))));
                int yLow = d2 - blueFaceNumbering.get(getRightFace(blueToDCEL.get(originalToBlue.get(v))));
                int yHigh = d2 - blueFaceNumbering.get(getLeftFace(blueToDCEL.get(originalToBlue.get(v))));

                rectangles.put(v, new Rectangle(xLeft, xRight, yLow, yHigh));
            }
        }

        // Exterior vertices are special
        rectangles.put(graph.getVN(), new Rectangle(1, d1 - 1, d2 - 1, d2));
        rectangles.put(graph.getVE(), new Rectangle(d1 - 1, d1, 0, d2));
        rectangles.put(graph.getVS(), new Rectangle(1, d1 - 1, 0, 1));
        rectangles.put(graph.getVW(), new Rectangle(0, 1, 0, d2));
    }

    public Pair<Graph, HashMap<Face, Vertex>> constructDirectedDualGraph(EmbeddedGraph g) {
        Pair<Graph, HashMap<Face, Vertex>> dualPair = g.getDualGraph();
        Graph dual = dualPair.getFirst();
        HashMap<Face, Vertex> faceMap = dualPair.getSecond();

        for (HalfEdge dart : g.getDarts()) {
            if (dart.isDirected() && dart.isEdgeDirection()) {
                // Find the edge corresponding to this adjacency
                Edge edge = null;

                for (Edge e : faceMap.get(dart.getFace()).getEdges()) {
                    if ((e.getVA() == faceMap.get(dart.getFace()) && e.getVB() == faceMap.get(dart.getTwin().getFace())) ||
                            (e.getVB() == faceMap.get(dart.getFace()) && e.getVA() == faceMap.get(dart.getTwin().getFace()))) {
                        edge = e;
                        break;
                    }
                }

                if (edge == null) {
                    System.err.println("No edge found for dart");
                    continue;
                }

                // Direct the edge from left to right = from own face to twins face
                if (edge.getVA() == faceMap.get(dart.getFace())) {
                    edge.setDirection(Edge.Direction.AB);
                } else {
                    edge.setDirection(Edge.Direction.BA);
                }
            }
        }

        // Remove the outer face
        for (Face face : g.getFaces()) {
            if (face.isOuterFace()) {
                dual.removeVertex(faceMap.get(face));
                faceMap.remove(face);
            }
        }

        return new Pair<Graph, HashMap<Face, Vertex>>(dual, faceMap);
    }

    /**
     * Assumption: all edges of the graph are directed
     * @param graph
     * @return
     */
    private HashMap<Vertex, Integer> computeLongestPathFromSource(Graph graph) {
        HashMap<Vertex, Integer> pathLengths = new HashMap<Vertex, Integer>(2 * graph.getVertices().size());
        HashMap<Vertex, Integer> unmarkedPredecessors = new HashMap<Vertex, Integer>(2 * graph.getVertices().size());
        Queue<Vertex> nextVertices = new LinkedList<Vertex>(); // Vertices with no unmarked predecessors, but which haven't been numbered themselves

        // Fill the pathLengths with 0
        for (Vertex v : graph.getVertices()) {
            pathLengths.put(v, 0);
        }

        // Compute the number of predecessors for each vertex and add those with no predecessors to the queue
        for (Vertex v : graph.getVertices()) {
            int predecessors = 0;

            for (Edge e : v.getEdges()) {
                if (e.getDestination() == v) {
                    predecessors++;
                }
            }

            unmarkedPredecessors.put(v, predecessors);

            if (predecessors == 0) {
                nextVertices.add(v);
            }
        }

        while (!nextVertices.isEmpty()) {
            Vertex v = nextVertices.remove();

            // The number v gets is the maximum of its predecessors, plus one
            int maxPredNumber = 0;

            for (Edge e : v.getEdges()) {
                if (e.getDestination() == v) {
                    // e is incoming
                    maxPredNumber = Math.max(maxPredNumber, pathLengths.get(e.getOrigin()));
                } else {
                    // e is outgoing; let the next vertices know that v is now marked
                    int unmarked = unmarkedPredecessors.get(e.getDestination());

                    unmarkedPredecessors.put(e.getDestination(), unmarked - 1);

                    // If v was the last unmarked predecessor, this vertex is ready to be marked as well
                    if (unmarked == 1) {
                        nextVertices.add(e.getDestination());
                    }
                }
            }

            pathLengths.put(v, maxPredNumber + 1);
        }

        return pathLengths;
    }

    private HashMap<Vertex, Integer> topologicalSort(Graph graph) {
        HashMap<Vertex, Integer> indices = new HashMap<Vertex, Integer>(2 * graph.getVertices().size());
        HashMap<Vertex, Integer> unmarkedPredecessors = new HashMap<Vertex, Integer>(2 * graph.getVertices().size());
        Queue<Vertex> nextVertices = new LinkedList<Vertex>(); // Vertices with no unmarked predecessors, but which haven't been numbered yet

        // Fill the indices with 0 TODO: unnecessary?
        for (Vertex v : graph.getVertices()) {
            indices.put(v, 0);
        }

        // Compute the number of predecessors for each vertex and add those with no predecessors to the queue
        for (Vertex v : graph.getVertices()) {
            int predecessors = 0;

            for (Edge e : v.getEdges()) {
                if (e.getDestination() == v) {
                    predecessors++;
                }
            }

            unmarkedPredecessors.put(v, predecessors);

            if (predecessors == 0) {
                nextVertices.add(v);
            }
        }

        int currentIndex = 1;

        while (!nextVertices.isEmpty()) {
            Vertex v = nextVertices.remove();

            // Assign the current index to v
            indices.put(v, currentIndex);
            currentIndex++;

            for (Edge e : v.getEdges()) {
                if (e.getOrigin() == v) {
                    // e is outgoing; let the next vertices know that v is now marked
                    int unmarked = unmarkedPredecessors.get(e.getDestination());

                    unmarkedPredecessors.put(e.getDestination(), unmarked - 1);

                    // If v was the last unmarked predecessor, this vertex is ready to be marked as well
                    if (unmarked == 1) {
                        nextVertices.add(e.getDestination());
                    }
                }
            }
        }

        return indices;
    }

    /**
     * Returns the face that lies between the incoming and outgoing edges of v, in clockwise order. (incoming edges -> leftFace -> outgoing edges)
     * @param v
     * @return
     */
    private Face getLeftFace(EmbeddedVertex v) {
        List<HalfEdge> darts = v.getEdges();

        // Find the incoming edges. After this loop, the index will point to the first incoming edge in the list (not necessarily the first incoming edge around the vertex)
        boolean foundIncoming = false;
        int i = 0;

        while (!foundIncoming) {
            if (!darts.get(i).isEdgeDirection()) {
                foundIncoming = true;
            } else {
                i++;
            }
        }

        // Find the first outgoing edge around the vertex in clockwise order: it is the first outgoing edge after all incoming edges
        boolean foundLeft = false;
        Face left = null;

        while (!foundLeft) {
            if (darts.get(i).isEdgeDirection()) {
                // the left face is the face assigned to the outgoing dart that corresponds to the first outgoing edge around this vertex
                left = darts.get(i).getFace();
                foundLeft = true;
            } else {
                i = (i + 1) % darts.size();
            }
        }

        return left;
    }

    /**
     * Returns the face that lies between the outgoing and incoming edges of v, in clockwise order. (outgoing edges -> leftFace -> incoming edges)
     * @param v
     * @return
     */
    private Face getRightFace(EmbeddedVertex v) {
        List<HalfEdge> darts = v.getEdges();

        // Find the outgoing edges. After this loop, the index will point to the first outgoing edge in the list (not necessarily the first outgoing edge around the vertex)
        boolean foundOutgoing = false;
        int i = 0;

        while (!foundOutgoing && i < darts.size()) {
            if (darts.get(i).isEdgeDirection()) {
                foundOutgoing = true;
            } else {
                i++;
            }
        }

        if (i == darts.size()) {
            System.out.println("Error!");
        }

        // Find the first incoming edge around the vertex in clockwise order: it is the first incoming edge after all outgoing edges
        boolean foundRight = false;
        Face right = null;

        while (!foundRight) {
            if (!darts.get(i).isEdgeDirection()) {
                // the right face is the face assigned to the outgoing dart that corresponds to the first incoming edge around this vertex
                right = darts.get(i).getFace();
                foundRight = true;
            } else {
                i = (i + 1) % darts.size();
            }
        }

        return right;
    }

    class Rectangle {

        int xLeft, xRight, yLow, yHigh;

        public Rectangle(int xLeft, int xRight, int yLow, int yHigh) {
            this.xLeft = xLeft;
            this.xRight = xRight;
            this.yLow = yLow;
            this.yHigh = yHigh;
        }
    }
}
