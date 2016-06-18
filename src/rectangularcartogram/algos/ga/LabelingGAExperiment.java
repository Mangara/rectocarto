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

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import rectangularcartogram.algos.CartogramMaker;
import rectangularcartogram.algos.DiameterCounter;
import rectangularcartogram.algos.MinimumLabelingComputer;
import rectangularcartogram.algos.SimulatedAnnealingTraverser;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.algos.ga.selection.RouletteSelection;
import rectangularcartogram.algos.ga.selection.Selection;
import rectangularcartogram.algos.ga.selection.TournamentSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.BoundedCartographicErrorMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.CartographicErrorMeasure;
import rectangularcartogram.measures.CombinedMeasure;
import rectangularcartogram.measures.QualityMeasure;
import rectangularcartogram.measures.ResultingAngleDeviationMeasure;

public class LabelingGAExperiment {

    public static void main(String[] args) throws IOException, IncorrectGraphException, IloException {
        //runOne();
        //makeCartograms();
        //runExperiments();
        //runTimingExperiments();
        comparisons();
        //loop();
    }

    private static void runExperiments() throws IOException, IncorrectGraphException {
        Subdivision sub = loadSubdivision(new File("Hexagons.sub"));
        QualityMeasure measure = new BoundingBoxSeparationMeasure(sub);

        LabelingGA.DEBUG_LEVEL = 0;
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);

        int maxGenerations = 500;
        int nRuns = 50;

        SimulatedAnnealingTraverser.DEBUG = false;

        int[] popSizes = new int[]{50};
        double[] elitism = new double[]{0.04}; // 0.04
        double[] crossChance = new double[]{0.05}; // 0 - 0.1. Inconclusive results. 0.05
        double[] mutationChance = new double[]{0.9}; // 0.85 - 1
        TournamentSelection tournamentSelection = new TournamentSelection();
        tournamentSelection.setN(20);
        tournamentSelection.setP(1);
        RankSelection rankSelection = new RankSelection();
        rankSelection.setP(0.65);
        RouletteSelection rouletteSelection = new RouletteSelection();
        Selection[] selections = new Selection[]{rankSelection};

        double[] rankP = new double[]{0.9}; // 0.8 - 1. 0.95 performed better in every case than 0.5, 0.65 and 0.8

