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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectocarto.algos.lp.SegmentIdentification.FaceSegments;
import rectocarto.algos.lp.solver.LinearSolver;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

class FeasibleSolutionBuilder {

    public static void main(String[] args) {

    }

    static Solution constructFeasibleSolution1(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        try {
            Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> dual = (new RectangularDualDrawer()).drawSubdivision(sub, true);
            Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

            for (SubdivisionFace face : sub.getTopLevelFaces()) {
                FaceSegments s = segments.get(face);
                List<Vertex> vertices = dual.getSecond().get(face).getVertices();
                variables.put(s.left, vertices.stream().mapToDouble(Vertex::getX).min().getAsDouble());
                variables.put(s.right, vertices.stream().mapToDouble(Vertex::getX).max().getAsDouble());
                variables.put(s.bottom, vertices.stream().mapToDouble(Vertex::getY).min().getAsDouble());
                variables.put(s.top, vertices.stream().mapToDouble(Vertex::getY).max().getAsDouble());
            }

            scaleToCartogramSize(sub, settings, segments, variables);
            fixBorder(variables, sub, settings, segments);
            computeError(variables, settings, problem);

            return new Solution(problem.getObjective().evaluate(variables), variables);
        } catch (IncorrectGraphException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    static Solution constructFeasibleSolution3(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, Map<String, Map<String, SubdivisionToBilinearProblem.PredecessorRelation>> predecessors, Map<String, Set<String>> successors, LinearSolver solver) {
        Set<String> horizontalSegments = sub.getTopLevelFaces().stream()
                .filter(f -> !f.isBoundary())
                .map(f -> segments.get(f))
                .flatMap(s -> Stream.of(s.top, s.bottom))
                .collect(Collectors.toSet());

        Solution sol = buildHorizontalGuess(horizontalSegments, predecessors, successors, sub, settings, problem, segments, solver);

        if (sol != Solution.INFEASIBLE) {
            return sol;
        }

        // Perform an exponential search for a large value of minFeature that works
        double upperBound = settings.minimumFeatureSize;
        sol = buildHorizontalSolution(upperBound, horizontalSegments, predecessors, successors, sub, settings, problem, segments, solver);

        if (sol == Solution.INFEASIBLE) {
            // Infeasible even with the minimum
            return Solution.INFEASIBLE;
        }

        do {
            upperBound *= 2;
            sol = buildHorizontalSolution(upperBound, horizontalSegments, predecessors, successors, sub, settings, problem, segments, solver);
        } while (sol != Solution.INFEASIBLE);

        // Binary search the remaining interval
        double lowerBound = upperBound / 2;

        while (upperBound - lowerBound >= settings.minimumFeatureSize) {
            double mid = lowerBound + (upperBound - lowerBound) / 2;

            if (buildHorizontalSolution(mid, horizontalSegments, predecessors, successors, sub, settings, problem, segments, solver) == Solution.INFEASIBLE) {
                upperBound = mid;
            } else {
                lowerBound = mid;
            }
        }

        return buildHorizontalSolution(lowerBound, horizontalSegments, predecessors, successors, sub, settings, problem, segments, solver);
    }

    /**
     * compute the dependencies in terms of (#minSea, #minFeature), taking the
     * element-wise max, then set minSea to its actual value and scale
     * minFeature to be as big as possible
     *
     * @param horizontalSegments
     * @param predecessors
     * @param successors
     * @param settings
     * @return
     */
    private static Solution buildHorizontalGuess(
            Set<String> horizontalSegments,
            Map<String, Map<String, SubdivisionToBilinearProblem.PredecessorRelation>> predecessors,
            Map<String, Set<String>> successors,
            Subdivision sub,
            CartogramSettings settings,
            MinimizationProblem problem,
            Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments,
            LinearSolver solver) {
        Map<String, Integer> unprocessedPredecessors = new HashMap<>(horizontalSegments.size() * 2);
        Queue<String> frontier = new ArrayDeque<>();

        for (String segment : horizontalSegments) {
            Map<String, SubdivisionToBilinearProblem.PredecessorRelation> pred = predecessors.get(segment);
            unprocessedPredecessors.put(segment, pred.size());

            if (pred.isEmpty()) {
                frontier.add(segment);
            }
        }

        Map<String, Pair<Integer, Integer>> segmentIncrements = new HashMap<>(horizontalSegments.size() * 2);
        int maxSeaIncrements = 0;
        int maxFeatureIncrements = 0;

        while (!frontier.isEmpty()) {
            String segment = frontier.remove();
            int seaIncrements = 0;
            int featureIncrements = 0;

            for (Map.Entry<String, SubdivisionToBilinearProblem.PredecessorRelation> entry : predecessors.get(segment).entrySet()) {
                Pair<Integer, Integer> predIncrements = segmentIncrements.get(entry.getKey());
                int predSea = predIncrements.getFirst() + (entry.getValue() == SubdivisionToBilinearProblem.PredecessorRelation.STANDARD ? 0 : 1);
                int predFeature = predIncrements.getSecond() + (entry.getValue() == SubdivisionToBilinearProblem.PredecessorRelation.SEA ? 0 : 1);

                seaIncrements = Math.max(seaIncrements, predSea);
                featureIncrements = Math.max(featureIncrements, predFeature);
            }

            segmentIncrements.put(segment, new Pair<>(seaIncrements, featureIncrements));
            maxSeaIncrements = Math.max(maxSeaIncrements, seaIncrements);
            maxFeatureIncrements = Math.max(maxFeatureIncrements, featureIncrements);

            for (String successor : successors.get(segment)) {
                int nPredsLeft = unprocessedPredecessors.get(successor);
                nPredsLeft--;
                unprocessedPredecessors.put(successor, nPredsLeft);

                if (nPredsLeft == 0) {
                    frontier.add(successor);
                }
            }
        }

        double featureSize = (settings.cartogramHeight - maxSeaIncrements * settings.minimumSeaDimension) / maxFeatureIncrements;

        if (featureSize < settings.minimumFeatureSize) {
            return Solution.INFEASIBLE;
        }

        Map<String, Double> variables = new HashMap<>(horizontalSegments.size() * 2);

        for (String segment : horizontalSegments) {
            Pair<Integer, Integer> increments = segmentIncrements.get(segment);
            double value = increments.getFirst() * settings.minimumSeaDimension + increments.getSecond() * featureSize;
            variables.put(segment, value);
        }

        return completeSolutionFromHorizontalSegments(variables, sub, settings, problem, segments, solver);
    }

    private static Solution buildHorizontalSolution(
            double featureSize,
            Set<String> horizontalSegments,
            Map<String, Map<String, SubdivisionToBilinearProblem.PredecessorRelation>> predecessors,
            Map<String, Set<String>> successors,
            Subdivision sub,
            CartogramSettings settings,
            MinimizationProblem problem,
            Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments,
            LinearSolver solver) {
        Map<String, Integer> unprocessedPredecessors = new HashMap<>(horizontalSegments.size() * 2);
        Queue<String> frontier = new ArrayDeque<>();

        for (String segment : horizontalSegments) {
            Map<String, SubdivisionToBilinearProblem.PredecessorRelation> pred = predecessors.get(segment);
            unprocessedPredecessors.put(segment, pred.size());

            if (pred.isEmpty()) {
                frontier.add(segment);
            }
        }

        Map<String, Double> variables = new HashMap<>(horizontalSegments.size() * 2);

        while (!frontier.isEmpty()) {
            String segment = frontier.remove();
            double value = 0;

            for (Map.Entry<String, SubdivisionToBilinearProblem.PredecessorRelation> entry : predecessors.get(segment).entrySet()) {
                double pred = variables.get(entry.getKey());

                switch (entry.getValue()) {
                    case STANDARD:
                        pred += featureSize;
                        break;
                    case SEA:
                        pred += settings.minimumSeaDimension;
                        break;
                    case BOTH:
                        pred += Math.max(featureSize, settings.minimumSeaDimension);
                        break;
                    default:
                        throw new InternalError("Unrecognized PredecessorRelation: " + entry.getValue());
                }

                value = Math.max(value, pred);
            }

            variables.put(segment, value);

            for (String successor : successors.get(segment)) {
                int nPredsLeft = unprocessedPredecessors.get(successor);
                nPredsLeft--;
                unprocessedPredecessors.put(successor, nPredsLeft);

                if (nPredsLeft == 0) {
                    frontier.add(successor);
                }
            }
        }

        return completeSolutionFromHorizontalSegments(variables, sub, settings, problem, segments, solver);
    }

    private static Solution completeSolutionFromHorizontalSegments(Map<String, Double> horizontalSegments, Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, LinearSolver solver) {
        Map<String, Double> variables = new HashMap<>(horizontalSegments);
        
        scaleHorizontalSegments(variables, sub, settings, segments);

        Solution sol = solver.solve(BilinearToLinear.restrictToLinear(problem, variables));

        if (sol.isInfeasible()) {
            return Solution.INFEASIBLE;
        }

        variables.putAll(sol);
        fixBorder(variables, sub, settings, segments);

        return new Solution(sol.getObjectiveValue(), variables);
    }

    private static void scaleToCartogramSize(Subdivision sub, CartogramSettings settings, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, Map<String, Double> variables) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                SegmentIdentification.FaceSegments s = segments.get(f);
                minX = Math.min(minX, variables.get(s.left));
                maxX = Math.max(maxX, variables.get(s.right));
                minY = Math.min(minY, variables.get(s.bottom));
                maxY = Math.max(maxY, variables.get(s.top));
            }
        }

