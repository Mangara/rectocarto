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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.embedded.EmbeddedGraph;
import rectangularcartogram.data.embedded.EmbeddedVertex;
import rectangularcartogram.data.embedded.Face;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.exceptions.LowDegreeVertexException;

public class Subdivision {

    private ArrayList<SubdivisionFace> faces;
    private Graph dualGraph;
    private HashMap<Vertex, SubdivisionFace> faceMap;
    private int cartogramWidth; // The width a cartogram of this subdivision should have
    private int cartogramHeight; // The height a cartogram of this subdivision should have

    public Subdivision() {
        faces = new ArrayList<SubdivisionFace>();
        dualGraph = null;
        faceMap = new HashMap<Vertex, SubdivisionFace>();
    }

    public Subdivision(Graph subdivision) throws LowDegreeVertexException {
        constructSubdivision(subdivision);
        setBoundaryRegions(true);
    }

    public SubdivisionFace merge(SubdivisionFace face, SubdivisionFace mergeInto) {
        // Create and add the new face
        CompositeFace mergedFace = new CompositeFace(face, mergeInto);

        faces.add(mergedFace);
        dualGraph.addVertex(mergedFace.getCorrespondingVertex());
        faceMap.put(mergedFace.getCorrespondingVertex(), mergedFace);

        // Remove the merged faces (but leave them in the faces list so we can still update their weights)
        dualGraph.removeVertex(face.getCorrespondingVertex());
        dualGraph.removeVertex(mergeInto.getCorrespondingVertex());
        faceMap.remove(face.getCorrespondingVertex());
        faceMap.remove(mergeInto.getCorrespondingVertex());
        face.getCorrespondingVertex().removeAllEdges();
        mergeInto.getCorrespondingVertex().removeAllEdges();

        // Add edges to the right neighbours
        ArrayList<SubdivisionFace> neighbours = new ArrayList<SubdivisionFace>(mergeInto.getNeighbours().size());

        for (SubdivisionFace neighbour : mergeInto.getNeighbours()) {
            if (neighbour != face) {
                if (faceMap.containsKey(neighbour.getCorrespondingVertex())) {
                    dualGraph.addEdge(mergedFace.getCorrespondingVertex(), neighbour.getCorrespondingVertex());
                    neighbours.add(neighbour);
                } else {
                    // This neighbour is inside some composite region
                    for (SubdivisionFace f : faceMap.values()) {
                        if (f != mergedFace && (f instanceof CompositeFace) && ((CompositeFace) f).contains(neighbour)) {
                            dualGraph.addEdge(mergedFace.getCorrespondingVertex(), f.getCorrespondingVertex());
                            neighbours.add(f);
                            break;
                        }
                    }
                }
            }
        }

        mergedFace.setNeighbours(neighbours);

        System.out.println("Neighbours of " + face + ": " + face.getNeighbours());
        System.out.println("Top-level: " + getTopLevelNeighbours(face, true));
        System.out.println("Neighbours of " + mergeInto + ": " + mergeInto.getNeighbours());
        System.out.println("Top-level: " + getTopLevelNeighbours(mergeInto, true));
        System.out.println("Neighbours of " + mergedFace + ": " + mergedFace.getNeighbours());
        System.out.println("Top-level: " + getTopLevelNeighbours(mergedFace, true));

        return mergedFace;
    }

    public SubdivisionFace unmerge(CompositeFace face) {
        // Remove the composite face
        faces.remove(face);
        faceMap.remove(face.getCorrespondingVertex());
        dualGraph.removeVertex(face.getCorrespondingVertex());

        // Add the merged faces
        SubdivisionFace f1 = face.getFace1();
        SubdivisionFace f2 = face.getFace2();
        Vertex v1 = f1.getCorrespondingVertex();
        Vertex v2 = f2.getCorrespondingVertex();

        //faces.add(f1); No longer necessary
        //faces.add(f2);
        faceMap.put(v1, f1);
        faceMap.put(v2, f2);
        dualGraph.addVertex(v1);
        dualGraph.addVertex(v2);

        // Add the correct edges
        connect(f1);
        connect(f2);

        System.out.println("Neighbours of " + f1 + ": " + f1.getNeighbours());
        System.out.println("Top-level: " + getTopLevelNeighbours(f1, true));
        System.out.println("Neighbours of " + f2 + ": " + f2.getNeighbours());
        System.out.println("Top-level: " + getTopLevelNeighbours(f2, true));

        return f1;
    }

