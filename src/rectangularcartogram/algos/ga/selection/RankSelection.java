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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import rectangularcartogram.data.Pair;

/**
 * Roulette selection with fixed cumulative qualities
 */
public class RankSelection extends RouletteSelection {

    private double p = 0.9;
    private int[] rankToIndividual;

    public RankSelection() {
    }

    public RankSelection(double p) {
        this.p = p;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
        cumulativeQuality = null;
    }

    @Override
    public void preprocess(double[] qualities) {
        int n = qualities.length;

        if (cumulativeQuality == null || cumulativeQuality.length != n) {
            // We need to rebuild the cumulative qualities
            cumulativeQuality = new double[n];
            rankToIndividual = new int[n];

            double quality = p;
            cumulativeQuality[0] = p;
            totalQuality = p;

            for (int i = 1; i < n; i++) {
                quality *= p;
                cumulativeQuality[i] = cumulativeQuality[i - 1] + quality;
                totalQuality += quality;
            }
        }

        // Find out the ranks
        ArrayList<Pair<Integer, Double>> individuals = new ArrayList<Pair<Integer, Double>>(n);

        for (int i = 0; i < qualities.length; i++) {
            individuals.add(new Pair<Integer, Double>(i, qualities[i]));
        }

        Collections.sort(individuals, new Comparator<Pair<Integer, Double>>() {

            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return -Double.compare(o1.getSecond(), o2.getSecond());
            }
        });

        for (int i = 0; i < n; i++) {
            rankToIndividual[i] = individuals.get(i).getFirst();
        }
    }

    @Override
    public int select(double[] qualities) {
        int rank = super.select(qualities); // The roulette selection gives us the rank of the individual we want to return
        return rankToIndividual[rank];
    }

    @Override
    public String toString() {
        return "Rank";
    }
}
