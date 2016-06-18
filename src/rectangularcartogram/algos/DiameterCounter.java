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

import rectangularcartogram.data.RegularEdgeColoring;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class DiameterCounter {

    private Graph graph;
    private long count;

    public DiameterCounter(Graph graph) throws IncorrectGraphException {
        this.graph = graph;
    }

    protected void traverseLabelings() throws IncorrectGraphException {
        // Go up the lattice by flipping the colours inside the first left alternating 4-cycle
        RegularEdgeColoring currentLabeling = MinimumLabelingComputer.getMinimalColoring(graph);

        while (currentLabeling != null) {
            count++;

            currentLabeling = currentLabeling.moveUp();
        }

        count--; // The length of the path is n-1
    }

    public long countDiameter() throws IncorrectGraphException {
        count = 0;
        
        traverseLabelings();

        return count;
    }

    public long getCount() {
        return count;
    }
}
