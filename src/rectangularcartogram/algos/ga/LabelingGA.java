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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import rectangularcartogram.algos.DiameterCounter;
import rectangularcartogram.algos.MinimumLabelingComputer;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.algos.ga.selection.Selection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.QualityMeasure;

public class LabelingGA {
    // Parameters that influence the algorithm's bahaviour

    protected List<RegularEdgeLabeling> population;
    protected int populationSize;
    protected double crossoverChance = 0.7;
    protected double mutationChance = 0.01;
    protected double elitistFraction = 0;
    protected Selection selection = new RankSelection();
    protected QualityMeasure measure;
    protected Graph graph;
    // Temporary fields used by the algorithm
    protected double[] quality;
    protected RegularEdgeLabeling bestIndividual;
    protected double bestQuality;
    protected static Random rand = new Random();
    protected RegularEdgeLabeling minimum;
    protected long diameter;
    // Debug settings
    public static int DEBUG_LEVEL = 1; // 0 = no output, 1 = per generation summary, 2 = full population details, 3 = full debug spam
    protected int generation;

    public LabelingGA(Graph graph, QualityMeasure measure) throws IncorrectGraphException {
        this.graph = graph;
        this.measure = measure;

        DiameterCounter dc = new DiameterCounter(graph);
        diameter = dc.countDiameter();

        // Compute the minimum labeling
        minimum = MinimumLabelingComputer.getMinimalLabeling(graph);
    }

    /**
     * Initializes this GA with a random population consisting of populationSize individuals, each with chromosomeLength bits.
     * @param chromosomeLength
     * @param populationSize
     */
    public void initialize(int populationSize) throws IncorrectGraphException {
        this.populationSize = populationSize;
        population = new ArrayList<RegularEdgeLabeling>(populationSize);

        generateRandomPopulation();

        quality = new double[populationSize];
        bestQuality = Double.NEGATIVE_INFINITY;

        computeQualities();

        generation = 0;

        if (DEBUG_LEVEL > 1) {
            System.out.println("Initial (random) population:");
            printPopulation();
            System.out.println();
        }
    }

    /**
     * Returns the best individual and quality found after the specified number of generations has been evolved.
     * @param nGenerations
     * @return
     */
    public Pair<RegularEdgeLabeling, Double> getBestAfter(int nGenerations) throws IncorrectGraphException {
        for (int i = 0; i < nGenerations; i++) {
            iterate();
        }

        return new Pair<RegularEdgeLabeling, Double>(bestIndividual, bestQuality);
    }

    /**
     * Returns the best individual found when either the best quality is above the given threshold, or the maximum number of generations has been used. Note that in the second case, the returned solution may have a quality below the given threshold.
     * Also returns the quality of the returned solution and the number of generations used to find it.
     * @param threshold
     * @param maxGenerations
     * @return
     */
    public Pair<RegularEdgeLabeling, Pair<Double, Integer>> getFirstAbove(double threshold, int maxGenerations) throws IncorrectGraphException {
        while (generation < maxGenerations && bestQuality < threshold) {
            iterate();
        }

        return new Pair<RegularEdgeLabeling, Pair<Double, Integer>>(bestIndividual, new Pair<Double, Integer>(bestQuality, generation));
    }

    public RegularEdgeLabeling getBestIndividual() {
        return bestIndividual;
    }

    public double getBestQuality() {
        return bestQuality;
    }

    public double getCrossoverChance() {
        return crossoverChance;
    }

    public void setCrossoverChance(double crossoverChance) {
        this.crossoverChance = crossoverChance;
    }

    public double getElitistFraction() {
        return elitistFraction;
    }

    public void setElitistFraction(double elitistFraction) {
        this.elitistFraction = elitistFraction;
    }

    public double getMutationChance() {
        return mutationChance;
    }

    public void setMutationChance(double mutationChance) {
        this.mutationChance = mutationChance;
    }

    public Selection getSelection() {
        return selection;
    }

    public void setSelection(Selection selection) {
        this.selection = selection;
    }

    public QualityMeasure getMeasure() {
        return measure;
    }

    public void setMeasure(QualityMeasure measure) {
        this.measure = measure;
    }

