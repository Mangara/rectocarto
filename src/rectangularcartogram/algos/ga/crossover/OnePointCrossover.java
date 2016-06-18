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

import java.util.Random;

public class OnePointCrossover implements Crossover, IntegerCrossover {

    private static final Random rand = new Random();

    public void crossover(boolean[] parent1, boolean[] parent2, boolean[] child1, boolean[] child2) {
        int chromosomeLength = parent1.length;

        int j = rand.nextInt(chromosomeLength);

        // Copy the first part of the first individual to the first child and likewise for the second
        System.arraycopy(parent1, 0, child1, 0, j);
        System.arraycopy(parent2, 0, child2, 0, j);

        // Copy the second part of the first individual to the second child and likewise for the second
        System.arraycopy(parent1, j, child2, j, chromosomeLength - j);
        System.arraycopy(parent2, j, child1, j, chromosomeLength - j);
    }

    public void crossover(int[] parent1, int[] parent2, int[] child1, int[] child2) {
        int chromosomeLength = parent1.length;

        int j = rand.nextInt(chromosomeLength);

        // Copy the first part of the first individual to the first child and likewise for the second
        System.arraycopy(parent1, 0, child1, 0, j);
        System.arraycopy(parent2, 0, child2, 0, j);

        // Copy the second part of the first individual to the second child and likewise for the second
        System.arraycopy(parent1, j, child2, j, chromosomeLength - j);
        System.arraycopy(parent2, j, child1, j, chromosomeLength - j);
    }

    @Override
    public String toString() {
        return "1-Point";
    }
}
