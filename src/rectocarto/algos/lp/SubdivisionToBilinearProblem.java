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

        addBoundaryConstraints(problem, segments, sub, settings);
        addPlanarityConstraints(problem, segments, sub, settings);
        addAdjacencyConstraints(problem, segments, sub, settings);
        addAspectRatioConstraints(problem, segments, sub, settings);
        addAreaConstraints(problem, segments, sub, settings);
        
        return problem;
    }

    private static void checkForIssues(Subdivision sub, CartogramSettings settings) {
        if (sub.getDualGraph().getRegularEdgeLabeling() == null) {
            throw new IllegalArgumentException("The subdivision must have a valid regular edge labeling.");
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
        
        switch (settings.objective) {
            case MAX_ERROR:
                lin = new ObjectiveFunction.Linear();
                lin.addTerm(1, "E_MAX");
                return lin;
            case AVERAGE_ERROR:
                lin = new ObjectiveFunction.Linear();
                for (int i = 0; i < sub.getTopLevelFaces().size(); i++) {
                    lin.addTerm(1, "E_" + i);
                }
                return lin;
            case MAX_AND_AVERAGE_ERROR:
                lin = new ObjectiveFunction.Linear();
                lin.addTerm(sub.getTopLevelFaces().size(), "E_MAX");
                for (int i = 0; i < sub.getTopLevelFaces().size(); i++) {
                    lin.addTerm(1, "E_" + i);
                }
                return lin;
            case AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();
                for (int i = 0; i < sub.getTopLevelFaces().size(); i++) {
                    quad.addQuadraticTerm(1, "E_" + i);
                }
                return quad;
            case MAX_AND_AVERAGE_ERROR_SQUARED:
                quad = new ObjectiveFunction.Quadratic();
                quad.addLinearTerm(sub.getTopLevelFaces().size(), "E_MAX");
                for (int i = 0; i < sub.getTopLevelFaces().size(); i++) {
                    quad.addQuadraticTerm(1, "E_" + i);
                }
                return quad;
            default:
                throw new IllegalArgumentException("Unexpected objective function type: " + settings.objective);
        }
    }
    
    private static void addBoundaryConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        
    }
    
    private static void addPlanarityConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        for (SubdivisionFace f : sub.getTopLevelFaces()) {
            if ("NORTH".equals(f.getName()) || "EAST".equals(f.getName()) || "SOUTH".equals(f.getName()) || "WEST".equals(f.getName())) {
                continue;
            }
            
            double sep = (f.isSea() ? settings.minimumSeaDimension : settings.minimumSeparation);
            
            // f.right - f.left => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segments.get(f).right.name), 
                    new Pair<>(-1d, segments.get(f).left.name)), 
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL, 
                    sep));
            // f.top - f.bottom => eps
            problem.addConstraint(new Constraint.Linear(Arrays.asList(
                    new Pair<>(1d, segments.get(f).top.name), 
                    new Pair<>(-1d, segments.get(f).bottom.name)), 
                    Constraint.Comparison.GREATER_THAN_OR_EQUAL, 
                    sep));
        }
    }

    private static void addAdjacencyConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        
    }

    private static void addAspectRatioConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        
    }

    private static void addAreaConstraints(MinimizationProblem problem, Map<SubdivisionFace, FaceSegments> segments, Subdivision sub, CartogramSettings settings) {
        
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