        for (int popSize : popSizes) {
            for (double elite : elitism) {
                for (double crossP : crossChance) {
                    for (double mutationP : mutationChance) {
                        for (Selection selection : selections) {
                            for (double rP : rankP) {
                                rankSelection.setP(rP);
                                //SimulatedAnnealingTraverser sa = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, maxGenerations * popSize);

                                ga.elitistFraction = elite;
                                ga.crossoverChance = crossP;
                                ga.setMutationChance(mutationP);
                                ga.selection = selection;

                                System.out.print("PopulationSize " + popSize + ", elitism " + elite + ", crossoverChance " + crossP + ", mutationChance " + mutationP + ", rankP " + rP);


                                //experimentAgainstSA(ga, sa, nRuns, popSize, maxGenerations);
                                experiment(ga, nRuns, popSize, maxGenerations);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void experiment(LabelingGA ga, int nRuns, int popS, int maxGenerations) throws IncorrectGraphException {
        double totalQuality = 0;

        for (int i = 0; i < nRuns; i++) {
            ga.initialize(popS);
            Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);

            totalQuality += best.getSecond();
        }

        System.out.println(": Average quality: " + (totalQuality / (double) nRuns));
    }

    private static void experimentAgainstSA(LabelingGA ga, SimulatedAnnealingTraverser sa, int nRuns, int popS, int maxGenerations) throws IncorrectGraphException {
        int totalGenerations = 0;
        int timesOptimum = 0;
        long saTime = 0;
        long gaTime = 0;

        System.out.println();

        for (int i = 0; i < nRuns; i++) {
            sa.setSeed((new Random()).nextLong());

            long saStart = System.nanoTime();
            sa.findBestLabeling();
            saTime += System.nanoTime() - saStart;
            double saQuality = ga.getMeasure().getQuality(sa.getBestLabeling());
            double toBeat = 1 / saQuality; // For our current measure, lower is better

            System.out.print("SA quality: " + saQuality + ".");

            long gaStart = System.nanoTime();
            ga.initialize(popS);
            Pair<RegularEdgeLabeling, Pair<Double, Integer>> best = ga.getFirstAbove(toBeat, maxGenerations);
            gaTime += System.nanoTime() - gaStart;

            if (best.getSecond().getFirst() >= toBeat) {
                totalGenerations += best.getSecond().getSecond();
                timesOptimum++;
                System.out.println(" Beaten SA after " + best.getSecond().getSecond() + " generations.");
            } else {
                totalGenerations += maxGenerations;
                System.out.println(" Not beaten SA. Best quality: " + best.getSecond().getFirst());
            }
        }

        System.out.println(": " + (100 * timesOptimum / (double) nRuns) + "% beaten SA. Average generations: " + (totalGenerations / (double) nRuns) + " Total SA time: " + saTime * 10e-9 + " Total GA time: " + gaTime * 10e-9);
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

    private static void runOne() throws IOException, IncorrectGraphException, IloException {
        Subdivision sub = loadSubdivision(new File("../Data/US/United States Population.sub"));
        IloCplex cplex = new IloCplex();
        //QualityMeasure measure = new BoundingBoxSeparationMeasure(sub, false);
        //ResultingAngleDeviationAndCartographicErrorMeasure measure = new ResultingAngleDeviationAndCartographicErrorMeasure(sub, cplex);
        BoundedCartographicErrorMeasure measure = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), new BoundingBoxSeparationMeasure(sub), 0.25);

        LabelingGA.DEBUG_LEVEL = 1;
        long start = System.nanoTime();
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);
        long create = System.nanoTime() - start;
        ga.elitistFraction = 0.04;
        ga.crossoverChance = 0.05;
        ga.setMutationChance(0.9);
        ga.selection = new RankSelection(0.9);

        int maxGenerations = 100;
        int populationSize = 50;

        start = System.nanoTime();
        ga.initialize(populationSize);
        long init = System.nanoTime() - start;

        start = System.nanoTime();
        Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);
        long gaRun = System.nanoTime() - start;

        if (measure.higherIsBetter()) {
            System.out.println("Best quality: " + best.getSecond());
        } else {
            System.out.println("Best quality: " + 1 / best.getSecond());
        }

        start = System.nanoTime();
        sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

        CartogramMaker carto = new CartogramMaker(sub, cplex);

        for (int i = 0; i < 50; i++) {
            carto.iterate();
            carto.iterate();
        }

        Subdivision cartogram = carto.getCartogram();
        long cartoCreation = System.nanoTime() - start;

        saveSubdivision(cartogram, new File(cartogram.getAverageCartographicError() + " " + cartogram.getMaximumCartographicError() + " GARunOne.sub"));

        System.out.println(String.format("GA Creation: %f, GA Initialization: %f, GA Running Time: %f, Cartogram Construction: %f", create * 10e-9, init * 10e-9, gaRun * 10e-9, cartoCreation * 10e-9));
    }

    private static void loop() throws IOException, IncorrectGraphException, IloException {
        Subdivision sub = loadSubdivision(new File("../Data/US/United States 2 Population.sub"));
        IloCplex cplex = new IloCplex();

        // Weighted ACE / resulting ad
        CombinedMeasure m1 = new CombinedMeasure();
        m1.addMeasure(new CartographicErrorMeasure(sub, cplex), 7);
        m1.addMeasure(new ResultingAngleDeviationMeasure(sub, cplex), 3);

        // Weighted MCE / average bbsd
        CombinedMeasure m2 = new CombinedMeasure();
        m2.addMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), 7);
        m2.addMeasure(new BoundingBoxSeparationMeasure(sub), 3);

        // Bounded ACE / maximum resulting ad
        BoundedCartographicErrorMeasure m3 = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex), new ResultingAngleDeviationMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), 0.05);

        // Bounded MCE / average bbsd
        BoundedCartographicErrorMeasure m4 = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), new BoundingBoxSeparationMeasure(sub), 0.25);

        // Weighted ACE / average bbsd
        CombinedMeasure m5 = new CombinedMeasure();
        m5.addMeasure(new CartographicErrorMeasure(sub, cplex), 7);
        m5.addMeasure(new BoundingBoxSeparationMeasure(sub), 3);

        QualityMeasure[] measures = {m1, m2, m3, m4, m5};
        String[] measureNames = {"Weighted ACE - resulting ad", "Weighted MCE - average bbsd", "Bounded ACE - maximum resulting ad", "Bounded MCE - average bbsd", "Weighted ACE - average bbsd"};

        LabelingGA.DEBUG_LEVEL = 1;
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), null);
        ga.elitistFraction = 0.04;
        ga.crossoverChance = 0.05;
        ga.setMutationChance(0.9);
        ga.selection = new RankSelection(0.9);

        int maxGenerations = 50;
        int populationSize = 50;

        int m = 0;

        while (true) {
            ga.setMeasure(measures[m]);
            m = (m + 1) % measures.length;

            ga.initialize(populationSize);

            Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);

            sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

            CartogramMaker carto = new CartogramMaker(sub, cplex);

            for (int i = 0; i < 50; i++) {
                carto.iterate();
                carto.iterate();
            }

            Subdivision cartogram = carto.getCartogram();

            saveSubdivision(cartogram, new File("Results", cartogram.getAverageCartographicError() + " " + cartogram.getMaximumCartographicError() + " " + measureNames[(m - 1 + measures.length) % measures.length] + ".sub"));
        }
    }

    private static void runTimingExperiments() throws IOException, IncorrectGraphException {
        Subdivision sub = loadSubdivision(new File("Hexagons.sub"));
        QualityMeasure measure = new BoundingBoxSeparationMeasure(sub);

        LabelingGA.DEBUG_LEVEL = 0;
        long start = System.nanoTime();
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);
        long oldCreate = System.nanoTime() - start;
        ga.elitistFraction = 0.1;
        ga.crossoverChance = 0.05;
        ga.setMutationChance(1);
        ga.selection = new RankSelection(0.95);

        /*LabelingGA2.DEBUG_LEVEL = 0;
        start = System.nanoTime();
        LabelingGA2 ga2 = new LabelingGA2(sub.getDualGraph(), measure);
        long newCreate = System.nanoTime() - start;
        ga2.elitistFraction = 0.1;
        ga2.crossoverChance = 0.05;
        ga2.setMutationChance(1);
        ga2.selection = new RankSelection(0.65);*/

        int maxGenerations = 500;
        int populationSize = 50;

        start = System.nanoTime();
        ga.initialize(populationSize);
        long oldInit = System.nanoTime() - start;

        start = System.nanoTime();
        Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);
        long oldRun = System.nanoTime() - start;

        /*start = System.nanoTime();
        ga2.initialize(populationSize);
        long newInit = System.nanoTime() - start;

        start = System.nanoTime();
        Pair<RegularEdgeLabeling, Double> best2 = ga2.getBestAfter(maxGenerations);
        long newRun = System.nanoTime() - start;*/

        System.out.println("Old. Create: " + oldCreate * 10e-9 + " Init: " + oldInit * 10e-9 + " Run: " + oldRun * 10e-9 + " Quality: " + best.getSecond());
        //System.out.println("New. Create: " + newCreate * 10e-9 + " Init: " + newInit * 10e-9 + " Run: " + newRun * 10e-9 + " Quality: " + best2.getSecond());*/
    }

    private static void comparisons() throws IOException, IncorrectGraphException {
        Subdivision sub = loadSubdivision(new File("../Maps/World/World.sub"));
        QualityMeasure measure = new BoundingBoxSeparationMeasure(sub);

        int nRuns = 100;
        int nAssessments = 10000;

        double totalQuality;
        double minQuality;
        double maxQuality;

        //// SA ////
        System.out.print("SA:          ");
        SimulatedAnnealingTraverser.DEBUG = false;
        SimulatedAnnealingTraverser sa = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, nAssessments);

        totalQuality = 0;
        minQuality = Double.POSITIVE_INFINITY;
        maxQuality = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nRuns; i++) {
            sa.setSeed((new Random()).nextLong());
            sa.findBestLabeling();
            double quality = measure.getQuality(sa.getBestLabeling());
            totalQuality += quality;
            minQuality = Math.min(minQuality, quality);
            maxQuality = Math.max(maxQuality, quality);
            System.out.println(i + "/" + nRuns);
        }

        System.out.println("Average quality: " + totalQuality / nRuns + " Minimum quality: " + minQuality + " Maximum quality: " + maxQuality);
        //// SA ///*/

        //// SA Modified ////
        System.out.print("SA Initial:    ");

        SimulatedAnnealingTraverser sa2 = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, nAssessments);

        totalQuality = 0;
        minQuality = Double.POSITIVE_INFINITY;
        maxQuality = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nRuns; i++) {
            sa2.setSeed((new Random()).nextLong());
            sa2.setInitialLabeling(randomLabeling2(sub.getDualGraph()));
            sa2.findBestLabeling();
            double quality = measure.getQuality(sa2.getBestLabeling());

            totalQuality += quality;
            minQuality = Math.min(minQuality, quality);
            maxQuality = Math.max(maxQuality, quality);
            System.out.println(i + "/" + nRuns);
        }

        System.out.println("Average quality: " + totalQuality / nRuns + " Minimum quality: " + minQuality + " Maximum quality: " + maxQuality);
        //// SA Modified ///*/

        //// SA Modified ////
        System.out.print("SA Initial, 10x:    ");

        SimulatedAnnealingTraverser sa3 = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, nAssessments / 10);

        totalQuality = 0;
        minQuality = Double.POSITIVE_INFINITY;
        maxQuality = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nRuns; i++) {
            double quality = Double.POSITIVE_INFINITY;

            for (int j = 0; j < 10; j++) {
                sa3.setSeed((new Random()).nextLong());
                sa3.setInitialLabeling(randomLabeling2(sub.getDualGraph()));
                sa3.findBestLabeling();
                double q = measure.getQuality(sa3.getBestLabeling());

                if (q < quality) {
                    quality = q;
                }
            }

            totalQuality += quality;
            minQuality = Math.min(minQuality, quality);
            maxQuality = Math.max(maxQuality, quality);
            System.out.println(i + "/" + nRuns);
        }

        System.out.println("Average quality: " + totalQuality / nRuns + " Minimum quality: " + minQuality + " Maximum quality: " + maxQuality);
        //// SA Modified ///*/

        //// LGA ////
        LabelingGA.DEBUG_LEVEL = 0;

        LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);

        ga.setElitistFraction(0.04);
        ga.setCrossoverChance(0.05);
        ga.setMutationChance(0.9);
        ga.setSelection(new RankSelection(0.9));

        System.out.print("Labeling GA: ");
        totalQuality = 0;
        minQuality = Double.POSITIVE_INFINITY;
        maxQuality = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nRuns; i++) {
        ga.initialize(50);
        Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(nAssessments / 50);
        double quality = 1 / best.getSecond();
        totalQuality += quality;
        minQuality = Math.min(minQuality, quality);
        maxQuality = Math.max(maxQuality, quality);
        System.out.println(i + "/" + nRuns);
        }

        System.out.println("Average quality: " + totalQuality / nRuns + " Minimum quality: " + minQuality + " Maximum quality: " + maxQuality);
        //// LGA ///*/

        /*/// SGA ////
        SimpleLabelingGA.DEBUG_LEVEL = 0;
        SimpleLabelingGA sga = new SimpleLabelingGA(sub.getDualGraph(), measure);

        sga.setElitistFraction(0.01);
        sga.setCrossoverChance(0.95);
        sga.setMutationChance(0.02);
        sga.setSelection(new RankSelection(0.9));

        System.out.print("Simple GA:   ");
        int missed = 0;
        totalQuality = 0;
        minQuality = Double.POSITIVE_INFINITY;
        maxQuality = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < nRuns; i++) {
        sga.initialize(100);
        Pair<boolean[], Double> best = sga.getBestAfter(nAssessments / 100);

        if (best.getSecond() > sub.getDualGraph().getVertices().size()) {
        double quality = 1 / (best.getSecond() - sub.getDualGraph().getVertices().size());
        totalQuality += quality;
        minQuality = Math.min(minQuality, quality);
        maxQuality = Math.max(maxQuality, quality);
        } else {
        missed++;
        }
        }

        System.out.println("Average quality: " + totalQuality / (nRuns - missed) + " Minimum quality: " + minQuality + " Maximum quality: " + maxQuality + " Missed: " + missed);
        //// SGA ///*/
    }
    private static RegularEdgeLabeling minimum = null;
    private static long diameter = -1;

    private static RegularEdgeLabeling randomLabeling2(Graph graph) throws IncorrectGraphException {
        if (minimum == null) {
            minimum = MinimumLabelingComputer.getMinimalLabeling(graph);
        }

        if (diameter == -1) {
            DiameterCounter dc = new DiameterCounter(graph);
            diameter = dc.countDiameter();
        }

        RegularEdgeLabeling result = new RegularEdgeLabeling(minimum);

        int height = (int) Math.round(diameter / 2.0 + (diameter / 8.0) * (new Random()).nextGaussian());

        // Move to a random height in the lattice, normally distributed around the center
        for (int i = 0; i < height; i++) {
            result.moveUpRandomlyLocal();
        }

        return result;
    }

    private static void saveSubdivision(Subdivision sub, File file) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            // Write the data
            sub.save(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void makeCartograms() throws IOException, IloException, IncorrectGraphException {
        Subdivision sub = loadSubdivision(new File("../Data/Europe/Europe Population.sub"));
        IloCplex cplex = new IloCplex();
        QualityMeasure measure = new BoundingBoxSeparationMeasure(sub);

        LabelingGA.DEBUG_LEVEL = 1;
        long start = System.nanoTime();
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), measure);
        long create = System.nanoTime() - start;
        ga.elitistFraction = 0.04;
        ga.crossoverChance = 0.05;
        ga.setMutationChance(0.9);
        ga.selection = new RankSelection(0.9);

        int maxGenerations = 100;
        int populationSize = 50;

        start = System.nanoTime();
        ga.initialize(populationSize);
        long init = System.nanoTime() - start;

        start = System.nanoTime();
        Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);
        long gaRun = System.nanoTime() - start;

        if (measure.higherIsBetter()) {
            System.out.println("Best quality: " + best.getSecond());
        } else {
            System.out.println("Best quality: " + 1 / best.getSecond());
        }

        start = System.nanoTime();
        sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

        CartogramMaker carto = new CartogramMaker(sub, cplex);

        for (int i = 0; i < 50; i++) {
            carto.iterate();
            carto.iterate();
        }

        Subdivision cartogram = carto.getCartogram();
        long cartoCreation = System.nanoTime() - start;

        saveSubdivision(cartogram, new File(cartogram.getAverageCartographicError() + " " + cartogram.getMaximumCartographicError() + " GARunOne.sub"));
    }
}
