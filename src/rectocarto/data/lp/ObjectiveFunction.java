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
import rectangularcartogram.data.Pair;

public interface ObjectiveFunction {
    public abstract double evaluate(Map<String,Double> variableAssignment);
    
    public class Linear implements ObjectiveFunction {
        private final List<Pair<Double,String>> terms;

        public Linear() {
            terms = new ArrayList<>();
        }
        
        public void addTerm(double factor, String variable) {
            terms.add(new Pair<>(factor, variable));
        }

        public List<Pair<Double, String>> getTerms() {
            return terms;
        }

        @Override
        public double evaluate(Map<String, Double> variableAssignment) {
            double result = 0;
            
            for (Pair<Double, String> term : terms) {
                result += term.getFirst() * variableAssignment.get(term.getSecond());
            }
            
            return result;
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
                
                sb.append(" ").append(term.getSecond());
            }
            
            return sb.toString();
        }
    }
    
    public class Quadratic implements ObjectiveFunction {
        private final List<Pair<Double,String>> linearTerms;
        private final List<Pair<Double,String>> quadraticTerms;

        public Quadratic() {
            linearTerms = new ArrayList<>();
            quadraticTerms = new ArrayList<>();
        }
        
        public List<Pair<Double, String>> getLinearTerms() {
            return linearTerms;
        }
        
        public void addLinearTerm(double factor, String variable) {
            linearTerms.add(new Pair<>(factor, variable));
        }

        public List<Pair<Double, String>> getQuadraticTerms() {
            return quadraticTerms;
        }

        public void addQuadraticTerm(double factor, String variable) {
            quadraticTerms.add(new Pair<>(factor, variable));
        }

        @Override
        public double evaluate(Map<String, Double> variableAssignment) {
            double result = 0;
            
            for (Pair<Double, String> term : linearTerms) {
                result += term.getFirst() * variableAssignment.get(term.getSecond());
            }
            
            for (Pair<Double, String> term : quadraticTerms) {
                double val = variableAssignment.get(term.getSecond());
                result += term.getFirst() * val * val;
            }
            
            return result;
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
            
            for (Pair<Double, String> term : quadraticTerms) {
                if (first) {
                    sb.append(term.getFirst());
                    first = false;
                } else if (term.getFirst() < 0) {
                    sb.append(" - ").append(Double.toString(-1 * term.getFirst()));
                } else {
                    sb.append(" + ").append(term.getFirst());
                }
                
                sb.append(" ").append(term.getSecond()).append("^2");
            }
            
            return sb.toString();
        }
    }
}
