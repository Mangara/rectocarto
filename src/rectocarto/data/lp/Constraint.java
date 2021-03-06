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
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    public static class Linear extends Constraint {

        private final List<Pair<Double, String>> terms;

        /**
         * Creates a new linear constraint of the form: "0 op rightHandSide".
         *
         * @param op
         * @param rightHandSide
         */
        public Linear(Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            terms = new ArrayList<>();
        }

        /**
         * Creates a new linear constraint of the form: "terms op
         * rightHandSide".
         *
         * @param terms
         * @param op
         * @param rightHandSide
         */
        public Linear(List<Pair<Double, String>> terms, Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            this.terms = new ArrayList<>(terms);
        }

        /**
         * Returns the actual list of terms. Changes made to this list will be
         * reflected in the constraint.
         *
         * @return
         */
        public List<Pair<Double, String>> getTerms() {
            return terms;
        }

        /**
         * Add the term "factor * variable" to the left hand side.
         *
         * @param factor
         * @param variable
         */
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

        /**
         * Creates a new bilinear constraint of the form "0 op rightHandSide".
         *
         * @param op
         * @param rightHandSide
         */
        public Bilinear(Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            linearTerms = new ArrayList<>();
            bilinearTerms = new ArrayList<>();
        }

        /**
         * Creates a new bilinear constraint of the form "linearTerms +
         * bilinearTerms op rightHandSide".
         *
         * @param linearTerms
         * @param bilinearTerms
         * @param op
         * @param rightHandSide
         */
        public Bilinear(List<Pair<Double, String>> linearTerms, List<Pair<Double, Pair<String, String>>> bilinearTerms, Comparison op, double rightHandSide) {
            super(op, rightHandSide);
            this.linearTerms = new ArrayList<>(linearTerms);
            this.bilinearTerms = new ArrayList<>(bilinearTerms);
        }

        /**
         * Returns the actual list of linear terms. Changes made to this list
         * will be reflected in the constraint.
         *
         * @return
         */
        public List<Pair<Double, String>> getLinearTerms() {
            return linearTerms;
        }

        /**
         * Adds a term of the form "factor * variable" to the left hand side.
         *
         * @param factor
         * @param variable
         */
        public void addLinearTerm(double factor, String variable) {
            linearTerms.add(new Pair<>(factor, variable));
        }

        /**
         * Returns the actual list of bilinear terms. Changes made to this list
         * will be reflected in the constraint.
         *
         * @return
         */
        public List<Pair<Double, Pair<String, String>>> getBilinearTerms() {
            return bilinearTerms;
        }

        /**
         * Adds a term of the form "factor * variable1 * variable2" to the left
         * hand side.
         *
         * @param factor
         * @param variable1
         * @param variable2
         */
        public void addBilinearTerm(double factor, String variable1, String variable2) {
            bilinearTerms.add(new Pair<>(factor, new Pair<>(variable1, variable2)));
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
