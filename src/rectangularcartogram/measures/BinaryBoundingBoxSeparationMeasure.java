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

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class BinaryBoundingBoxSeparationMeasure extends QualityMeasure {

    private RegularEdgeLabeling correctLabels;

    public BinaryBoundingBoxSeparationMeasure(Subdivision sub, CycleGraph graph) {
        findCorrectLabels(sub, graph);
        setHigherIsBetter(true);
    }

    private void findCorrectLabels(Subdivision sub, CycleGraph graph) {
        correctLabels = new RegularEdgeLabeling(graph);

        // Compute the bounding box for every face
        HashMap<Vertex, double[]> boundingBoxes = new HashMap<Vertex, double[]>(sub.getDualGraph().getVertices().size() * 2);

        for (SubdivisionFace face : sub.getFaces()) {
            boundingBoxes.put(face.getCorrespondingVertex(), getBoundingBox(face.getVertices()));
        }

        // Each edge should cause the minimal amount of separation distance
        for (Edge edge : sub.getDualGraph().getEdges()) {
            double[] distances = getDistances(boundingBoxes.get(edge.getVA()), boundingBoxes.get(edge.getVB()));
            Labeling expectedLabel;
            Direction expectedDirection;

            int minIndex = 0;
            double minDistance = distances[0];

            for (int i = 1; i < distances.length; i++) {
                if (distances[i] < minDistance) {
                    minIndex = i;
                    minDistance = distances[i];
                }
            }

            switch (minIndex) {
                case 0: expectedLabel = Labeling.BLUE; expectedDirection = Direction.AB; break;
                case 1: expectedLabel = Labeling.RED;  expectedDirection = Direction.AB; break;
                case 2: expectedLabel = Labeling.BLUE; expectedDirection = Direction.BA; break;
                case 3: expectedLabel = Labeling.RED;  expectedDirection = Direction.BA; break;
                default: throw new InternalError("Impossible minimum index");
            }

            correctLabels.put(edge, new Pair<Labeling, Direction>(expectedLabel, expectedDirection));
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

    private double[] getDistances(double[] box1, double[] box2) {
        return new double[]{box1[2] - box2[0], box1[3] - box2[1], box2[2] - box1[0], box2[3] - box1[1]};
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        int nCorrect = 0;

        for (Entry<Edge, Pair<Labeling, Direction>> entry : labeling.entrySet()) {
            if (entry.getValue().equals(correctLabels.get(entry.getKey()))) {
                nCorrect++;
            }
        }

        return nCorrect / (double) correctLabels.size();
    }

}
