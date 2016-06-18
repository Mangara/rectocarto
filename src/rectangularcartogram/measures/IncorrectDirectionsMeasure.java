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
package rectangularcartogram.measures;

import java.util.List;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.exceptions.IncorrectDirectionException;

public class IncorrectDirectionsMeasure extends QualityMeasure {

    private Graph graph;

    public IncorrectDirectionsMeasure(Graph graph) {
        this.graph = graph;
        setHigherIsBetter(true);
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        int nIncorrectDirections = 0;

        for (Vertex vertex : graph.getVertices()) {
            if (!graph.getExteriorVertices().contains(vertex)) {
                nIncorrectDirections += getIncorrectDirections(vertex, labeling);
            }
        }

        if (nIncorrectDirections > 0) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>> INCORRECT DIRECTIONS <<<<<<<<<<<<<<<<<<<<<<<<");
        }

        return nIncorrectDirections;
    }

    private int getIncorrectDirections(Vertex v, RegularEdgeLabeling labeling) {
        // All edges in the same interval should have the same direction
        int nIncorrect = 0;

        List<Edge> edges = v.getEdges();

        Labeling currentLabel = labeling.get(edges.get(edges.size() - 1)).getFirst();
        boolean outgoing = isOutgoing(edges.get(edges.size() - 1), v, labeling.get(edges.get(edges.size() - 1)).getSecond());

        for (Edge edge : v.getEdges()) {
            Labeling edgeLabel = labeling.get(edge).getFirst();
            boolean edgeOutgoing = isOutgoing(edge, v, labeling.get(edge).getSecond());

            if (edgeLabel == currentLabel) {
                if (outgoing != edgeOutgoing) {
                    nIncorrect++;
                }
            } else {
                currentLabel = edgeLabel;

                if (edgeLabel == Labeling.RED) {
                    outgoing = !outgoing;
                }

                if (outgoing != edgeOutgoing) {
                    nIncorrect++;
                }
            }
        }

        return nIncorrect;
    }

    private static boolean isOutgoing(Edge e, Vertex v, Direction d) {
        if (e.getVA() == v) {
            return d == Direction.AB;
        } else if (e.getVB() == v) {
            return d == Direction.BA;
        } else {
            throw new IllegalArgumentException("Vertex should be one of the edge endpoints");
        }
    }

    public static void checkDirections(Graph g, RegularEdgeLabeling labeling) throws IncorrectDirectionException {
        for (Vertex v : g.getVertices()) {
            if (!g.getExteriorVertices().contains(v)) {
                List<Edge> edges = v.getEdges();

                boolean allDirected = true;

                // Check if all edges of v are directed
                for (Edge edge : edges) {
                    if (labeling.get(edge).getSecond() == Direction.NONE) {
                        allDirected = false;
                        break;
                    }
                }

                if (allDirected) {
                    Labeling currentLabel = labeling.get(edges.get(edges.size() - 1)).getFirst();
                    boolean outgoing = isOutgoing(edges.get(edges.size() - 1), v, labeling.get(edges.get(edges.size() - 1)).getSecond());

                    for (Edge edge : v.getEdges()) {
                        Labeling edgeLabel = labeling.get(edge).getFirst();
                        boolean edgeOutgoing = isOutgoing(edge, v, labeling.get(edge).getSecond());

                        if (edgeLabel == currentLabel) {
                            if (outgoing != edgeOutgoing) {
                                System.out.println("Labels: ");

                                for (Edge edge1 : edges) {
                                    System.out.print(" " + labeling.get(edge1).getFirst());
                                }

                                System.out.println();
                                System.out.println("Directions: ");

                                for (Edge edge1 : edges) {
                                    System.out.print(" " + isOutgoing(edge1, v, labeling.get(edge1).getSecond()));
                                }

                                System.out.println();

                                throw new IncorrectDirectionException(v);
                            }
                        } else {
                            currentLabel = edgeLabel;

                            if (edgeLabel == Labeling.RED) {
                                outgoing = !outgoing;
                            }

                            if (outgoing != edgeOutgoing) {
                                System.out.println("Labels: ");

                                for (Edge edge1 : edges) {
                                    System.out.print(" " + labeling.get(edge1).getFirst());
                                }

                                System.out.println();
                                System.out.println("Directions: ");

                                for (Edge edge1 : edges) {
                                    System.out.print(" " + isOutgoing(edge1, v, labeling.get(edge1).getSecond()));
                                }

                                System.out.println();

                                throw new IncorrectDirectionException(v);
                            }
                        }
                    }
                }
            }
        }
    }
}
