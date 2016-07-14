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
package rectocarto;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.algos.ga.LabelingGA;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectocarto.algos.lp.SegmentIdentification;
import rectocarto.algos.lp.SubdivisionToBilinearProblem;
import rectocarto.algos.lp.solver.CLPSolver;
import rectocarto.algos.lp.solver.IteratedLinearSolver;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.MinimizationProblem;

public class Test {

    public static void main(String[] args) throws IOException, IncorrectGraphException {
        generateSimpleCartogramLP();
    }
    
    private static void generateSimpleCartogramLP() throws IOException, IncorrectGraphException {
        try (BufferedReader in = Files.newBufferedReader(Paths.get("exampleData/Subdivisions/Simple.sub"))) {
            // Load a subdivision
            Subdivision sub = Subdivision.load(in);
            computeDecentREL(sub);

            SubdivisionToBilinearProblem builder = new SubdivisionToBilinearProblem(sub, new CartogramSettings());
            MinimizationProblem p = builder.getProblem();
            System.out.println(p);
            System.out.println(p.getConstraints().size() + " constraints");
            System.out.println();
            System.out.println("Feasible solution: ");
            System.out.println(builder.getFeasibleSolution());

            IteratedLinearSolver solver = new IteratedLinearSolver(new CLPSolver());
            solver.solve(p, new Pair<>(builder.getHorizontalSegmentVariables(), builder.getVerticalSegmentVariables()), builder.getFeasibleSolution());
        }
    }

    private static void computeDecentREL(Subdivision sub) throws IncorrectGraphException {
        // Run a genetic algorithm to look for a REL with good Bounding Box separation Distance
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), new BoundingBoxSeparationMeasure(sub));
        LabelingGA.DEBUG_LEVEL = 0;
        ga.setElitistFraction(0.04);
        ga.setCrossoverChance(0.05);
        ga.setMutationChance(0.9);
        ga.setSelection(new RankSelection(0.9));
        
        int maxGenerations = 50;
        int populationSize = 50;
        
        ga.initialize(populationSize);
        Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);
        sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());
    }
}
