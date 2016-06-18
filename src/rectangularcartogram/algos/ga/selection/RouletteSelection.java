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

public class RouletteSelection implements Selection {

    protected double[] cumulativeQuality;
    protected double totalQuality;

    public void preprocess(double[] qualities) {
        cumulativeQuality = new double[qualities.length];
        totalQuality = 0;

        if (qualities.length > 0) {
            cumulativeQuality[0] = qualities[0];
            totalQuality = qualities[0];
        }

        for (int i = 1; i < qualities.length; i++) {
            cumulativeQuality[i] = cumulativeQuality[i - 1] + qualities[i];
            totalQuality += qualities[i];
        }
    }

    public int select(double[] qualities) {
        double slice = Math.random() * totalQuality;

        int currentBegin = -1;
        int currentEnd = qualities.length;
        int middle;

        while (currentBegin < (currentEnd - 1)) {
            middle = (currentBegin + currentEnd) / 2;

            if (slice < cumulativeQuality[middle]) {
                currentEnd = middle;
            } else if (slice >= cumulativeQuality[middle]) {
                currentBegin = middle;
            }
        }

        return Math.min(qualities.length - 1, currentEnd);
    }

    @Override
    public String toString() {
        return "Roulette";
    }
}
