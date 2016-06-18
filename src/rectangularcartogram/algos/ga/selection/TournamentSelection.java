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
package rectangularcartogram.algos.ga.selection;

import java.util.Random;

public class TournamentSelection implements Selection {

    private static final Random rand = new Random();
    private int n = 20;
    private double p = 0.9;

    public TournamentSelection() {
    }

    public TournamentSelection(int n, double p) {
        this.n = n;
        this.p = p;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public void preprocess(double[] qualities) {
    }

    public int select(double[] qualities) {
        // Select n random individuals
        int[] contestants = new int[n];

        for (int i = 0; i < n; i++) {
            contestants[i] = rand.nextInt(qualities.length);
        }

        if (rand.nextDouble() < p) {
            // Return the best
            int best = -1;
            double bestQuality = Double.NEGATIVE_INFINITY;

            for (int i : contestants) {
                if (qualities[i] > bestQuality) {
                    best = i;
                    bestQuality = qualities[i];
                }
            }

            return best;
        } else {
            // Return a random one
            return contestants[rand.nextInt(n)];
        }
    }

    @Override
    public String toString() {
        return "Tournament";
    }
}
