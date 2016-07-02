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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class ProblemReduction {

    /**
     * Simplifies a given problem by (iteratively) finding all equality
     * constraints in one variable and substituting the solution into other
     * constraints that reference that variable.
     *
     * @param problem
     * @return
     */
    public static MinimizationProblem substituteFixedVariables(MinimizationProblem problem) {
        boolean nothingChanged = false;
        MinimizationProblem p = problem;

        while (!nothingChanged) {
            // Find the values
            Map<String, Double> fixedValues = new HashMap<>();

            for (Constraint constraint : p.getConstraints()) {
                if (constraint.getComparison() == Constraint.Comparison.EQUAL) {
                    extractFixedValue(constraint, fixedValues);
                }
            }

            if (fixedValues.isEmpty()) {
                nothingChanged = true;
            } else {
                // Substitute the values
                MinimizationProblem result = new MinimizationProblem();

                result.setObjective(substituteFixedValues(p.getObjective(), fixedValues));

                for (Constraint constraint : p.getConstraints()) {
                    Constraint substituted = substituteFixedValues(constraint, fixedValues);

                    if (substituted != null) {
                        result.addConstraint(substituted);
                    }
                }

                p = result;
            }
        }

        return p;
    }

    private static void extractFixedValue(Constraint constraint, Map<String, Double> fixedValues) {
        if (constraint instanceof Constraint.Linear) {
            Constraint.Linear lin = (Constraint.Linear) constraint;

            if (lin.getTerms().size() == 1) {
                Pair<Double, String> term = lin.getTerms().get(0);
                fixedValues.put(term.getSecond(), lin.getRightHandSide() / term.getFirst());
            }
        } else if (constraint instanceof Constraint.Bilinear) {
            Constraint.Bilinear bilin = (Constraint.Bilinear) constraint;

            if (bilin.getLinearTerms().size() == 1 && bilin.getBilinearTerms().isEmpty()) {
                Pair<Double, String> term = bilin.getLinearTerms().get(0);
                fixedValues.put(term.getSecond(), bilin.getRightHandSide() / term.getFirst());
            } else if (bilin.getLinearTerms().isEmpty() && bilin.getBilinearTerms().size() == 1) {
                Pair<Double, Pair<String, String>> term = bilin.getBilinearTerms().get(0);

                if (term.getSecond().getFirst().equals(term.getSecond().getSecond())) {
                    fixedValues.put(term.getSecond().getFirst(), Math.sqrt(bilin.getRightHandSide() / term.getFirst()));
                }
            }
        } else {
            throw new IllegalArgumentException("Unexpected constraint type: " + constraint);
        }
    }

    private static ObjectiveFunction substituteFixedValues(ObjectiveFunction objective, Map<String, Double> fixedValues) {
        // Remove all fixed variables
        if (objective instanceof ObjectiveFunction.Linear) {
            ObjectiveFunction.Linear linear = (ObjectiveFunction.Linear) objective;
            ObjectiveFunction.Linear result = new ObjectiveFunction.Linear();

            for (Pair<Double, String> term : linear.getTerms()) {
                if (!fixedValues.containsKey(term.getSecond())) {
                    result.addTerm(term.getFirst(), term.getSecond());
                }
            }

            return result;
        } else if (objective instanceof ObjectiveFunction.Quadratic) {
            ObjectiveFunction.Quadratic result = new ObjectiveFunction.Quadratic();
            ObjectiveFunction.Quadratic quadratic = (ObjectiveFunction.Quadratic) objective;

            for (Pair<Double, String> term : quadratic.getLinearTerms()) {
                if (!fixedValues.containsKey(term.getSecond())) {
                    result.addLinearTerm(term.getFirst(), term.getSecond());
                }
            }

            for (Pair<Double, String> term : quadratic.getQuadraticTerms()) {
                if (!fixedValues.containsKey(term.getSecond())) {
                    result.addQuadraticTerm(term.getFirst(), term.getSecond());
                }
            }

            return result;
        } else {
            throw new IllegalArgumentException("Unexpected objective function type: " + objective);
        }
    }

    private static Constraint substituteFixedValues(Constraint constraint, Map<String, Double> fixedValues) {
        if (constraint instanceof Constraint.Linear) {
            Constraint.Linear lin = (Constraint.Linear) constraint;
            double newRightHand = lin.getRightHandSide();
            List<Pair<Double, String>> newTerms = new ArrayList<>();

            for (Pair<Double, String> term : lin.getTerms()) {
                if (fixedValues.containsKey(term.getSecond())) {
                    newRightHand -= term.getFirst() * fixedValues.get(term.getSecond());
                } else {
                    newTerms.add(new Pair<>(term.getFirst(), term.getSecond()));
                }
            }

            return (newTerms.isEmpty() ? null : new Constraint.Linear(newTerms, lin.getComparison(), newRightHand));
        } else if (constraint instanceof Constraint.Bilinear) {
            Constraint.Bilinear bilin = (Constraint.Bilinear) constraint;
            double newRightHand = bilin.getRightHandSide();
            List<Pair<Double, String>> newLinearTerms = new ArrayList<>();
            List<Pair<Double, Pair<String, String>>> newBilinearTerms = new ArrayList<>();

            for (Pair<Double, String> term : bilin.getLinearTerms()) {
                if (fixedValues.containsKey(term.getSecond())) {
                    newRightHand -= term.getFirst() * fixedValues.get(term.getSecond());
                } else {
                    newLinearTerms.add(new Pair<>(term.getFirst(), term.getSecond()));
                }
            }

            for (Pair<Double, Pair<String, String>> term : bilin.getBilinearTerms()) {
                if (fixedValues.containsKey(term.getSecond().getFirst())) {
                    if (fixedValues.containsKey(term.getSecond().getSecond())) {
                        newRightHand -= term.getFirst() * fixedValues.get(term.getSecond().getFirst()) * fixedValues.get(term.getSecond().getSecond());
                    } else {
                        newLinearTerms.add(new Pair<>(term.getFirst() * fixedValues.get(term.getSecond().getFirst()), term.getSecond().getSecond()));
                    }
                } else {
                    if (fixedValues.containsKey(term.getSecond().getSecond())) {
                        newLinearTerms.add(new Pair<>(term.getFirst() * fixedValues.get(term.getSecond().getSecond()), term.getSecond().getFirst()));
                    } else {
                        newBilinearTerms.add(new Pair<>(term.getFirst(), new Pair<>(term.getSecond().getFirst(), term.getSecond().getSecond())));
                    }
                }
            }

            return (newLinearTerms.isEmpty() && newBilinearTerms.isEmpty() ? null : new Constraint.Bilinear(newLinearTerms, newBilinearTerms, bilin.getComparison(), newRightHand));
        } else {
            throw new IllegalArgumentException("Unexpected constraint type: " + constraint);
        }
    }
}
