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
package rectangularcartogram.data.lp;

import java.util.ArrayList;
import java.util.List;
import rectangularcartogram.data.Pair;

public abstract class Constraint {

    private final Comparison comparison;
    private final double rightHandSide;

    public Constraint(Comparison op, double rightHandSide) {
        this.comparison = op;
        this.rightHandSide = rightHandSide;
    }
    
    public class Linear extends Constraint {
        private final List<Pair<Double,String>> terms;

        public Linear(Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            terms = new ArrayList<>();
        }

        public List<Pair<Double, String>> getTerms() {
            return terms;
        }
        
        public void addTerm(double factor, String variable) {
            terms.add(new Pair<>(factor, variable));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            
            for (Pair<Double, String> term : terms) {
                if (first) {
                    sb.append(term.getFirst());
                    first = false;
                } else if (term.getFirst() < 0) {
                    sb.append(" - ").append(Double.toString(-1 * term.getFirst()));
                } else {
                    sb.append(" + ").append(term.getFirst());
                }
                
                sb.append(" ").append(term.getSecond()).append(" ");
            }
            
            sb.append(comparison).append(" ").append(Double.toString(rightHandSide));
            
            return sb.toString();
        }
    }

    public Comparison getComparison() {
        return comparison;
    }

    public double getRightHandSide() {
        return rightHandSide;
    }
    
    public enum Comparison {

        EQUAL, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL;

        @Override
        public String toString() {
            switch (this) {
                case EQUAL:
                    return "=";
                case LESS_THAN_OR_EQUAL:
                    return "<=";
                case GREATER_THAN_OR_EQUAL:
                    return ">=";
                default:
                    throw new InternalError("Incorrect comparison type - " + this.name());
            }
        }
    }
}
