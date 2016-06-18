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
package rectangularcartogram.data.remembergraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;

public class RememberGraph {

    private HashSet<RememberVertex> vertices;

    /**
     * Creates an empty RememberGraph.
     * Runs in O(1) time.
     */
    public RememberGraph() {
        vertices = new HashSet<RememberVertex>();
    }

    /**
     * Adds the given vertex to the graph, with no adjacent edges.
     * Runs in O(1) expected time.
     * @param v
     */
    public void addVertex(RememberVertex v) {
        vertices.add(v);
    }

    /**
     * Adds an undirected edge between the given pair of vertices. The edge will be present in both adjacency lists.
     * Runs in O(1) time.
     * @param a
     * @param b
     */
    public void addEdge(RememberVertex a, RememberVertex b) {
        addEdge(a, b, false);
    }

    /**
     * Adds an edge between the given pair of vertices.
     * If the edge is directed, it will be present only in the adjacency list of a, otherwise it will be present in both adjacency lists.
     * Runs in O(1) time.
     * @param a
     * @param b
     */
    public void addEdge(RememberVertex a, RememberVertex b, boolean directed) {
        if (directed) {
            a.addNeighbour(b);
        } else {
            RememberVertexEntry ea = a.addNeighbour(b);
            RememberVertexEntry eb = b.addNeighbour(a);

            ea.twin = eb;
            eb.twin = ea;
        }
    }

    /**
     * Returns true if there is an edge between a and b in either direction, false otherwise.
     * Runs in O(degree of a + degree of b) time.
     * @param a
     * @param b
     * @return
     */
    public boolean containsEdge(RememberVertex a, RememberVertex b) {
        boolean neighbours = a.isAdjacentTo(b);

        if (!neighbours) {
            neighbours = b.isAdjacentTo(a);
        }

        return neighbours;
    }

    /**
     * Sorts the adjacency list of each vertex so that its edges appear in clockwise order around the vertex.
     * Runs in O(d log d) time per vertex, where d is the degree.
     */
    public void sortEdgesClockwise() {
        for (RememberVertex v : vertices) {
            v.sortEdges(new ClockwiseNeighbourOrder(v));
        }
    }

    /**
     * Returns a collection of all vertices in this graph. Runs in O(1) time.
     * @return
     */
    public Collection<RememberVertex> getVertices() {
        return vertices;
    }

    /**
     * Removes the given vertex and all its edges from the graph. Directed incoming edges will not be removed
     * Runs in O(|adjacency list of v|) time.
     * @param v
     */
    public void removeVertex(RememberVertex v) {
        // Remove all edges to v
        directEdgesOutward(v);
        vertices.remove(v);
    }

    /**
     * Makes all undirected edges of this vertex outgoing by removing the corresponding entries from the adjacency lists of its neighbours.
     * Runs in O(|adjacency list of v|) time.
     * @param v
     */
    public void directEdgesOutward(RememberVertex v) {
        RememberVertexEntry e = v.head;

        while (e != null) {
            if (e.twin != null) {
                e.twin.myVertex.degree--;
                e.twin.remove();
            } else {
                System.err.println("Twin was null");
            }

            e = e.next;
        }
    }

    /**
     * Converts this RememberGraph into a Graph.
     * Runs in O(n + m) expected time, where n and m are the number of vertices and edges in this RememberGraph.
     * @return
     */
    public Graph toGraph() {
        Graph g = new Graph();
        HashMap<RememberVertex, Vertex> vMap = new HashMap<RememberVertex, Vertex>(2 * vertices.size());

        for (RememberVertex v : vertices) {
            Vertex gv = new Vertex(v.getX(), v.getY());
            vMap.put(v, gv);
            g.addVertex(gv);
        }

        HashSet<RememberVertex> processed = new HashSet<RememberVertex>(2 * vertices.size());

        for (RememberVertex v : vertices) {
            Vertex gv = vMap.get(v);
            RememberVertexEntry e = v.head;

            while (e != null) {
                Vertex nv = vMap.get(e.neighbour);

                if (e.twin == null) {
                    g.addEdge(gv, nv);
                } else {
                    if (!processed.contains(e.neighbour)) {
                        g.addEdge(gv, nv);
                    }
                }

                e = e.next;
            }

            processed.add(v);
        }

        return g;
    }

    /**
     * Converts the given Graph into a RememberGraph and returns the constructed RememberGraph and a mapping of vertices of the original Graph to the RememberGraph.\
     * Runs in O(n + m) expected time, where n and m are the number of vertices and edges in the given Graph.
     * @param g
     * @return
     */
    public static Pair<RememberGraph, Map<Vertex, RememberVertex>> fromGraph(Graph g) {
        RememberGraph rg = new RememberGraph();

        HashMap<Vertex, RememberVertex> vMap = new HashMap<Vertex, RememberVertex>(2 * g.getVertices().size());

        for (Vertex v : g.getVertices()) {
            RememberVertex rv = new RememberVertex(v.getX(), v.getY());
            vMap.put(v, rv);
            rg.addVertex(rv);
        }

        for (Edge e : g.getEdges()) {
            RememberVertex a = vMap.get(e.getVA());
            RememberVertex b = vMap.get(e.getVB());
            rg.addEdge(a, b);
        }

        return new Pair<RememberGraph, Map<Vertex, RememberVertex>>(rg, vMap);
    }
}
