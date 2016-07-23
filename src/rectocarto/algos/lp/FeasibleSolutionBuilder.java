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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Edge;
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

    static Solution constructFeasibleSolution1(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        try {
            Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> dual = (new RectangularDualDrawer()).drawSubdivision(sub, true);
            Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

            for (SubdivisionFace face : sub.getTopLevelFaces()) {
                FaceSegments s = segments.get(face);
                List<Vertex> vertices = dual.getSecond().get(face).getVertices();
                variables.put(s.left.name, vertices.stream().mapToDouble(Vertex::getX).min().getAsDouble());
                variables.put(s.right.name, vertices.stream().mapToDouble(Vertex::getX).max().getAsDouble());
                variables.put(s.bottom.name, vertices.stream().mapToDouble(Vertex::getY).min().getAsDouble());
                variables.put(s.top.name, vertices.stream().mapToDouble(Vertex::getY).max().getAsDouble());
            }

            scaleToCartogramSize(sub, settings, segments, variables);
            fixBorder(variables, segments, sub, settings);
            computeError(variables, settings, problem);

            return new Solution(problem.getObjective().evaluate(variables), variables);
        } catch (IncorrectGraphException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    static Solution constructFeasibleSolution2(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, LinearSolver solver) {
        Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

        System.out.println("Problem: " + problem);

        // Build a feasible solution for horizontal segments, then solve the remaining LP
        buildFeasibleHorizontalSolution(variables, sub, settings, problem, segments);
        
        System.out.println("variables after step 1: " + variables);
        
        MinimizationProblem lp = BilinearToLinear.restrictToLinear(problem, variables);
        
        System.out.println("Reduced LP:");
        System.out.println(lp);
        
        Solution sol = solver.solve(lp);
        
        System.out.println("variables after LP: " + sol);
        
        if (sol.isInfeasible()) {
            System.out.println("Infeasible.");
        } else if (sol.isUnbounded()) {
            System.out.println("Unbounded.");
        }

        // If this doesn't work, repeat for the vertical segments
        // If both don't work, give up
        // toplogical sort on dual of st-graphs, incrementing by minimumSeparation or minimumSeaDimension
        fixBorder(variables, segments, sub, settings);
        computeError(variables, settings, problem);

        return new Solution(problem.getObjective().evaluate(variables), variables);
    }

    private static void buildFeasibleHorizontalSolution(Map<String, Double> variables, Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments) {
        Set<String> horizontalSegments = segments.entrySet().stream()
                .filter(e -> !e.getKey().isBoundary())
                .flatMap(e -> Arrays.asList(e.getValue().bottom.name, e.getValue().top.name).stream())
                .collect(Collectors.toSet());

        Set<String> fixedSegments = new HashSet<>();
        HashMap<String, List<String>> successors = new HashMap<>();
        HashMap<String, List<Pair<String, Pair<Double, Double>>>> predecessors = computePredecessorsAndFixedSegments(problem, horizontalSegments, variables, fixedSegments, successors);

        HashMap<String, Integer> nUnmarkedPredecessors = new HashMap<>(2 * horizontalSegments.size());
        Queue<String> nextSegments = new LinkedList<>(); // Vertices with no unmarked predecessors, but which haven't been numbered themselves

        // Compute the number of predecessors for each segment and add those with no predecessors to the queue
        for (String s : horizontalSegments) {
            int pred = predecessors.getOrDefault(s, Collections.<Pair<String, Pair<Double, Double>>>emptyList()).size();

            nUnmarkedPredecessors.put(s, pred);

            if (pred == 0 || fixedSegments.contains(s)) {
                nextSegments.add(s);
            }
        }

        while (!nextSegments.isEmpty()) {
            String seg = nextSegments.remove();

            if (!fixedSegments.contains(seg)) {
                // Find the smallest double that satisfies all predecessor constraints
                double lowerBound = 0;

                for (Pair<String, Pair<Double, Double>> pred : predecessors.getOrDefault(seg, Collections.<Pair<String, Pair<Double, Double>>>emptyList())) {
                    double val = pred.getSecond().getFirst() * variables.get(pred.getFirst()) + pred.getSecond().getSecond();
                    lowerBound = Math.max(lowerBound, val);
                }

                variables.put(seg, lowerBound);
            }

            // Let successors know that this segment now has a value
            for (String succ : successors.getOrDefault(seg, Collections.<String>emptyList())) {
                int remainingPredecessors = nUnmarkedPredecessors.get(succ);
                nUnmarkedPredecessors.put(succ, remainingPredecessors - 1);

                if (remainingPredecessors == 1) {
                    nextSegments.add(succ);
                }
            }
        }
    }

    private static HashMap<String, List<Pair<String, Pair<Double, Double>>>> computePredecessorsAndFixedSegments(MinimizationProblem problem, Set<String> horizontalSegments, Map<String, Double> variables, Set<String> fixedSegments, HashMap<String, List<String>> successors) {
        HashMap<String, List<Pair<String, Pair<Double, Double>>>> predecessors = new HashMap<>();

        for (Constraint constraint : problem.getConstraints()) {
            if (constraint instanceof Constraint.Linear) {
                Constraint.Linear linear = (Constraint.Linear) constraint;

                if (linear.getTerms().size() == 1) {
                    String var = linear.getTerms().get(0).getSecond();

                    if (horizontalSegments.contains(var)) {
                        double val = linear.getRightHandSide() / linear.getTerms().get(0).getFirst();

                        if (linear.getComparison() == Constraint.Comparison.EQUAL) {
                            variables.put(var, val);
                            fixedSegments.add(var);
                        } else if (linear.getComparison() == Constraint.Comparison.GREATER_THAN_OR_EQUAL && !fixedSegments.contains(var)) {
                            variables.merge(var, val, Math::max);
                        }
                    }
                } else if (linear.getTerms().size() == 2) {
                    String var1 = linear.getTerms().get(0).getSecond();
                    String var2 = linear.getTerms().get(1).getSecond();

                    if (horizontalSegments.contains(var1) && horizontalSegments.contains(var2)) {
                        double f1 = linear.getTerms().get(0).getFirst();
                        double f2 = linear.getTerms().get(1).getFirst();

                        if (linear.getComparison() == Constraint.Comparison.GREATER_THAN_OR_EQUAL) {
                            // f1 var1 + f2 var2 >= rh

                            if (f1 < 0 && f2 > 0 && !fixedSegments.contains(var2)) {
                                // Change to "var2 >= factor var1 + constant"
                                double factor = -f1 / f2;
                                double constant = linear.getRightHandSide() / f2;

                                addPredecessor(predecessors, successors, var2, var1, factor, constant);
                            } else if (f2 < 0 && f1 > 0 && !fixedSegments.contains(var1)) {
                                // Change to "var1 >= factor var2 + constant"
                                double factor = -f2 / f1;
                                double constant = linear.getRightHandSide() / f1;

                                addPredecessor(predecessors, successors, var1, var2, factor, constant);
                            }
                        } else if (linear.getComparison() == Constraint.Comparison.LESS_THAN_OR_EQUAL) {
                            // f1 var1 + f2 var2 <= rh

                            if (f1 < 0 && f2 > 0 && !fixedSegments.contains(var1)) {
                                // Change to "var1 >= factor var2 + constant"
                                double factor = f2 / f1;
                                double constant = -linear.getRightHandSide() / f1;

                                addPredecessor(predecessors, successors, var1, var2, factor, constant);
                            } else if (f2 < 0 && f1 > 0 && !fixedSegments.contains(var2)) {
                                // Change to "var2 >= factor var1 + constant"
                                double factor = f1 / f2;
                                double constant = -linear.getRightHandSide() / f2;

                                addPredecessor(predecessors, successors, var2, var1, factor, constant);
                            }
                        }
                    }
                }
            }
        }

        return predecessors;
    }

    /**
     * Adds the predecessor relationship that "successor >= factor * predecessor
     * + constant". Also adds successor as a successor to predecessor.
     *
     * @param predecessors
     * @param successors
     * @param successor
     * @param predecessor
     * @param factor
     * @param constant
     */
    private static void addPredecessor(HashMap<String, List<Pair<String, Pair<Double, Double>>>> predecessors, HashMap<String, List<String>> successors, String successor, String predecessor, double factor, double constant) {
        List<Pair<String, Pair<Double, Double>>> preds = predecessors.get(successor);

        if (preds == null) {
            preds = new ArrayList<>();
            predecessors.put(successor, preds);
        }

        preds.add(new Pair<>(predecessor, new Pair<>(factor, constant)));

        List<String> succs = successors.get(predecessor);

        if (succs == null) {
            succs = new ArrayList<>();
            successors.put(predecessor, succs);
        }

        succs.add(successor);
    }

    private static void scaleToCartogramSize(Subdivision sub, CartogramSettings settings, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments, Map<String, Double> variables) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if (!f.isBoundary()) {
                SegmentIdentification.FaceSegments s = segments.get(f);
                minX = Math.min(minX, variables.get(s.left.name));
                maxX = Math.max(maxX, variables.get(s.right.name));
                minY = Math.min(minY, variables.get(s.bottom.name));
                maxY = Math.max(maxY, variables.get(s.top.name));
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
                .flatMap(e -> Arrays.asList(e.getValue().bottom.name, e.getValue().top.name).stream())
                .distinct()
                .forEach(s -> variables.compute(s, (key, val) -> yScale * (val - yOffset)));
        segments.entrySet().stream()
                .filter(e -> !e.getKey().isBoundary())
                .flatMap(e -> Arrays.asList(e.getValue().left.name, e.getValue().right.name).stream())
                .distinct()
                .forEach(s -> variables.compute(s, (key, val) -> xScale * (val - xOffset)));
    }

    private static void fixBorder(Map<String, Double> variables, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        variables.put(segments.get(sub.getNorthFace()).top.name, settings.cartogramHeight + settings.boundaryWidth);
        variables.put(segments.get(sub.getSouthFace()).bottom.name, -settings.boundaryWidth);
        variables.put(segments.get(sub.getEastFace()).right.name, settings.cartogramWidth + settings.boundaryWidth);
        variables.put(segments.get(sub.getWestFace()).left.name, -settings.boundaryWidth);
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
