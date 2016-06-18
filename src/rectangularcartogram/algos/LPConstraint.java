/*
 * Copyright 2010-2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectangularcartogram.algos;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.Pair;

public class LPConstraint {

    public enum Comparison {

        EQUAL, LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL;

        @Override
        public String toString() {
            switch (this) {
                case EQUAL:
                    return "=";
                case LESS_THAN:
                    return "<";
                case LESS_THAN_EQUAL:
                    return "<=";
                case GREATER_THAN:
                    return ">";
                case GREATER_THAN_EQUAL:
                    return ">=";
                default:
                    return "??";
            }
        }
    }
    private double rightHandSide;
    private List<Pair<String, Double>> variables;
    private Comparison type;

    public LPConstraint() {
        variables = new ArrayList<Pair<String, Double>>(2);
    }

    public double getRightHandSide() {
        return rightHandSide;
    }

    public void setRightHandSide(double rightHandSide) {
        this.rightHandSide = rightHandSide;
    }

    public List<Pair<String, Double>> getVariables() {
        return variables;
    }

    public void setVariables(List<Pair<String, Double>> variables) {
        this.variables = variables;
    }

    public void addVariable(String variable, double factor) {
        variables.add(new Pair<String, Double>(variable, factor));
    }

    public Comparison getType() {
        return type;
    }

    public void setType(Comparison type) {
        this.type = type;
    }

    public void addToCPLEX(IloCplex cplex, Map<String, IloNumVar> cplexVariables) throws IloException {
        // Build the linear expression that forms the left-hand side of the constraint
        IloLinearNumExpr expr = cplex.linearNumExpr();

        for (Pair<String, Double> pair : variables) {
            // Check if our variable exists already and if not, create it
            IloNumVar var = cplexVariables.get(pair.getFirst());

            if (var == null) {
                var = cplex.numVar(0, Double.MAX_VALUE, pair.getFirst());
                cplexVariables.put(pair.getFirst(), var);
            }

            expr.addTerm(var, pair.getSecond());
        }

        // Add the correct type of constraint to cplex
        switch (type) {
            case EQUAL:
                cplex.addEq(expr, rightHandSide);
                break;
            case GREATER_THAN:
                throw new AssertionError("Can't add strictly greater than!");
            case GREATER_THAN_EQUAL:
                cplex.addGe(expr, rightHandSide);
                break;
            case LESS_THAN:
                throw new AssertionError("Can't add strictly less than!");
            case LESS_THAN_EQUAL:
                cplex.addLe(expr, rightHandSide);
                break;
            default:
                throw new AssertionError("Unknown type of equation!");
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        boolean first = true;

        for (Pair<String, Double> var : variables) {
            if (var.getSecond() != 0) {
                if (var.getSecond() > 0) {
                    if (!first) {
                        sb.append(" + ");
                    }
                } else {
                    sb.append(" - ");
                }

                first = false;

                if (Math.abs(var.getSecond()) != 1) {
                    sb.append(Math.abs(var.getSecond()));
                    sb.append(" ");
                }

                sb.append(var.getFirst());
            }
        }

        sb.append(" ");
        sb.append(type.toString());
        sb.append(" ");

        sb.append(rightHandSide);

        return sb.toString();
    }
}
