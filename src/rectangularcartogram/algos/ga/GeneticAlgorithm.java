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
import rectangularcartogram.algos.ga.crossover.Crossover;
import rectangularcartogram.algos.ga.crossover.UniformCrossover;
import rectangularcartogram.algos.ga.mutation.BitFlipMutation;
import rectangularcartogram.algos.ga.mutation.Mutation;
import rectangularcartogram.algos.ga.selection.RouletteSelection;
import rectangularcartogram.algos.ga.selection.Selection;
import rectangularcartogram.data.Pair;

public abstract class GeneticAlgorithm {

    // Parameters that influence the algorithm's bahaviour
    protected boolean[][] population; // population.length = populationSize, population[0].length = chromosomeLength (if populationSize >0)
    protected int chromosomeLength;
    protected int populationSize;
    protected double crossoverChance = 0.7;
    protected double elitistFraction = 0;
    protected Crossover crossover = new UniformCrossover();
    protected Mutation mutation = new BitFlipMutation(0.001);
    protected boolean allowDuplicates = true;
    protected Selection selection = new RouletteSelection();
    // Temporary fields used by the algorithm
    protected double[] quality;
    protected boolean[] bestIndividual;
    protected double bestQuality;
    protected static Random rand = new Random();
    // Debug settings
    protected static int DEBUG_LEVEL = 1; // 0 = no output, 1 = per generation summary, 2 = full population details, 3 = full debug spam
    protected int generation;

