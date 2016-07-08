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
package rectocarto.algos.lp.solver;

import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

public interface BilinearSolver extends QuadraticSolver {

    /**
     * Attempts to find a solution to the given minimization problem with
     * bilinear objective function and constraints.
     *
     * @param bilinearProgram
     * @return
     */
    @Override
    public abstract Solution solve(MinimizationProblem bilinearProgram);
}
