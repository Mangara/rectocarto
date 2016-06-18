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
package rectangularcartogram.data.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.embedded.EmbeddedGraph;
import rectangularcartogram.data.graph.Edge.Direction;

public class Graph {

    public static enum Labeling {

        RED, BLUE, PATH, NONE;
    }
    private static final String NEWLINE = "\n";
    private ArrayList<Vertex> vertices;
    private ArrayList<Edge> edges;
    private RegularEdgeLabeling regularEdgeLabeling;
    private ArrayList<Vertex> exteriorVertices; // [vN, vE, vS, vW]

    public Graph() {
        vertices = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();
        regularEdgeLabeling = null;
    }

    public Graph(Graph graph) {
        vertices = new ArrayList<Vertex>(graph.getVertices());
        edges = new ArrayList<Edge>(graph.getEdges());
        regularEdgeLabeling = null;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public RegularEdgeLabeling getRegularEdgeLabeling() {
        return regularEdgeLabeling;
    }

    public void setRegularEdgeLabeling(RegularEdgeLabeling regularEdgeLabeling) {
        if (regularEdgeLabeling == null) {
            this.regularEdgeLabeling = null;
        } else {
            this.regularEdgeLabeling = new RegularEdgeLabeling(regularEdgeLabeling);

            // TODO: remove direction from Edge entirely and remove all dependencies on it
            for (Entry<Edge, Pair<Labeling, Direction>> entry : regularEdgeLabeling.entrySet()) {
                entry.getKey().setDirection(entry.getValue().getSecond());
            }
        }
    }

    public Labeling getEdgeLabel(Edge e) {
        if (regularEdgeLabeling == null || !regularEdgeLabeling.containsKey(e)) {
            return Labeling.NONE;
        } else {
            Labeling label = regularEdgeLabeling.get(e).getFirst();

            if (label == null) {
                return Labeling.NONE;
            } else {
                return label;
            }
        }
    }

    public void setLabel(Edge e, Labeling label) {
        if (regularEdgeLabeling == null) {
            regularEdgeLabeling = new RegularEdgeLabeling(new CycleGraph(this));
        }

        Pair<Labeling, Direction> pair = regularEdgeLabeling.get(e);

        if (pair == null) {
            regularEdgeLabeling.put(e, new Pair<Labeling, Direction>(label, Direction.NONE));
        } else {
            pair.setFirst(label);
        }
    }

    public void addVertex(final Vertex v) {
        if (v == null) {
            throw new NullPointerException();
        }

        if (!containsVertex(v)) {
            vertices.add(v);
        }
    }

    public boolean containsVertex(Vertex v) {
        for (Vertex vertex : vertices) {
            if (vertex.getX() == v.getX() && vertex.getY() == v.getY()) {
                return true;
            }
        }

        return false;
    }

    public Edge addEdge(final Vertex vA, final Vertex vB) {
        if (vA == null || vB == null) {
            throw new NullPointerException();
        }

        if (vA != vB) {
            Edge e = new Edge(vA, vB);

            if (!containsEdge(e)) {
                vA.addEdge(e);
                vB.addEdge(e);
                edges.add(e);

                return e;
            }
        }

        return null;
    }

    public boolean containsEdge(Edge e) {
        for (Edge edge : edges) {
            if ((edge.getVA() == e.getVA() && edge.getVB() == e.getVB())
                    || (edge.getVB() == e.getVA() && edge.getVA() == e.getVB())) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Vertex> getExteriorVertices() {
        if (exteriorVertices == null) {
            computeExteriorVertices();
        }
        return exteriorVertices;
    }

    public Vertex getVN() {
        if (exteriorVertices == null) {
            computeExteriorVertices();
        }

        if (exteriorVertices != null) {
            return exteriorVertices.get(0);
        } else {
            return null;
        }
    }

    public Vertex getVE() {
        if (exteriorVertices == null) {
            computeExteriorVertices();
        }

        if (exteriorVertices != null) {
            return exteriorVertices.get(1);
        } else {
            return null;
        }
    }

    public Vertex getVS() {
        if (exteriorVertices == null) {
            computeExteriorVertices();
        }

        if (exteriorVertices != null) {
            return exteriorVertices.get(2);
        } else {
            return null;
        }
    }

    public Vertex getVW() {
        if (exteriorVertices == null) {
            computeExteriorVertices();
        }

        if (exteriorVertices != null) {
            return exteriorVertices.get(3);
        } else {
            return null;
        }
    }

    public void setExteriorVertices(ArrayList<Vertex> exteriorVertices) {
        this.exteriorVertices = exteriorVertices;
    }

    public Vertex getVertexAt(final double x, final double y, final double precision) {
        for (Vertex v : vertices) {
            if (v.isNear(x, y, precision)) {
                return v;
            }
        }
        return null;
    }

    public Edge getEdgeAt(final double x, final double y, final double precision) {
        for (Edge e : edges) {
            if (e.isNear(x, y, precision)) {
                return e;
            }
        }
        return null;
    }

    public void removeVertex(final Vertex v) {
        for (Edge e : v.getEdges()) {
            if (e.getVA() != v) {
                e.getVA().removeEdge(e);
            }

            if (e.getVB() != v) {
                e.getVB().removeEdge(e);
            }

            edges.remove(e);

            if (regularEdgeLabeling != null) {
                regularEdgeLabeling.remove(e);
            }
        }

        vertices.remove(v);
    }

    public void removeEdge(final Edge e) {
        edges.remove(e);
        e.getVA().removeEdge(e);
        e.getVB().removeEdge(e);

        if (regularEdgeLabeling != null) {
            regularEdgeLabeling.remove(e);
        }
    }

    public Graph getDualGraph() {
        EmbeddedGraph dcel = new EmbeddedGraph(this);
        return dcel.getDualGraph().getFirst();
    }

    private void computeExteriorVertices() {
        if (vertices != null && !vertices.isEmpty()) {
            // compute the exterior vertices
            exteriorVertices = new ArrayList<Vertex>(4);
            exteriorVertices.add(Collections.max(vertices, Vertex.increasingY)); // North
            exteriorVertices.add(Collections.max(vertices, Vertex.increasingX)); // East
            exteriorVertices.add(Collections.min(vertices, Vertex.increasingY)); // South
            exteriorVertices.add(Collections.min(vertices, Vertex.increasingX)); // West
        }
    }

    public String toSaveString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Vertices");
        buffer.append(NEWLINE);

        buffer.append(vertices.size());
        buffer.append(NEWLINE);

        for (Vertex vertex : vertices) {
            buffer.append(vertex.toSaveString());
            buffer.append(NEWLINE);
        }

        buffer.append(NEWLINE);
        buffer.append("Edges");
        buffer.append(NEWLINE);

        buffer.append(edges.size());
        buffer.append(NEWLINE);

        for (Edge edge : edges) {
            // print the indices of the endpoints of this edge
            buffer.append(vertices.indexOf(edge.getVA()));
            buffer.append(" ");
            buffer.append(vertices.indexOf(edge.getVB()));
            buffer.append(NEWLINE);
        }

        return buffer.toString();
    }

    public static Graph fromSaveString(String s) throws IOException {
        String[] lines = s.split("\n");
        Graph result = new Graph();

        // Read until the first line "Vertices"
        int i = 0;

        while (i < lines.length && !"Vertices".equals(lines[i])) {
            i++;
        }

        if (i == lines.length) {
            throw new IOException("Incorrect file format");
        }

        // Read the number of vertices
        i++;
        int nVertices = Integer.parseInt(lines[i]);

        ArrayList<Vertex> vertices = new ArrayList<Vertex>(nVertices);

        // Read the vertices
        i++;
        while (i < lines.length && !"Edges".equals(lines[i])) {
            String line = lines[i].trim();

            // Skip blank lines
            if (line.length() > 0) {
                Vertex v = Vertex.fromSaveString(lines[i]);

                if (v != null) {
                    vertices.add(v);
                    result.addVertex(v);
                }
            }

            i++;
        }

        if (i == lines.length) {
            throw new IOException("Incorrect file format");
        }

        // Read the number of edges
        i++;
        int nEdges = Integer.parseInt(lines[i]);

        // Read the edges
        i++;
        while (i < lines.length) {
            String line = lines[i].trim();

            // Skip blank lines
            if (line.length() > 0) {
                String[] parts = line.split(" ");
                int v1 = Integer.parseInt(parts[0]);
                int v2 = Integer.parseInt(parts[1]);

                result.addEdge(vertices.get(v1), vertices.get(v2));
            }

            i++;
        }

        return result;
    }

    public void save(BufferedWriter out) throws IOException {
        out.write("Vertices");
        out.newLine();

        out.write(Integer.toString(vertices.size()));
        out.newLine();

        for (Vertex vertex : vertices) {
            vertex.save(out);
        }

        out.newLine();
        out.write("Edges");
        out.newLine();

        out.write(Integer.toString(edges.size()));
        out.newLine();

        for (Edge edge : edges) {
            // print the indices of the endpoints of this edge
            out.write(Integer.toString(vertices.indexOf(edge.getVA())));
            out.write(" ");
            out.write(Integer.toString(vertices.indexOf(edge.getVB())));
            out.newLine();
        }

        out.newLine();
    }

    public static Graph load(BufferedReader in) throws IOException {
        Graph result = new Graph();

        // Read until the first line "Vertices"
        String line = in.readLine();

        while (line != null && !"Vertices".equals(line)) {
            line = in.readLine();
        }

        if (line == null) {
            throw new IOException("Incorrect file format: 'Vertices' not found.");
        }

        // Read the number of vertices
        line = in.readLine();
        int nVertices = Integer.parseInt(line);

        ArrayList<Vertex> vertices = new ArrayList<Vertex>(nVertices);

        // Read the vertices
        for (int i = 0; i < nVertices; i++) {
            Vertex v = Vertex.load(in);

            if (v != null) {
                vertices.add(v);
                result.addVertex(v);
            }
        }

        // Read until the first line "Edges"
        line = in.readLine();

        while (line != null && !"Edges".equals(line)) {
            line = in.readLine();
        }

        if (line == null) {
            throw new IOException("Incorrect file format: 'Edges' not found.");
        }

        // Read the number of edges
        line = in.readLine();
        int nEdges = Integer.parseInt(line);

        // Read the edges
        for (int i = 0; i < nEdges; i++) {
            line = in.readLine();

            if (line == null) {
                throw new IOException("Incorrect file format: Reached end of file, while expecting a vertex.");
            }

            String[] parts = line.split(" ");

            if (parts.length != 2) {
                throw new IOException("Incorrect file format: '" + line + "' is not a valid edge format.");
            }

            int v1 = Integer.parseInt(parts[0]);
            int v2 = Integer.parseInt(parts[1]);

            result.addEdge(vertices.get(v1), vertices.get(v2));
        }

        return result;
    }
}
