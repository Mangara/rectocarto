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

import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.Graph;

public abstract class LabelingTraverser {

    protected Graph graph;

    public LabelingTraverser(Graph graph) throws IncorrectGraphException {
        GraphChecker.checkGraph(graph);
        this.graph = graph;
    }
    
    protected void traverseLabelings() throws IncorrectGraphException {
        traverseLabelings(MinimumLabelingComputer.getMinimalLabeling(graph), new CycleGraph(graph));
    }

    private void traverseLabelings(RegularEdgeLabeling currentLabeling, CycleGraph graph) throws IncorrectGraphException {
        processLabeling(currentLabeling);

        for (int i = 0; i < graph.getFourCycles().size(); i++) {
            RegularEdgeLabeling neighbour = currentLabeling.getNeighbour(i);

            if (neighbour != null) {
                int localSearchIndexFromNext = neighbour.getMoveDownCycleIndex();

                if (localSearchIndexFromNext == i) {
                    traverseLabelings(neighbour, graph);
                }
            }
        }
    }

    /*
     * The following is a direct implementation of reverse search by Avis and Fukuda.
     * The current direct DFS implementation is essentially the same, but the code is simpler and it performs better.
     *
    public static final boolean DEBUG = false;

    protected void traverseLabelings() throws IncorrectGraphException {
        if (DEBUG) {
            System.out.println("Traversing labelings. Number of cycles: " + graph.getFourCycles().size());
        }

        // Run reverse search
        RegularEdgeLabeling currentLabeling = minimum;
        int j = 0; // neighbour index

        do {
            while (j < graph.getFourCycles().size()) {
                j++;

                if (DEBUG) {
                    System.out.print("Considering neighbour " + j + " of labeling " + currentLabeling.hashCode() + ".");
                }

                RegularEdgeLabeling nextLabeling = currentLabeling.getNeighbour(j - 1);

                if (nextLabeling != null) {
                    if (DEBUG) {
                        System.out.print(" This neighbour (" + nextLabeling.hashCode() + ") exists");
                    }

                    int localSearchIndexFromNext = nextLabeling.getMoveDownCycleIndex();

                    if (localSearchIndexFromNext == j - 1) {
                        if (DEBUG) {
                            System.out.println(" and the local search traverses this edge.");
                            System.out.println();
                        }

                        currentLabeling = nextLabeling;
                        j = 0;
                    } else {
                        if (DEBUG) {
                            System.out.println(", but local search takes a different edge.");
                        }
                    }
                } else {
                    if (DEBUG) {
                        System.out.println(" This neighbour does not exist.");
                    }
                }
            }

            if (DEBUG) {
                System.out.println("Processing this labeling and moving back");
            }
            processLabeling(currentLabeling);

            if (!currentLabeling.equals(minimum)) {
                // Forward traverse
                RegularEdgeLabeling prevLabeling = currentLabeling;
                currentLabeling = currentLabeling.moveDown();

                // Restore j
                j = 0;
                do {
                    j++;
                } while (!prevLabeling.equals(currentLabeling.getNeighbour(j - 1)));

                if (DEBUG) {
                    System.out.println("Restored j. Back at neighbour " + j + " of labeling " + currentLabeling.hashCode() + ".");
                    System.out.println();
                }
            }
        } while (j != graph.getFourCycles().size() || !currentLabeling.equals(minimum));
    }
    */

    /**
     * This method is called exactly once for each labeling of the graph.
     * @param labeling
     */
    protected abstract void processLabeling(RegularEdgeLabeling labeling);
}