        if (maxX - minX > settings.cartogramWidth || maxY - minY > settings.cartogramHeight) {
            throw new IllegalArgumentException("No cartogram can be constructed with these settings. Either increase the cartogram width and height or decrease the minimum separation.");
        }

        double xScale = settings.cartogramWidth / (maxX - minX);
        double yScale = settings.cartogramHeight / (maxY - minY);
        double xOffset = minX;
        double yOffset = minY;

        segments.entrySet().stream()
                .filter(e -> !e.getKey().isBoundary())
                .flatMap(e -> Arrays.asList(e.getValue().bottom, e.getValue().top).stream())
                .distinct()
                .forEach(s -> variables.compute(s, (key, val) -> yScale * (val - yOffset)));
        segments.entrySet().stream()
                .filter(e -> !e.getKey().isBoundary())
                .flatMap(e -> Arrays.asList(e.getValue().left, e.getValue().right).stream())
                .distinct()
                .forEach(s -> variables.compute(s, (key, val) -> xScale * (val - xOffset)));
    }

    private static void scaleHorizontalSegments(Map<String, Double> horizontalSegments, Subdivision sub, CartogramSettings settings, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                SegmentIdentification.FaceSegments s = segments.get(f);
                minY = Math.min(minY, horizontalSegments.get(s.bottom));
                maxY = Math.max(maxY, horizontalSegments.get(s.top));
            }
        }

        if (maxY - minY > settings.cartogramHeight) {
            throw new IllegalArgumentException("No cartogram can be constructed with these settings. Either increase the cartogram width and height or decrease the minimum separation.");
        }

        double yScale = settings.cartogramHeight / (maxY - minY);
        double yOffset = minY;

        segments.entrySet().stream()
                .filter(e -> !e.getKey().isBoundary())
                .flatMap(e -> Arrays.asList(e.getValue().bottom, e.getValue().top).stream())
                .distinct()
                .forEach(s -> horizontalSegments.compute(s, (key, val) -> yScale * (val - yOffset)));
    }

    private static void fixBorder(Map<String, Double> variables, Subdivision sub, CartogramSettings settings, Map<SubdivisionFace, FaceSegments> segments) {
        variables.put(segments.get(sub.getNorthFace()).top, settings.cartogramHeight + settings.boundaryWidth);
        variables.put(segments.get(sub.getSouthFace()).bottom, -settings.boundaryWidth);
        variables.put(segments.get(sub.getEastFace()).right, settings.cartogramWidth + settings.boundaryWidth);
        variables.put(segments.get(sub.getWestFace()).left, -settings.boundaryWidth);
    }

    private static void computeError(Map<String, Double> variables, CartogramSettings settings, MinimizationProblem problem) {
        double maxError = 0;

        for (Constraint constraint : problem.getConstraints()) {
            if (constraint instanceof Constraint.Bilinear) { // Only area constraints are bilinear
                Constraint.Bilinear areaConstraint = (Constraint.Bilinear) constraint;
                List<Pair<Double, Pair<String, String>>> areaTerms = areaConstraint.getBilinearTerms();
                String errorVar = areaConstraint.getLinearTerms().get(0).getSecond();
                double desiredArea = Math.abs(areaConstraint.getLinearTerms().get(0).getFirst()); // Not the right hand side - it might be affected by problem reduction techniques

                double area = areaTerms.stream()
                        .mapToDouble(t -> t.getFirst() * variables.get(t.getSecond().getFirst()) * variables.get(t.getSecond().getSecond()))
                        .sum();

                double error = Math.abs(area - desiredArea) / desiredArea + 0.01;
                if (error < 0) {
                    System.out.println("ERROR < 0: " + area + " " + desiredArea + " " + error);
                    System.out.println("Constraint: " + constraint);
                }
                variables.put(errorVar, error); // Increase to avoid infeasibility because of accuracy

                maxError = Math.max(maxError, error);
            }
        }

        if (settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR
                || settings.objective == CartogramSettings.Objective.MAX_AND_AVERAGE_ERROR_SQUARED
                || settings.objective == CartogramSettings.Objective.MAX_ERROR) {
            variables.put(SubdivisionToBilinearProblem.MAX_ERROR_VARIABLE_NAME, maxError);
        }
    }

    private FeasibleSolutionBuilder() {
    }
}
