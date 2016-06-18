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
package rectangularcartogram;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileNameExtensionFilter;
import rectangularcartogram.algos.CartogramMaker;
import rectangularcartogram.algos.RELExtracter;
import rectangularcartogram.algos.ga.LabelingGA;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.BoundedCartographicErrorMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.CartographicErrorMeasure;
import rectangularcartogram.measures.CombinedMeasure;
import rectangularcartogram.measures.QualityMeasure;
import rectangularcartogram.measures.QualityMeasure.Fold;
import rectangularcartogram.webimport.WFBImporter;

public class MassExperimentsEurope {

    private static IloCplex cplex;
    private static File resultsDirectory;

    public static void main(String[] args) throws IloException, IOException, IncorrectGraphException {
        //runExperiments();
        summarizeData(true);
    }

    private static void runExperiments() throws IloException, IOException, IncorrectGraphException {
        // Obtain a CPLEX license
        cplex = new IloCplex();

        try {
            // Generate cartograms for each WFB ranked data set for which data for each country is available
            for (int i : WFBImporter.getDataSets().keySet()) {
                System.out.println("Trying data set " + i + ": " + WFBImporter.getDataSets().get(i));
                System.out.flush();

                // Load the map of Europe
                Subdivision europe = loadSubdivision(new File("../Maps/Europe/Europe Composite.sub"));

                // Import the current data set from the WFB
                try {
                    WFBImporter.importWeightsFromWFB(i, europe);
                } catch (IllegalArgumentException e) {
                    //System.err.println("Above errors were for data set " + i + " (" + dataSets.get(i) + ")");
                    continue;
                }

                // Create a new directory to store the results
                resultsDirectory = new File("../Data/Europe/Results_" + i);

                if (!resultsDirectory.exists()) {
                    resultsDirectory.mkdir();
                }

                // Make nice cartograms
                makeCartograms(europe);
            }
        } finally {
            cplex.end();
        }
    }

