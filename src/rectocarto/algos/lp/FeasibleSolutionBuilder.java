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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectocarto.algos.lp.SegmentIdentification.FaceSegments;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

class FeasibleSolutionBuilder {

    static Solution constructFeasibleSolution1(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        try {
            Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> dual = (new RectangularDualDrawer()).drawSubdivision(sub, true);
            Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

            for (SubdivisionFace face : dual.getFirst().getFaces()) {
                FaceSegments s = segments.get(dual.getSecond().get(face));
                variables.put(s.left.name, face.getVertices().stream().mapToDouble(Vertex::getX).min().getAsDouble());
                variables.put(s.right.name, face.getVertices().stream().mapToDouble(Vertex::getX).max().getAsDouble());
                variables.put(s.bottom.name, face.getVertices().stream().mapToDouble(Vertex::getY).min().getAsDouble());
                variables.put(s.top.name, face.getVertices().stream().mapToDouble(Vertex::getY).max().getAsDouble());
            }

            computeError(variables, settings, problem);

            return new Solution(problem.getObjective().evaluate(variables), variables);
        } catch (IncorrectGraphException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    static Solution constructFeasibleSolution2(Subdivision sub, CartogramSettings settings, MinimizationProblem problem, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) {
        Map<String, Double> variables = new HashMap<>(2 * sub.getTopLevelFaces().size());

        // toplogical sort on dual of st-graphs, incrementing by minimumSeparation or minimumSeaDimension
        scaleToCartogramSize(sub, settings, segments, variables);
        computeError(variables, settings, problem);

        return new Solution(problem.getObjective().evaluate(variables), variables);
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
                SegmentIdentification.FaceSegments s = segments.get(f);
                variables.compute(s.left.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.right.name, (key, val) -> settings.boundaryWidth + xScale * val);
                variables.compute(s.bottom.name, (key, val) -> settings.boundaryWidth + yScale * val);
                variables.compute(s.top.name, (key, val) -> settings.boundaryWidth + yScale * val);
            }
        }
    }

    private static void computeError(Map<String, Double> variables, CartogramSettings settings, MinimizationProblem problem) {
        double maxError = 0;
        
        for (Constraint constraint : problem.getConstraints()) {
            if (constraint instanceof Constraint.Bilinear) { // Only area constraints are bilinear
                Constraint.Bilinear areaConstraint = (Constraint.Bilinear) constraint;
                List<Pair<Double, Pair<String, String>>> areaTerms = areaConstraint.getBilinearTerms();
                String errorVar = areaConstraint.getLinearTerms().get(0).getSecond();
                double desiredArea = areaConstraint.getRightHandSide();

                double area = areaTerms.stream()
                        .mapToDouble(t -> t.getFirst() * variables.get(t.getSecond().getFirst()) * variables.get(t.getSecond().getSecond()))
                        .sum();

                double error = Math.abs(area - desiredArea) / desiredArea + 0.01;
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
