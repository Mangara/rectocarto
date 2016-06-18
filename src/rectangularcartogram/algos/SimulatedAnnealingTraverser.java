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
package rectangularcartogram.algos;

import java.util.Random;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.measures.QualityMeasure;

public class SimulatedAnnealingTraverser extends GoodLabelingFinder {

    private int maxSteps;
    private RegularEdgeLabeling initialLabeling;
    private QualityMeasure steeringMeasure;
    private Random random;
    private long seed = (new Random()).nextLong(); // Generate a random seed in case the user doesn't provide one
    public static boolean DEBUG = true;

    public SimulatedAnnealingTraverser(Graph graph, QualityMeasure measure, int maxSteps) throws IncorrectGraphException {
        this(graph, measure, measure, maxSteps);
    }

    public SimulatedAnnealingTraverser(Graph graph, QualityMeasure measure, QualityMeasure steeringMeasure, int maxSteps) throws IncorrectGraphException {
        super(graph, measure);
        this.steeringMeasure = steeringMeasure;
        this.maxSteps = maxSteps;
        initialLabeling = MinimumLabelingComputer.getMinimalLabeling(graph);
    }

    @Override
    protected void traverseLabelings() throws IncorrectGraphException {
        // Source: http://en.wikipedia.org/wiki/Simulated_annealing
        random = new Random(seed);

        if (DEBUG) {
            System.out.println("Simulated annealing with " + maxSteps + " steps and seed " + Long.toString(seed));
        }

        RegularEdgeLabeling currentLabeling = new RegularEdgeLabeling(initialLabeling);
        double currentQuality = steeringMeasure.getQuality(currentLabeling);

        int stepCount = 0;

        while (stepCount < maxSteps) {
            RegularEdgeLabeling randomNeighbour = currentLabeling.moveRandomly();

            if (randomNeighbour != null) {
                double neighbourQuality = steeringMeasure.getQuality(randomNeighbour);
                double p = acceptanceProbability(currentQuality, neighbourQuality, stepCount);

                if (steeringMeasure == measure) {
                    // If these measures are the same, we already computed the quality of the new labeling, so we can reuse it
                    if ((measure.higherIsBetter() && neighbourQuality > bestQuality) || (!measure.higherIsBetter() && neighbourQuality < bestQuality) || (neighbourQuality == bestQuality && measure.compare(randomNeighbour, bestLabeling) > 0)) {
                        bestLabeling = randomNeighbour;
                        bestQuality = neighbourQuality;
                    }
                } else {
                    processLabeling(randomNeighbour);
                }

                if (p > random.nextDouble()) {
                    currentLabeling = randomNeighbour;
                    currentQuality = neighbourQuality;
                }

                stepCount++;

                if (stepCount % Math.max(maxSteps / 1000, 10) == 0) {
                    double percentage = 100 * stepCount / (double) maxSteps;

                    if (DEBUG) {
                        System.out.println(String.format("%.1f%% (%d / %d)", percentage, stepCount, maxSteps));
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println("Best found: " + bestQuality);
        }
    }

    public RegularEdgeLabeling getInitialLabeling() {
        return initialLabeling;
    }

    public void setInitialLabeling(RegularEdgeLabeling initialLabeling) {
        this.initialLabeling = new RegularEdgeLabeling(initialLabeling);
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    private double acceptanceProbability(double currentQuality, double neighbourQuality, int stepNumber) {
        double diff = currentQuality - neighbourQuality;

        if (measure.higherIsBetter()) {
            if (diff < 0) {
                // If the neighbour is better, we always step.
                return 2; // to make sure it's always larger than Math.random()
            } else {
                // 0.002^(x/M)
                double temp = Math.pow(0.002, stepNumber / (double) maxSteps);

                // e^(-d / T)
                return Math.exp(-diff / temp);
            }
        } else {
            if (diff > 0) {
                // If the neighbour is better, we always step.
                return 2; // to make sure it's always larger than Math.random()
            } else {
                // 0.002^(x/M)
                double temp = Math.pow(0.002, stepNumber / (double) maxSteps);

                // e^(d / T)
                return Math.exp(diff / temp);
            }
        }
    }
}
