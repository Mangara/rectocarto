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
package rectocarto.data.lp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import rectangularcartogram.data.Pair;

public abstract class Constraint {

    private final Comparison comparison;
    private final double rightHandSide;

    public Constraint(Comparison op, double rightHandSide) {
        this.comparison = op;
        this.rightHandSide = rightHandSide;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.comparison);
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.rightHandSide) ^ (Double.doubleToLongBits(this.rightHandSide) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Constraint other = (Constraint) obj;
        if (this.comparison != other.comparison) {
            return false;
        }
        if (Double.doubleToLongBits(this.rightHandSide) != Double.doubleToLongBits(other.rightHandSide)) {
            return false;
        }
        return true;
    }

    public static class Linear extends Constraint {

        private final List<Pair<Double, String>> terms;

        public Linear(Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            terms = new ArrayList<>();
        }

        public Linear(List<Pair<Double, String>> terms, Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            this.terms = new ArrayList<>(terms);
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
                    sb.append("- ").append(Double.toString(-1 * term.getFirst()));
                } else {
                    sb.append("+ ").append(term.getFirst());
                }

                sb.append(" ").append(term.getSecond()).append(" ");
            }

            sb.append(getComparison()).append(" ").append(Double.toString(getRightHandSide()));

            return sb.toString();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.terms);
            hash = 67 * hash + Objects.hashCode(getComparison());
            hash = 67 * hash + (int) (Double.doubleToLongBits(getRightHandSide()) ^ (Double.doubleToLongBits(getRightHandSide()) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Linear other = (Linear) obj;
            if (!Objects.equals(this.terms, other.terms)) {
                return false;
            }
            if (this.getComparison() != other.getComparison()) {
                return false;
            }
            if (Double.doubleToLongBits(this.getRightHandSide()) != Double.doubleToLongBits(other.getRightHandSide())) {
                return false;
            }
            return true;
        }
    }

    public static class Bilinear extends Constraint {

        private final List<Pair<Double, String>> linearTerms;
        private final List<Pair<Double, Pair<String, String>>> bilinearTerms;

        public Bilinear(Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            linearTerms = new ArrayList<>();
            bilinearTerms = new ArrayList<>();
        }

        public Bilinear(List<Pair<Double, String>> linearTerms, List<Pair<Double, Pair<String, String>>> bilinearTerms, Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            this.linearTerms = new ArrayList<>(linearTerms);
            this.bilinearTerms = new ArrayList<>(bilinearTerms);
        }

        public List<Pair<Double, String>> getLinearTerms() {
            return linearTerms;
        }

        public void addLinearTerm(double factor, String variable) {
            linearTerms.add(new Pair<>(factor, variable));
        }

        public List<Pair<Double, Pair<String, String>>> getBilinearTerms() {
            return bilinearTerms;
        }

        public void addBilinearTerm(double factor, String variable1, String variable2) {
            bilinearTerms.add(new Pair<>(factor, new Pair<>(variable1, variable2)));
        }

        public Linear restrictToLinear(Map<String, Double> variableAssignment) {
            double newRightHandSide = getRightHandSide();
            List<Pair<Double, String>> newLinearTerms = new ArrayList<>();

            for (Pair<Double, String> linearTerm : linearTerms) {
                if (variableAssignment.containsKey(linearTerm.getSecond())) {
                    newRightHandSide -= linearTerm.getFirst() * variableAssignment.get(linearTerm.getSecond());
                } else {
                    newLinearTerms.add(new Pair<>(linearTerm.getFirst(), linearTerm.getSecond()));
                }
            }

            for (Pair<Double, Pair<String, String>> bilinearTerm : bilinearTerms) {
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

            return new Linear(newLinearTerms, getComparison(), newRightHandSide);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;

            for (Pair<Double, String> term : linearTerms) {
                if (first) {
                    sb.append(term.getFirst());
                    first = false;
                } else if (term.getFirst() < 0) {
                    sb.append(" - ").append(Double.toString(-1 * term.getFirst()));
                } else {
                    sb.append(" + ").append(term.getFirst());
                }

                sb.append(" ").append(term.getSecond());
            }

            for (Pair<Double, Pair<String, String>> term : bilinearTerms) {
                if (first) {
                    sb.append(term.getFirst());
                    first = false;
                } else if (term.getFirst() < 0) {
                    sb.append(" - ").append(Double.toString(-1 * term.getFirst()));
                } else {
                    sb.append(" + ").append(term.getFirst());
                }

                sb.append(" ").append(term.getSecond().getFirst()).append(" * ").append(term.getSecond().getSecond());
            }

            sb.append(" ").append(getComparison()).append(" ").append(Double.toString(getRightHandSide()));

            return sb.toString();
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.linearTerms);
            hash = 67 * hash + Objects.hashCode(this.bilinearTerms);
            hash = 67 * hash + Objects.hashCode(getComparison());
            hash = 67 * hash + (int) (Double.doubleToLongBits(getRightHandSide()) ^ (Double.doubleToLongBits(getRightHandSide()) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Bilinear other = (Bilinear) obj;
            if (!Objects.equals(this.linearTerms, other.linearTerms)) {
                return false;
            }
            if (!Objects.equals(this.bilinearTerms, other.bilinearTerms)) {
                return false;
            }
            if (this.getComparison() != other.getComparison()) {
                return false;
            }
            if (Double.doubleToLongBits(this.getRightHandSide()) != Double.doubleToLongBits(other.getRightHandSide())) {
                return false;
            }
            return true;
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
