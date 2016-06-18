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

import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeColoring;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class MinimumLabelingComputer {
    public static RegularEdgeColoring getMinimalColoring(Graph graph) throws IncorrectGraphException {
        // TODO: re-implement Fusy's algorithm such that it doesn't touch the graph or edges and produces a regular edge coloring
        Graph temp = new Graph(graph);

        RELFusy f = new RELFusy();
        f.computeREL(temp);

        RegularEdgeColoring result = new RegularEdgeColoring(new CycleGraph(graph));

        for (Edge edge : temp.getEdges()) {
            result.put(edge, temp.getEdgeLabel(edge));
        }

        return result;
    }

    public static RegularEdgeLabeling getMinimalLabeling(Graph graph) throws IncorrectGraphException {
        // TODO: re-implement Fusy's algorithm such that it doesn't touch the graph or edges and produces a regular edge labeling
        Graph temp = new Graph(graph);

        RELFusy f = new RELFusy();
        f.computeREL(temp);

        RegularEdgeLabeling result = new RegularEdgeLabeling(new CycleGraph(graph));

        for (Edge edge : temp.getEdges()) {
            result.put(edge, new Pair<Graph.Labeling, Edge.Direction>(temp.getEdgeLabel(edge), edge.getDirection()));
        }

        return result;
    }
}
