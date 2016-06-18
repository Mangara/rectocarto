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

import ilog.cplex.IloCplex;
import java.util.HashMap;
import java.util.Map;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.measures.QualityMeasure.Fold;

public class ResultingAngleDeviationMeasure extends CartogramQualityMeasure {

    private HashMap<Edge, Double> edgeAngles;
    // Settings
    private boolean ignoreSeaSeaAdjacencies = true;
    private boolean ignoreSeaLandAdjacencies = false;

    public ResultingAngleDeviationMeasure(Subdivision sub, IloCplex cplex) {
        this(sub, cplex, Fold.AVERAGE_SQUARED, 10, true, false);
    }

    public ResultingAngleDeviationMeasure(Subdivision sub, IloCplex cplex, Fold fold) {
        this(sub, cplex, fold, 10, true, false);
    }

    public ResultingAngleDeviationMeasure(Subdivision sub, IloCplex cplex, Fold fold, int nIterations, boolean ignoreSeaSeaAdjacencies, boolean ignoreSeaLandAdjacencies) {
        super(sub, cplex, nIterations);
        setHigherIsBetter(false);
        this.fold = fold;
        this.ignoreSeaSeaAdjacencies = ignoreSeaSeaAdjacencies;
        this.ignoreSeaLandAdjacencies = ignoreSeaLandAdjacencies;
        computeAngles();
    }

    @Override
    public double getCartogramQuality(Subdivision cartogram, Map<SubdivisionFace, SubdivisionFace> faceMap) {
        double deviation = 0;

        for (Edge edge : sub.getDualGraph().getEdges()) {
            if (considerEdge(edge, sub)) {
                double cartoAngle = getAngle(faceMap.get(sub.getFace(edge.getVA())).getCorrespondingVertex(), faceMap.get(sub.getFace(edge.getVB())).getCorrespondingVertex());
                double originalAngle = edgeAngles.get(edge);

                double d = Math.min(Math.abs(cartoAngle - originalAngle), 2 * Math.PI - Math.abs(cartoAngle - originalAngle));

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
                    default:
                        throw new AssertionError("Unrecognized fold type: " + fold);
                }
            }
        }

        switch (fold) {
            case MAXIMUM:
                return deviation / Math.PI;
            case AVERAGE:
                return deviation / (Math.PI * edgeAngles.size());
            case AVERAGE_SQUARED:
                return deviation / (Math.PI * Math.PI * edgeAngles.size());
            default:
                throw new AssertionError("Unrecognized Fold type: " + fold);
        }
    }

    public boolean isIgnoreSeaLandAdjacencies() {
        return ignoreSeaLandAdjacencies;
    }

    public void setIgnoreSeaLandAdjacencies(boolean ignoreSeaLandAdjacencies) {
        this.ignoreSeaLandAdjacencies = ignoreSeaLandAdjacencies;
    }

    public boolean isIgnoreSeaSeaAdjacencies() {
        return ignoreSeaSeaAdjacencies;
    }

    public void setIgnoreSeaSeaAdjacencies(boolean ignoreSeaSeaAdjacencies) {
        this.ignoreSeaSeaAdjacencies = ignoreSeaSeaAdjacencies;
    }

    private void computeAngles() {
        edgeAngles = new HashMap<Edge, Double>(sub.getDualGraph().getEdges().size() * 2);

        for (Edge e : sub.getDualGraph().getEdges()) {
            if (considerEdge(e, sub)) {
                edgeAngles.put(e, getAngle(e.getVA(), e.getVB()));
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
     * Returns the counter-clockwise angle from the positive x-axis to the line segment ab with the origin in a.
     * @param a
     * @param b
     * @return
     */
    private double getAngle(Vertex a, Vertex b) {
        double angle = Math.atan((b.getY() - a.getY()) / (b.getX() - a.getX()));

        if (b.getX() < a.getX()) {
            angle += Math.PI;
        } else if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }
}
