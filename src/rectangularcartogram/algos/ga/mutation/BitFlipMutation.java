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
package rectangularcartogram.algos.ga.mutation;

public class BitFlipMutation implements Mutation {
    
    private double mutationChance;

    public BitFlipMutation(double flipChance) {
        this.mutationChance = flipChance;
    }

    public double getMutationChance() {
        return mutationChance;
    }

    public void setMutationChance(double mutationChance) {
        this.mutationChance = mutationChance;
    }

    public void mutate(boolean[] individual) {
        for (int i = 0; i < individual.length; i++) {
            if (Math.random() < mutationChance) {
                individual[i] = !individual[i];
            }
        }
    }
}
