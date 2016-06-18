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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import rectangularcartogram.algos.DiameterCounter;
import rectangularcartogram.algos.FusyLabelingTraverser;
import rectangularcartogram.algos.LabelingTraverser;
import rectangularcartogram.algos.MinimumLabelingComputer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class RandomLabelingTest extends LabelingTraverser {

    private HashMap<String, Integer> count;
    private RegularEdgeLabeling minimum;
    private long diameter;
    private Random rand = new Random();

    public static void main(String[] args) throws IOException, IncorrectGraphException {
        //Graph graph = loadGraph(new File("3x3 Triangulated Grid.grp"));
        Graph graph = loadSubdivision(new File("Europe Population.sub")).getDualGraph();

        new RandomLabelingTest(graph);
    }

    public RandomLabelingTest(Graph graph) throws IncorrectGraphException {
        super(graph);

        // Set all counts to 0
        count = new LinkedHashMap<String, Integer>();
        //traverseLabelings();

        // Generate a lot of random labelings and check out the distribution
        long n = 10000;

        long start = System.currentTimeMillis();
        testRandomWalk(n);
        //testRandomFusy(n);
        long time = System.currentTimeMillis() - start;

        // Compute statistics on the distribution?
        long nLabelings = count.keySet().size();
        double sumSquaredError = 0;

        System.out.println("n: " + n + " nLabelings: " + nLabelings + " in " + time + " ms");

        /*for (Entry<String, Integer> entry : count.entrySet()) {
            //System.out.println(entry.getKey() + " - " + entry.getValue() + " (" + 100 * entry.getValue() / (double) n + " %)");
            double error = (entry.getValue() - (n / (double) nLabelings)) / 1000.0;
            sumSquaredError += error * error;
        }

        System.out.println("Mean Square Error: " + sumSquaredError / nLabelings);*/
    }

    private void testRandomWalk(long n) throws IncorrectGraphException {
        minimum = MinimumLabelingComputer.getMinimalLabeling(graph);
        DiameterCounter dc = new DiameterCounter(graph);
        dc.countDiameter();
        diameter = dc.getCount();

        long i = 0;

        while (i < n) {
            RegularEdgeLabeling rel = randomLabeling2();

            if (rel != null) {
                i++;
                String id = getIdentifier(rel);

                if (count.get(id) == null) {
                    count.put(id, 1);
                } else {
                    count.put(id, count.get(id) + 1);
                }
            }
        }
    }

    private void testRandomFusy(long n) throws IncorrectGraphException {
        FusyLabelingTraverser t = new FusyLabelingTraverser(graph) {

            @Override
            protected void processLabeling(RegularEdgeLabeling labeling) {
            }
        };

        for (long i = 0; i < n; i++) {
            String id = getIdentifier(t.generateRandomLabeling());

            if (count.get(id) == null) {
                count.put(id, 1);
            } else {
                count.put(id, count.get(id) + 1);
            }
        }
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

    @Override
    protected void processLabeling(RegularEdgeLabeling labeling) {
        count.put(getIdentifier(labeling), 0);
    }

    private String getIdentifier(RegularEdgeLabeling rel) {
        StringBuilder sb = new StringBuilder();

        for (Edge edge : graph.getEdges()) {
            Pair<Graph.Labeling, Edge.Direction> label = rel.get(edge);

            if (label == null) {
                sb.append("n");
            } else {
                switch (label.getFirst()) {
                    case BLUE:
                        sb.append("B");
                        break;
                    case RED:
                        sb.append("R");
                        break;
                    case PATH:
                        sb.append("$");
                        break;
                    case NONE:
                        sb.append("n");
                        break;
                }

                /*switch (label.getSecond()) {
                case AB:
                sb.append(">");
                break;
                case BA:
                sb.append("<");
                break;
                case NONE:
                sb.append("-");
                }*/
            }
        }

        return sb.toString();
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

    private static Graph loadGraph(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Graph.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
