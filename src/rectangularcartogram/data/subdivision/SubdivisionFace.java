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
package rectangularcartogram.data.subdivision;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import rectangularcartogram.data.graph.Vertex;

public class SubdivisionFace {

    enum CorrespondingVertexType {

        AVERAGE, CENTROID;
    }
    private List<Vertex> vertices;
    private Color color;
    private String name;
    private double weight;
    private double area;
    private Vertex correspondingVertex;
    private boolean sea;
    private List<SubdivisionFace> neighbours;
    private double cartographicError; // Signed cartographic error of this region. Only non-zero if this region is part of a rectangular cartogram. Set by CartogramMaker.
    // Constants
    public static final Color SEA_COLOR = new Color(154, 181, 219);
    public static final double SEA_WEIGHT = 1;
    public static final Color BOUNDARY_COLOR = new Color(160, 160, 160);
    private static final CorrespondingVertexType correspondingVertexType = CorrespondingVertexType.CENTROID;

    public SubdivisionFace(List<Vertex> vertices, Color color, String name, double weight, boolean sea, boolean boundary) {
        this.vertices = vertices;
        this.color = color;
        this.name = name;
        this.weight = weight;
        this.sea = sea;

        if (sea && !boundary) {
            this.color = SEA_COLOR;
            this.weight = SEA_WEIGHT;
        }

        updateCorrespondingVertex();
    }

    public SubdivisionFace(ArrayList<Vertex> vertices) {
        this(vertices, Color.white, "Face", 1, false, false);
    }