    /**
     * Connect this region to its neighbour regions
     * @param face
     */
    private void connect(SubdivisionFace face) {
        Vertex v = face.getCorrespondingVertex();

        for (SubdivisionFace neighbour : face.getNeighbours()) {
            Vertex nv = neighbour.getCorrespondingVertex();

            if (faceMap.containsKey(nv)) {
                System.out.println("Connected " + face + " to " + neighbour);
                dualGraph.addEdge(v, nv);
            } else {
                // This neighbour is inside some composite region
                for (SubdivisionFace f : faceMap.values()) {
                    if ((f instanceof CompositeFace) && ((CompositeFace) f).contains(neighbour)) {
                        System.out.println("Connected " + face + " to " + f);
                        dualGraph.addEdge(v, f.getCorrespondingVertex());
                        break;
                    }
                }
            }
        }
    }

    public Graph getDualGraph() {
        return dualGraph;
    }

    public void setDualGraph(Graph dualGraph) {
        this.dualGraph = dualGraph;
    }

    public void addFace(SubdivisionFace f) {
        faces.add(f);
    }

    public List<SubdivisionFace> getFaces() {
        return faces;
    }

    public Collection<SubdivisionFace> getTopLevelFaces() {
        return faceMap.values();
    }

    public SubdivisionFace getFace(Vertex correspondingVertex) {
        return faceMap.get(correspondingVertex);
    }

    public HashMap<Vertex, SubdivisionFace> getFaceMap() {
        return faceMap;
    }

    public List<SubdivisionFace> getTopLevelNeighbours(SubdivisionFace face, boolean includeSea) {
        List<Vertex> neighbourVertices = face.getCorrespondingVertex().getNeighbours();
        List<SubdivisionFace> neighbours = new ArrayList<SubdivisionFace>(neighbourVertices.size());

        for (Vertex v : neighbourVertices) {
            SubdivisionFace f = getFace(v);

            if (includeSea || !f.isSea()) {
                neighbours.add(f);
            }
        }

        return neighbours;
    }

    public int getCartogramHeight() {
        return cartogramHeight;
    }

    public void setCartogramHeight(int cartogramHeight) {
        this.cartogramHeight = cartogramHeight;
    }

    public int getCartogramWidth() {
        return cartogramWidth;
    }

    public void setCartogramWidth(int cartogramWidth) {
        this.cartogramWidth = cartogramWidth;
    }

    public double getMaximumCartographicError() {
        double maxError = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace face : faces) {
            maxError = Math.max(maxError, face.getCartographicError(false));
        }

