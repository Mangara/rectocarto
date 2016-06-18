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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.algos.ga.crossover.UniformCrossover;
import rectangularcartogram.algos.ga.mutation.BitFlipMutation;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeColoring;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.QualityMeasure;

public class SimpleLabelingGA extends GeneticAlgorithm {

    private CycleGraph graph;
    private QualityMeasure measure;
    private HashMap<Edge, Labeling> fixed;

    public SimpleLabelingGA(Graph graph, QualityMeasure measure) {
        this.graph = new CycleGraph(graph);
        this.measure = measure;

        setElitistFraction(0.01);
        setCrossoverChance(0.8);
        setCrossover(new UniformCrossover());
        setMutation(new BitFlipMutation(0.01));
        setSelection(new RankSelection(0.8));

        fixEdges();
    }

    public void initialize(int populationSize) {
        initialize(graph.getEdges().size() - fixed.size(), populationSize);
    }

    public void findBestLabeling(int maxGenerations) throws IncorrectGraphException {
        Pair<boolean[], Double> result = getBestAfter(maxGenerations);

        if (result.getSecond() >= graph.getVertices().size()) {
            System.out.println("All " + graph.getVertices().size() + " vertex constraints satisfied.");
        } else {
            System.out.println(result.getSecond() + " of " + graph.getVertices().size() + " vertex constraints satisfied.");
        }

        RegularEdgeLabeling bestLabeling = new RegularEdgeLabeling(getColoring(result.getFirst()));

        System.out.println("Quality: " + measure.getQuality(bestLabeling));

        graph.setRegularEdgeLabeling(bestLabeling);
    }

    @Override
    protected double computeQuality(boolean[] individual) {
        // return the number of vertices that satisfy the local constraints
        // if all vertices satisfy the constraints, add the quality obtained from the quality measure
        RegularEdgeColoring coloring = getColoring(individual);

        int correctVertices = 4;

        for (Vertex vertex : graph.getVertices()) {
            if (!graph.getExteriorVertices().contains(vertex)) {
                if (checkVertexLabeling(vertex, coloring)) {
                    correctVertices++;
                }
            }
        }

        if (correctVertices == graph.getVertices().size()) {
            try {
                double q = measure.getQuality(new RegularEdgeLabeling(coloring));

                if (measure.higherIsBetter()) {
                    return correctVertices + q;
                } else {
                    return correctVertices + 1 / q;
                }
            } catch (IncorrectGraphException ex) {
                Logger.getLogger(SimpleLabelingGA.class.getName()).log(Level.SEVERE, null, ex);
                return correctVertices;
            }
        } else {
            return correctVertices;
        }
    }

    private void fixEdges() {
        fixed = new HashMap<Edge, Labeling>(graph.getEdges().size());

        List<Pair<Vertex, Edge>> boundary = new ArrayList<Pair<Vertex, Edge>>();

        for (Edge edge : graph.getEdges()) {
            boolean aIsNS = edge.getVA() == graph.getVN() || edge.getVA() == graph.getVS();
            boolean bIsNS = edge.getVB() == graph.getVN() || edge.getVB() == graph.getVS();
            boolean aIsEW = edge.getVA() == graph.getVE() || edge.getVA() == graph.getVW();
            boolean bIsEW = edge.getVB() == graph.getVE() || edge.getVB() == graph.getVW();

            boolean incidentNS = aIsNS || bIsNS;
            boolean incidentEW = aIsEW || bIsEW;

            if (incidentNS && !incidentEW) {
                fixed.put(edge, Labeling.RED);
            } else if (incidentEW && !incidentNS) {
                fixed.put(edge, Labeling.BLUE);
            } else if (incidentNS && incidentEW) { // Exterior edge
                fixed.put(edge, Labeling.NONE);
            }

            // Add the non-exterior vertex to the boundary for further processing
            if (!(incidentEW && incidentNS)) {
                if (aIsNS || aIsEW) {
                    boundary.add(new Pair<Vertex, Edge>(edge.getVB(), edge));
                } else if (bIsNS || bIsEW) {
                    boundary.add(new Pair<Vertex, Edge>(edge.getVA(), edge));
                }
            }
        }

        for (Pair<Vertex, Edge> p : boundary) {
            Vertex v = p.getFirst();

            List<Edge> edges = v.getEdges();

            int exteriorEdgeIndex = edges.indexOf(p.getSecond());

            Labeling l = fixed.get(p.getSecond());

            if (edges.size() == 4) {
                // All edges around this vertex are fixed
                fixed.put(edges.get((exteriorEdgeIndex + 2) % 4), l);
            }

            Labeling ll = (l == Labeling.RED ? Labeling.BLUE : Labeling.RED);

            fixed.put(edges.get((exteriorEdgeIndex + 1) % edges.size()), ll);
            fixed.put(edges.get((exteriorEdgeIndex - 1 + edges.size()) % edges.size()), ll);
        }
    }

