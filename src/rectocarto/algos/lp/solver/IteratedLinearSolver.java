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
package rectocarto.algos.lp.solver;

import java.util.Map;
import java.util.Set;
import rectangularcartogram.data.Pair;
import rectocarto.algos.lp.BilinearToLinear;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

public class IteratedLinearSolver {

    private int nIterations = 50;
    private final LinearSolver solver;

    public IteratedLinearSolver(LinearSolver solver) {
        this.solver = solver;
    }

    public IteratedLinearSolver(LinearSolver solver, int nIterations) {
        this.solver = solver;
        this.nIterations = nIterations;
    }

    public int getnIterations() {
        return nIterations;
    }

    public void setnIterations(int nIterations) {
        this.nIterations = nIterations;
    }

    public Solution solve(MinimizationProblem bilinearProgram, Pair<Set<String>, Set<String>> variablePartition, Solution feasibleSolution) {
        if (bilinearProgram.getObjective() instanceof ObjectiveFunction.Quadratic && !(solver instanceof QuadraticSolver)) {
            throw new IllegalArgumentException("Quadratic program passed, while the underlying solver cannot solve quadratic programs.");
        }

        // Build a first partial solution from the feasible solution
        Solution lastSolution = new Solution(feasibleSolution.getObjectiveValue());
        for (String var : variablePartition.getFirst()) {
            lastSolution.put(var, feasibleSolution.get(var));
        }

        for (int i = 0; i < nIterations; i++) {
            lastSolution.keySet().retainAll(variablePartition.getFirst());
            System.out.println("Iteration " + i + "a. Last solution: " + lastSolution);
            lastSolution = solver.solve(BilinearToLinear.restrictToLinear(bilinearProgram, lastSolution));
            
            lastSolution.keySet().retainAll(variablePartition.getSecond());
            System.out.println("Iteration " + i + "b. Last solution: " + lastSolution);
            lastSolution = solver.solve(BilinearToLinear.restrictToLinear(bilinearProgram, lastSolution));
        }
        
        // Run a final iteration to build a complete solution
        lastSolution.keySet().retainAll(variablePartition.getFirst());
        Solution finalSolution = solver.solve(BilinearToLinear.restrictToLinear(bilinearProgram, lastSolution));
        finalSolution.putAll(lastSolution);
        
        for (Map.Entry<String, Double> entry : feasibleSolution.entrySet()) {
            finalSolution.putIfAbsent(entry.getKey(), entry.getValue());
        }
        
        System.out.println("Last solution: " + lastSolution);
        System.out.println("Final solution: " + finalSolution);

        return finalSolution;
    }
}
