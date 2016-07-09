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

import java.util.HashMap;
import java.util.Map;

public class Solution extends HashMap<String, Double> {

    private final double objectiveValue;

    /**
     * Creates a new solution.
     *
     * @param objectiveValue the value of the objective function with this
     * solution.
     */
    public Solution(double objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    /**
     * Creates a new solution.
     *
     * @param objectiveValue the value of the objective function with this
     * solution.
     * @param variableAssignment the variable assignment corresponding to this solution
     */
    public Solution(double objectiveValue, Map<? extends String, ? extends Double> variableAssignment) {
        super(variableAssignment);
        this.objectiveValue = objectiveValue;
    }

    /**
     * Returns the value of the objective function.
     *
     * @return
     */
    public double getObjectiveValue() {
        return objectiveValue;
    }

    @Override
    public Object clone() {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }
}