    private RegularEdgeColoring getColoring(boolean[] individual) {
        RegularEdgeColoring coloring = new RegularEdgeColoring(graph);

        int i = 0;

        for (Edge edge : graph.getEdges()) {
            Labeling l = fixed.get(edge);

            if (l == null) {
                coloring.put(edge, individual[i] ? Labeling.RED : Labeling.BLUE);
                i++;
            } else {
                coloring.put(edge, l);
            }
        }

        return coloring;
    }

    /**
     * A vertex should have blue incoming edges, red outgoing edges, blue outgoing edges and red incoming edges, in clockwise direction around itself.
     * This method only checks the colours, ignoring the directions. Unlabeled edges are ignored as well.
     * @param v
     * @return true if it is possible to label the unlabeled edges such that this vertex satisfies constraint C1 with respect to the edge colours (not the orientations)
     */
    private boolean checkVertexLabeling(Vertex v, HashMap<Edge, Labeling> labeling) {
        List<Edge> edges = v.getEdges();

        // If there are less than 4 edges, this is never going to work
        if (edges.size() < 4) {
            return false;
        }

        // See what intervals we have
        List<Labeling> intervals = new ArrayList<Labeling>();
        int redIntervals = 0;
        int blueIntervals = 0;
        int unlabeledIntervals = 0;

        for (Edge edge : edges) {
            Labeling label = labeling.get(edge);

            // Skip this label if it fits into the current interval
            if (intervals.isEmpty() || label != intervals.get(intervals.size() - 1)) {
                intervals.add(label);

                switch (label) {
                    case RED:
                        redIntervals++;
                        break;
                    case BLUE:
                        blueIntervals++;
                        break;
                    case NONE:
                        unlabeledIntervals++;
                        break;
                }
            }
        }

        // If the first and last interval are the same, merge them
        if (intervals.size() > 1 && intervals.get(0) == intervals.get(intervals.size() - 1)) {
            intervals.remove(intervals.size() - 1);

            switch (intervals.get(0)) {
                case RED:
                    redIntervals--;
                    break;
                case BLUE:
                    blueIntervals--;
                    break;
                case NONE:
                    unlabeledIntervals--;
                    break;
            }
        }

        if (redIntervals == 0 && blueIntervals == 0) {
            // All unlabeled edges, so they can still be labeled correctly
            return true;
        } else if (blueIntervals == 0) {
            // If there is only one interval of red edges, we need at least three unlabeled edges
            if (redIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    Labeling label = labeling.get(edge);

                    if (label == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                return nUnlabeled >= 3;
            } else {
                // If there are more, there must also be at least two unlabeled intervals separating them
                // so these can be coloured blue, which satiesfies the vertex constraint
                return true;
            }
        } else if (redIntervals == 0) {
            // If there is only one interval of blue edges, we need at least three unlabeled edges
            if (blueIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    Labeling label = labeling.get(edge);

                    if (label == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                return nUnlabeled >= 3;
            } else {
                // If there are more, there must also be at least two unlabeled intervals separating them
                // so these can be coloured red, which satiesfies the vertex constraint
                return true;
            }
        } else {
            // We have both red and blue intervals
            if (unlabeledIntervals == 0) {
                return redIntervals == 2 && blueIntervals == 2;
            } else if (redIntervals == 1 && blueIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    Labeling label = labeling.get(edge);

                    if (label == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                if (unlabeledIntervals == 1) {
                    // if there are at least two unlabeled edges between the red and blue intervals, it can be labeled, otherwise it's impossible
                    return nUnlabeled >= 2;
                } else {
                    // there are two unlabeled intervals between the red and blue intervals,
                    // if one of those has at least 2 edges, we can separate the intervals and create a valid labeling
                    // this is the case iff there are at least 3 unlabeled edges
                    return nUnlabeled >= 3;
                }
            } else if (redIntervals == 1 || blueIntervals == 1) {
                // the multiple intervals of the other colour must be separated on one side by a NONE interval. This interval can be given the opposite colour, to produce a valid labeling
                return true;
            } else {
                // let's see what we have left if we omit the unlabeled edges
                List<Labeling> realIntervals = new ArrayList<Labeling>();
                int realRedIntervals = 0;
                int realBlueIntervals = 0;

                for (Edge edge : edges) {
                    Labeling label = labeling.get(edge);

                    // Skip this label if it fits into the current interval or if its NONE
                    if (label != Labeling.NONE && (realIntervals.isEmpty() || label != realIntervals.get(realIntervals.size() - 1))) {
                        realIntervals.add(label);

                        switch (label) {
                            case RED:
                                realRedIntervals++;
                                break;
                            case BLUE:
                                realBlueIntervals++;
                                break;
                        }
                    }
                }

                // If the first and last interval are the same, merge them
                if (realIntervals.size() > 1 && realIntervals.get(0) == realIntervals.get(realIntervals.size() - 1)) {
                    realIntervals.remove(realIntervals.size() - 1);

                    switch (realIntervals.get(0)) {
                        case RED:
                            realRedIntervals--;
                            break;
                        case BLUE:
                            realBlueIntervals--;
                            break;
                    }
                }

                if (realRedIntervals <= 2 && realBlueIntervals <= 2) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
