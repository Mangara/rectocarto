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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectocarto.data.CartogramSettings;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectocarto.algos.lp.SegmentIdentification.FaceSegments;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

public class SubdivisionToBilinearProblem {

    private static final String MAX_ERROR_VARIABLE_NAME = "E_MAX";

    // User-specified variables
    private final Subdivision sub;
    private final CartogramSettings settings;
    // Variables used internally by the class
    private MinimizationProblem problem;
    private Map<SubdivisionFace, FaceSegments> segments;
    private Solution feasibleSolution;

    public SubdivisionToBilinearProblem(Subdivision sub, CartogramSettings settings) {
        this.sub = sub;
        this.settings = settings;
    }

    /**
     * Returns the bilinear optimization problem corresponding to the given
     * subdivision.
     *
     * @return
     */
    public MinimizationProblem getProblem() {
        if (problem == null) {
            constructProblem();
        }

        return problem;
    }

    /**
     * Returns a feasible solution to the bilinear optimization problem.
     *
     * @return
     */
    public Solution getFeasibleSolution() {
        if (feasibleSolution == null) {
            if (problem == null) {
                constructProblem();
            }

            constructFeasibleSolution();
        }

        return feasibleSolution;
    }

    /**
     * Returns the set of variables representing horizontal segment positions.
     *
     * @return
     */
    public Set<String> getHorizontalSegmentVariables() {
        if (problem == null) {
            constructProblem();
        }

        Set<String> horizontal = new HashSet<>();

        for (FaceSegments s : segments.values()) {
            horizontal.add(s.bottom.name);
            horizontal.add(s.top.name);
        }

        return horizontal;
    }

    /**
     * Returns the set of variables representing vertical segment positions.
     *
     * @return
     */
    public Set<String> getVerticalSegmentVariables() {
        if (problem == null) {
            constructProblem();
        }

        Set<String> vertical = new HashSet<>();

        for (FaceSegments s : segments.values()) {
            vertical.add(s.left.name);
            vertical.add(s.right.name);
        }

        return vertical;
    }

    /**
     * Returns the set of variables representing cartographic errors.
     *
     * @return
     */
    public Set<String> getErrorVariables() {
        if (problem == null) {
            constructProblem();
        }

        Set<String> error = new HashSet<>();

        int count = 0;
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary() && !f.isSea()) {
                error.add(getErrorVariableName(f, count));
            }
            count++;
        }

        if (settings.objective == CartogramSettings.Objective.MAX_ERROR
                || settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR
                || settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR_SQUARED) {
            error.add(MAX_ERROR_VARIABLE_NAME);
        }

        return error;
    }

    /**
     * Constructs the appropriate bilinear optimization problem for a given
     * subdivision with these cartogram settings.
     *
     * TODO: handle child regions explicitly
     *
     * TODO: incorrect sea adjacencies
     *
     * @return
     */
    private void constructProblem() {
        checkForIssues();

        segments = SegmentIdentification.identifySegments(sub);
        problem = new MinimizationProblem();

        problem.setObjective(buildObjectiveFunction());

        addPlanarityConstraints();
        addAdjacencyConstraints();
        addAspectRatioConstraints();
        addAreaConstraints();

        problem = ProblemReduction.substituteFixedVariables(problem);
        problem = ProblemReduction.removeDuplicateConstraints(problem);
    }

    private void checkForIssues() {
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

    private ObjectiveFunction buildObjectiveFunction() {
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

    private void addBoundaryConstraint(SubdivisionFace face) {
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

    private void addPlanarityConstraints() {
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (f.isBoundary()) {
                addBoundaryConstraint(f);
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

    private void addAdjacencyConstraints() {
        RegularEdgeLabeling rel = sub.getDualGraph().getRegularEdgeLabeling();

        for (Edge edge : sub.getDualGraph().getEdges()) {
            Pair<Graph.Labeling, Edge.Direction> label = rel.get(edge);

            if (label.getFirst() == Graph.Labeling.NONE) {
                continue;
            }

            SubdivisionFace fromFace = sub.getFace(edge.getOrigin());
            SubdivisionFace toFace = sub.getFace(edge.getDestination());

            if (fromFace.isBoundary() || toFace.isBoundary()) {
                continue;
            }

            if (label.getFirst() == Graph.Labeling.BLUE) { // Horizontal; left-to-right
                // bottom <= top for both combinations
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(fromFace).top.name),
                        new Pair<>(-1d, segments.get(toFace).bottom.name)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumSeparation));
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(toFace).top.name),
                        new Pair<>(-1d, segments.get(fromFace).bottom.name)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumSeparation));
            } else if (label.getFirst() == Graph.Labeling.RED) { // Vertical; bottom-to-top
                // left <= right for both combinations
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(toFace).right.name),
                        new Pair<>(-1d, segments.get(fromFace).left.name)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumSeparation));
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(fromFace).right.name),
                        new Pair<>(-1d, segments.get(toFace).left.name)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumSeparation));
            }
        }
    }

    private void addAspectRatioConstraints() {
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

    private void addAreaConstraints() {
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

    private void constructFeasibleSolution() {
        Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

        // toplogical sort on dual of st-graphs, incrementing by minimumSeparation or minimumSeaDimension
        
        scaleToCartogramSize(variables);

        feasibleSolution = new Solution(problem.getObjective().evaluate(variables), variables);
    }

    private void scaleToCartogramSize(Map<String, Double> variables) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                FaceSegments s = segments.get(f);
                minX = Math.min(minX, variables.get(s.left.name));
                maxX = Math.max(maxX, variables.get(s.right.name));
                minY = Math.min(minY, variables.get(s.bottom.name));
                maxY = Math.max(maxY, variables.get(s.top.name));
            }
        }

        if (minX != settings.boundaryWidth || minY != settings.boundaryWidth) {
            throw new InternalError("Minimum dimensions not equal to boundary width.");
        }

        if (maxX > settings.cartogramWidth - settings.boundaryWidth || maxY > settings.cartogramHeight - settings.boundaryWidth) {
            throw new IllegalArgumentException("No cartogram can be constructed with these settings. Either increase the cartogram width and height or decrease the minimum separation.");
        }

        double xScale = (settings.cartogramWidth - 2 * settings.boundaryWidth) / (maxX - minX);
        double yScale = (settings.cartogramHeight - 2 * settings.boundaryWidth) / (maxY - minY);
        
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                FaceSegments s = segments.get(f);
                variables.compute(s.left.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.right.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.bottom.name, (key, val) -> settings.boundaryWidth + yScale * val);
                variables.compute(s.top.name, (key, val) -> settings.boundaryWidth + yScale * val);
            }
        }
    }
}