    /**
     * Fill the entire population with random individuals
     */
    protected void generateRandomPopulation() {
        while (population.size() < populationSize) {
            // Generate
            if (DEBUG_LEVEL > 2) {
                System.out.println("Trying a random labeling... Current population size: " + population.size());
            }

            RegularEdgeLabeling rel = randomLabeling2();

            if (rel != null) {
                population.add(rel);
            }
        }
    }

    private RegularEdgeLabeling randomLabeling() {
        RegularEdgeLabeling result = new RegularEdgeLabeling(minimum);

        // Move to roughly the center of the lattice
        for (int i = 0; i < diameter / 2; i++) {
            result.moveUpRandomlyLocal();
        }

        // Move to a random labeling
        for (int i = 0; i < 2 * diameter; i++) {
            if (rand.nextBoolean()) {
                result.moveUpRandomlyLocal();
            } else {
                result.moveDownRandomlyLocal();
            }
        }

        return result;
    }

    private RegularEdgeLabeling randomLabeling2() {
        RegularEdgeLabeling result = new RegularEdgeLabeling(minimum);

        int height = (int) Math.round(diameter / 2.0 + (diameter / 8.0) * rand.nextGaussian());

        // Move to a random height in the lattice, normally distributed around the center
        for (int i = 0; i < height; i++) {
            result.moveUpRandomlyLocal();
        }

        return result;
    }

    /**
     * Compute the quality of a single individual. A higher score should be better.
     * @param individual
     * @return
     */
    protected double computeQuality(RegularEdgeLabeling individual) throws IncorrectGraphException {
        double q = measure.getQuality(individual);

        if (measure.higherIsBetter()) {
            return q;
        } else {
            return 1 / q;
        }
    }

    protected void iterate() throws IncorrectGraphException {
        // generate offspring
        // pre-computation for the selection
        selection.preprocess(quality);

        if (DEBUG_LEVEL > 2) {
            System.out.println("Quality: " + Arrays.toString(quality));
        }

        // copy the best performing individuals
        List<RegularEdgeLabeling> newPopulation = new ArrayList<RegularEdgeLabeling>(populationSize);
        double[] newQuality = elitistSelection(newPopulation);

        // form the rest by crossover and mutation
        while (newPopulation.size() < populationSize) {
            int i = selection.select(quality);

            newPopulation.add(mutate(crossover(population.get(i))));
        }

        // Replace the population by the new population
        population = newPopulation;

        // Compute qualities for the next round
        computeQualities(newQuality);

        generation++;

        // Give debug output
        if (DEBUG_LEVEL > 1) {
            System.out.println(generation + ". New population:");
            printPopulation();
            System.out.println();
        } else if (DEBUG_LEVEL > 0) {
            System.out.print(generation + ". ");
            //printBest(false);
            System.out.print("Best quality: " + bestQuality);
            printAverage();
        }
    }

    protected void computeQualities() throws IncorrectGraphException {
        for (int i = 0; i < populationSize; i++) {
            quality[i] = computeQuality(population.get(i));

            if (quality[i] > bestQuality) {
                bestQuality = quality[i];
                bestIndividual = new RegularEdgeLabeling(population.get(i));
            }
        }
    }

    protected void computeQualities(double[] newQualities) throws IncorrectGraphException {
        for (int i = 0; i < populationSize; i++) {
            if (Double.isNaN(newQualities[i])) {
                quality[i] = computeQuality(population.get(i));

                if (quality[i] > bestQuality) {
                    bestQuality = quality[i];
                    bestIndividual = new RegularEdgeLabeling(population.get(i));
                }
            } else {
                quality[i] = newQualities[i];
            }
        }
    }

