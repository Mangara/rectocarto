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

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import rectangularcartogram.data.Pair;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.remembergraph.RememberGraph;
import rectangularcartogram.data.remembergraph.RememberVertex;
import rectangularcartogram.data.remembergraph.RememberVertexEntry;
import rectangularcartogram.exceptions.IntersectingEdgesException;
import rectangularcartogram.exceptions.LowDegreeVertexException;
import rectangularcartogram.exceptions.SeparatingTriangleException;

public class GraphChecker {

    public static void checkGraph(Graph g) throws IncorrectGraphException {
        checkDegree(g);
        checkPlanarity(g);
        check4Connectivity(g);
    }

    public static boolean markAllIntersections(Graph g) {
        boolean planar = true;

        // Clear all edge labels
        for (Edge edge : g.getEdges()) {
            g.setLabel(edge, Graph.Labeling.NONE);
        }

        for (int i = 0; i < g.getEdges().size(); i++) {
            for (int j = i + 1; j < g.getEdges().size(); j++) {
                if (edgesIntersect(g.getEdges().get(i), g.getEdges().get(j))) {
                    planar = false;
                    g.setLabel(g.getEdges().get(i), Graph.Labeling.RED);
                    g.setLabel(g.getEdges().get(j), Graph.Labeling.RED);
                }
            }
        }

        return planar;
    }

    public static Vertex findLowDegreeVertex(Graph g) {
        for (Vertex v : g.getVertices()) {
            if (v.getDegree() < 4 && !g.getExteriorVertices().contains(v)) {
                return v;
            }
        }

        return null;
    }

    /**
     * All interior vertices should have degree at least four
     * @throws IncorrectGraphException
     */
    private static void checkDegree(Graph g) throws IncorrectGraphException {
        for (Vertex v : g.getVertices()) {
            if (v.getDegree() < 4 && !g.getExteriorVertices().contains(v)) {
                throw new LowDegreeVertexException(v);
            }
        }
    }

    private static void checkPlanarity(Graph g) throws IncorrectGraphException {
        for (int i = 0; i < g.getEdges().size(); i++) {
            for (int j = i + 1; j < g.getEdges().size(); j++) {
                if (edgesIntersect(g.getEdges().get(i), g.getEdges().get(j))) {
                    throw new IntersectingEdgesException(g.getEdges().get(i), g.getEdges().get(j));
                }
            }
        }
    }

    private static boolean edgesIntersect(Edge edge1, Edge edge2) {
        if (edge1.getVA() == edge2.getVA() || edge1.getVA() == edge2.getVB()
                || edge1.getVB() == edge2.getVA() || edge1.getVB() == edge2.getVB()) {
            return false;
        } else {
            Line2D line1 = new java.awt.geom.Line2D.Double(edge1.getVA().getX(), edge1.getVA().getY(), edge1.getVB().getX(), edge1.getVB().getY());
            Line2D line2 = new java.awt.geom.Line2D.Double(edge2.getVA().getX(), edge2.getVA().getY(), edge2.getVB().getX(), edge2.getVB().getY());

            return line1.intersectsLine(line2);
        }
    }

    private static void check4Connectivity(Graph g) throws IncorrectGraphException {
        List<List<Edge>> separatingTriangles = listSeparatingTriangles(g);

        if (!separatingTriangles.isEmpty()) {
            throw new SeparatingTriangleException(separatingTriangles);
        }
    }

