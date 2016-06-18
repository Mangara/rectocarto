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

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class CartogramMaker {

    // User settings
    private static final File tempLPDirectory = new File("Temp");
    private double seaAreaFraction = 0.4; // fraction of the total area that should be sea;  // 0 for London, 0.2 for Netherlands, Europe and the US, 0.4 for the world
    private boolean allowFalseSeaAdjacencies = true;
    private boolean useMaxAspectRatio = true;
    private double maxAspectRatio = 12; // Used 12 for Europe and US, 25 for World
    // The connection to the CPLEX solver
    private final IloCplex cplex;
    // Stuff created at initialization
    private Subdivision rectangularDual;
    private Map<SubdivisionFace, SubdivisionFace> faceMap; // Mapping of faces of the original subdivision to the cartogram
    private Map<SubdivisionFace, String> leftSegment;
    private Map<SubdivisionFace, String> rightSegment;
    private Map<SubdivisionFace, String> topSegment;
    private Map<SubdivisionFace, String> bottomSegment;
    private List<LPConstraint> constantHorizontalSegmentConstraints;
    private List<LPConstraint> constantVerticalSegmentConstraints;
    private Map<SubdivisionFace, String> uniqueNames;
    private Map<SubdivisionFace, Double> areas;
    private boolean failed = false; // Whether 2 consecutive iterations have failed. In this case, there is no point in continuing
    private boolean firstIteration = true; // true if no iteration has been completed yet, false otherwise
    // Stuff created or updated for every iteration
    private boolean moveHorizontalSegmentsNext = false;
    private double bestObjectiveValue = Double.POSITIVE_INFINITY;
    private Map<String, Double> xValues;
    private Map<String, Double> yValues;
    private List<LPConstraint> iterationConstraints;
    // Constant stuff
    public static final double BOUNDARY_WIDTH = 20; // pixels
    public static final double MIN_SEA_SIZE = 5; // pixels
    public static final double EPSILON = 0.001;

    public CartogramMaker(Subdivision sub, IloCplex cplex) throws IncorrectGraphException {
        // Check if any of the weights are 0; in that case, we shouldn't even try to make a cartogram
        for (SubdivisionFace f : sub.getFaces()) {
            if (!f.isSea() && f.getWeight() <= 0) {
                throw new IncorrectGraphException("Region \"" + f.getName() + "\" has weight 0 - impossible to construct a cartogram.");
            }
        }

        this.cplex = cplex;
        cplex.setOut(null);

        if (!tempLPDirectory.exists()) {
            tempLPDirectory.mkdir();
        }
        assert tempLPDirectory.exists() && tempLPDirectory.isDirectory();

        Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> drawerOutput = (new RectangularDualDrawer()).drawSubdivision(sub, false);
        rectangularDual = drawerOutput.getFirst();
        faceMap = drawerOutput.getSecond();

        List<Map<SubdivisionFace, Double>> coordinates = findVariables();
        generateConstantConstraints(coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3));
        initializeVariableValues(coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3));

        generateUniqueNames();
        computeAreas(coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3));

        /*/// DEBUG ////
        System.out.println("Constant constraints used when moving horizontal segments (y-coordinates):");

        for (LPConstraint c : constantHorizontalSegmentConstraints) {
        System.out.println(c.toString());
        }

        System.out.println("Constant constraints used when moving vertical segments (x-coordinates):");

        for (LPConstraint c : constantVerticalSegmentConstraints) {
        System.out.println(c.toString());
        }

        System.out.println("Current variable values:");

        for (Map.Entry<String, Double> entry : xValues.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        for (Map.Entry<String, Double> entry : yValues.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("Face names and areas:");
        for (SubdivisionFace face : rectangularDual.getFaces()) {
        System.out.println(face.getName() + " = " + uniqueNames.get(face) + " with weight " + face.getWeight() + " becomes area " + areas.get(face));
        }

        System.out.println("Iteration constraints used when moving horizontal segments (y-coordinates):");
        moveHorizontalSegmentsNext = true;
        generateIterationConstraints();
        for (LPConstraint c : iterationConstraints) {
        System.out.println(c.toString());
        }

        System.out.println("Iteration constraints used when moving vertical segments (x-coordinates):");
        moveHorizontalSegmentsNext = false;
        generateIterationConstraints();
        for (LPConstraint c : iterationConstraints) {
        System.out.println(c.toString());
        }
        //// DEBUG ///*/
    }

    public boolean isAllowFalseSeaAdjacencies() {
        return allowFalseSeaAdjacencies;
    }

    public void setAllowFalseSeaAdjacencies(boolean allowFalseSeaAdjacencies) {
        this.allowFalseSeaAdjacencies = allowFalseSeaAdjacencies;
    }

    public double getMaxAspectRatio() {
        return maxAspectRatio;
    }

    public void setMaxAspectRatio(double maxAspectRatio) {
        this.maxAspectRatio = maxAspectRatio;
    }

    public double getSeaAreaFraction() {
        return seaAreaFraction;
    }

    public void setSeaAreaFraction(double seaAreaFraction) {
        this.seaAreaFraction = seaAreaFraction;
    }

    public boolean isUseMaxAspectRatio() {
        return useMaxAspectRatio;
    }

    public void setUseMaxAspectRatio(boolean useMaxAspectRatio) {
        this.useMaxAspectRatio = useMaxAspectRatio;
    }

    /**
     * Performs the specified number of double iterations (solving both horizontal and vertical constraints)
     * @param nIterations
     */
    public void iterate(int nIterations) throws IOException, IloException {
        int i = 0;

        while (i < 2 * nIterations && !failed) {
            iterate();
            /*/// DEBUG ////
            updateCartographicError();
            System.out.println("Iteration " + i + ": " + rectangularDual.getAverageCartographicError() + " avg, " + rectangularDual.getMaximumCartographicError() + " max");
            //// DEBUG ///*/
            i++;
        }
    }

    public void iterate() throws IOException, IloException {
        if (!failed) {
            generateIterationConstraints();

            File tempFile = null;
            boolean valid = false;

            while (!valid) {
                tempFile = new File(tempLPDirectory, "temporaryLP" + (int) (10000000 * Math.random()) + ".lp");

                if (!tempFile.exists()) {
                    valid = true;
                }
            }

            writeLPFile(tempFile);

            boolean succes = solveLP(tempFile.getCanonicalPath());

            if (firstIteration) {
                firstIteration = false;
            } else {
                if (!succes) {
                    failed = true;
                    //System.out.println("Could not solve LP further.");
                }
            }

            // Switch the type of segments moved
            moveHorizontalSegmentsNext = !moveHorizontalSegmentsNext;

            // Clean up our temporary file
            tempFile.delete();
        }
    }

    public Subdivision getCartogram() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            ArrayList<Vertex> corners = new ArrayList<Vertex>(4);

            corners.add(new Vertex(xValues.get(leftSegment.get(face)), yValues.get(bottomSegment.get(face))));
            corners.add(new Vertex(xValues.get(rightSegment.get(face)), yValues.get(bottomSegment.get(face))));
            corners.add(new Vertex(xValues.get(rightSegment.get(face)), yValues.get(topSegment.get(face))));
            corners.add(new Vertex(xValues.get(leftSegment.get(face)), yValues.get(topSegment.get(face))));

            face.setVertices(corners);
        }

        updateCartographicError();

        return rectangularDual;
    }

    public Map<SubdivisionFace, SubdivisionFace> getFaceMap() {
        return faceMap;
    }

    private void writeLPFile(File file) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            // Write the objective function
            out.write("Minimize");
            out.newLine();

            List<String> pieces = new ArrayList<String>(rectangularDual.getFaces().size());
            boolean first = true;

            for (SubdivisionFace face : rectangularDual.getFaces()) {
                if (!face.isSea()) {
                    if (first) {
                        pieces.add("[ CE_" + uniqueNames.get(face) + " ^2");
                        first = false;
                    } else {
                        pieces.add(" + CE_" + uniqueNames.get(face) + " ^2");
                    }
                }
            }

            pieces.add("] / 2");

            writeMaximumWidth(pieces, out, 255);

            // Write the constraints
            out.newLine();
            out.write("Subject To");
            out.newLine();
            out.write("\\ Constant constraints");
            out.newLine();

            if (moveHorizontalSegmentsNext) {
                for (LPConstraint c : constantHorizontalSegmentConstraints) {
                    out.write(c.toString());
                    out.newLine();
                }
            } else {
                for (LPConstraint c : constantVerticalSegmentConstraints) {
                    out.write(c.toString());
                    out.newLine();
                }
            }

            out.newLine();
            out.write("\\ Iteration constraints");
            out.newLine();

            for (LPConstraint c : iterationConstraints) {
                out.write(c.toString());
                out.newLine();
            }

            out.newLine();
            out.write("End");
            out.newLine();
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    private List<Map<SubdivisionFace, Double>> findVariables() {
        HashMap<SubdivisionFace, Double> minX = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> minY = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> maxX = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> maxY = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);

        leftSegment = new HashMap<SubdivisionFace, String>(rectangularDual.getFaces().size() * 2);
        rightSegment = new HashMap<SubdivisionFace, String>(rectangularDual.getFaces().size() * 2);
        topSegment = new HashMap<SubdivisionFace, String>(rectangularDual.getFaces().size() * 2);
        bottomSegment = new HashMap<SubdivisionFace, String>(rectangularDual.getFaces().size() * 2);

        for (SubdivisionFace face : rectangularDual.getFaces()) {
            double minFaceX = Double.POSITIVE_INFINITY;
            double minFaceY = Double.POSITIVE_INFINITY;
            double maxFaceX = Double.NEGATIVE_INFINITY;
            double maxFaceY = Double.NEGATIVE_INFINITY;

            for (Vertex v : face.getVertices()) {
                minFaceX = Math.min(minFaceX, v.getX());
                minFaceY = Math.min(minFaceY, v.getY());
                maxFaceX = Math.max(maxFaceX, v.getX());
                maxFaceY = Math.max(maxFaceY, v.getY());
            }

            minX.put(face, minFaceX);
            minY.put(face, minFaceY);
            maxX.put(face, maxFaceX);
            maxY.put(face, maxFaceY);

            leftSegment.put(face, "x_" + minFaceX);
            rightSegment.put(face, "x_" + maxFaceX);
            topSegment.put(face, "y_" + maxFaceY);
            bottomSegment.put(face, "y_" + minFaceY);
        }

        List<Map<SubdivisionFace, Double>> coordinates = new ArrayList<Map<SubdivisionFace, Double>>(4);
        coordinates.add(minX);
        coordinates.add(minY);
        coordinates.add(maxX);
        coordinates.add(maxY);

        return coordinates;
    }

    private void generateConstantConstraints(Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxX, Map<SubdivisionFace, Double> maxY) {
        constantHorizontalSegmentConstraints = new ArrayList<LPConstraint>();
        constantVerticalSegmentConstraints = new ArrayList<LPConstraint>();

        generateVerticalSegmentOrderConstraints(minX, maxX);
        generateHorizontalSegmentOrderConstraints(minY, maxY);

        generateMinSeaSizeConstraints();

        generateExteriorBoundaryConstraints(minX, minY, maxX, maxY);
    }

    private void generateVerticalSegmentOrderConstraints(Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
        for (Edge edge : rectangularDual.getDualGraph().getEdges()) {
            if (rectangularDual.getDualGraph().getEdgeLabel(edge) == Labeling.RED) {
                // Add a constraint
                double x1 = minX.get(rectangularDual.getFace(edge.getOrigin()));
                double x2 = maxX.get(rectangularDual.getFace(edge.getOrigin()));
                double x3 = minX.get(rectangularDual.getFace(edge.getDestination()));
                double x4 = maxX.get(rectangularDual.getFace(edge.getDestination()));

                // Determine if this constraint is necessary
                boolean addConstraint = false;
                boolean land1 = !rectangularDual.getFace(edge.getOrigin()).isSea();
                boolean land2 = !rectangularDual.getFace(edge.getDestination()).isSea();

                if (x1 == x3) {
                    addConstraint = (x2 < x4 && land1) || (x2 > x4 && land2);
                } else if (x2 == x4) {
                    addConstraint = (x1 < x3 && land2) || (x1 > x3 && land1);
                } else if (x1 < x3) {
                    if (x2 > x4) {
                        addConstraint = land2;
                    } else {
                        if (!allowFalseSeaAdjacencies || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedVerticalConstraintCase1(edge, land1, land2, minX, maxX);
                        }
                    }
                } else { // x1 > x3
                    if (x2 < x4) {
                        addConstraint = land1;
                    } else {
                        if (!allowFalseSeaAdjacencies || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedVerticalConstraintCase2(edge, land1, land2, minX, maxX);
                        }
                    }
                }

                if (addConstraint) {
                    double[] positions = new double[]{x1, x2, x3, x4};
                    Arrays.sort(positions);

                    LPConstraint c = new LPConstraint();
                    c.addVariable("x_" + positions[1], 1);
                    c.addVariable("x_" + positions[2], -1);
                    c.setType(LPConstraint.Comparison.LESS_THAN);
                    c.setRightHandSide(-EPSILON);
                    constantVerticalSegmentConstraints.add(c);
                }
            }
        }
    }

    private void generateHorizontalSegmentOrderConstraints(Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
        for (Edge edge : rectangularDual.getDualGraph().getEdges()) {
            if (rectangularDual.getDualGraph().getEdgeLabel(edge) == Labeling.BLUE) {
                double y1 = minY.get(rectangularDual.getFace(edge.getOrigin()));
                double y2 = maxY.get(rectangularDual.getFace(edge.getOrigin()));
                double y3 = minY.get(rectangularDual.getFace(edge.getDestination()));
                double y4 = maxY.get(rectangularDual.getFace(edge.getDestination()));

                // Determine if this constraint is necessary
                boolean addConstraint = false;
                boolean land1 = !rectangularDual.getFace(edge.getOrigin()).isSea();
                boolean land2 = !rectangularDual.getFace(edge.getDestination()).isSea();

                if (y1 == y3) {
                    addConstraint = (y2 < y4 && land1) || (y2 > y4 && land2);
                } else if (y2 == y4) {
                    addConstraint = (y1 < y3 && land2) || (y1 > y3 && land1);
                } else if (y1 < y3) {
                    if (y2 > y4) {
                        addConstraint = land2;
                    } else {
                        if (!allowFalseSeaAdjacencies || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedHorizontalConstraintCase1(edge, land1, land2, minY, maxY);
                        }
                    }
                } else { // y1 > y3
                    if (y2 < y4) {
                        addConstraint = land1;
                    } else {
                        if (!allowFalseSeaAdjacencies || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedHorizontalConstraintCase2(edge, land1, land2, minY, maxY);
                        }
                    }
                }

                if (addConstraint) {
                    double[] positions = new double[]{y1, y2, y3, y4};
                    Arrays.sort(positions);

                    LPConstraint c = new LPConstraint();
                    c.addVariable("y_" + positions[1], 1);
                    c.addVariable("y_" + positions[2], -1);
                    c.setType(LPConstraint.Comparison.LESS_THAN);
                    c.setRightHandSide(-EPSILON);
                    constantHorizontalSegmentConstraints.add(c);
                }
            }
        }
    }

    private void generateMinSeaSizeConstraints() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (face.isSea() && !rectangularDual.getDualGraph().getExteriorVertices().contains(face.getCorrespondingVertex())) {
                // This face is an inner sea region

                // Vertical constraint: right - left > MIN_SEA_SIZE
                LPConstraint c = new LPConstraint();
                c.addVariable(rightSegment.get(face), 1);
                c.addVariable(leftSegment.get(face), -1);
                c.setType(LPConstraint.Comparison.GREATER_THAN);
                c.setRightHandSide(MIN_SEA_SIZE);
                constantVerticalSegmentConstraints.add(c);

                // Horizontal constraint: top - bottom > MIN_SEA_SIZE
                c = new LPConstraint();
                c.addVariable(topSegment.get(face), 1);
                c.addVariable(bottomSegment.get(face), -1);
                c.setType(LPConstraint.Comparison.GREATER_THAN);
                c.setRightHandSide(MIN_SEA_SIZE);
                constantHorizontalSegmentConstraints.add(c);
            }
        }
    }

    private void generateExteriorBoundaryConstraints(Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxX, Map<SubdivisionFace, Double> maxY) {
        // Keep the exterior boundaries at their current position, fix the interior boundaries one BOUNDARY_WIDTH inside of the exterior boundary.

        // Fix North boundaries
        LPConstraint c = new LPConstraint();
        c.addVariable(topSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVN())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(maxY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVN())));
        constantHorizontalSegmentConstraints.add(c);

        c = new LPConstraint();
        c.addVariable(bottomSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVN())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(maxY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVN())) - BOUNDARY_WIDTH);
        constantHorizontalSegmentConstraints.add(c);

        // Fix South boundary
        c = new LPConstraint();
        c.addVariable(bottomSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVS())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(minY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVS())));
        constantHorizontalSegmentConstraints.add(c);

        c = new LPConstraint();
        c.addVariable(topSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVS())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(minY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVS())) + BOUNDARY_WIDTH);
        constantHorizontalSegmentConstraints.add(c);

        // Fix West boundary
        c = new LPConstraint();
        c.addVariable(leftSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVW())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(minX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVW())));
        constantVerticalSegmentConstraints.add(c);

        c = new LPConstraint();
        c.addVariable(rightSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVW())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(minX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVW())) + BOUNDARY_WIDTH);
        constantVerticalSegmentConstraints.add(c);

        // Fix East boundary
        c = new LPConstraint();
        c.addVariable(rightSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVE())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(maxX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVE())));
        constantVerticalSegmentConstraints.add(c);

        c = new LPConstraint();
        c.addVariable(leftSegment.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVE())), 1);
        c.setType(LPConstraint.Comparison.EQUAL);
        c.setRightHandSide(maxX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVE())) - BOUNDARY_WIDTH);
        constantVerticalSegmentConstraints.add(c);
    }

    private void initializeVariableValues(Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxX, Map<SubdivisionFace, Double> maxY) {
        xValues = new HashMap<String, Double>(2 * leftSegment.values().size());
        yValues = new HashMap<String, Double>(2 * bottomSegment.values().size());

        for (SubdivisionFace face : rectangularDual.getFaces()) {
            xValues.put(leftSegment.get(face), minX.get(face));
            xValues.put(rightSegment.get(face), maxX.get(face));
            yValues.put(bottomSegment.get(face), minY.get(face));
            yValues.put(topSegment.get(face), maxY.get(face));
        }
    }

    /**
     * Make sure all non-sea faces have unique names that are valid for CPLEX.
     * Long names are truncated to 11 characters, as the CPLEX LP format only allows names of up to 16 characters and we need 3 characters for the CE_ prefix, thus leaving room for a two-digit count for equal names.
     */
    private void generateUniqueNames() {
        uniqueNames = new HashMap<SubdivisionFace, String>(rectangularDual.getFaces().size());

        Map<String, Integer> counts = new HashMap<String, Integer>(rectangularDual.getFaces().size());

        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                String name = face.getName();

                if (name.length() > 11) {
                    // Truncate it to 11 characters, to leave room for a two-digit count
                    name = name.substring(0, 11);
                }

                // Get rid of illegal characters
                name = name.replaceAll("[^\\w!\"#$%&()/,\\.;\\?@_`'{}|~]", "_");

                // Integer, because int gives a NullPointerException when the name isn't in the map yet
                Integer count = counts.get(name);

                if (count == null) {
                    count = 0;
                }

                count++;

                if (count > 1) {
                    // We have seen this name before, add the count
                    name = face.getName() + count;
                }

                counts.put(face.getName(), count);
                uniqueNames.put(face, name);
            }
        }
    }

    private void computeAreas(Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxX, Map<SubdivisionFace, Double> maxY) {
        areas = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);

        // Compute the total land area
        double totalArea = (maxX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVE())) - minX.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVW())) - 2 * BOUNDARY_WIDTH)
                * (maxY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVN())) - minY.get(rectangularDual.getFace(rectangularDual.getDualGraph().getVS())) - 2 * BOUNDARY_WIDTH);
        double nonSeaArea = totalArea * (1 - seaAreaFraction);

        // Compute the sum of all the weights
        double totalWeight = 0;

        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                totalWeight += face.getWeight();
            }
        }

        // Assign the correct areas
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double area = (face.getWeight() / totalWeight) * nonSeaArea;
                areas.put(face, area);
            }
        }
    }

    private void generateIterationConstraints() {
        iterationConstraints = new ArrayList<LPConstraint>();

        if (moveHorizontalSegmentsNext) {
            generateHorizontalAreaConstraints();

            if (useMaxAspectRatio) {
                generateHorizontalAspectRatioConstraints();
            }
        } else {
            generateVerticalAreaConstraints();

            if (useMaxAspectRatio) {
                generateVerticalAspectRatioConstraints();
            }
        }
    }

    private void generateHorizontalAreaConstraints() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double width = xValues.get(rightSegment.get(face)) - xValues.get(leftSegment.get(face));

                // width * y_top - width * y_bottom + desired area * error variable >= desired area
                LPConstraint c = new LPConstraint();
                c.addVariable(topSegment.get(face), width);
                c.addVariable(bottomSegment.get(face), -width);
                c.addVariable("CE_" + uniqueNames.get(face), areas.get(face));
                c.setType(LPConstraint.Comparison.GREATER_THAN_EQUAL);
                c.setRightHandSide(areas.get(face));
                iterationConstraints.add(c);

                // width * y_top - width * y_bottom - desired area * error variable <= desired area
                c = new LPConstraint();
                c.addVariable(topSegment.get(face), width);
                c.addVariable(bottomSegment.get(face), -width);
                c.addVariable("CE_" + uniqueNames.get(face), -areas.get(face));
                c.setType(LPConstraint.Comparison.LESS_THAN_EQUAL);
                c.setRightHandSide(areas.get(face));
                iterationConstraints.add(c);
            }
        }
    }

    private void generateVerticalAreaConstraints() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double height = yValues.get(topSegment.get(face)) - yValues.get(bottomSegment.get(face));

                // height * x_right - height * x_left + desired area * error variable >= desired area
                LPConstraint c = new LPConstraint();
                c.addVariable(rightSegment.get(face), height);
                c.addVariable(leftSegment.get(face), -height);
                c.addVariable("CE_" + uniqueNames.get(face), areas.get(face));
                c.setType(LPConstraint.Comparison.GREATER_THAN_EQUAL);
                c.setRightHandSide(areas.get(face));
                iterationConstraints.add(c);

                // height * x_right - height * x_left - desired area * error variable <= desired area
                c = new LPConstraint();
                c.addVariable(rightSegment.get(face), height);
                c.addVariable(leftSegment.get(face), -height);
                c.addVariable("CE_" + uniqueNames.get(face), -areas.get(face));
                c.setType(LPConstraint.Comparison.LESS_THAN_EQUAL);
                c.setRightHandSide(areas.get(face));
                iterationConstraints.add(c);
            }
        }
    }

    private void generateHorizontalAspectRatioConstraints() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double width = xValues.get(rightSegment.get(face)) - xValues.get(leftSegment.get(face));
                double factor = 1 / width;

                // 1 / width * y_top - 1 / width * y_bottom <= max AR
                LPConstraint c = new LPConstraint();
                c.addVariable(topSegment.get(face), factor);
                c.addVariable(bottomSegment.get(face), -factor);
                c.setType(LPConstraint.Comparison.LESS_THAN_EQUAL);
                c.setRightHandSide(maxAspectRatio);
                iterationConstraints.add(c);

                // 1 / width * y_top - 1 / width * y_bottom >= 1 / max AR
                c = new LPConstraint();
                c.addVariable(topSegment.get(face), factor);
                c.addVariable(bottomSegment.get(face), -factor);
                c.setType(LPConstraint.Comparison.GREATER_THAN_EQUAL);
                c.setRightHandSide(1 / maxAspectRatio);
                iterationConstraints.add(c);
            }
        }
    }

    private void generateVerticalAspectRatioConstraints() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double height = yValues.get(topSegment.get(face)) - yValues.get(bottomSegment.get(face));
                double factor = 1 / height;

                // 1 / height * x_right - 1 / height * x_left <= max AR
                LPConstraint c = new LPConstraint();
                c.addVariable(rightSegment.get(face), factor);
                c.addVariable(leftSegment.get(face), -factor);
                c.setType(LPConstraint.Comparison.LESS_THAN_EQUAL);
                c.setRightHandSide(maxAspectRatio);
                iterationConstraints.add(c);

                // 1 / height * x_right - 1 / height * x_left >= 1 / max AR
                c = new LPConstraint();
                c.addVariable(rightSegment.get(face), factor);
                c.addVariable(leftSegment.get(face), -factor);
                c.setType(LPConstraint.Comparison.GREATER_THAN_EQUAL);
                c.setRightHandSide(1 / maxAspectRatio);
                iterationConstraints.add(c);
            }
        }
    }

    /**
     * Writes all the pieces of string to the specified writer, inserting newlines after certain pieces to ensure that no line is longer than maxChars characters.
     * @param pieces
     * @param out
     * @param maxChars
     */
    private void writeMaximumWidth(List<String> pieces, BufferedWriter out, int maxChars) throws IOException {
        int charCount = 0;

        for (String s : pieces) {
            if (charCount + s.length() > maxChars) {
                out.newLine();
                charCount = 0;
            }

            out.write(s);
            charCount += s.length();
        }

        out.newLine();
    }

    private boolean solveLP(String fileName) throws IloException {
        if (cplex != null) {
            // Load the new model from the specified file and try to solve it
            cplex.clearModel();
            cplex.importModel(fileName);

            if (cplex.solve()) {
                if (cplex.getCplexStatus() != CplexStatus.Optimal) {
                    System.err.println("Solve returned true. CPLEX status after solving: " + cplex.getCplexStatus());
                }

                // This solution should always be better, but let's check just in case
                if (cplex.getObjValue() < bestObjectiveValue) {
                    bestObjectiveValue = cplex.getObjValue();
                    copyValues();
                }

                return true;
            } else {
                if (cplex.getCplexStatus() != CplexStatus.NumBest && cplex.getCplexStatus() != CplexStatus.Infeasible && cplex.getCplexStatus() != CplexStatus.AbortDualObjLim) {
                    System.err.println("Solve returned false. CPLEX status after solving: " + cplex.getCplexStatus());
                }

                if (cplex.isPrimalFeasible()) {
                    // A feasible solution was found, but not necessarily an optimal one. If it improves the objective value, we'll take it.
                    if (cplex.getObjValue() < bestObjectiveValue) {
                        bestObjectiveValue = cplex.getObjValue();
                        copyValues();
                    }

                    return true;
                } else {
                    // No feasible solution was found
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Copy the value assigned to each variable during the last run of the LP solver.
     */
    private void copyValues() throws IloException {
        // Retrieve the values of all variables (the matrix is used to indicate which variables we want - in this case, those for the most recently solved model)
        IloLPMatrix lpMatrix = (IloLPMatrix) cplex.LPMatrixIterator().next();
        double[] values = cplex.getValues(lpMatrix);

        // Copy each variable's value
        for (int i = 0; i < values.length; i++) {
            String s = lpMatrix.getNumVar(i).getName();

            if (s.startsWith("y")) {
                yValues.put(s, values[i]);
            } else if (s.startsWith("x")) {
                xValues.put(s, values[i]);
            } else if (s.startsWith("CE_")) {
                // We can ignore these
            } else {
                throw new InternalError("Unknown variable type: " + s);
            }
        }
    }

    private void updateCartographicError() {
        for (SubdivisionFace face : rectangularDual.getFaces()) {
            if (!face.isSea()) {
                double desiredArea = areas.get(face);
                double width = xValues.get(rightSegment.get(face)) - xValues.get(leftSegment.get(face));
                double height = yValues.get(topSegment.get(face)) - yValues.get(bottomSegment.get(face));
                double cartogramArea = width * height;

                face.setCartographicError((cartogramArea - desiredArea) / desiredArea); // Use the signed error here
            }
        }
    }

    /**
     * Situation:
     *  3 |---
     * ---| 2
     *  1 |---
     * ---| 4
     *
     * Precondition: either region 1 or 2 is sea
     */
    private void addRelaxedHorizontalConstraintCase1(Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        SubdivisionFace lowestFace = null;
        SubdivisionFace highestFace = null;

        // Find the lowest land face horizontally adjacent to region 2 (except for region 1)
        for (Edge e : edge.getDestination().getEdges()) {
            if (e != edge && e.getDestination() == edge.getDestination() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.BLUE && !rectangularDual.getFace(e.getOrigin()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (minY.get(rectangularDual.getFace(e.getOrigin())) < lowest) {
                    lowest = minY.get(rectangularDual.getFace(e.getOrigin()));
                    lowestFace = rectangularDual.getFace(e.getOrigin());
                }
            }
        }

        // Find the highest land face horizontally adjacent to region 1 (except for region 2)
        for (Edge e : edge.getOrigin().getEdges()) {
            if (e != edge && e.getOrigin() == edge.getOrigin() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.BLUE && !rectangularDual.getFace(e.getDestination()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (maxY.get(rectangularDual.getFace(e.getDestination())) > highest) {
                    highest = maxY.get(rectangularDual.getFace(e.getDestination()));
                    highestFace = rectangularDual.getFace(e.getDestination());
                }
            }
        }

        // Add a constraint between the lowest and highest places
        if (lowest != Double.POSITIVE_INFINITY && highest != Double.NEGATIVE_INFINITY) {
            // highest < lowest
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + highest, 1);
            c.addVariable("y_" + lowest, -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: " + highestFace.getName() + " < " + lowestFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land2 && lowest != Double.POSITIVE_INFINITY) {
            // bottom of 2 < top of lowest face
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + minY.get(rectangularDual.getFace(edge.getDestination())), 1);
            c.addVariable("y_" + maxY.get(lowestFace), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getDestination()).getName() + " < top of " + lowestFace.getName());
        } else if (land1 && highest != Double.NEGATIVE_INFINITY) {
            // bottom of highest face < top of 1
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + minY.get(highestFace), 1);
            c.addVariable("y_" + maxY.get(rectangularDual.getFace(edge.getOrigin())), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: bottom of " + highestFace.getName() + " < top of " + rectangularDual.getFace(edge.getOrigin()).getName());
        }
    }

    /**
     * Situation:
     * ---| 4
     *  1 |---
     * ---| 2
     *  3 |---
     *
     * Precondition: either region 1 or 2 is sea
     */
    private void addRelaxedHorizontalConstraintCase2(Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        SubdivisionFace lowestFace = null;
        SubdivisionFace highestFace = null;

        // Find the lowest land face horizontally adjacent to region 1 (except for region 2)
        for (Edge e : edge.getOrigin().getEdges()) {
            if (e != edge && e.getOrigin() == edge.getOrigin() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.BLUE && !rectangularDual.getFace(e.getDestination()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (minY.get(rectangularDual.getFace(e.getDestination())) < lowest) {
                    lowest = minY.get(rectangularDual.getFace(e.getDestination()));
                    lowestFace = rectangularDual.getFace(e.getDestination());
                }
            }
        }

        // Find the highest land face horizontally adjacent to region 2 (except for region 1)
        for (Edge e : edge.getDestination().getEdges()) {
            if (e != edge && e.getDestination() == edge.getDestination() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.BLUE && !rectangularDual.getFace(e.getOrigin()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (maxY.get(rectangularDual.getFace(e.getOrigin())) > highest) {
                    highest = maxY.get(rectangularDual.getFace(e.getOrigin()));
                    highestFace = rectangularDual.getFace(e.getOrigin());
                }
            }
        }

        // Add a constraint between the lowest and highest places
        if (lowest != Double.POSITIVE_INFINITY && highest != Double.NEGATIVE_INFINITY) {
            // highest < lowest
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + highest, 1);
            c.addVariable("y_" + lowest, -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: " + highestFace.getName() + " < " + lowestFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land1 && lowest != Double.POSITIVE_INFINITY) {
            // bottom of 1 < top of lowest face
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + minY.get(rectangularDual.getFace(edge.getOrigin())), 1);
            c.addVariable("y_" + maxY.get(lowestFace), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getOrigin()).getName() + " < top of " + lowestFace.getName());
        } else if (land2 && highest != Double.NEGATIVE_INFINITY) {
            // bottom of highest face < top of 2
            LPConstraint c = new LPConstraint();
            c.addVariable("y_" + minY.get(highestFace), 1);
            c.addVariable("y_" + maxY.get(rectangularDual.getFace(edge.getDestination())), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantHorizontalSegmentConstraints.add(c);

            //System.out.println("Constraint added: bottom of " + highestFace.getName() + " < top of " + rectangularDual.getFace(edge.getDestination()).getName());
        }
    }

    /**
     * Situation:
     *  4 | 2 |
     * --------
     * | 1 | 3
     *
     * Precondition: either region 1 or 2 is sea
     */
    private void addRelaxedVerticalConstraintCase1(Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
        double leftmost = Double.POSITIVE_INFINITY;
        double rightmost = Double.NEGATIVE_INFINITY;
        SubdivisionFace leftmostFace = null;
        SubdivisionFace rightmostFace = null;

        // Find the leftmost land face vertically adjacent to region 2 (except for region 1)
        for (Edge e : edge.getDestination().getEdges()) {
            if (e != edge && e.getDestination() == edge.getDestination() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.RED && !rectangularDual.getFace(e.getOrigin()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (minX.get(rectangularDual.getFace(e.getOrigin())) < leftmost) {
                    leftmost = minX.get(rectangularDual.getFace(e.getOrigin()));
                    leftmostFace = rectangularDual.getFace(e.getOrigin());
                }
            }
        }

        // Find the rightmost land face vertically adjacent to region 1 (except for region 2)
        for (Edge e : edge.getOrigin().getEdges()) {
            if (e != edge && e.getOrigin() == edge.getOrigin() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.RED && !rectangularDual.getFace(e.getDestination()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (maxX.get(rectangularDual.getFace(e.getDestination())) > rightmost) {
                    rightmost = maxX.get(rectangularDual.getFace(e.getDestination()));
                    rightmostFace = rectangularDual.getFace(e.getDestination());
                }
            }
        }

        // Add a constraint between the leftmost and rightmost places
        if (leftmost != Double.POSITIVE_INFINITY && rightmost != Double.NEGATIVE_INFINITY) {
            // rightmost < leftmost
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + rightmost, 1);
            c.addVariable("x_" + leftmost, -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: " + rightmostFace.getName() + " < " + leftmostFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land2 && leftmost != Double.POSITIVE_INFINITY) {
            // left of 2 < right of leftmost face
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + minX.get(rectangularDual.getFace(edge.getDestination())), 1);
            c.addVariable("x_" + maxX.get(leftmostFace), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: left of " + rectangularDual.getFace(edge.getDestination()).getName() + " < right of " + leftmostFace.getName());
        } else if (land1 && rightmost != Double.NEGATIVE_INFINITY) {
            // left of rightmost face < right of 1
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + minX.get(rightmostFace), 1);
            c.addVariable("x_" + maxX.get(rectangularDual.getFace(edge.getOrigin())), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: left of " + rightmostFace.getName() + " < right of " + rectangularDual.getFace(edge.getOrigin()).getName());
        }
    }

    /**
     * Situation:
     * | 2 | 4
     * --------
     *  3 | 1 |
     */
    private void addRelaxedVerticalConstraintCase2(Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
        double leftmost = Double.POSITIVE_INFINITY;
        double rightmost = Double.NEGATIVE_INFINITY;
        SubdivisionFace leftmostFace = null;
        SubdivisionFace rightmostFace = null;

        // Find the leftmost land face vertically adjacent to region 1 (except for region 2)
        for (Edge e : edge.getOrigin().getEdges()) {
            if (e != edge && e.getOrigin() == edge.getOrigin() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.RED && !rectangularDual.getFace(e.getDestination()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (minX.get(rectangularDual.getFace(e.getDestination())) < leftmost) {
                    leftmost = minX.get(rectangularDual.getFace(e.getDestination()));
                    leftmostFace = rectangularDual.getFace(e.getDestination());
                }
            }
        }

        // Find the rightmost land face vertically adjacent to region 2 (except for region 1)
        for (Edge e : edge.getDestination().getEdges()) {
            if (e != edge && e.getDestination() == edge.getDestination() && rectangularDual.getDualGraph().getEdgeLabel(e) == Labeling.RED && !rectangularDual.getFace(e.getOrigin()).isSea()) {
                // This edge is labeled exactly the same as our edge: consider it for the constraint
                if (maxX.get(rectangularDual.getFace(e.getOrigin())) > rightmost) {
                    rightmost = maxX.get(rectangularDual.getFace(e.getOrigin()));
                    rightmostFace = rectangularDual.getFace(e.getOrigin());
                }
            }
        }

        // Add a constraint between the lowest and highest places
        if (leftmost != Double.POSITIVE_INFINITY && rightmost != Double.NEGATIVE_INFINITY) {
            // rightmost face < leftmost face
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + rightmost, 1);
            c.addVariable("x_" + leftmost, -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: " + rightmostFace.getName() + " < " + leftmostFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land1 && leftmost != Double.POSITIVE_INFINITY) {
            // left of 1 < right of leftmost face
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + minX.get(rectangularDual.getFace(edge.getOrigin())), 1);
            c.addVariable("x_" + maxX.get(leftmostFace), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: left of " + rectangularDual.getFace(edge.getOrigin()).getName() + " < right of " + leftmostFace.getName());
        } else if (land2 && rightmost != Double.NEGATIVE_INFINITY) {
            // left of rightmost face < right of 2
            LPConstraint c = new LPConstraint();
            c.addVariable("x_" + minX.get(rightmostFace), 1);
            c.addVariable("x_" + maxX.get(rectangularDual.getFace(edge.getDestination())), -1);
            c.setType(LPConstraint.Comparison.LESS_THAN);
            c.setRightHandSide(-EPSILON);
            constantVerticalSegmentConstraints.add(c);

            //System.out.println("Constraint added: left of " + rightmostFace.getName() + " < right of " + rectangularDual.getFace(edge.getDestination()).getName());
        }
    }
}