    protected double[] elitistSelection(List<RegularEdgeLabeling> newPopulation) {
        int n = (int) Math.ceil(elitistFraction * populationSize);

        double[] newQuality = new double[populationSize];
        Arrays.fill(newQuality, Double.NaN);

        if (DEBUG_LEVEL > 2) {
            System.out.println("Copying the best " + n + " individuals to the next generation.");
        }

        if (n == 1) {
            // Find the maximum quality and copy the corresponding individual
            int bestQualityIndex = -1;
            double bestPopulationQuality = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < populationSize; i++) {
                if (quality[i] > bestPopulationQuality) {
                    bestPopulationQuality = quality[i];
                    bestQualityIndex = i;
                }
            }

            if (DEBUG_LEVEL > 2) {
                System.out.print("Best individual found: ");
                printIndividual(population.get(bestQualityIndex), bestPopulationQuality);
            }

            newPopulation.add(population.get(bestQualityIndex));
            newQuality[0] = bestPopulationQuality;
        } else if (n > 1) { // this should be the case, but it doesn't hurt to check before we start doing expensive stuff
            // Build a list of all qualities with their corresponding index
            List<Pair<Integer, Double>> qualities = new ArrayList<Pair<Integer, Double>>(populationSize);

            for (int i = 0; i < populationSize; i++) {
                qualities.add(new Pair<Integer, Double>(i, quality[i]));
            }

            // Sort the list by quality
            Collections.sort(qualities, new Comparator<Pair<Integer, Double>>() {

                public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
                    return -Double.compare(p1.getSecond(), p2.getSecond());
                }
            });

            if (DEBUG_LEVEL > 2) {
                System.out.println("Sorted population:");
                for (int i = 0; i < populationSize; i++) {
                    printIndividual(population.get(qualities.get(i).getFirst()), qualities.get(i).getSecond());
                }
                System.out.println();
            }

            // Copy the best individuals to the new population
            for (int i = 0; i < n; i++) {
                newPopulation.add(population.get(qualities.get(i).getFirst()));
                newQuality[i] = qualities.get(i).getSecond();
            }
        }

        return newQuality;
    }

    /**
     *
     * @param labeling
     * @return
     */
    protected RegularEdgeLabeling crossover(RegularEdgeLabeling labeling) {
        if (rand.nextDouble() < crossoverChance) {
            if (DEBUG_LEVEL > 2) {
                System.out.println("Performing crossover.");
                System.out.print("Initial: ");
                printIndividual(labeling);
            }

            RegularEdgeLabeling result = new RegularEdgeLabeling(labeling);

            double moves = rand.nextGaussian() * (double) diameter / (double) 6;

            if (moves < 0) {
                // We're going to move down the lattice
                int nSteps = (int) Math.round(-moves);

                for (int i = 0; i < nSteps; i++) {
                    result.moveDownRandomlyLocal();
                }
            } else {
                // We're going to move up the lattice
                int nSteps = (int) Math.round(moves);

                for (int i = 0; i < nSteps; i++) {
                    result.moveUpRandomlyLocal();
                }
            }

            if (DEBUG_LEVEL > 2) {
                System.out.print("Result:  ");
                printIndividual(result);
            }

            return result;
        } else {
            return labeling;
        }
    }

    protected RegularEdgeLabeling mutate(RegularEdgeLabeling labeling) {
        if (rand.nextDouble() < mutationChance) {
            if (DEBUG_LEVEL > 2) {
                System.out.println("Performing mutation.");
                System.out.print("Initial: ");
                printIndividual(labeling);
            }

            RegularEdgeLabeling result = labeling.moveRandomly();

            if (DEBUG_LEVEL > 2) {
                System.out.print("Result:  ");
                printIndividual(result);
            }

            return result;
        } else {
            return labeling;
        }
    }

    private void printPopulation() {
        for (int i = 0; i < populationSize; i++) {
            printIndividual(population.get(i), quality[i]);
        }
        printBest(false);
        printAverage();
    }

    private void printBest(boolean newLine) {
        System.out.print("Best: ");
        printIndividual(bestIndividual, bestQuality, newLine);
    }

    private void printAverage() {
        double sum = 0;

        for (double d : quality) {
            sum += d;
        }

        System.out.println(" Average quality: " + sum / populationSize);
    }

    private void printIndividual(RegularEdgeLabeling labeling, double q, boolean newLine) {
        System.out.print("[");

        for (Edge edge : graph.getEdges()) {
            System.out.print(labeling.get(edge) + ",");
        }

        if (newLine) {
            System.out.println("] (" + q + ")");
        } else {
            System.out.print("] (" + q + ")");
        }
    }

    private void printIndividual(RegularEdgeLabeling labeling, double q) {
        printIndividual(labeling, q, true);
    }

    private void printIndividual(RegularEdgeLabeling labeling) {
        System.out.print("[");

        for (Edge edge : graph.getEdges()) {
            System.out.print(labeling.get(edge) + ",");
        }

        System.out.println("]");
    }
}