    /**
     * Initializes this GA with a random population consisting of populationSize individuals, each with chromosomeLength bits.
     * @param chromosomeLength
     * @param populationSize
     */
    public void initialize(int chromosomeLength, int populationSize) {
        this.chromosomeLength = chromosomeLength;
        this.populationSize = populationSize;
        population = new boolean[populationSize][chromosomeLength];

        randomizePopulation();

        quality = new double[populationSize];
        bestIndividual = new boolean[chromosomeLength];
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
     * Initializes this GA with a population consisting of a copy of the given population.
     * @param population
     */
    public void initialize(boolean[][] population) {
        if (population == null || population.length == 0) {
            chromosomeLength = 0;
            populationSize = 0;
        } else {
            populationSize = population.length;
            chromosomeLength = population[0].length;
        }

        this.population = new boolean[populationSize][chromosomeLength];

        for (int i = 0; i < populationSize; i++) {
            System.arraycopy(population[i], 0, this.population[i], 0, chromosomeLength);
        }

        quality = new double[populationSize];
        bestIndividual = new boolean[chromosomeLength];
        bestQuality = Double.NEGATIVE_INFINITY;
        computeQualities();

        generation = 0;

        if (DEBUG_LEVEL > 1) {
            System.out.println("Initial (given) population:");
            printPopulation();
            System.out.println();
        }
    }

    /**
     * Returns the best individual and quality found after the specified number of generations has been evolved.
     * @param nGenerations
     * @return
     */
    public Pair<boolean[], Double> getBestAfter(int nGenerations) {
        for (int i = 0; i < nGenerations; i++) {
            iterate();
        }

        return new Pair<boolean[], Double>(bestIndividual, bestQuality);
    }

    /**
     * Returns the best individual found when either the best quality is above the given threshold, or the maximum number of generations has been used. Note that in the second case, the returned solution may have a quality below the given threshold.
     * Also returns the quality of the returned solution and the number of generations used to find it.
     * @param threshold
     * @param maxGenerations
     * @return
     */
    public Pair<boolean[], Pair<Double, Integer>> getFirstAbove(double threshold, int maxGenerations) {
        while (generation < maxGenerations && bestQuality < threshold) {
            iterate();
        }

        return new Pair<boolean[], Pair<Double, Integer>>(bestIndividual, new Pair<Double, Integer>(bestQuality, generation));
    }

    public boolean[] getBestIndividual() {
        return bestIndividual;
    }

    public double getBestQuality() {
        return bestQuality;
    }

    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    public Crossover getCrossover() {
        return crossover;
    }

    public void setCrossover(Crossover crossover) {
        this.crossover = crossover;
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

    public Mutation getMutation() {
        return mutation;
    }

    /**
     * NOTE: also changes the mutation chance to the given mutation's mutation chance
     * @param mutation
     */
    public void setMutation(Mutation mutation) {
        this.mutation = mutation;
    }

    public double getMutationChance() {
        return mutation.getMutationChance();
    }

    public void setMutationChance(double mutationChance) {
        mutation.setMutationChance(mutationChance);
    }

    public Selection getSelection() {
        return selection;
    }

    public void setSelection(Selection selection) {
        this.selection = selection;
    }

    /**
     * Fill the entire population with random individuals
     */
    protected void randomizePopulation() {
        for (int i = 0; i < populationSize; i++) {
            if (allowDuplicates) {
                for (int j = 0; j < chromosomeLength; j++) {
                    population[i][j] = rand.nextBoolean();
                }
            } else {
                boolean unique = false;

                while (!unique) {
                    for (int j = 0; j < chromosomeLength; j++) {
                        population[i][j] = rand.nextBoolean();
                    }

                    unique = true;
                    for (int j = 0; j < i; j++) {
                        if (Arrays.equals(population[i], population[j])) {
                            unique = false;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute the quality of a single individual. A higher score should be better.
     * @param individual
     * @return
     */
    protected abstract double computeQuality(boolean[] individual);

    protected void iterate() {
        // generate offspring
        // pre-computation for the selection
        selection.preprocess(quality);

        if (DEBUG_LEVEL > 2) {
            System.out.println("Quality: " + Arrays.toString(quality));
        }

        // copy the best performing individuals
        boolean[][] newPopulation = new boolean[populationSize][chromosomeLength];
        int n = elitistSelection(newPopulation);

        // form the rest by crossover and mutation
        while (n < populationSize) {
            if (Math.random() < crossoverChance && n < populationSize - 1) {
                int i1 = selection.select(quality);
                int i2 = selection.select(quality);

                crossover(newPopulation, n, i1, i2);
                mutate(newPopulation, n);
                mutate(newPopulation, n + 1);

                if (allowDuplicates) {
                    n += 2;
                } else {
                    boolean new1 = true;
                    boolean new2 = !Arrays.equals(newPopulation[n], newPopulation[n + 1]);

                    for (int i = 0; i < n; i++) {
                        if (new1 && Arrays.equals(newPopulation[i], newPopulation[n])) {
                            new1 = false;
                        }

                        if (new2 && Arrays.equals(newPopulation[i], newPopulation[n + 1])) {
                            new2 = false;
                        }
                    }

                    if (!new1 && new2) {
                        System.arraycopy(newPopulation[n + 1], 0, newPopulation[n], 0, chromosomeLength);
                    }

                    n += 2;

                    if (!new1) {
                        n--;
                    }

                    if (!new2) {
                        n--;
                    }
                }
            } else {
                int i = selection.select(quality);

                System.arraycopy(population[i], 0, newPopulation[n], 0, chromosomeLength);
                mutate(newPopulation, n);

                if (allowDuplicates) {
                    n++;
                } else {
                    boolean newI = true;

                    for (int j = 0; j < n; j++) {
                        if (Arrays.equals(newPopulation[j], newPopulation[n])) {
                            newI = false;
                            break;
                        }
                    }

                    if (newI) {
                        n++;
                    }
                }
            }
        }

        // Replace the population by the new population
        for (int i = 0; i < populationSize; i++) {
            System.arraycopy(newPopulation[i], 0, population[i], 0, chromosomeLength);
        }

        // Compute qualities for the next round
        computeQualities();
        generation++;

        // Give debug output
        if (DEBUG_LEVEL > 1) {
            System.out.println(generation + ". New population:");
            printPopulation();
            System.out.println();
        } else if (DEBUG_LEVEL > 0) {
            System.out.print(generation + ". ");
            printBest(false);
            printAverage();
        }
    }

    protected void computeQualities() {
        for (int i = 0; i < populationSize; i++) {
            quality[i] = computeQuality(population[i]);

            if (quality[i] > bestQuality) {
                bestQuality = quality[i];
                System.arraycopy(population[i], 0, bestIndividual, 0, chromosomeLength);
            }
        }
    }

    protected int elitistSelection(boolean[][] newPopulation) {
        int n = (int) Math.ceil(elitistFraction * populationSize);

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
                printIndividual(population[bestQualityIndex], bestPopulationQuality);
            }

            System.arraycopy(population[bestQualityIndex], 0, newPopulation[0], 0, chromosomeLength);
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
                    printIndividual(population[qualities.get(i).getFirst()], qualities.get(i).getSecond());
                }
                System.out.println();
            }

            // Copy the best individuals to the new population
            for (int i = 0; i < n; i++) {
                System.arraycopy(population[qualities.get(i).getFirst()], 0, newPopulation[i], 0, chromosomeLength);
            }
        }

        return n;
    }

    /**
     * Forms two new children by crossing over population[i1] and population[i2]. The children are stored in newPopulation[n] and new Population[n + 1].
     * @param newPopulation
     * @param n
     * @param i1
     * @param i2
     */
    protected void crossover(boolean[][] newPopulation, int n, int i1, int i2) {
        if (DEBUG_LEVEL > 2) {
            System.out.print("Parent 1: ");
            printIndividual(population[i1]);
            System.out.print("Parent 2: ");
            printIndividual(population[i2]);
        }

        crossover.crossover(population[i1], population[i2], newPopulation[n], newPopulation[n + 1]);

        if (DEBUG_LEVEL > 2) {
            System.out.println("Performed " + crossover.toString() + " crossover.");
            System.out.print("Child 1: ");
            printIndividual(newPopulation[n]);
            System.out.print("Child 2: ");
            printIndividual(newPopulation[n + 1]);
        }
    }

    protected void mutate(boolean[][] newPopulation, int n) {
        if (DEBUG_LEVEL > 2) {
            System.out.println("Performing mutation.");
            System.out.print("Initial: ");
            printIndividual(newPopulation[n]);
        }

        mutation.mutate(newPopulation[n]);

        if (DEBUG_LEVEL > 2) {
            System.out.print("Result:  ");
            printIndividual(newPopulation[n]);
        }
    }

    private void printPopulation() {
        for (int i = 0; i < populationSize; i++) {
            printIndividual(population[i], quality[i]);
        }
        printBest(false);
        printAverage();
    }

    private void printBest() {
        System.out.print("Best: ");
        printIndividual(bestIndividual, bestQuality);
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

    private void printIndividual(boolean[] bs, double q, boolean newLine) {
        System.out.print("[");

        for (boolean b : bs) {
            if (b) {
                System.out.print("1");
            } else {
                System.out.print("0");
            }
        }

        if (newLine) {
            System.out.println("] (" + q + ")");
        } else {
            System.out.print("] (" + q + ")");
        }
    }

    private void printIndividual(boolean[] bs, double q) {
        printIndividual(bs, q, true);
    }

    private void printIndividual(boolean[] bs) {
        System.out.print("[");

        for (boolean b : bs) {
            if (b) {
                System.out.print("1");
            } else {
                System.out.print("0");
            }
        }

        System.out.println("]");
    }
}
