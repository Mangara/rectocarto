/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectocarto.algos.lp;

import java.util.HashMap;
import java.util.LinkedList;
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
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

class FeasibleSolutionBuilder {

    static Solution constructFeasibleSolution(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

        // toplogical sort on dual of st-graphs, incrementing by minimumSeparation or minimumSeaDimension
        
        
        
        scaleToCartogramSize(sub, settings, segments, variables);
        computeError(variables);

        return new Solution(problem.getObjective().evaluate(variables), variables);
    }

    private void constructStGraphs(Graph graph) {
        RegularEdgeLabeling labeling = graph.getRegularEdgeLabeling();

        // Construct the blue and red st-graphs
        Graph red = new Graph();
        Graph blue = new Graph();

        Map<Vertex, Vertex> originalToRed = new HashMap<Vertex, Vertex>(2 * graph.getVertices().size());
        Map<Vertex, Vertex> originalToBlue = new HashMap<Vertex, Vertex>(2 * graph.getVertices().size());

        for (Vertex v : graph.getVertices()) {
            Vertex blueVertex = new Vertex(v.getX(), v.getY());
            Vertex redVertex = new Vertex(v.getX(), v.getY());

            red.addVertex(blueVertex);
            blue.addVertex(redVertex);

            originalToRed.put(v, blueVertex);
            originalToBlue.put(v, redVertex);
        }

        for (Edge e : graph.getEdges()) {
            if (labeling.get(e).getFirst() == Graph.Labeling.RED) {
                Edge newEdge = red.addEdge(originalToRed.get(e.getVA()), originalToRed.get(e.getVB()));
                newEdge.setDirection(e.getDirection());
            } else if (labeling.get(e).getFirst() == Graph.Labeling.BLUE) {
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

        EmbeddedGraph redDCEL = new EmbeddedGraph(red);
        Map<Vertex, EmbeddedVertex> redToDCEL = redDCEL.getVertexMap();
        EmbeddedGraph blueDCEL = new EmbeddedGraph(blue);
        Map<Vertex, EmbeddedVertex> blueToDCEL = blueDCEL.getVertexMap();
    }

    private void computeFaceNumbering(EmbeddedGraph redDCEL, EmbeddedGraph blueDCEL) {
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

        redDualNumbering = computeLongestPathFromSource(redDual);
        blueDualNumbering = computeLongestPathFromSource(blueDual);

        // Translate it to face numberings
        Map<Face, Integer> redFaceNumbering = new HashMap<Face, Integer>(2 * redDCEL.getFaces().size());
        Map<Face, Integer> blueFaceNumbering = new HashMap<Face, Integer>(2 * blueDCEL.getFaces().size());

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

    public Pair<Graph, HashMap<Face, Vertex>> constructDirectedDualGraph(EmbeddedGraph g) {
        Pair<Graph, HashMap<Face, Vertex>> dualPair = g.getDualGraph();
        Graph dual = dualPair.getFirst();
        HashMap<Face, Vertex> faceMap = dualPair.getSecond();

        for (HalfEdge dart : g.getDarts()) {
            if (dart.isDirected() && dart.isEdgeDirection()) {
                // Find the edge corresponding to this adjacency
                Edge edge = null;

                for (Edge e : faceMap.get(dart.getFace()).getEdges()) {
                    if ((e.getVA() == faceMap.get(dart.getFace()) && e.getVB() == faceMap.get(dart.getTwin().getFace()))
                            || (e.getVB() == faceMap.get(dart.getFace()) && e.getVA() == faceMap.get(dart.getTwin().getFace()))) {
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
     *
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

    private static void scaleToCartogramSize(Subdivision sub, CartogramSettings settings, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, Map<String, Double> variables) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                SegmentIdentification.FaceSegments s = segments.get(f);
                minX = Math.min(minX, variables.get(s.left.name));
                maxX = Math.max(maxX, variables.get(s.right.name));
                minY = Math.min(minY, variables.get(s.bottom.name));
                maxY = Math.max(maxY, variables.get(s.top.name));
            }
        }

        if (minX != settings.boundaryWidth || minY != settings.boundaryWidth) {
            throw new InternalError("Minimum dimensions not equal to boundary width.");
        }

        if (maxX > settings.cartogramWidth - settings.boundaryWidth || maxY > settings.cartogramHeight - settings.boundaryWidth) {
            throw new IllegalArgumentException("No cartogram can be constructed with these settings. Either increase the cartogram width and height or decrease the minimum separation.");
        }

        double xScale = (settings.cartogramWidth - 2 * settings.boundaryWidth) / (maxX - minX);
        double yScale = (settings.cartogramHeight - 2 * settings.boundaryWidth) / (maxY - minY);

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                SegmentIdentification.FaceSegments s = segments.get(f);
                variables.compute(s.left.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.right.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.bottom.name, (key, val) -> settings.boundaryWidth + yScale * val);
                variables.compute(s.top.name, (key, val) -> settings.boundaryWidth + yScale * val);
            }
        }
    }

    private FeasibleSolutionBuilder() {
    }
}
