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
import rectocarto.algos.lp.solver.CLPSolver;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

public class SubdivisionToBilinearProblem {

    static final String MAX_ERROR_VARIABLE_NAME = "E_MAX";

    // User-specified variables
    private final Subdivision sub;
    private final CartogramSettings settings;
    // Variables used internally by the class
    private MinimizationProblem problem;
    Map<SubdivisionFace, FaceSegments> segments; // DEBUG: non-oprivate for testing purposes TODO
    private Solution feasibleSolution;
    private final Map<SubdivisionFace, String> errorVariables = new HashMap<>();
    Map<String, Map<String, PredecessorRelation>> predecessors; // DEBUG: non-oprivate for testing purposes TODO
    Map<String, Set<String>> successors; // DEBUG: non-oprivate for testing purposes TODO

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

            feasibleSolution = FeasibleSolutionBuilder.constructFeasibleSolution3(sub, settings, problem, segments, predecessors, successors, new CLPSolver());
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

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                FaceSegments s = segments.get(f);
                horizontal.add(s.bottom);
                horizontal.add(s.top);
            }
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

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                FaceSegments s = segments.get(f);
                vertical.add(s.left);
                vertical.add(s.right);
            }
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

        Set<String> error = new HashSet<>(errorVariables.values());

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
     * @return
     */
    private void constructProblem() {
        checkForIssues();

        segments = SegmentIdentification.identifySegments(sub);
        problem = new MinimizationProblem();

        problem.setObjective(buildObjectiveFunction());

        Pair<Map<String, Map<String, PredecessorRelation>>, Map<String, Set<String>>> predAndSucc = findPredecessorsAndSuccessors();
        predecessors = predAndSucc.getFirst();
        successors = predAndSucc.getSecond();

        addBoundaryConstraints();
        addPlanarityAndAdjacencyConstraints();
        addAspectRatioConstraints();
        addAreaConstraints();

        //problem = ProblemReduction.substituteFixedVariables(problem);
        //problem = ProblemReduction.removeDuplicateConstraints(problem);
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

    enum PredecessorRelation {
        STANDARD, SEA, BOTH;
    }

    private Pair<Map<String, Map<String, PredecessorRelation>>, Map<String, Set<String>>> findPredecessorsAndSuccessors() {
        Map<String, Map<String, PredecessorRelation>> predecessors = new HashMap<>();
        Map<String, Set<String>> successors = new HashMap<>();

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                FaceSegments segs = segments.get(f);
                predecessors.putIfAbsent(segs.left, new HashMap<>());
                predecessors.putIfAbsent(segs.right, new HashMap<>());
                predecessors.putIfAbsent(segs.top, new HashMap<>());
                predecessors.putIfAbsent(segs.bottom, new HashMap<>());
                successors.putIfAbsent(segs.left, new HashSet<>());
                successors.putIfAbsent(segs.right, new HashSet<>());
                successors.putIfAbsent(segs.top, new HashSet<>());
                successors.putIfAbsent(segs.bottom, new HashSet<>());
            }
        }

        // planarity
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (f.isBoundary()) {
                continue;
            }

            FaceSegments segs = segments.get(f);
            PredecessorRelation rel = (f.isSea() ? PredecessorRelation.SEA : PredecessorRelation.STANDARD);

            addPredecessor(segs.right, segs.left, rel, predecessors, successors);
            addPredecessor(segs.top, segs.bottom, rel, predecessors, successors);
        }

        // adjacencies
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
                addPredecessor(segments.get(fromFace).top, segments.get(toFace).bottom, PredecessorRelation.STANDARD, predecessors, successors);
                addPredecessor(segments.get(toFace).top, segments.get(fromFace).bottom, PredecessorRelation.STANDARD, predecessors, successors);
            } else if (label.getFirst() == Graph.Labeling.RED) { // Vertical; bottom-to-top
                // left <= right for both combinations
                addPredecessor(segments.get(fromFace).right, segments.get(toFace).left, PredecessorRelation.STANDARD, predecessors, successors);
                addPredecessor(segments.get(toFace).right, segments.get(fromFace).left, PredecessorRelation.STANDARD, predecessors, successors);
            }
        }

        return new Pair<>(predecessors, successors);
    }

    private void addPredecessor(String successor, String predecessor, PredecessorRelation rel, Map<String, Map<String, PredecessorRelation>> predecessors, Map<String, Set<String>> successors) {
        Map<String, PredecessorRelation> pred = predecessors.get(successor);
        PredecessorRelation currentRelation = pred.get(predecessor);

        if (currentRelation == null) {
            pred.put(predecessor, rel);
        } else if (currentRelation != rel) {
            pred.put(predecessor, PredecessorRelation.BOTH);
        }

        successors.get(predecessor).add(successor);
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

                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        lin.addTerm(1, getErrorVariableName(f));
                    }
                }

                return lin;
            case MAX_AND_AVERAGE_ERROR:
                lin = new ObjectiveFunction.Linear();
                lin.addTerm(nWeightedFaces, MAX_ERROR_VARIABLE_NAME);

                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        lin.addTerm(1, getErrorVariableName(f));
                    }
                }

                return lin;
            case AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();

                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        quad.addQuadraticTerm(1, getErrorVariableName(f));
                    }
                }

                return quad;
            case MAX_AND_AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();
                quad.addLinearTerm(nWeightedFaces, MAX_ERROR_VARIABLE_NAME);

                for (SubdivisionFace f : sub.getTopLevelFaces()) {
                    if (!f.isBoundary() && !f.isSea()) {
                        quad.addQuadraticTerm(1, getErrorVariableName(f));
                    }
                }

                return quad;
            default:
                throw new IllegalArgumentException("Unexpected objective function type: " + settings.objective);
        }
    }

    private void addBoundaryConstraints() {
        // North.bottom = cartogramHeight
        problem.addConstraint(new Constraint.Linear(Arrays.asList(
                new Pair<>(1d, segments.get(sub.getNorthFace()).bottom)),
                Constraint.Comparison.EQUAL,
                settings.cartogramHeight));

        // East.left = cartogramWidth
        problem.addConstraint(new Constraint.Linear(Arrays.asList(
                new Pair<>(1d, segments.get(sub.getEastFace()).left)),
                Constraint.Comparison.EQUAL,
                settings.cartogramWidth));

        // South.top = 0
        problem.addConstraint(new Constraint.Linear(Arrays.asList(
                new Pair<>(1d, segments.get(sub.getSouthFace()).top)),
                Constraint.Comparison.EQUAL,
                0));

        // West.right = 0
        problem.addConstraint(new Constraint.Linear(Arrays.asList(
                new Pair<>(1d, segments.get(sub.getWestFace()).right)),
                Constraint.Comparison.EQUAL,
                0));
    }

    private void addPlanarityAndAdjacencyConstraints() {
        for (String segment : predecessors.keySet()) {
            for (Map.Entry<String, PredecessorRelation> entry : predecessors.get(segment).entrySet()) {
                String segment2 = entry.getKey();
                PredecessorRelation rel = entry.getValue();

                double sep;

                switch (rel) {
                    case STANDARD:
                        sep = settings.minimumFeatureSize;
                        break;
                    case SEA:
                        sep = settings.minimumSeaDimension;
                        break;
                    case BOTH:
                        sep = Math.max(settings.minimumFeatureSize, settings.minimumSeaDimension);
                        break;
                    default:
                        throw new InternalError("Unrecognized PredecessorRelation: " + rel);
                }

                // segment => segment2 + sep
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segment),
                        new Pair<>(-1d, segment2)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        sep));
            }
        }
    }

    private void addBoundaryConstraint(SubdivisionFace face) {
        switch (face.getName()) {
            case "NORTH":
                // bottom = cartogramHeight
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).bottom)),
                        Constraint.Comparison.EQUAL,
                        settings.cartogramHeight));
                break;
            case "EAST":
                // left = cartogramWidth
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).left)),
                        Constraint.Comparison.EQUAL,
                        settings.cartogramWidth));
                break;
            case "SOUTH":
                // top = 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).top)),
                        Constraint.Comparison.EQUAL,
                        0));
                break;
            case "WEST":
                // right = 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(face).right)),
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
            double sep = (f.isSea() ? Math.max(settings.minimumSeaDimension, settings.minimumFeatureSize) : settings.minimumFeatureSize);

            // f.right - f.left => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.right),
                    new Pair<>(-1d, segs.left)),
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                    sep));
            // f.top - f.bottom => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segs.top),
                    new Pair<>(-1d, segs.bottom)),
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                    sep));
        }
    }

    /**
     * TODO: compress multiple same-label edges incident to one face into fewer
     * constraints TODO: false sea adjacencies
     */
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
                        new Pair<>(1d, segments.get(fromFace).top),
                        new Pair<>(-1d, segments.get(toFace).bottom)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumFeatureSize));
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(toFace).top),
                        new Pair<>(-1d, segments.get(fromFace).bottom)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumFeatureSize));
            } else if (label.getFirst() == Graph.Labeling.RED) { // Vertical; bottom-to-top
                // left <= right for both combinations
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(toFace).right),
                        new Pair<>(-1d, segments.get(fromFace).left)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumFeatureSize));
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segments.get(fromFace).right),
                        new Pair<>(-1d, segments.get(toFace).left)),
                        Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                        settings.minimumFeatureSize));
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
                        new Pair<>(1d, segs.right),
                        new Pair<>(-1d, segs.left),
                        new Pair<>(-settings.maximumAspectRatio, segs.top),
                        new Pair<>(settings.maximumAspectRatio, segs.bottom)),
                        Constraint.Comparison.LESS_THAN_OR_EQUAL,
                        0));

                //     (top - bottom)/(right - left) <= maximumAR
                // or: top - bottom - maximumAR * right + maximumAR * left <= 0
                problem.addConstraint(new Constraint.Linear(Arrays.asList(
                        new Pair<>(1d, segs.top),
                        new Pair<>(-1d, segs.bottom),
                        new Pair<>(-settings.maximumAspectRatio, segs.right),
                        new Pair<>(settings.maximumAspectRatio, segs.left)),
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
        double weightToArea = (settings.cartogramWidth * settings.cartogramHeight) / totalWeight;
        
        if (sub.getTopLevelFaces().stream().anyMatch(f -> f.isSea() && !f.isBoundary())) {
            weightToArea *= (1 - settings.seaAreaFraction);
        }

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary() && !f.isSea()) {
                double desiredArea = weightToArea * f.getWeight();
                FaceSegments segs = segments.get(f);
                String err = getErrorVariableName(f);

                List<Pair<Double, Pair<String, String>>> area = Arrays.asList(
                        new Pair<>(1d, new Pair<>(segs.right, segs.top)),
                        new Pair<>(-1d, new Pair<>(segs.right, segs.bottom)),
                        new Pair<>(-1d, new Pair<>(segs.left, segs.top)),
                        new Pair<>(1d, new Pair<>(segs.left, segs.bottom))
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
                            new Pair<>(-1d, getErrorVariableName(f))),
                            Constraint.Comparison.GREATER_THAN_OR_EQUAL,
                            0));
                }
            }
        }
    }

    private String getErrorVariableName(SubdivisionFace face) {
        String err = errorVariables.get(face);

        if (err == null) {
            if (face.isBoundary() || face.isSea()) {
                throw new IllegalArgumentException("Sea and boundary regions do not have error variables.");
            }

            err = "E_" + errorVariables.size() + "_" + face.getName();
            errorVariables.put(face, err);
        }

        return err;
    }
}
