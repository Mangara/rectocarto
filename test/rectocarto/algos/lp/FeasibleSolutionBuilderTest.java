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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rectangularcartogram.algos.MinimumLabelingComputer;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.ipe.IPEExporter;
import rectocarto.algos.lp.solver.CLPSolver;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class FeasibleSolutionBuilderTest {

    private static final double[] minimumSeparation = new double[]{0.1, 1, 10, 25};
    private static final double[] minimumSeaDimension = new double[]{1, 25, 50};
    private static final double[] maximumAspectRatio = new double[]{2, 5, 10, 20};
    private static final double[] cartogramsize = new double[]{100, 200, 400, 800, 1600};

    private static List<CartogramSettings> allSettingCombinations;

    public FeasibleSolutionBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        allSettingCombinations = new ArrayList<>();

        for (double mf : minimumSeparation) {
            for (double ms : minimumSeaDimension) {
                for (double mar : maximumAspectRatio) {
                    for (double s : cartogramsize) {
                        CartogramSettings settings = new CartogramSettings();
                        settings.cartogramHeight = 3 * s;
                        settings.cartogramWidth = 4 * s;
                        settings.maximumAspectRatio = mar;
                        settings.minimumFeatureSize = mf;
                        settings.minimumSeaDimension = ms;
                        allSettingCombinations.add(settings);
                    }
                }
            }
        }
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static final String[] maps = new String[]{
        //"exampleData/Subdivisions/Simple.sub",
        "exampleData/Subdivisions/Europe.sub",
        "exampleData/Subdivisions/Netherlands Area.sub",
        "exampleData/Subdivisions/World.sub"
    };
    
    /**
     * Test of constructFeasibleSolution1 method, of class
     * FeasibleSolutionBuilder.
     */
    //@Test
    public void testConstructFeasibleSolution1() throws IOException, IncorrectGraphException {
        System.out.println("constructFeasibleSolution1 - old method"); // 132 feasible

        for (String map : maps) {
            System.out.println();
            System.out.println("Map: " + map);
            try (BufferedReader in = Files.newBufferedReader(Paths.get(map))) {
                Subdivision sub = Subdivision.load(in);
                sub.getDualGraph().setRegularEdgeLabeling(MinimumLabelingComputer.getMinimalLabeling(sub.getDualGraph()));

                for (CartogramSettings settings : allSettingCombinations) {
                    SubdivisionToBilinearProblem s2bp = new SubdivisionToBilinearProblem(sub, settings);
                    MinimizationProblem problem = s2bp.getProblem();

                    try {
                        Solution sol = FeasibleSolutionBuilder.constructFeasibleSolution1(sub, settings, problem, s2bp.segments);

                        //(new IPEExporter()).exportIPEFile(Paths.get("temp.ipe").toFile(), getCartogram(sub, sol, s2bp.segments), false);
                        if (!testFeasibility(sol, problem)) {
                            System.out.println("Infeasible.");
                        } else {
                            System.out.println("Feasible");
                        }
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().startsWith("No cartogram")) {
                            System.out.println("Infeasible. (Exception)");
                            continue;
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }
    
    @Test
    public void testConstructFeasibleSolution3() throws IOException, IncorrectGraphException {
        System.out.println("constructFeasibleSolution3 - newer method"); // 152 feasible

        for (String map : maps) {
            System.out.println();
            System.out.println("Map: " + map);
            try (BufferedReader in = Files.newBufferedReader(Paths.get(map))) {
                Subdivision sub = Subdivision.load(in);
                sub.getDualGraph().setRegularEdgeLabeling(MinimumLabelingComputer.getMinimalLabeling(sub.getDualGraph()));

                for (CartogramSettings settings : allSettingCombinations) {
                    SubdivisionToBilinearProblem s2bp = new SubdivisionToBilinearProblem(sub, settings);
                    MinimizationProblem problem = s2bp.getProblem();

                    try {
                        Solution sol = FeasibleSolutionBuilder.constructFeasibleSolution3(sub, settings, problem, s2bp.segments, s2bp.predecessors, s2bp.successors, new CLPSolver());

                        if (sol != Solution.INFEASIBLE) {
                            //(new IPEExporter()).exportIPEFile(Paths.get("temp.ipe").toFile(), getCartogram(sub, sol, s2bp.segments), false);
                        }

                        if (sol == Solution.INFEASIBLE) {
                            System.out.println("Infeasible.");
                        } else if (!testFeasibility(sol, problem)) {
                            System.out.println("Solution returned was actually infeasible.");
                        } else {
                            System.out.println("Feasible");
                        }
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().startsWith("No cartogram")) {
                            System.out.println("Infeasible. (Exception)");
                            continue;
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private boolean testFeasibility(Solution sol, MinimizationProblem problem) {
        if (sol == Solution.INFEASIBLE) {
            return false;
        }

        for (Constraint constraint : problem.getConstraints()) {
            if (!constraintIsSatisfied(constraint, sol)) {
                System.out.println("Constraint " + constraint + " was violated.");
                System.out.println("Solution: " + sol);
                return false;
            }
        }

        return true;
    }

    private static final double EPSILON = 0.0000001;

    private boolean constraintIsSatisfied(Constraint constraint, Solution sol) {
        double val;

        if (constraint instanceof Constraint.Linear) {
            val = ((Constraint.Linear) constraint).getTerms().stream().mapToDouble(t -> t.getFirst() * sol.get(t.getSecond())).sum();
        } else if (constraint instanceof Constraint.Bilinear) {
            val = ((Constraint.Bilinear) constraint).getLinearTerms().stream().mapToDouble(t -> t.getFirst() * sol.get(t.getSecond())).sum();
            val += ((Constraint.Bilinear) constraint).getBilinearTerms().stream().mapToDouble(t -> t.getFirst() * sol.get(t.getSecond().getFirst()) * sol.get(t.getSecond().getSecond())).sum();
        } else {
            throw new IllegalArgumentException("Unexpected constraint type: " + constraint);
        }

        //System.out.println("Constraint: " + constraint);
        //System.out.println("LHS: " + val);
        
        switch (constraint.getComparison()) {
            case EQUAL:
                return Math.abs(val - constraint.getRightHandSide()) < EPSILON;
            case GREATER_THAN_OR_EQUAL:
                return val >= constraint.getRightHandSide() - EPSILON;
            case LESS_THAN_OR_EQUAL:
                return val <= constraint.getRightHandSide() + EPSILON;
            default:
                throw new IllegalArgumentException("Unexpected comparison: " + constraint.getComparison());
        }
    }

    private Subdivision getCartogram(Subdivision sub, Solution sol, Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments) throws IncorrectGraphException {
        Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> dual = (new RectangularDualDrawer()).drawSubdivision(sub, true);

        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            ArrayList<Vertex> corners = new ArrayList<>(4);

            double left = sol.get(segments.get(face).left);
            double right = sol.get(segments.get(face).right);
            double bottom = sol.get(segments.get(face).bottom);
            double top = sol.get(segments.get(face).top);

            corners.add(new Vertex(left, bottom));
            corners.add(new Vertex(right, bottom));
            corners.add(new Vertex(right, top));
            corners.add(new Vertex(left, top));

            dual.getSecond().get(face).setVertices(corners);
        }

        return dual.getFirst();
    }
}
