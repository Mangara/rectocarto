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

public class MinimizationProblem {

    private ObjectiveFunction objective;
    private final List<Constraint> constraints;

    public MinimizationProblem() {
        constraints = new ArrayList<>();
    }

    /**
     * Returns the function to be minimized.
     *
     * @return
     */
    public ObjectiveFunction getObjective() {
        return objective;
    }

    /**
     * Sets the function to be minimized.
     *
     * @param objective
     */
    public void setObjective(ObjectiveFunction objective) {
        this.objective = objective;
    }

    /**
     * Returns the actual list of constraints. Changes to this list will be
     * reflected in the problem.
     *
     * @return
     */
    public List<Constraint> getConstraints() {
        return constraints;
    }
    
    public void addConstraint(Constraint c) {
        constraints.add(c);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (objective != null) {
            sb.append("Minimize ").append(objective.toString()).append('\n');
        }

        if (!constraints.isEmpty()) {
            sb.append("Such that ");

            for (Constraint c : constraints) {
                sb.append(c.toString()).append("\n          ");
            }
        } else {
            sb.append("With no constraints.");
        }

        return sb.toString();
    }
}
