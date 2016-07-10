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
import rectangularcartogram.algos.RELFusy;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectocarto.algos.lp.SubdivisionToBilinearProblem;
import rectocarto.algos.lp.solver.CLPSolver;
import rectocarto.algos.lp.solver.IteratedLinearSolver;
import rectocarto.data.CartogramSettings;
import rectocarto.data.lp.MinimizationProblem;

public class Test {

    public static void main(String[] args) throws IOException, IncorrectGraphException {
        Subdivision sub;
        try (BufferedReader in = Files.newBufferedReader(Paths.get("exampleData/Subdivisions/Simple.sub"))) {
            sub = Subdivision.load(in);
            (new RELFusy()).computeREL(sub.getDualGraph());
            
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
}
