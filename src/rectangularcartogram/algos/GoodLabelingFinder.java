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
import rectangularcartogram.measures.QualityMeasure;

public class GoodLabelingFinder extends LabelingTraverser {

    protected RegularEdgeLabeling bestLabeling;
    protected double bestQuality; // Invariant: computeQuality(bestLabeling) == bestQuality
    protected QualityMeasure measure;

    public GoodLabelingFinder(Graph graph, QualityMeasure measure) throws IncorrectGraphException {
        super(graph);
        this.measure = measure;
    }

    public RegularEdgeLabeling findBestLabeling() throws IncorrectGraphException {
        bestLabeling = null;

        if (measure.higherIsBetter()) {
            bestQuality = Double.NEGATIVE_INFINITY;
        } else {
            bestQuality = Double.POSITIVE_INFINITY;
        }

        traverseLabelings();

        //graph.setRegularEdgeLabeling(bestLabeling);
        return bestLabeling;

        //System.out.println("Best Quality: " + bestQuality);
    }

    public RegularEdgeLabeling getBestLabeling() {
        return bestLabeling;
    }

    @Override
    protected void processLabeling(RegularEdgeLabeling labeling) {
        double quality = measure.getQuality(labeling);

        if ((measure.higherIsBetter() && quality > bestQuality) || (!measure.higherIsBetter() && quality < bestQuality) || (quality == bestQuality && measure.compare(labeling, bestLabeling) > 0)) {
            bestLabeling = labeling;
            bestQuality = quality;
        }
    }
}
