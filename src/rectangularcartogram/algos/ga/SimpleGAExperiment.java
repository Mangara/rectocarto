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
package rectangularcartogram.algos.ga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import rectangularcartogram.algos.ga.crossover.Crossover;
import rectangularcartogram.algos.ga.crossover.UniformCrossover;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.algos.ga.selection.RouletteSelection;
import rectangularcartogram.algos.ga.selection.Selection;
import rectangularcartogram.algos.ga.selection.TournamentSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.QualityMeasure;

public class SimpleGAExperiment {

    public static void main(String[] args) throws IOException {
        Subdivision sub = loadSubdivision(new File("nederland (Subdivision).sub"));
        QualityMeasure measure = new BoundingBoxSeparationMeasure(sub);

        SimpleLabelingGA.DEBUG_LEVEL = 0;
        SimpleLabelingGA ga = new SimpleLabelingGA(sub.getDualGraph(), measure);

        int maxGenerations = 40;

        int nRuns = 100;

        int[] popSizes = new int[]{200};
        double[] elitism = new double[]{0.005};
        double[] crossChance = new double[]{0.95};
        double[] mutationChance = new double[]{0.02};
        Crossover[] crossTypes = new Crossover[]{new UniformCrossover()};
        TournamentSelection tournamentSelection = new TournamentSelection();
        tournamentSelection.setN(20);
        tournamentSelection.setP(1);
        RankSelection rankSelection = new RankSelection();
        rankSelection.setP(0.91);
        RouletteSelection rouletteSelection = new RouletteSelection();
        Selection[] selections = new Selection[]{rankSelection};

        for (int popSize : popSizes) {
            for (double elite : elitism) {
                for (double crossP : crossChance) {
                    for (double mutationP : mutationChance) {
                        for (Crossover crossoverType : crossTypes) {
                            for (Selection selection : selections) {
                                ga.allowDuplicates = true;

                                ga.elitistFraction = elite;
                                ga.crossoverChance = crossP;
                                ga.setMutationChance(mutationP);
                                ga.crossover = crossoverType;
                                ga.selection = selection;

                                System.out.print("PopulationSize " + popSize + ", elitism " + elite + ", crossoverChance " + crossP + ", mutationChance " + mutationP + ", crossOverType " + crossoverType + ", selection " + selection);

                                experiment(ga, nRuns, popSize, maxGenerations);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void experiment(SimpleLabelingGA ga, int nRuns, int popS, int maxGenerations) {
        int totalGenerations = 0;
        int timesOptimum = 0;
        double opt = 20 + 1 / 0.17569818507632823 - 0.0001;

        for (int i = 0; i < nRuns; i++) {
            ga.initialize(popS);
            Pair<boolean[], Pair<Double, Integer>> best = ga.getFirstAbove(opt, maxGenerations);

            if (best.getSecond().getFirst() >= opt) {
                totalGenerations += best.getSecond().getSecond();
                timesOptimum++;
                //System.out.println("Optimum after " + best.getSecond().getSecond() + " generations.");
            } else {
                totalGenerations += maxGenerations;
                //System.out.println("No optimum.");
            }
        }

        System.out.println(": " + (100 * timesOptimum / (double) nRuns) + "% opt. Average generations: " + (totalGenerations / (double) nRuns));
    }

    private static Subdivision loadSubdivision(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Subdivision.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
