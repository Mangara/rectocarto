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

import java.util.HashMap;
import java.util.Map;

public class Solution {
    private final double objectiveValue;
    private final Map<String, Double> bestAssignment;

    public Solution(double objectiveValue) {
        this.objectiveValue = objectiveValue;
        bestAssignment = new HashMap<>();
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    public Map<String, Double> getBestAssignment() {
        return bestAssignment;
    }
    
    public void addVariableAssignment(String variable, double value) {
        bestAssignment.put(variable, value);
    }
}
