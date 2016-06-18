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
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.Graph;

public class LabelingCounter extends LabelingTraverser {

    private long nLabelings = 0;
    private int orderOfMagnitude = 1;
    public static final int MAX_MAGNITUDE = 1000000; // I always want to see the millions reported

    public LabelingCounter(Graph graph) throws IncorrectGraphException {
        super(graph);
    }

    public void countLabelings() throws IncorrectGraphException {
        traverseLabelings();
        System.out.println(nLabelings + " total.");
    }

    @Override
    protected void processLabeling(RegularEdgeLabeling labeling) {
        nLabelings++;

        if (nLabelings % orderOfMagnitude == 0) {
            System.out.println(nLabelings + " so far.");

            if (nLabelings >= 10 * orderOfMagnitude) {
                orderOfMagnitude = Math.min(orderOfMagnitude * 10, MAX_MAGNITUDE);
            }
        }
    }

    public long getnLabelings() {
        return nLabelings;
    }
}