    public SubdivisionFace() {
        this(new ArrayList<Vertex>(), Color.white, "Face", 1, false, false);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isSea() {
        return sea;
    }

    public void setSea(boolean sea) {
        this.sea = sea;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public List<SubdivisionFace> getNeighbours() {
        return neighbours;
    }

    void setNeighbours(List<SubdivisionFace> neighbours) {
        this.neighbours = neighbours;
    }

    public double getArea() {
        return area;
    }

    public double getCartographicError(boolean signed) {
        if (signed) {
            return cartographicError;
        } else {
            return Math.abs(cartographicError);
        }
    }

    public void setCartographicError(double cartographicError) {
        this.cartographicError = cartographicError;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void addVertex(Vertex v) {
        addVertex(v, true);
    }

    /**
     *
     * @param v - the Vertex to be added to the 'end' of the border of this face.
     * @param update - whether to update the corresponding vertex. If this method is called multiple times before the vertex is needed again, it is probably more efficient to recompute it after all modifications are completed using updateCorrespondingVertex(), rather than updating it incrementally.
     */
    public void addVertex(Vertex v, boolean update) {
        if (update) {
            updateCorrespondingVertexForAdd(v);
        }

        vertices.add(v);
    }

    public void removeVertex(Vertex v) {
        removeVertex(v, true);
    }

    /**
     *
     * @param v - the Vertex to be removed from the border of this face.
     * @param update - whether to update the corresponding vertex. If this method is called multiple times before the vertex is needed again, it is probably more efficient to recompute it after all modifications are completed using updateCorrespondingVertex(), rather than updating it incrementally.
     */
    public void removeVertex(Vertex v, boolean update) {
        if (update) {
            updateCorrespondingVertexForRemove(v);
        }

        vertices.remove(v);
    }

    public void setVertices(List<Vertex> vertices) {
        this.vertices = new ArrayList<Vertex>(vertices);
        updateCorrespondingVertex();
    }

    public Vertex getCorrespondingVertex() {
        return correspondingVertex;
    }

    public void setCorrespondingVertex(Vertex correspondingVertex) {
        this.correspondingVertex = correspondingVertex;
    }

    public void updateCorrespondingVertex() {
        if (vertices != null && vertices.size() > 0) {
            if (correspondingVertexType == CorrespondingVertexType.AVERAGE) {
                // Make the corresponding vertex the average of all vertices
                double sumX = 0;
                double sumY = 0;

                for (Vertex v : vertices) {
                    sumX += v.getX();
                    sumY += v.getY();
                }

                if (correspondingVertex == null) {
                    correspondingVertex = new Vertex(sumX / vertices.size(), sumY / vertices.size());
                } else {
                    correspondingVertex.setX(sumX / vertices.size());
                    correspondingVertex.setY(sumY / vertices.size());
                }

                updateArea();
            } else if (correspondingVertexType == CorrespondingVertexType.CENTROID) {
                if (correspondingVertex == null) {
                    correspondingVertex = centerOfMass(vertices);
                } else {
                    Vertex center = centerOfMass(vertices);

                    correspondingVertex.setX(center.getX());
                    correspondingVertex.setY(center.getY());
                }
            }
        } else {
            area = 0;

            if (correspondingVertex == null) {
                correspondingVertex = new Vertex(0, 0);
            } else {
                correspondingVertex.setX(0);
                correspondingVertex.setY(0);
            }
        }
    }

    private void updateCorrespondingVertexForAdd(Vertex v) {
        if (correspondingVertexType == CorrespondingVertexType.AVERAGE) {
            // Update the average
            double sumX = correspondingVertex.getX() * vertices.size() + v.getX();
            double sumY = correspondingVertex.getY() * vertices.size() + v.getY();

            correspondingVertex.setX(sumX / (vertices.size() + 1));
            correspondingVertex.setY(sumY / (vertices.size() + 1));
        } else if (correspondingVertexType == CorrespondingVertexType.CENTROID) {
            // Update the center of mass
            double sumArea = area * 2;
            double sumCx = correspondingVertex.getX() * 6 * area;
            double sumCy = correspondingVertex.getY() * 6 * area;

            // Remove the contribution from the previous last point, as it changes
            Vertex last = vertices.get(vertices.size() - 1);
            Vertex next = vertices.get(0);

            double areaIncrement = last.getX() * next.getY() - next.getX() * last.getY();

            sumArea -= areaIncrement;
            sumCx -= (last.getX() + next.getX()) * areaIncrement;
            sumCy -= (last.getY() + next.getY()) * areaIncrement;

            // Add the new contribution from the previous last vertex
            areaIncrement = last.getX() * v.getY() - v.getX() * last.getY();

            sumArea += areaIncrement;
            sumCx += (last.getX() + v.getX()) * areaIncrement;
            sumCy += (last.getY() + v.getY()) * areaIncrement;

            // Add the contribution from the new vertex
            areaIncrement = v.getX() * next.getY() - next.getX() * v.getY();

            sumArea += areaIncrement;
            sumCx += (v.getX() + next.getX()) * areaIncrement;
            sumCy += (v.getY() + next.getY()) * areaIncrement;

            // Compute the new center of mass
            area = sumArea / 2;
            correspondingVertex.setX(sumCx / (6 * area));
            correspondingVertex.setY(sumCy / (6 * area));
        }
    }

    private void updateCorrespondingVertexForRemove(Vertex v) {
        if (correspondingVertexType == CorrespondingVertexType.AVERAGE) {
            // Update the average
            double sumX = correspondingVertex.getX() * vertices.size() - v.getX();
            double sumY = correspondingVertex.getY() * vertices.size() - v.getY();

            correspondingVertex.setX(sumX / (vertices.size() - 1));
            correspondingVertex.setY(sumY / (vertices.size() - 1));
        } else if (correspondingVertexType == CorrespondingVertexType.CENTROID) {
            // Update the center of mass
            double sumCx = correspondingVertex.getX() * 6 * area;
            double sumCy = correspondingVertex.getY() * 6 * area;
            double sumArea = area * 2;

            // Remove the contribution from the vertex before the vertex to be removed, as it changes
            int index = vertices.indexOf(v);
            Vertex prev = vertices.get((index - 1 + vertices.size()) % vertices.size());

            double areaIncrement = prev.getX() * v.getY() - v.getX() * prev.getY();

            sumArea -= areaIncrement;
            sumCx -= (prev.getX() + v.getX()) * areaIncrement;
            sumCy -= (prev.getY() + v.getY()) * areaIncrement;

            // Remove the contribution from the vertex to be removed
            Vertex next = vertices.get((index + 1) % vertices.size());

            areaIncrement = v.getX() * next.getY() - next.getX() * v.getY();

            sumArea -= areaIncrement;
            sumCx -= (v.getX() + next.getX()) * areaIncrement;
            sumCy -= (v.getY() + next.getY()) * areaIncrement;

            // Add the new contribution from the vertex before the one to be removed
            areaIncrement = prev.getX() * next.getY() - next.getX() * prev.getY();

            sumArea += areaIncrement;
            sumCx += (prev.getX() + next.getX()) * areaIncrement;
            sumCy += (prev.getY() + next.getY()) * areaIncrement;

            // Compute the new center of mass
            area = sumArea / 2;
            correspondingVertex.setX(sumCx / (6 * area));
            correspondingVertex.setY(sumCy / (6 * area));
        }
    }

    private Vertex centerOfMass(List<Vertex> vertices) {
        area = 0;

        if (vertices != null && vertices.size() > 0) {
            double cx = 0;
            double cy = 0;

            for (int i = 0; i < vertices.size(); i++) {
                Vertex v = vertices.get(i);
                Vertex next = vertices.get((i + 1) % vertices.size());

                double areaIncrement = v.getX() * next.getY() - next.getX() * v.getY();

                area += areaIncrement;
                cx += (v.getX() + next.getX()) * areaIncrement;
                cy += (v.getY() + next.getY()) * areaIncrement;
            }

            area /= 2;
            cx /= 6 * area;
            cy /= 6 * area;

            return new Vertex(cx, cy);
        } else {
            return new Vertex(0, 0);
        }
    }

    private void updateArea() {
        area = 0;

        if (vertices != null && vertices.size() > 0) {
            for (int i = 0; i < vertices.size(); i++) {
                Vertex v = vertices.get(i);
                Vertex next = vertices.get((i + 1) % vertices.size());

                double areaIncrement = v.getX() * next.getY() - next.getX() * v.getY();

                area += areaIncrement;
            }

            area /= 2;
        }
    }

    public void save(BufferedWriter out) throws IOException {
        if (this instanceof CompositeFace) {
            out.write("Composite");
            out.newLine();
        }

        out.write("Vertices");
        out.newLine();

        out.write(Integer.toString(vertices.size()));
        out.newLine();

        for (Vertex v : vertices) {
            v.save(out);
        }

        out.write("Color");
        out.newLine();
        out.write(color.getRed() + " " + color.getGreen() + " " + color.getBlue());
        out.newLine();

        out.write("Name");
        out.newLine();
        out.write(name);
        out.newLine();

        out.write("CartographicError");
        out.newLine();
        out.write(Double.toString(cartographicError));
        out.newLine();

        out.write("Weight");
        out.newLine();
        out.write(Double.toString(weight));
        out.newLine();

        out.write("Sea");
        out.newLine();
        out.write(Boolean.toString(sea));
        out.newLine();

        out.write("CorrespondingVertex");
        out.newLine();
        correspondingVertex.save(out);
        out.newLine();
    }

    public static SubdivisionFace load(BufferedReader in, Subdivision sub) throws IOException {
        boolean composite = false;

        String line = in.readLine();
        while (line != null && line.isEmpty()) {
            line = in.readLine();
        }

        if (line.equals("Composite")) {
            composite = true;
        }

        if (!line.equals("Vertices")) {
            findFlag("Vertices", in);
        }

        int nVertices = Integer.parseInt(in.readLine());

        List<Vertex> vertices = new ArrayList<Vertex>(nVertices);

        for (int i = 0; i < nVertices; i++) {
            vertices.add(Vertex.load(in));
        }

        findFlag("Color", in);

        line = in.readLine();
        String[] colorParts = line.split(" ");
        Color color = null;

        if (colorParts.length == 3) {
            int red = Integer.parseInt(colorParts[0]);
            int green = Integer.parseInt(colorParts[1]);
            int blue = Integer.parseInt(colorParts[2]);

            color = new Color(red, green, blue);
        } else {
            throw new IOException("Incorrect file format: '" + line + "' is not a valid color format.");
        }

        findFlag("Name", in);
        String name = in.readLine();

        line = in.readLine();
        double cartographicError = 0;
        double weight;

        if (line != null && line.equals("CartographicError")) {
            cartographicError = Double.parseDouble(in.readLine());

            findFlag("Weight", in);
            weight = Double.parseDouble(in.readLine());
        } else if (line != null && line.equals("Weight")) {
            weight = Double.parseDouble(in.readLine());
        } else {
            throw new IOException("Incorrect file format: No 'CartographicError' or 'Weight' found.");
        }

        line = in.readLine();
        boolean sea = false;
        Vertex correspondingVertex;

        if (line != null && line.equals("Sea")) {
            sea = Boolean.parseBoolean(in.readLine());

            findFlag("CorrespondingVertex", in);
            correspondingVertex = Vertex.load(in);
        } else if (line != null && line.equals("CorrespondingVertex")) {
            correspondingVertex = Vertex.load(in);
        } else {
            throw new IOException("Incorrect file format: No 'Sea' or 'CorrespondingVertex' found.");
        }

        SubdivisionFace result;

        if (composite) {
            result = new CompositeFace(vertices, color, name, weight, sea, false);
        } else {
            result = new SubdivisionFace(vertices, color, name, weight, sea, false);
        }

        result.setCorrespondingVertex(correspondingVertex);
        result.setCartographicError(cartographicError);

        return result;
    }

    public static void findFlag(String flag, BufferedReader in) throws IOException {
        String line = in.readLine();

        while (line != null && !line.equals(flag)) {
            line = in.readLine();
        }

        if (line == null) {
            throw new IOException("Incorrect file format: '" + flag + "' not found.");
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
