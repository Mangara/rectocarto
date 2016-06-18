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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class FalseSeaAdjacenciesTest {

    public void labelHorizontalConstraintEdges(Subdivision rectangularDual) {
        List<Map<SubdivisionFace, Double>> coordinates = findVariables(rectangularDual);

        //labelHorizontalSegmentOrderConstraints(rectangularDual, coordinates.get(0), coordinates.get(2));
        labelVerticalSegmentOrderConstraints(rectangularDual, coordinates.get(1), coordinates.get(3));
    }

    private List<Map<SubdivisionFace, Double>> findVariables(Subdivision rectangularDual) {
        HashMap<SubdivisionFace, Double> minX = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> minY = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> maxX = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);
        HashMap<SubdivisionFace, Double> maxY = new HashMap<SubdivisionFace, Double>(rectangularDual.getFaces().size() * 2);

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
        }

        List<Map<SubdivisionFace, Double>> coordinates = new ArrayList<Map<SubdivisionFace, Double>>(4);
        coordinates.add(minX);
        coordinates.add(minY);
        coordinates.add(maxX);
        coordinates.add(maxY);

        return coordinates;
    }

    private void labelHorizontalSegmentOrderConstraints(Subdivision rectangularDual, Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
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
                        if (!true || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedHorizontalConstraintCase1(rectangularDual, edge, land1, land2, minX, maxX);
                        }
                    }
                } else { // x1 > x3
                    if (x2 < x4) {
                        addConstraint = land1;
                    } else {
                        if (!true || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedHorizontalConstraintCase2(rectangularDual, edge, land1, land2, minX, maxX);
                        }
                    }
                }

                if (addConstraint) {
                    rectangularDual.getDualGraph().setLabel(edge, Labeling.RED);
                } else {
                    rectangularDual.getDualGraph().setLabel(edge, Labeling.NONE);
                }
            } else {
                rectangularDual.getDualGraph().setLabel(edge, Labeling.NONE);
            }
        }
    }

    private void labelVerticalSegmentOrderConstraints(Subdivision rectangularDual, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
        Map<Edge, Labeling> testLabeling = new HashMap<Edge, Labeling>(2 * rectangularDual.getDualGraph().getEdges().size());

        for (Edge edge : rectangularDual.getDualGraph().getEdges()) {
            if (rectangularDual.getDualGraph().getEdgeLabel(edge) == Labeling.BLUE) {
                // Add a constraint
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
                        if (!true || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedVerticalConstraintCase1(rectangularDual, edge, land1, land2, minY, maxY);
                        }
                    }
                } else { // y1 > y3
                    if (y2 < y4) {
                        addConstraint = land1;
                    } else {
                        if (!true || (land1 && land2)) {
                            addConstraint = true;
                        } else {
                            addConstraint = false; // we have to add a more complicated constraint
                            addRelaxedVerticalConstraintCase2(rectangularDual, edge, land1, land2, minY, maxY);
                        }
                    }
                }

                if (addConstraint) {
                    testLabeling.put(edge, Labeling.BLUE);
                } else {
                    testLabeling.put(edge, Labeling.NONE);
                }
            } else {
                testLabeling.put(edge, Labeling.NONE);
            }
        }

        // Apply our test labeling
        for (Edge edge : rectangularDual.getDualGraph().getEdges()) {
            rectangularDual.getDualGraph().setLabel(edge, testLabeling.get(edge));
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
    private void addRelaxedVerticalConstraintCase1(Subdivision rectangularDual, Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
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
            System.out.println("Constraint added: " + highestFace.getName() + " < " + lowestFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land2 && lowest != Double.POSITIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getDestination()).getName() + " < top of " + lowestFace.getName());
        } else if (land1 && highest != Double.NEGATIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + highestFace.getName() + " < top of " + rectangularDual.getFace(edge.getOrigin()).getName());
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
    private void addRelaxedVerticalConstraintCase2(Subdivision rectangularDual, Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minY, Map<SubdivisionFace, Double> maxY) {
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
            System.out.println("Constraint added: " + highestFace.getName() + " < " + lowestFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land1 && lowest != Double.POSITIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getOrigin()).getName() + " < top of " + lowestFace.getName());
        } else if (land2 && highest != Double.NEGATIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + highestFace.getName() + " < top of " + rectangularDual.getFace(edge.getDestination()).getName());
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
    private void addRelaxedHorizontalConstraintCase1(Subdivision rectangularDual, Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
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
            System.out.println("Constraint added: " + rightmostFace.getName() + " < " + leftmostFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land2 && leftmost != Double.POSITIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getDestination()).getName() + " < top of " + leftmostFace.getName());
        } else if (land1 && rightmost != Double.NEGATIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rightmostFace.getName() + " < top of " + rectangularDual.getFace(edge.getOrigin()).getName());
        }
    }

    /**
     * Situation:
     * | 2 | 4
     * --------
     *  3 | 1 |
     */
    private void addRelaxedHorizontalConstraintCase2(Subdivision rectangularDual, Edge edge, boolean land1, boolean land2, Map<SubdivisionFace, Double> minX, Map<SubdivisionFace, Double> maxX) {
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
            System.out.println("Constraint added: " + rightmostFace.getName() + " < " + leftmostFace.getName());
        }

        // Add a constraint between the land region and the touching highest or lowest region
        if (land1 && leftmost != Double.POSITIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rectangularDual.getFace(edge.getOrigin()).getName() + " < top of " + leftmostFace.getName());
        } else if (land2 && rightmost != Double.NEGATIVE_INFINITY) {
            System.out.println("Constraint added: bottom of " + rightmostFace.getName() + " < top of " + rectangularDual.getFace(edge.getDestination()).getName());
        }
    }
}
