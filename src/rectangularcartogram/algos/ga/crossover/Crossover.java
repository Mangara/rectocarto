/*
 * Copyright 2010-2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectangularcartogram.algos.ga.crossover;

public interface Crossover {

    /**
     * Forms two new children by crossing over parent1 and parent2.
     * @param parent1
     * @param parent2
     * @param child1
     * @param child2
     */
    public abstract void crossover(boolean[] parent1, boolean[] parent2, boolean[] child1, boolean[] child2);

}
