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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class BoundingBoxSeparationMeasure extends QualityMeasure {

    private HashMap<Edge, double[]> weights; // weights for: [A->B and Blue, A->B and Red, B->A and Blue, B->A and Red]
    private double maxWeight;
    private boolean ignoreSeaSeaAdjacencies;
    private boolean ignoreSeaLandAdjacencies;

    public BoundingBoxSeparationMeasure(Subdivision sub) {
        this(sub, Fold.AVERAGE_SQUARED, true, false);
    }

    public BoundingBoxSeparationMeasure(Subdivision sub, Fold fold) {
        this(sub, fold, true, false);
    }

    public BoundingBoxSeparationMeasure(Subdivision sub, Fold fold, boolean ignoreSeaSeaAdjacencies, boolean ignoreSeaLandAdjacencies) {
        this.fold = fold;
        this.ignoreSeaSeaAdjacencies = ignoreSeaSeaAdjacencies;
        this.ignoreSeaLandAdjacencies = ignoreSeaLandAdjacencies;
        setHigherIsBetter(false);

        computeWeights(sub);
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        return getQuality(labeling, null);
    }

    public double getQuality(RegularEdgeLabeling labeling, Map<Edge, Edge> edgeMap) {
        double weight = 0;
        double[] edgeWeights = computeLabelingWeights(labeling, edgeMap);

        for (double w : edgeWeights) {
            switch (fold) {
                case MAXIMUM:
                    weight = Math.max(weight, w);
                    break;
                case AVERAGE:
                    weight += w;
                    break;
                case AVERAGE_SQUARED:
                    weight += w * w;
                    break;
                default:
                    throw new AssertionError("Unrecognized fold type: " + fold);
            }
        }

        return weight / maxWeight;
    }

    @Override
    public int compare(RegularEdgeLabeling labeling1, RegularEdgeLabeling labeling2) {
        if (fold == Fold.MAXIMUM) {
            double[] weights1 = computeLabelingWeights(labeling1, null);
            double[] weights2 = computeLabelingWeights(labeling2, null);

            // Sort the deviations in ascending order
            Arrays.sort(weights1);
            Arrays.sort(weights2);

            // Compare the largest deviations, then the second largest, etc. until one is better than the other.
            for (int i = weights1.length - 1; i >= 0; i--) {
                int comp = Double.compare(weights1[i], weights2[i]);

                if (comp != 0) {
                    //System.out.println("Comparison ended on " + (weights1.length - i) + "th distance.");
                    if (higherIsBetter()) {
                        return comp;
                    } else {
                        return -comp;
                    }
                }
            }

            return 0;
        } else {
            // Just compare the qualities
            return super.compare(labeling1, labeling2);
        }
    }

    private double[] computeLabelingWeights(RegularEdgeLabeling labeling, Map<Edge, Edge> edgeMap) {
        double[] edgeWeights = new double[weights.size()];
        int i = 0;

        for (Entry<Edge, double[]> entry : weights.entrySet()) {
            double[] weight = entry.getValue();
            Pair<Labeling, Direction> label = (edgeMap == null ? labeling.get(entry.getKey()) : labeling.get(edgeMap.get(entry.getKey())));

            if (label.getSecond() == Direction.AB) {
                if (label.getFirst() == Labeling.BLUE) {
                    edgeWeights[i] = weight[0];
                } else {
                    edgeWeights[i] = weight[1];
                }
            } else {
                if (label.getFirst() == Labeling.BLUE) {
                    edgeWeights[i] = weight[2];
                } else {
                    edgeWeights[i] = weight[3];
                }
            }

            i++;
        }

        return edgeWeights;
    }

    private void computeWeights(Subdivision sub) {
        weights = new HashMap<Edge, double[]>(sub.getDualGraph().getEdges().size() * 2);
        maxWeight = 0;
        HashMap<Vertex, double[]> boundingBoxes = new HashMap<Vertex, double[]>(sub.getDualGraph().getVertices().size() * 2);

        for (SubdivisionFace face : sub.getFaces()) {
            boundingBoxes.put(face.getCorrespondingVertex(), getBoundingBox(face.getVertices()));
        }

        for (Edge edge : sub.getDualGraph().getEdges()) {
            if (considerEdge(edge, sub)) {
                double[] distances = getDistances(boundingBoxes.get(edge.getVA()), boundingBoxes.get(edge.getVB()));

                weights.put(edge, distances);

                double max = Math.max(distances[0], Math.max(distances[1], Math.max(distances[2], distances[3])));

                switch (fold) {
                    case MAXIMUM:
                        maxWeight = Math.max(maxWeight, max);
                        break;
                    case AVERAGE:
                        maxWeight += max;
                        break;
                    case AVERAGE_SQUARED:
                        maxWeight += max * max;
                        break;
                }
            }
        }
    }

    private boolean considerEdge(Edge e, Subdivision subdivision) {
        boolean seaA = subdivision.getFace(e.getVA()).isSea();
        boolean seaB = subdivision.getFace(e.getVB()).isSea();

        if (seaA) {
            if (seaB) {
                return !ignoreSeaSeaAdjacencies;
            } else {
                return !ignoreSeaLandAdjacencies;
            }
        } else {
            if (seaB) {
                return !ignoreSeaLandAdjacencies;
            } else {
                return true;
            }
        }
    }

    private double[] getBoundingBox(List<Vertex> vertices) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
        }

        return new double[]{minX, minY, maxX, maxY};
    }

    /**
     * distances[0] <-> AB, BLUE
     * distances[1] <-> AB, RED
     * distances[2] <-> BA, BLUE
     * distances[3] <-> BA, RED
     * @param box1
     * @param box2
     * @return
     */
    private double[] getDistances(double[] box1, double[] box2) {
        return new double[]{box1[2] - box2[0], box1[3] - box2[1], box2[2] - box1[0], box2[3] - box1[1]};
    }
}
