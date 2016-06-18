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
package rectangularcartogram.data.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class CycleGraph extends Graph {

    /**
     * A list of all 4-cycles in the graph.
     * The ordering is lexicographically on the first and third edge.
     * The edges are ordered lexicographically on their smaller endpoint and larger endpoint.
     * The vertices are ordered lexicographically by their x- and y-coordinate.
     */
    private List<Edge[]> fourCycles;

    public CycleGraph(Graph graph) {
        super(graph);
        computeFourCycles();
    }

    public List<Edge[]> getFourCycles() {
        return fourCycles;
    }

    /**
     * Returns all edges inside the given 4-cycle.
     * @param fourCycle
     * @return A Set containing all edges inside the given 4-cycle.
     */
    public Set<Edge> getEdgesInside(Edge[] fourCycle) {
        List<Edge> cornerEdges = getEdgesBetween(fourCycle[0], fourCycle[1]);
        List<Edge> cornerEdges2 = getEdgesBetween(fourCycle[1], fourCycle[2]);

        if (cornerEdges.isEmpty()) {
            // There is only one edge inside this 4-cycle and it runs between the two neighbours of this corner
            return new HashSet<Edge>(cornerEdges2);
        } else if (cornerEdges2.isEmpty()) {
            // There is only one edge inside this 4-cycle and it runs between the this corner and the opposite one
            return new HashSet<Edge>(cornerEdges);
        } else {
            // Do a DFS, starting from the cycle, staying inside the cycle.
            // Mark the cycle vertices as visited
            HashSet<Vertex> visited = new HashSet<Vertex>();
            visited.add(fourCycle[0].getVA());
            visited.add(fourCycle[0].getVB());
            visited.add(fourCycle[2].getVA());
            visited.add(fourCycle[2].getVB());

            // Populate the frontier with the first edge inside the cycle
            Stack<Vertex> frontier = new Stack<Vertex>();

            if (visited.contains(cornerEdges.get(0).getVA())) {
                frontier.add(cornerEdges.get(0).getVB());
            } else {
                frontier.add(cornerEdges.get(0).getVA());
            }

            // Collect the edges in a set to avoid duplicates (might not be necessary)
            HashSet<Edge> edgesInside = new HashSet<Edge>(cornerEdges);
            edgesInside.addAll(cornerEdges2);
            edgesInside.addAll(getEdgesBetween(fourCycle[2], fourCycle[3]));
            edgesInside.addAll(getEdgesBetween(fourCycle[3], fourCycle[0]));

            while (!frontier.isEmpty()) {
                Vertex currentVertex = frontier.pop();

                // Add all neighbours that aren't visited yet to the frontier if that hasn't been done already
                List<Edge> edges = currentVertex.getEdges();

                for (Edge edge : edges) {
                    Vertex neighbour = (edge.getVA() == currentVertex ? edge.getVB() : edge.getVA());

                    // Visited vertices have already added all their edges to the list
                    if (!visited.contains(neighbour)) {
                        edgesInside.add(edge);

                        if (!frontier.contains(neighbour)) {
                            frontier.add(neighbour);
                        }
                    }
                }

                // Mark this vertex as visited
                visited.add(currentVertex);
            }

            return edgesInside;
        }
    }

    /**
     * Returns a list of edges between the two given edges in the counter-clockwise order around their shared vertex.
     * @param edge
     * @param next
     * @return
     */
    public List<Edge> getEdgesBetween(Edge edge, Edge next) {
        // Find the shared vertex
        Vertex v = (edge.getVA() == next.getVA() || edge.getVA() == next.getVB() ? edge.getVA() : edge.getVB());

        List<Edge> edges = v.getEdges(); // Already sorted in clockwise order

        int edgeIndex = edges.indexOf(edge);
        int nextIndex = edges.indexOf(next);

        // Start at nextIndex + 1, keep going until we get back to edgeIndex
        List<Edge> edgesBetween = new ArrayList<Edge>(edges.size());
        int index = (nextIndex + 1) % edges.size();

        while (index != edgeIndex) {
            edgesBetween.add(edges.get(index));

            index = (index + 1) % edges.size();
        }

        return edgesBetween;
    }

    private void computeFourCycles() {
        fourCycles = new ArrayList<Edge[]>();

        // Sort the vertices lexicographically by x- and y-coordinate
        List<Vertex> vertices = new ArrayList<Vertex>(getVertices());
        Collections.sort(vertices, Vertex.increasingX);

        // Find out the rank of each vertex
        final HashMap<Vertex, Integer> vertexRank = new HashMap<Vertex, Integer>(2 * vertices.size());

        for (int i = 0; i < vertices.size(); i++) {
            vertexRank.put(vertices.get(i), i);
        }

        // Sort all the edges lexicographically
        List<Edge> edges = new ArrayList<Edge>(getEdges());
        Collections.sort(edges, new Comparator<Edge>() {

            public int compare(Edge e1, Edge e2) {
                int a1 = vertexRank.get(e1.getVA());
                int b1 = vertexRank.get(e1.getVB());
                int a2 = vertexRank.get(e2.getVA());
                int b2 = vertexRank.get(e2.getVB());

                int low1 = Math.min(a1, b1);
                int high1 = Math.max(a1, b1);
                int low2 = Math.min(a2, b2);
                int high2 = Math.max(a2, b2);

                int compLow = (new Integer(low1)).compareTo(low2);

                if (compLow == 0) {
                    return (new Integer(high1)).compareTo(high2);
                } else {
                    return compLow;
                }
            }
        });

        // Build all 4-cycles by choosing two edges and checking if they form a 4-cycle in which they are not adjacent
        for (int i = 0; i < edges.size() - 1; i++) {
            Edge edgeA = edges.get(i);

            // This edge can never form an alternating 4-cycle if it is adjacent to one of the exterior vertices
            if (!getExteriorVertices().contains(edgeA.getVA()) && !getExteriorVertices().contains(edgeA.getVB())) {
                for (int j = i + 1; j < edges.size(); j++) {
                    // Check if edge i and j define a valid 4-cycle
                    Edge edgeC = edges.get(j);

                    // This edge can never form an alternating 4-cycle if it is adjacent to one of the exterior vertices
                    if (!getExteriorVertices().contains(edgeC.getVA()) && !getExteriorVertices().contains(edgeC.getVB())) {
                        // Check if the two edges share a vertex: in that case they don't define a valid 4-cycle
                        if (edgeA.getVA() != edgeC.getVA() && edgeA.getVA() != edgeC.getVB()
                                && edgeA.getVB() != edgeC.getVA() && edgeA.getVB() != edgeC.getVB()) {
                            Edge aa = getEdgeBetween(edgeA.getVA(), edgeC.getVA());

                            if (aa != null) {
                                Edge bb = getEdgeBetween(edgeA.getVB(), edgeC.getVB());

                                if (bb != null) {
                                    // This is a 4-cycle
                                    // Check if this cycle or its 'twin' should be saved
                                    if (Math.min(edges.indexOf(edgeA), edges.indexOf(edgeC)) < Math.min(edges.indexOf(aa), edges.indexOf(bb))) {
                                        fourCycles.add(getFourCycle(edgeA, aa, edgeC, bb));
                                    }

                                    continue; // This pair of edges can only define one kind of cycle, move to the next edge
                                }
                            }

                            Edge ab = getEdgeBetween(edgeA.getVA(), edgeC.getVB());

                            if (ab != null) {
                                Edge ba = getEdgeBetween(edgeA.getVB(), edgeC.getVA());

                                if (ba != null) {
                                    // This is a 4-cycle
                                    // Check if this cycle or its 'twin' should be saved
                                    if (Math.min(edges.indexOf(edgeA), edges.indexOf(edgeC)) < Math.min(edges.indexOf(ab), edges.indexOf(ba))) {
                                        fourCycles.add(getFourCycle(edgeA, ab, edgeC, ba));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Edge getEdgeBetween(Vertex a, Vertex b) {
        // v1 is the vertex with the smallest degree
        Vertex v1, v2;

        if (a.getDegree() < b.getDegree()) {
            v1 = a;
            v2 = b;
        } else {
            v1 = b;
            v2 = a;
        }

        for (Edge edge : v1.getEdges()) {
            // If this edge is adjacent to v2 as well, it's the edge we want
            if (edge.getVA() == v2 || edge.getVB() == v2) {
                return edge;
            }
        }

        return null;
    }

    private Edge[] getFourCycle(Edge edgeA, Edge edgeB, Edge edgeC, Edge edgeD) {
        if (isClockwise(edgeA, edgeB, edgeC, edgeD)) {
            return new Edge[]{edgeA, edgeB, edgeC, edgeD};
        } else {
            return new Edge[]{edgeA, edgeD, edgeC, edgeB};
        }
    }

    /**
     * Returns true if the specified 4-cycle is clockwise and false otherwise.
     * @param edgeA
     * @param edgeB
     * @param edgeC
     * @param edgeD
     * @return
     */
    private boolean isClockwise(Edge edgeA, Edge edgeB, Edge edgeC, Edge edgeD) {
        // Determine the area of the polygon defined by the cycle. If the sign is negative then the cycle is ordered clockwise, otherwise the cycle is ordered counter-clockwise.

        // v1 - edgeA - v2 - edgeB - v3 - edgeC - v4 - edgeD - v1

        boolean v2IsA = (edgeB.getVA() == edgeA.getVA() || edgeB.getVB() == edgeA.getVA()); // whether the common vertex of edgeA and edgeB is the first vertex of edgeA

        Vertex v1 = (v2IsA ? edgeA.getVB() : edgeA.getVA());
        Vertex v2 = (v2IsA ? edgeA.getVA() : edgeA.getVB());
        Vertex v3 = (edgeB.getVA() == v2 ? edgeB.getVB() : edgeB.getVA());
        Vertex v4 = (edgeC.getVA() == v3 ? edgeC.getVB() : edgeC.getVA());

        assert ((edgeA.getVA() == v1 && edgeA.getVB() == v2) || (edgeA.getVA() == v2 && edgeA.getVB() == v1));
        assert ((edgeB.getVA() == v2 && edgeB.getVB() == v3) || (edgeB.getVA() == v3 && edgeB.getVB() == v2));
        assert ((edgeC.getVA() == v3 && edgeC.getVB() == v4) || (edgeC.getVA() == v4 && edgeC.getVB() == v3));
        assert ((edgeD.getVA() == v4 && edgeD.getVB() == v1) || (edgeD.getVA() == v1 && edgeD.getVB() == v4));

        double area = 0;
        area += v1.getX() * v2.getY() - v2.getX() * v1.getY();
        area += v2.getX() * v3.getY() - v3.getX() * v2.getY();
        area += v3.getX() * v4.getY() - v4.getX() * v3.getY();
        area += v4.getX() * v1.getY() - v1.getX() * v4.getY();
        // area /= 2; (we're only interested in the sign)

        return area < 0;
    }
}
