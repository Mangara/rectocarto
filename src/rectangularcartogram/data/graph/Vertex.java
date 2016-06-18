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
import java.util.Comparator;
import java.util.List;

public class Vertex {

    protected double x, y;
    private ArrayList<Edge> edges;
    private boolean visible;
    private ClockwiseOrder clockwise;

    public Vertex(double x, double y) {
        this(x, y, true);
    }

    public Vertex(double x, double y, boolean visible) {
        this.x = x;
        this.y = y;
        this.edges = new ArrayList<Edge>(4);
        this.visible = visible;
        this.clockwise = new ClockwiseOrder(this);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     * Returns the edges incident to this vertex, in clockwise order around it.
     * @see ClockwiseOrder
     * @return
     */
    public List<Edge> getEdges() {
        return edges;
    }

    public void addEdge(Edge e) {
        edges.add(e);

        // sort the edges in clockwise order around the vertex
        Collections.sort(edges, clockwise);
    }

    public void removeEdge(Edge e) {
        edges.remove(e);
    }

    public void removeAllEdges() {
        edges.clear();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getDegree() {
        return edges.size();
    }

    public boolean isNear(double x, double y, double precision) {
        double dX = x - this.x;
        double dY = y - this.y;
        return (dX * dX + dY * dY <= precision * precision);
    }

    public boolean isNear(Vertex v, double precision) {
        return isNear(v.x, v.y, precision);
    }

    /**
     * Returns the neighbours of this vertex in clockwise order.
     * @return
     */
    public List<Vertex> getNeighbours() {
        List<Vertex> neighbours = new ArrayList<Vertex>(edges.size());

        for (Edge edge : edges) {
            if (edge.getVA() == this) {
                neighbours.add(edge.getVB());
            } else {
                neighbours.add(edge.getVA());
            }
        }

        return neighbours;
    }

    public String toSaveString() {
        return x + " " + y;
    }

    public static Vertex fromSaveString(String s) throws IOException {
        if (s == null) {
            throw new IOException("Incorrect file format: Reached end of file, while expecting a vertex.");
        }

        String[] parts = s.split(" ");

        if (parts.length != 2) {
            throw new IOException("Incorrect file format: '" + s + "' is not a valid vertex format.");
        }

        double x = java.lang.Double.parseDouble(parts[0]);
        double y = java.lang.Double.parseDouble(parts[1]);
       
        Vertex v = new Vertex(x, y);
        
        return v;

    }

    /**
     * A lexicographic order on the (x, y) coordinate pair.
     * Vertices with smaller x coordinates come before vertices with larger x coordinates.
     * If vertices have the same x coordinate, the vertices with smaller y coordinates come before the vertices with larger y coordinates.
     */
    public static final Comparator<Vertex> increasingX = new Comparator<Vertex>() {

        public int compare(Vertex v1, Vertex v2) {
            int compX = java.lang.Double.compare(v1.getX(), v2.getX());

            if (compX != 0) {
                return compX;
            } else {
                return java.lang.Double.compare(v1.getY(), v2.getY());
            }
        }
    };

    public static final Comparator<Vertex> increasingY = new Comparator<Vertex>() {

        public int compare(Vertex v1, Vertex v2) {
            int compY = java.lang.Double.compare(v1.getY(), v2.getY());

            if (compY != 0) {
                return compY;
            } else {
                return java.lang.Double.compare(v1.getX(), v2.getX());
            }
        }
    };

    @Override
    public String toString() {
        return "V[" + x + ", " + y + "]";
    }

    public void save(BufferedWriter out) throws IOException {
        out.write(toSaveString());
        out.newLine();
    }

    public static Vertex load(BufferedReader in) throws IOException {
        return fromSaveString(in.readLine());
    }
}