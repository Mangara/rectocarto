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

public class Solution {

    private final double objectiveValue;
    private final Map<String, Double> bestAssignment;

    /**
     * Creates a new solution.
     *
     * @param objectiveValue the value of the objective function with this
     * solution.
     */
    public Solution(double objectiveValue) {
        this.objectiveValue = objectiveValue;
        bestAssignment = new HashMap<>();
    }

    /**
     * Creates a new solution.
     *
     * @param objectiveValue the value of the objective function with this
     * solution.
     * @param bestAssignment this solution's variable assignment.
     */
    public Solution(double objectiveValue, Map<String, Double> bestAssignment) {
        this.objectiveValue = objectiveValue;
        this.bestAssignment = new HashMap<>(bestAssignment);
    }

    /**
     * Returns the value of the objective function.
     *
     * @return
     */
    public double getObjectiveValue() {
        return objectiveValue;
    }

    /**
     * Returns the variable assignment.
     *
     * @return
     */
    public Map<String, Double> getBestAssignment() {
        return bestAssignment;
    }

    /**
     * Sets the value of the given variable in this solution.
     *
     * @param variable
     * @param value
     */
    public void addVariableAssignment(String variable, double value) {
        bestAssignment.put(variable, value);
    }
}
