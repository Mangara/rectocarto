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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

public class IteratedLinearSolver implements BilinearSolver {

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

    @Override
    public Solution solve(MinimizationProblem bilinearProgram) {
        if (bilinearProgram.getObjective() instanceof ObjectiveFunction.Quadratic && !(solver instanceof QuadraticSolver)) {
            throw new IllegalArgumentException("Quadratic program passed, while the underlying solver cannot solve quadratic programs.");
        }

        for (int i = 0; i < nIterations; i++) {
            // TODO!
            // Fix half the variables
            // Solve for the others
            // Fix other half
            // Solve again

            Map<String, Double> variableAssignment = null;
            MinimizationProblem lp = restrictToLinear(bilinearProgram, variableAssignment);
            solver.solve(lp);
        }

        return null;
    }

    private MinimizationProblem restrictToLinear(MinimizationProblem bilinearProgram, Map<String, Double> variableAssignment) {
        MinimizationProblem linear = new MinimizationProblem();
        linear.setObjective(bilinearProgram.getObjective());

        for (Constraint c : bilinearProgram.getConstraints()) {
            if (c instanceof Constraint.Linear) {
                linear.addConstraint(c);
            } else if (c instanceof Constraint.Bilinear) {
                linear.addConstraint(restrictToLinear((Constraint.Bilinear) c, variableAssignment));
            } else {
                throw new AssertionError("Unexpected constraint type: " + c.getClass());
            }
        }

        return linear;
    }

    /**
     * Converts this bilinear constraint to a linear constraint by substituting
     * the given values for their variables. The caller should ensure that the
     * variable assignment includes at least one variable from each bilinear
     * term. Bilinear terms that simplify to linear terms in the same variable
     * are collected.
     *
     * @param constraint
     * @param variableAssignment
     * @return
     * @throws IllegalArgumentException If there is a bilinear term, both of
     * whose variables are not in the variable assignment.
     */
    public Constraint.Linear restrictToLinear(Constraint.Bilinear constraint, Map<String, Double> variableAssignment) {
        double newRightHandSide = constraint.getRightHandSide();
        List<Pair<Double, String>> newLinearTerms = new ArrayList<>();

        for (Pair<Double, String> linearTerm : constraint.getLinearTerms()) {
            if (variableAssignment.containsKey(linearTerm.getSecond())) {
                newRightHandSide -= linearTerm.getFirst() * variableAssignment.get(linearTerm.getSecond());
            } else {
                newLinearTerms.add(new Pair<>(linearTerm.getFirst(), linearTerm.getSecond()));
            }
        }

        for (Pair<Double, Pair<String, String>> bilinearTerm : constraint.getBilinearTerms()) {
            String var1 = bilinearTerm.getSecond().getFirst();
            String var2 = bilinearTerm.getSecond().getSecond();
            Pair<Double, String> newTerm = null;

            if (variableAssignment.containsKey(var1)) {
                if (variableAssignment.containsKey(var2)) {
                    newRightHandSide -= bilinearTerm.getFirst() * variableAssignment.get(var1) * variableAssignment.get(var2);
                } else {
                    newTerm = new Pair<>(bilinearTerm.getFirst() * variableAssignment.get(var1), var2);
                }
            } else if (variableAssignment.containsKey(var2)) {
                newTerm = new Pair<>(bilinearTerm.getFirst() * variableAssignment.get(var2), var1);
            } else {
                throw new IllegalArgumentException("The variable assignment must contain values for at least one of the variables of each bilinear term.");
            }

            if (newTerm != null) {
                boolean found = false;

                for (Pair<Double, String> newLinearTerm : newLinearTerms) {
                    if (newLinearTerm.getSecond().equals(newTerm.getSecond())) {
                        newLinearTerm.setFirst(newLinearTerm.getFirst() + newTerm.getFirst());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    newLinearTerms.add(newTerm);
                }
            }
        }

        return new Constraint.Linear(newLinearTerms, constraint.getComparison(), newRightHandSide);
    }

}
