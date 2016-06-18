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

import java.util.Map.Entry;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;

public class BinaryAngleDeviationMeasure extends QualityMeasure {

    private RegularEdgeLabeling correctLabels;

    public BinaryAngleDeviationMeasure(CycleGraph graph) {
        findCorrectLabels(graph);
        setHigherIsBetter(true);
    }

    private void findCorrectLabels(CycleGraph graph) {
        correctLabels = new RegularEdgeLabeling(graph);

        for (Edge edge : graph.getEdges()) {
            Vertex a = edge.getVA();
            Vertex b = edge.getVB();

            double angle = Math.atan(Math.abs(b.getY() - a.getY()) / Math.abs(b.getX() - a.getX()));

            Labeling expectedLabel;

            if (angle < 0.125 * Math.PI) {
                expectedLabel = Labeling.BLUE;
            } else if (angle > 0.375 * Math.PI) {
                expectedLabel = Labeling.RED;
            } else {
                expectedLabel = Labeling.NONE;
            }

            Direction expectedDirection;

            if (expectedLabel == Labeling.BLUE) {
                expectedDirection = (a.getX() <= b.getX() ? Direction.AB : Direction.BA);
            } else if (expectedLabel == Labeling.RED) {
                expectedDirection = (a.getY() <= b.getY() ? Direction.AB : Direction.BA);
            } else {
                expectedDirection = Direction.NONE;
            }

            correctLabels.put(edge, new Pair<Labeling, Direction>(expectedLabel, expectedDirection));
        }
    }

    /**
     *
     * @param labeling
     * @return the number of labels that match the 'correct' label for their respective edge
     */
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
