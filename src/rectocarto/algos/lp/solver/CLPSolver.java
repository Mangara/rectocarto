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

import com.quantego.clp.CLP;
import com.quantego.clp.CLPExpression;
import com.quantego.clp.CLPVariable;
import java.util.HashMap;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

public class CLPSolver implements LinearSolver, QuadraticSolver {

    @Override
    public Solution solve(MinimizationProblem quadraticProgram) {
        Pair<CLP, Map<String, CLPVariable>> conversion = convertToCLP(quadraticProgram);
        CLP model = conversion.getFirst();
        Map<String, CLPVariable> variables = conversion.getSecond();

        return extractSolution(model, variables);
    }

    private Pair<CLP, Map<String, CLPVariable>> convertToCLP(MinimizationProblem quadraticProgram) {
        CLP model = new CLP().minimization();
        Map<String, CLPVariable> variables = new HashMap<>(quadraticProgram.getConstraints().size());

        // Add all the constraints (variables are added as-needed)
        for (Constraint constraint : quadraticProgram.getConstraints()) {
            Constraint.Linear linear = (Constraint.Linear) constraint;
            CLPExpression clpConstraint = model.createExpression();

            for (Pair<Double, String> term : linear.getTerms()) {
                clpConstraint.add(term.getFirst(), getOrAddVariable(term.getSecond(), variables, model));
            }

            switch (linear.getComparison()) {
                case EQUAL:
                    clpConstraint.eq(linear.getRightHandSide());
                    break;
                case LESS_THAN_OR_EQUAL:
                    clpConstraint.leq(linear.getRightHandSide());
                    break;
                case GREATER_THAN_OR_EQUAL:
                    clpConstraint.geq(linear.getRightHandSide());
                    break;
            }
        }

        // Set the objective function
        if (quadraticProgram.getObjective() instanceof ObjectiveFunction.Linear) {
            ObjectiveFunction.Linear objective = (ObjectiveFunction.Linear) quadraticProgram.getObjective();

            for (Pair<Double, String> term : objective.getTerms()) {
                variables.get(term.getSecond()).obj(term.getFirst());
            }
        } else {
            ObjectiveFunction.Quadratic objective = (ObjectiveFunction.Quadratic) quadraticProgram.getObjective();
            
            // TODO
            throw new Error("Not implemented yet.");
        }

        return new Pair<>(model, variables);
    }

    private CLPVariable getOrAddVariable(String varName, Map<String, CLPVariable> variables, CLP model) {
        CLPVariable clpVar = variables.get(varName);

        if (clpVar == null) {
            clpVar = model.addVariable().name(varName);
            variables.put(varName, clpVar);
        }

        return clpVar;
    }

    private Solution extractSolution(CLP model, Map<String, CLPVariable> variables) {
        CLP.STATUS returnStatus = model.solve();
        Solution sol;

        switch (returnStatus) {
            case ERROR:
                return null;
            case INFEASIBLE:
                return Solution.INFEASIBLE;
            case UNBOUNDED:
                sol = new Solution(Double.POSITIVE_INFINITY);
                break;
            case LIMIT: // fallthrough
            case OPTIMAL:
                sol = new Solution(model.getObjectiveValue());
                break;
            case UNKNOWN: // fallthrough
            default:
                return null;
        }

        for (Map.Entry<String, CLPVariable> entry : variables.entrySet()) {
            sol.put(entry.getKey(), model.getSolution(entry.getValue()));
        }

        return sol;
    }

}
