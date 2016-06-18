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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import rectangularcartogram.data.RegularEdgeColoring;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.exceptions.IncorrectDirectionException;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class TreeAnalyzer {

    private CycleGraph graph;
    private long count;
    private HashMap<Integer, Integer> nodesAtDepth;
    private HashMap<Integer, Integer> leavesAtDepth;

    public TreeAnalyzer(Graph graph) throws IncorrectGraphException {
        this.graph = new CycleGraph(graph);
    }

    public void analyze() throws IncorrectDirectionException, IncorrectGraphException {
        System.out.println("Traversing labelings. Number of cycles: " + graph.getFourCycles().size());

        count = 0;
        nodesAtDepth = new LinkedHashMap<Integer, Integer>();
        leavesAtDepth = new LinkedHashMap<Integer, Integer>();
        explore(MinimumLabelingComputer.getMinimalColoring(graph), 0);

        System.out.println("Count: " + count);

        for (Integer i : nodesAtDepth.keySet()) {
            System.out.println("Nodes at depth " + i + ": " + nodesAtDepth.get(i) + " (" + leavesAtDepth.get(i) + " leaves)");
        }
    }

    private void explore(RegularEdgeColoring labeling, int depth) throws IncorrectGraphException {
        List<RegularEdgeColoring> children = new ArrayList<RegularEdgeColoring>();

        for (int i = 0; i < graph.getFourCycles().size(); i++) {
            int cycleIndex = graph.getFourCycles().size() - i - 1;//i;

            RegularEdgeColoring neighbour = labeling.getNeighbour(cycleIndex);

            if (neighbour != null) {
                int localSearchIndexFromNext = neighbour.getMoveDownCycleIndex();

                if (localSearchIndexFromNext == cycleIndex) {
                    children.add(neighbour);
                }
            }
        }

        count++;
        if (nodesAtDepth.containsKey(depth)) {
            nodesAtDepth.put(depth, nodesAtDepth.get(depth) + 1);
        } else {
            nodesAtDepth.put(depth, 1);
        }

        if (children.isEmpty()) {
            if (leavesAtDepth.containsKey(depth)) {
                leavesAtDepth.put(depth, leavesAtDepth.get(depth) + 1);
            } else {
                leavesAtDepth.put(depth, 1);
            }
        }

        if (count % 100 == 0) {
            System.out.println("Count: " + count);

            for (Integer i : nodesAtDepth.keySet()) {
                System.out.println("Nodes at depth " + i + ": " + nodesAtDepth.get(i) + " (" + leavesAtDepth.get(i) + " leaves)");
            }
        }

        System.out.print("Exploring labeling " + labeling.hashCode() + " at depth " + depth + ". " + children.size() + " children: [");
        for (RegularEdgeColoring child : children) {
            System.out.print(child.hashCode() + ", ");
        }
        System.out.println("]");

        //Collections.shuffle(children);

        for (RegularEdgeColoring child : children) {
            explore(child, depth + 1);
        }
    }
}