    public static List<List<Edge>> listSeparatingTriangles(Graph graph) {
        // Convert the given graph into a remembergraph
        Pair<RememberGraph, Map<Vertex, RememberVertex>> pair = RememberGraph.fromGraph(graph);
        RememberGraph rg = pair.getFirst();

        // Reverse the returned mapping
        HashMap<RememberVertex, Vertex> vMap = new HashMap<RememberVertex, Vertex>(2 * graph.getVertices().size());

        for (Map.Entry<Vertex, RememberVertex> entry : pair.getSecond().entrySet()) {
            vMap.put(entry.getValue(), entry.getKey());
        }

        // Make sure the edges are in clockwise order
        rg.sortEdgesClockwise();

        // 5-orient the remembergraph
        orient(rg);

        // Find all separating triangles in the remembergraph
        List<List<RememberVertex>> rgTriangles = listSeparatingTriangles(rg);

        // Convert these into the separating triangles in the original graph
        List<List<Vertex>> triangles = new ArrayList<List<Vertex>>(rgTriangles.size());

        for (List<RememberVertex> triangle : rgTriangles) {
            List<Vertex> graphTriangle = new ArrayList<Vertex>(3);

            graphTriangle.add(vMap.get(triangle.get(0)));
            graphTriangle.add(vMap.get(triangle.get(1)));
            graphTriangle.add(vMap.get(triangle.get(2)));

            triangles.add(graphTriangle);
        }

        // Get the edges instead of the vertices
        List<List<Edge>> triangleEdges = new ArrayList<List<Edge>>(triangles.size());

        for (List<Vertex> triangle : triangles) {
            List<Edge> edges = new ArrayList<Edge>(3);

            // v0 - v1 and v0 - v2
            for (Edge edge : triangle.get(0).getEdges()) {
                if (edge.getVA() == triangle.get(1) || edge.getVB() == triangle.get(1) ||
                        edge.getVA() == triangle.get(2) || edge.getVB() == triangle.get(2)) {
                    edges.add(edge);
                }
            }

            // v1 - v2
            for (Edge edge : triangle.get(1).getEdges()) {
                if (edge.getVA() == triangle.get(2) || edge.getVB() == triangle.get(2)) {
                    edges.add(edge);
                }
            }

            triangleEdges.add(edges);
        }

        return triangleEdges;
    }

    /**
     * Pre-condition: g is planar, all edges in g are undirected (maybe not necessary?)
     * Replaces each edge of g by a directed edge such that each vertex has outdegree at most 5.
     * @param g
     * @return
     */
    private static void orient(RememberGraph g) {
        // Add all vertices with degree 5 or less to the queue
        Queue<RememberVertex> q = new LinkedList<RememberVertex>();

        for (RememberVertex v : g.getVertices()) {
            if (v.getDegree() <= 5) {
                q.add(v);
            }
        }

        while (!q.isEmpty()) {
            RememberVertex v = q.remove();

            for (RememberVertex vn : v.getNeighbours()) {
                // vn's degree will decrease by 1, so if it's 6 now, we should add it to our queue
                if (vn.getDegree() == 6) {
                    q.add(vn);
                }
            }

            // Remove the incoming edges of the processed vertex
            g.directEdgesOutward(v);
        }
    }

    private static List<List<RememberVertex>> listSeparatingTriangles(RememberGraph graph) {
        List<List<RememberVertex>> triangles = new ArrayList<List<RememberVertex>>();

        for (RememberVertex v : graph.getVertices()) {
            List<RememberVertexEntry> neighbours = v.getNeighbourEntries();

            for (int i = 0; i < neighbours.size(); i++) {
                for (int j = i + 1; j < neighbours.size(); j++) {
                    RememberVertexEntry neighbour1 = neighbours.get(i);
                    RememberVertexEntry neighbour2 = neighbours.get(j);

                    // Check if this triangle is separating
                    boolean stuffInside = (j > i + 1) || neighbour1.hasEdgesRemoved();
                    boolean stuffOutside = !(i == 0 && j == v.getDegree() - 1) || neighbour2.hasEdgesRemoved();

                    if (stuffInside && stuffOutside && graph.containsEdge(neighbour1.getNeighbour(), neighbour2.getNeighbour())) {
                        List<RememberVertex> triangle = new ArrayList<RememberVertex>(3);

                        triangle.add(v);
                        triangle.add(neighbour1.getNeighbour());
                        triangle.add(neighbour2.getNeighbour());

                        triangles.add(triangle);
                    }
                }
            }
        }

        return triangles;
    }

    private GraphChecker() {
    }
}
