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

public class ObjectiveFunction {
    public class Linear {
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
    }
    
    public class Quadratic {
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
    }
}
