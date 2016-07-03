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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.algos.RELFusy;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectocarto.data.CartogramSettings;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;

public class SubdivisionToBilinearProblem {

    private static final String MAX_ERROR_VARIABLE_NAME = "E_MAX";

    /**
     * Constructs the appropriate bilinear optimization problem for a given
     * subdivision with these cartogram settings.
     *
     * TODO: handle child regions explicitly TODO: incorrect sea adjacencies
     * TODO: incorrect adjacency level
     *
     * @param sub
     * @param settings
     * @return
     */
    public static MinimizationProblem constructProblem(Subdivision sub, CartogramSettings settings) {
        checkForIssues(sub, settings);

        Map<SubdivisionFace, FaceSegments> segments = identifySegments(sub);
        MinimizationProblem problem = new MinimizationProblem();

        problem.setObjective(buildObjectiveFunction(sub, settings));

        addPlanarityConstraints(problem, segments, sub, settings);
        addAdjacencyConstraints(problem, segments, sub, settings);
        addAspectRatioConstraints(problem, segments, sub, settings);
        addAreaConstraints(problem, segments, sub, settings);

        problem = ProblemReduction.substituteFixedVariables(problem);

        return problem;
    }

    private static void checkForIssues(Subdivision sub, CartogramSettings settings) {
        if (sub.getDualGraph().getRegularEdgeLabeling() == null) {
            throw new IllegalArgumentException("The subdivision must have a valid regular edge labeling.");
        }

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary() && !f.isSea()) {
                if (f.getWeight() <= 0) {
                    throw new IllegalArgumentException("All regions must have positive weight.");
                }
            }
        }
    }

    private static Map<SubdivisionFace, FaceSegments> identifySegments(Subdivision sub) {
        // Create all segments
        Map<SubdivisionFace, FaceSegments> segments = new HashMap<>(2 * sub.getFaces().size());
        int count = 0;

        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            FaceSegments s = new FaceSegments();
            s.left = new Segment("v" + count);
            s.right = new Segment("v" + (count + 1));
            s.bottom = new Segment("h" + count);
            s.top = new Segment("h" + (count + 1));
            segments.put(face, s);
            count += 2;
        }

        // Run union-find to eliminate duplicates
        RegularEdgeLabeling rel = sub.getDualGraph().getRegularEdgeLabeling();
        for (Edge edge : sub.getDualGraph().getEdges()) {
            Pair<Graph.Labeling, Edge.Direction> label = rel.get(edge);

            if (label.getFirst() == Graph.Labeling.BLUE) { // Horizontal
                SubdivisionFace leftFace = sub.getFace(edge.getOrigin());
                SubdivisionFace rightFace = sub.getFace(edge.getDestination());
                segments.get(leftFace).right.merge(segments.get(rightFace).left);
            } else if (label.getFirst() == Graph.Labeling.RED) { // Vertical
                SubdivisionFace bottomFace = sub.getFace(edge.getOrigin());
                SubdivisionFace topFace = sub.getFace(edge.getDestination());
                segments.get(bottomFace).top.merge(segments.get(topFace).bottom);
            }
        }

        // Update the segment mapping
        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            FaceSegments s = segments.get(face);
            s.left = s.left.findRepresentative();
            s.right = s.right.findRepresentative();
            s.bottom = s.bottom.findRepresentative();
            s.top = s.top.findRepresentative();
        }

        return segments;
    }

    private static class FaceSegments {

        Segment left, top, right, bottom;
    }

    private static class Segment {

        String name;
        Segment parent = this;
        int rank = 0;

        Segment(String name) {
            this.name = name;
        }

        public Segment findRepresentative() {
            if (parent != this) {
                parent = parent.findRepresentative();
            }

            return parent;
        }

        public void merge(Segment other) {
            Segment rep1 = findRepresentative();
            Segment rep2 = other.findRepresentative();

            if (rep1 == rep2) {
                return;
            }

            if (rep1.rank < rep2.rank) {
                rep1.parent = rep2;
            } else if (rep1.rank > rep2.rank) {
                rep2.parent = rep1;
            } else {
                rep1.parent = rep2;
                rep2.rank++;
            }
        }
    }

    private static ObjectiveFunction buildObjectiveFunction(Subdivision sub, CartogramSettings settings) {
        ObjectiveFunction.Linear lin;
        ObjectiveFunction.Quadratic quad;

        long nWeightedFaces = sub.getTopLevelFaces().stream()
                .filter(f -> !f.isBoundary() && !f.isSea())
                .count();

        switch (settings.objective) {
            case MAX_ERROR:
                lin = new ObjectiveFunction.Linear();
                lin.addTerm(1, MAX_ERROR_VARIABLE_NAME);
                return lin;
            case AVERAGE_ERROR:
                lin = new ObjectiveFunction.Linear();
                int count = 0;
                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        lin.addTerm(1, getErrorVariableName(f, count));
                    }
                    count++;
                }
                return lin;
            case MAX_AND_AVERAGE_ERROR:
                lin = new ObjectiveFunction.Linear();
                lin.addTerm(nWeightedFaces, MAX_ERROR_VARIABLE_NAME);
                count = 0;
                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        lin.addTerm(1, getErrorVariableName(f, count));
                    }
                    count++;
                }
                return lin;
            case AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();
                count = 0;
                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        quad.addQuadraticTerm(1, getErrorVariableName(f, count));
                    }
                    count++;
                }
                return quad;
            case MAX_AND_AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();
                quad.addLinearTerm(nWeightedFaces, MAX_ERROR_VARIABLE_NAME);
                count = 0;
                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        quad.addQuadraticTerm(1, getErrorVariableName(f, count));
                    }
                    count++;
                }
                return quad;
            default:
                throw new IllegalArgumentException("Unexpected objective function type: " + settings.objective);
        }
    }

    private static void addBoundaryConstraint(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, SubdivisionFace face, CartogramSettings settings) {
        switch (face.getName()) {
            case "NORTH":
                // bottom = cartogramHeight
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).bottom.name)),
                        Constraint.Comparison.EQUAL,
                        settings.cartogramHeight));
                break;
            case "EAST":
                // left = cartogramWidth
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).left.name)),
                        Constraint.Comparison.EQUAL,
                        settings.cartogramWidth));
                break;
            case "SOUTH":
                // top = 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).top.name)),
                        Constraint.Comparison.EQUAL,
                        0));
                break;
            case "WEST":
                // right = 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).right.name)),
                        Constraint.Comparison.EQUAL,
                        0));
                break;
            default:
                throw new IllegalArgumentException("Unexpected boundary region name: " + face.getName());
        }
    }

    private static void addPlanarityConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (f.isBoundary()) {
                addBoundaryConstraint(problem, segments, f, settings);
                continue;
            }

            FaceSegments segs = segments.get(f);
            double sep = (f.isSea() ? settings.minimumSeaDimension : settings.minimumSeparation);

            // f.right - f.left => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.right.name),
                    new Pair<>(-1d, segs.left.name)),
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                    sep));
            // f.top - f.bottom => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.top.name),
                    new Pair<>(-1d, segs.bottom.name)),
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                    sep));
        }
    }

    private static void addAdjacencyConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {

    }

    private static void addAspectRatioConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary() && !f.isSea()) {
                FaceSegments segs = segments.get(f);
                
                //     (right - left)/(top - bottom) <= maximumAR
                // or: right - left <= maximumAR * (top - bottom)
                // or: right - left - maximumAR * top + maximumAR * bottom <= 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.right.name),
                    new Pair<>(-1d, segs.left.name),
                    new Pair<>(-settings.maximumAspectRatio, segs.top.name),
                    new Pair<>(settings.maximumAspectRatio, segs.bottom.name)),
                    Constraint.Comparison.LESS_THAN_OR_EQUAL,
                    0));
                
                //     (top - bottom)/(right - left) <= maximumAR
                // or: top - bottom - maximumAR * right + maximumAR * left <= 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.top.name),
                    new Pair<>(-1d, segs.bottom.name),
                    new Pair<>(-settings.maximumAspectRatio, segs.right.name),
                    new Pair<>(settings.maximumAspectRatio, segs.left.name)),
                    Constraint.Comparison.LESS_THAN_OR_EQUAL,
                    0));
            }
        }
    }

    private static void addAreaConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        // Count the total weight
        double totalWeight = sub.getTopLevelFaces().stream()
                .filter(f -> !f.isBoundary() && !f.isSea())
                .mapToDouble(SubdivisionFace::getWeight)
                .sum();
        double weightToArea = settings.cartogramWidth * settings.cartogramHeight / totalWeight;

        int count = 0;
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary() && !f.isSea()) {
                double desiredArea = weightToArea * f.getWeight();
                FaceSegments segs = segments.get(f);
                String err = getErrorVariableName(f, count);

                List<Pair<Double, Pair<String, String>>> area = Arrays.asList(
                        new Pair<>(1d, new Pair<>(segs.right.name, segs.top.name)),
                        new Pair<>(-1d, new Pair<>(segs.right.name, segs.bottom.name)),
                        new Pair<>(-1d, new Pair<>(segs.left.name, segs.top.name)),
                        new Pair<>(1d, new Pair<>(segs.left.name, segs.bottom.name))
                );

                //     area >= (1 - err) * desiredArea
                // or: area + desiredArea * err >= desiredArea
                problem.addConstraint(new Constraint.Bilinear(Arrays.asList(
                        new Pair<>(desiredArea, err)),
                        area,
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        desiredArea));

                //     area <= (1 + err) * desiredArea
                // or: area - desiredArea * err <= desiredArea
                problem.addConstraint(new Constraint.Bilinear(Arrays.asList(
                        new Pair<>(-desiredArea, err)),
                        area,
                        Constraint.Comparison.LESS_THAN_OR_EQUAL,
                        desiredArea));

                if (settings.objective == CartogramSettings.Objective.MAX_ERROR
                        || settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR
                        || settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR_SQUARED) {
                    // err <= max
                    problem.addConstraint(new Constraint.Linear(Arrays.asList(
                            new Pair<>(1d, MAX_ERROR_VARIABLE_NAME),
                            new Pair<>(-1d, getErrorVariableName(f, count))),
                            Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                            0));
                }
            }

            count++;
        }
    }

    private static String getErrorVariableName(SubdivisionFace face, int count) {
        return "E_" + count + "_" + face.getName();
    }

    private SubdivisionToBilinearProblem() {
    }

    //// DEBUG ////
    public static void main(String[] args) throws IOException, IncorrectGraphException {
        Subdivision sub;
        try (BufferedReader in = Files.newBufferedReader(Paths.get("exampleData/Subdivisions/Simple.sub"))) {
            sub = Subdivision.load(in);
            (new RELFusy()).computeREL(sub.getDualGraph());
            MinimizationProblem p = constructProblem(sub, new CartogramSettings());
            System.out.println(p);
        }
    }
}