    private static void makeCartograms(Subdivision sub) throws IncorrectGraphException, IOException, IloException {
        // Quality Measures
        // Bounded MCE / average bbsd
        //BoundedCartographicErrorMeasure m4 = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), new BoundingBoxSeparationMeasure(sub), 0.05);

        // Weighted ACE / average bbsd
        CombinedMeasure m5 = new CombinedMeasure();
        m5.addMeasure(new CartographicErrorMeasure(sub, cplex), 7);
        m5.addMeasure(new BoundingBoxSeparationMeasure(sub), 3);

        QualityMeasure[] measures = {m5};
        String[] measureNames = {"Weighted ACE - average bbsd"};//{"Bounded MCE - average bbsd", "Weighted ACE - average bbsd"};

        // Genetic Algorithm
        LabelingGA ga = new LabelingGA(sub.getDualGraph(), null);
        LabelingGA.DEBUG_LEVEL = 0; // No output
        ga.setElitistFraction(0.04);
        ga.setCrossoverChance(0.05);
        ga.setMutationChance(0.9);
        ga.setSelection(new RankSelection(0.9));

        int maxGenerations = 50;
        int populationSize = 50;

        int m = 0;

        for (int i = 0; i < 20; i++) {
            long start = System.currentTimeMillis();

            ga.setMeasure(measures[m]);
            m = (m + 1) % measures.length;

            ga.initialize(populationSize);

            Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);

            sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

            CartogramMaker carto = new CartogramMaker(sub, cplex);
            carto.setAllowFalseSeaAdjacencies(true);
            carto.setMaxAspectRatio(12);
            carto.setSeaAreaFraction(0.2);
            carto.setUseMaxAspectRatio(true);
            carto.iterate(50);

            Subdivision cartogram = carto.getCartogram();

            long time = System.currentTimeMillis() - start;

            String avgError = String.format("%.3f", cartogram.getAverageCartographicError());
            String maxError = String.format("%.3f", cartogram.getMaximumCartographicError());

            saveSubdivision(cartogram, new File(resultsDirectory, avgError + " " + maxError + " " + time / 1000 + "s " + measureNames[(m - 1 + measures.length) % measures.length] + " (" + i + ").sub"));

            System.out.println("Found cartogram: " + avgError + " average and " + maxError + " maximum error in " + time / 1000 + " seconds");
        }
    }

    private static void summarizeData(boolean csv) throws IOException {
        // Go through the results and produce aggregate statistics
        FileNameExtensionFilter mySubFilter = new FileNameExtensionFilter("Subdivisions", "sub");

        // Load the map of Europe
        Subdivision europe = loadSubdivision(new File("../Maps/Europe/Europe Composite.sub"));
        BoundingBoxSeparationMeasure bbsdMeasure = new BoundingBoxSeparationMeasure(europe, Fold.AVERAGE_SQUARED, true, true);

        List<Integer> indices = new ArrayList<Integer>(WFBImporter.getDataSets().keySet());
        Collections.sort(indices);

        if (csv) {
            System.out.println("Data set index,Data set description,"
                    + "Average average cartographic error,Minimum average cartographic error,Maximum average cartographic error,"
                    + "Average maximum cartographic error,Minimum maximum cartographic error,Maximum maximum cartographic error,"
                    + "Average bounding box separation distance,Minimum bounding box separation distance,Maximum bounding box separation distance,"
                    + "Average running time,Minimum running time,Maximum running time");
        }

        for (int i : indices) {
            resultsDirectory = new File("../Data/Europe/Results_" + i);

            if (resultsDirectory.exists()) {
                // There are results for this data set; examine them
                File[] files = resultsDirectory.listFiles();

                int n = 0;
                double sumAverageError = 0;
                double minAverageError = Double.POSITIVE_INFINITY;
                double maxAverageError = Double.NEGATIVE_INFINITY;
                double sumMaximumError = 0;
                double minMaximumError = Double.POSITIVE_INFINITY;
                double maxMaximumError = Double.NEGATIVE_INFINITY;
                double sumBBSD = 0;
                double minBBSD = Double.POSITIVE_INFINITY;
                double maxBBSD = Double.NEGATIVE_INFINITY;
                double sumTarget = 0;
                double minTarget = Double.POSITIVE_INFINITY;
                double maxTarget = Double.NEGATIVE_INFINITY;
                int sumTime = 0;
                int minTime = Integer.MAX_VALUE;
                int maxTime = Integer.MIN_VALUE;

                for (File f : files) {
                    if (f.isFile() && mySubFilter.accept(f)) {
                        Subdivision sub = loadSubdivision(f);

                        if (sub != null) {
                            double avgError = sub.getAverageCartographicError();
                            double maxError = sub.getMaximumCartographicError();
                            int time = getTime(f.getName());

                            // Compute its REL
                            RegularEdgeLabeling rel = RELExtracter.findRegularEdgeLabeling(sub);

                            // Compute the mappings
                            Map<SubdivisionFace, SubdivisionFace> faceMap = computeFaceMapping(europe, sub);
                            Map<Edge, Edge> edgeMap = computeEdgeMapping(europe, sub, faceMap);

                            double bbsd = bbsdMeasure.getQuality(rel, edgeMap);
                            double target = 0.7 * sub.getAverageSquaredCartographicError() + 0.3 * bbsd;

                            n++;
                            sumAverageError += avgError;
                            minAverageError = Math.min(minAverageError, avgError);
                            maxAverageError = Math.max(maxAverageError, avgError);
                            sumMaximumError += maxError;
                            minMaximumError = Math.min(minMaximumError, maxError);
                            maxMaximumError = Math.max(maxMaximumError, maxError);
                            sumTime += time;
                            minTime = Math.min(minTime, time);
                            maxTime = Math.max(maxTime, time);
                            sumBBSD += bbsd;
                            minBBSD = Math.min(minBBSD, bbsd);
                            maxBBSD = Math.max(maxBBSD, bbsd);
                            sumTarget += target;
                            minTarget = Math.min(minTarget, target);
                            maxTarget = Math.max(maxTarget, target);
                        }
                    }
                }

                // Print results
                if (csv) {
                    // CSV
                    System.out.println(String.format("%d,%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%d,%d",
                            i, WFBImporter.getDataSets().get(i),
                            sumAverageError / n, minAverageError, maxAverageError,
                            sumMaximumError / n, minMaximumError, maxMaximumError,
                            sumBBSD / n, minBBSD, maxBBSD,
                            sumTime / (double) n, minTime, maxTime));
                } else {
                    // Verbose
                    System.out.println(i + ": " + WFBImporter.getDataSets().get(i));
                    System.out.println(String.format("Average Error: %f (%f - %f)", sumAverageError / n, minAverageError, maxAverageError));
                    System.out.println(String.format("Maximum Error: %f (%f - %f)", sumMaximumError / n, minMaximumError, maxMaximumError));
                    System.out.println(String.format("Average Squared Bounding Box Separation Distance: %f (%f - %f)", sumBBSD / n, minBBSD, maxBBSD));
                    System.out.println(String.format("Target value (0.7 * average squared cartographic error + 0.3 * average squared bbsd): %f (%f - %f)", sumTarget / n, minTarget, maxTarget));
                    System.out.println(String.format("Running Time: %f (%d - %d)", sumTime / (double) n, minTime, maxTime));
                    System.out.println();
                }
            }
        }
    }
    private static Pattern timePattern = Pattern.compile(" ([0-9]+)s ");

    private static int getTime(String name) {
        Matcher matcher = timePattern.matcher(name);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Name does not contain time information.");
        }
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

    private static Map<SubdivisionFace, SubdivisionFace> computeFaceMapping(Subdivision map, Subdivision sub) {
        HashMap<SubdivisionFace, SubdivisionFace> faceMap = new HashMap<SubdivisionFace, SubdivisionFace>(2 * map.getFaces().size());

        for (SubdivisionFace f1 : map.getFaces()) {
            if (!f1.isSea() && f1.getName() != null && !f1.getName().isEmpty()) {
                for (SubdivisionFace f2 : sub.getFaces()) {
                    if (f1.getName().equals(f2.getName())) {
                        faceMap.put(f1, f2);
                        break;
                    }
                }
            }
        }

        return faceMap;
    }

    private static Map<Edge, Edge> computeEdgeMapping(Subdivision map, Subdivision sub, Map<SubdivisionFace, SubdivisionFace> faceMap) {
        HashMap<Edge, Edge> edgeMap = new HashMap<Edge, Edge>(2 * map.getDualGraph().getEdges().size());

        for (Edge e1 : map.getDualGraph().getEdges()) {
            SubdivisionFace f1a = map.getFace(e1.getVA());
            SubdivisionFace f1b = map.getFace(e1.getVB());

            if (faceMap.containsKey(f1a) && faceMap.containsKey(f1b)) {
                SubdivisionFace f2a = faceMap.get(f1a);
                SubdivisionFace f2b = faceMap.get(f1b);

                for (Edge e2 : sub.getDualGraph().getEdges()) {
                    SubdivisionFace fa = sub.getFace(e2.getVA());
                    SubdivisionFace fb = sub.getFace(e2.getVB());

                    if ((fa == f2a && fb == f2b) || (fa == f2b && fb == f2a)) {
                        edgeMap.put(e1, e2);
                    }
                }
            }
        }

        return edgeMap;
    }
}
