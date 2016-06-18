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
package rectangularcartogram.algos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import rectangularcartogram.data.RegularEdgeColoring;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class LabelingCountEstimator {

    private CycleGraph graph;
    private long nProbes;
    private double averageSize;
    private static Random rand = new Random();

    public LabelingCountEstimator(Graph graph) throws IncorrectGraphException {
        this.graph = new CycleGraph(graph);
    }

    public long estimateLabelingCount() throws IncorrectGraphException {
        nProbes = 0;
        averageSize = 0;

        for (int j = 0; j < 400; j++) {
            traverseLabelings();
            System.out.println();
        }

        return Math.round(averageSize);
    }

    public long getEstimate() {
        return Math.round(averageSize);
    }

    protected void traverseLabelings() throws IncorrectGraphException {
        // Do a single random probe of the reverse search tree and update the estimated size
        long estimatedSize = 1;
        long labelingsAtCurrentDepth = 1;

        RegularEdgeColoring currentLabeling = MinimumLabelingComputer.getMinimalColoring(graph);
        boolean hasNeighbours = true;

        while (hasNeighbours) {
            List<RegularEdgeColoring> neighbours = new ArrayList<RegularEdgeColoring>();

            for (int i = 0; i < graph.getFourCycles().size(); i++) {
                RegularEdgeColoring nextLabeling = currentLabeling.getNeighbour(i);

                if (nextLabeling != null) {
                    int localSearchIndexFromNext = nextLabeling.getMoveDownCycleIndex();

                    if (localSearchIndexFromNext == i) {
                        neighbours.add(nextLabeling);
                    }
                }
            }

            if (neighbours.size() > 0) {
                labelingsAtCurrentDepth *= neighbours.size();
                estimatedSize += labelingsAtCurrentDepth;

                System.out.println("currentLabeling:" + currentLabeling.hashCode() + ", estimated size: " + estimatedSize + ", labelingsAtCurrentDepth: " + labelingsAtCurrentDepth + " nNeighbours: " + neighbours.size());

                currentLabeling = neighbours.get(rand.nextInt(neighbours.size()));
            } else {
                hasNeighbours = false;
            }
        }

        double sum = nProbes * averageSize;
        sum += estimatedSize;
        nProbes++;
        averageSize = sum / (double) nProbes;
    }
}
