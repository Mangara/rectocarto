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
import java.util.Map;
import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;

public class AngleDeviationMeasure extends QualityMeasure {

    private HashMap<Edge, Double> edgeAngles;
    private double maxDeviation;
    private boolean ignoreSeaSeaAdjacencies;
    private boolean ignoreSeaLandAdjacencies;

    public AngleDeviationMeasure(Subdivision sub) {
        this(sub, Fold.AVERAGE_SQUARED, true, false);
    }

    public AngleDeviationMeasure(Subdivision sub, Fold fold) {
        this(sub, fold, true, false);
    }

    public AngleDeviationMeasure(Subdivision subdivision, Fold fold, boolean ignoreSeaSeaAdjacencies, boolean ignoreSeaLandAdjacencies) {
        setFold(fold);
        this.ignoreSeaSeaAdjacencies = ignoreSeaSeaAdjacencies;
        this.ignoreSeaLandAdjacencies = ignoreSeaLandAdjacencies;
        setHigherIsBetter(false);

        computeAngles(subdivision);
        maxDeviation = Math.PI;
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        return getQuality(labeling, null);
    }

    public double getQuality(RegularEdgeLabeling labeling, Map<Edge, Edge> edgeMap) {
        double deviation = 0;
        double[] deviations = computeDeviations(labeling, edgeMap);

        for (double d : deviations) {
            switch (fold) {
                case MAXIMUM:
                    deviation = Math.max(deviation, d);
                    break;
                case AVERAGE:
                    deviation += d;
                    break;
                case AVERAGE_SQUARED:
                    deviation += d * d;
                    break;
            }
        }

        switch (fold) {
            case MAXIMUM:
                return deviation / maxDeviation;
            case AVERAGE:
                return deviation / (maxDeviation * edgeAngles.size());
            case AVERAGE_SQUARED:
                return deviation / (maxDeviation * maxDeviation * edgeAngles.size());
            default:
                throw new AssertionError("Unexpected Fold type: " + fold);
        }
    }

    @Override
    public int compare(RegularEdgeLabeling labeling1, RegularEdgeLabeling labeling2) {
        if (getFold() == Fold.MAXIMUM) {
            double[] deviations1 = computeDeviations(labeling1, null);
            double[] deviations2 = computeDeviations(labeling2, null);

            // Sort the deviations in ascending order
            Arrays.sort(deviations1);
            Arrays.sort(deviations2);

            // Compare the largest deviations, then the second largest, etc. until one is better than the other.
            for (int i = deviations1.length - 1; i >= 0; i--) {
                int comp = Double.compare(deviations1[i], deviations2[i]);

                if (comp != 0) {
                    //System.out.println("Comparison ended on " + (deviations1.length - i) + "th deviation.");
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

    private double[] computeDeviations(RegularEdgeLabeling labeling, Map<Edge, Edge> edgeMap) {
        double[] deviations = new double[edgeAngles.size()];
        int i = 0;

        for (Entry<Edge, Double> entry : edgeAngles.entrySet()) {
            Edge edge = entry.getKey();
            double angle = entry.getValue();

            Pair<Labeling, Direction> label = (edgeMap == null ? labeling.get(edge) : labeling.get(edgeMap.get(edge)));

            if (label.getFirst() == Labeling.RED) {
                angle = 0.5 * Math.PI - angle;
            }

            if (wrongDirection(edge, label.getFirst(), label.getSecond())) {
                angle = Math.PI - angle;
            }

            deviations[i] = angle;
            i++;
        }

        return deviations;
    }

    private void computeAngles(Subdivision subdivision) {
        edgeAngles = new HashMap<Edge, Double>(subdivision.getDualGraph().getEdges().size() * 2);

        for (Edge e : subdivision.getDualGraph().getEdges()) {
            if (considerEdge(e, subdivision)) {
                Vertex a = e.getVA();
                Vertex b = e.getVB();

                double angle = Math.atan(Math.abs(b.getY() - a.getY()) / Math.abs(b.getX() - a.getX()));

                edgeAngles.put(e, angle);
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

    /**
     * Assumption: edge is labeled either red or blue and is directed
     * @param edge
     * @param labeling
     * @param direction
     * @return
     */
    private boolean wrongDirection(Edge edge, Labeling labeling, Direction direction) {
        if (labeling == Labeling.RED) {
            // The origin's y-coordinate should be lower than the destination's y-coordinate
            // but we want to know when it's wrong:
            if (direction == Direction.AB) {
                return edge.getVA().getY() > edge.getVB().getY();
            } else {
                return edge.getVB().getY() > edge.getVA().getY();
            }
        } else {
            // The origin's x-coordinate should be lower than the destination's x-coordinate
            // but we want to know when it's wrong:
            if (direction == Direction.AB) {
                return edge.getVA().getX() > edge.getVB().getX();
            } else {
                return edge.getVB().getX() > edge.getVA().getX();
            }
        }
    }
}
