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

public class UniformCrossover implements Crossover, IntegerCrossover {

    private static final Random rand = new Random();

    public void crossover(boolean[] parent1, boolean[] parent2, boolean[] child1, boolean[] child2) {
        int chromosomeLength = parent1.length;

        for (int i = 0; i < chromosomeLength; i++) {
            if (rand.nextBoolean()) { // Select with probability 0.5 from either parent
                child1[i] = parent1[i];
                child2[i] = parent2[i];
            } else {
                child1[i] = parent2[i];
                child2[i] = parent1[i];
            }
        }
    }

    public void crossover(int[] parent1, int[] parent2, int[] child1, int[] child2) {
        int chromosomeLength = parent1.length;

        for (int i = 0; i < chromosomeLength; i++) {
            if (rand.nextBoolean()) { // Select with probability 0.5 from either parent
                child1[i] = parent1[i];
                child2[i] = parent2[i];
            } else {
                child1[i] = parent2[i];
                child2[i] = parent1[i];
            }
        }
    }

    @Override
    public String toString() {
        return "Uniform";
    }
}