        return maxError;
    }

    /**
     * Returns the average cartographic error of the non-sea regions
     * @return
     */
    public double getAverageCartographicError() {
        double sumError = 0;
        int count = 0;

        for (SubdivisionFace face : faces) {
            if (!face.isSea()) {
                sumError += face.getCartographicError(false);
                count++;
            }
        }

        return sumError / count;
    }

    /**
     * Returns the average squared cartographic error of the non-sea regions
     * @return
     */
    public double getAverageSquaredCartographicError() {
        double sumSqError = 0;
        int count = 0;

        for (SubdivisionFace face : faces) {
            if (!face.isSea()) {
                double error = face.getCartographicError(false);
                sumSqError += error * error;
                count++;
            }
        }

        return sumSqError / count;
    }

    private void constructSubdivision(Graph subdivision) throws LowDegreeVertexException {
        // Check for vertices of degree 4 or more
        for (Vertex vertex : subdivision.getVertices()) {
            if (vertex.getDegree() >= 4) {
                throw new LowDegreeVertexException(vertex);
            }
        }

        EmbeddedGraph dcel = new EmbeddedGraph(subdivision);

        Pair<Graph, HashMap<Face, Vertex>> dual = dcel.getDualGraph();
        dualGraph = dual.getFirst();

        // Find the mapping between embedded and input vertices
        HashMap<EmbeddedVertex, Vertex> vertexMap = new HashMap<EmbeddedVertex, Vertex>(2 * dcel.getVertices().size()); // 2 * to make the hashing more efficient

        for (Entry<Vertex, EmbeddedVertex> entry : dcel.getVertexMap().entrySet()) {
            vertexMap.put(entry.getValue(), entry.getKey());
        }

        faces = new ArrayList<SubdivisionFace>(dcel.getFaces().size());
        faceMap = new HashMap<Vertex, SubdivisionFace>(2 * dcel.getFaces().size());

        for (Face face : dcel.getFaces()) {
            if (face.isOuterFace()) {
                // Remove the outer face from the dual
                dualGraph.removeVertex(dual.getSecond().get(face));
            } else {
                SubdivisionFace f = new SubdivisionFace();

                for (EmbeddedVertex embeddedVertex : face.getVertices()) {
                    f.addVertex(vertexMap.get(embeddedVertex), false); // we'll update later
                }

                f.setCorrespondingVertex(dual.getSecond().get(face));
                faceMap.put(f.getCorrespondingVertex(), f);

                f.updateCorrespondingVertex(); // Make sure its position is correct

                faces.add(f);
            }
        }

        // Find the neighbours of each face
        for (SubdivisionFace face : faces) {
            List<SubdivisionFace> neighbours = new ArrayList<SubdivisionFace>();

            for (Vertex nv : face.getCorrespondingVertex().getNeighbours()) {
                neighbours.add(faceMap.get(nv));
            }

            face.setNeighbours(neighbours);
        }
    }

    private void setBoundaryRegions(boolean moveCorrespondingVertices) {
        SubdivisionFace north = faceMap.get(dualGraph.getVN());
        SubdivisionFace east = faceMap.get(dualGraph.getVE());
        SubdivisionFace south = faceMap.get(dualGraph.getVS());
        SubdivisionFace west = faceMap.get(dualGraph.getVW());

        if (north != null) {
            // These should be treated as sea regions
            north.setSea(true);
            east.setSea(true);
            south.setSea(true);
            west.setSea(true);

            // Set the color
            north.setColor(SubdivisionFace.BOUNDARY_COLOR);
            east.setColor(SubdivisionFace.BOUNDARY_COLOR);
            south.setColor(SubdivisionFace.BOUNDARY_COLOR);
            west.setColor(SubdivisionFace.BOUNDARY_COLOR);

            // Set the right names
            north.setName("NORTH");
            east.setName("EAST");
            south.setName("SOUTH");
            west.setName("WEST");

            // Place corresponding vertices of boundary regions to avoid intersections
            if (moveCorrespondingVertices) {
                // Find the bounding box of non-boundary region vertices
                double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
                        minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

                for (SubdivisionFace f : faces) {
                    if (f != north && f != east && f != south && f != west) {
                        minX = Math.min(minX, f.getCorrespondingVertex().getX());
                        maxX = Math.max(maxX, f.getCorrespondingVertex().getX());
                        minY = Math.min(minY, f.getCorrespondingVertex().getY());
                        maxY = Math.max(maxY, f.getCorrespondingVertex().getY());
                    }
                }

                // Compute good locations for the boundary region vertices
                double offset = Math.sqrt((maxX - minX) * (maxY - minY));
                double centerX = (minX + maxX) / 2;
                double centerY = (minY + maxY) / 2;

                north.getCorrespondingVertex().setX(centerX);
                north.getCorrespondingVertex().setY(maxY + offset);
                east.getCorrespondingVertex().setX(maxX + offset);
                east.getCorrespondingVertex().setY(centerY);
                south.getCorrespondingVertex().setX(centerX);
                south.getCorrespondingVertex().setY(minY - offset);
                west.getCorrespondingVertex().setX(minX - offset);
                west.getCorrespondingVertex().setY(centerY);
            }
        }
    }

    public void save(BufferedWriter out) throws IOException {
        out.write("CartogramWidth");
        out.newLine();
        out.write(Integer.toString(cartogramWidth));
        out.newLine();

        out.write("CartogramHeight");
        out.newLine();
        out.write(Integer.toString(cartogramHeight));
        out.newLine();

        out.write("Faces");
        out.newLine();
        out.write(Integer.toString(faces.size()));
        out.newLine();

        HashMap<SubdivisionFace, Integer> index = new HashMap<SubdivisionFace, Integer>(faces.size() * 2);
        int i = 0;

        for (SubdivisionFace face : faces) {
            face.save(out);
            index.put(face, i);
            i++;
        }

        // Count the number of composite regions
        int n = 0;

        for (SubdivisionFace face : faces) {
            if (face instanceof CompositeFace) {
                n++;
            }
        }

        if (n > 0) {
            out.write("Composites");
            out.newLine();
            out.write(Integer.toString(n));
            out.newLine();

            for (SubdivisionFace face : faces) {
                if (face instanceof CompositeFace) {
                    out.write(Integer.toString(index.get(face)));
                    out.write("=");
                    out.write(Integer.toString(index.get(((CompositeFace) face).getFace1())));
                    out.write("+");
                    out.write(Integer.toString(index.get(((CompositeFace) face).getFace2())));
                    out.newLine();
                }
            }
        }

        out.write("Neighbours");
        out.newLine();

        for (SubdivisionFace face : faces) {
            if (face.getNeighbours() != null) {
                for (SubdivisionFace neighbour : face.getNeighbours()) {
                    out.write(index.get(neighbour) + ",");
                }
            }

            out.newLine();
        }

        out.write("Dualgraph");
        out.newLine();

        dualGraph.save(out);
    }

    public static Subdivision load(BufferedReader in) throws IOException {
        Subdivision result = new Subdivision();

        // load the cartogram size
        findFlag("CartogramWidth", in);
        int width = Integer.parseInt(in.readLine());
        result.setCartogramWidth(width);

        findFlag("CartogramHeight", in);
        int height = Integer.parseInt(in.readLine());
        result.setCartogramHeight(height);

        // load Faces
        findFlag("Faces", in);
        int nFaces = Integer.parseInt(in.readLine());

        for (int i = 0; i < nFaces; i++) {
            result.addFace(SubdivisionFace.load(in, result));
        }

        // Load the neighbours of each face (not required, for backwards compatibility)
        String line = in.readLine();

        while (line != null && line.isEmpty()) {
            line = in.readLine();
        }

        boolean computeNeighbours;
        HashSet<SubdivisionFace> topLevelRegions = new HashSet<SubdivisionFace>(result.faces);

        if (line.equals("Composites")) {
            line = in.readLine();
            int nComposites = Integer.parseInt(line);

            for (int i = 0; i < nComposites; i++) {
                line = in.readLine();

                int split1 = line.indexOf('=');
                int split2 = line.indexOf('+');

                int compositeIndex = Integer.parseInt(line.substring(0, split1));
                int index1 = Integer.parseInt(line.substring(split1 + 1, split2));
                int index2 = Integer.parseInt(line.substring(split2 + 1));

                //System.out.println("parsing Composite: " + line + " indices: (" + compositeIndex + ", " + index1 + ", " + index2 + ")");

                CompositeFace cf = (CompositeFace) result.faces.get(compositeIndex);
                cf.setFace1(result.faces.get(index1));
                cf.setFace2(result.faces.get(index2));

                topLevelRegions.remove(result.faces.get(index1));
                topLevelRegions.remove(result.faces.get(index2));
            }

            line = in.readLine();
        }

        if (line.equals("Neighbours")) {
            for (SubdivisionFace face : result.getFaces()) {
                List<SubdivisionFace> neighbours = new ArrayList<SubdivisionFace>();

                line = in.readLine();
                String[] parts = line.split(",");

                for (String part : parts) {
                    if (!part.isEmpty()) {
                        int index = Integer.parseInt(part);
                        neighbours.add(result.getFaces().get(index));
                    }
                }

                face.setNeighbours(neighbours);
            }

            // We already computed the neighbours
            computeNeighbours = false;

            // skip to the Dual graph
            findFlag("Dualgraph", in);
        } else if (line.equals("Dualgraph")) {
            // Remember to find the neighbours through the dual graph
            computeNeighbours = true;
        } else {
            throw new IOException("Incorrect file format: 'Dualgraph' not found.");
        }

        result.setDualGraph(Graph.load(in));

        // reconstruct face mapping
        result.faceMap = new HashMap<Vertex, SubdivisionFace>(nFaces);

        double precision = 0.0000001;

        for (SubdivisionFace face : topLevelRegions) {
            Vertex faceVertex = face.getCorrespondingVertex();
            boolean found = false;

            for (Vertex v : result.getDualGraph().getVertices()) {
                if (!result.faceMap.containsKey(v) && faceVertex.isNear(v, precision)) {
                    face.setCorrespondingVertex(v);
                    result.faceMap.put(v, face);
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IOException("Inconsistent subdivision: corresponding vertex of face \"" + face + "\" does not occur in the dual graph.");
            }
        }

        // Compute the neighbours of each region if they weren't specified in the file
        if (computeNeighbours) {
            for (SubdivisionFace face : result.faces) {
                List<SubdivisionFace> neighbours = new ArrayList<SubdivisionFace>();

                for (Vertex nv : face.getCorrespondingVertex().getNeighbours()) {
                    neighbours.add(result.faceMap.get(nv));
                }

                face.setNeighbours(neighbours);
            }
        }

        result.setBoundaryRegions(false);

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

    public void updateCompositeFaces() {
        for (SubdivisionFace topFace : faceMap.values()) {
            if (topFace instanceof CompositeFace) {
                ((CompositeFace) topFace).updateWeight();
                ((CompositeFace) topFace).updateColor();
            }
        }
    }
}
