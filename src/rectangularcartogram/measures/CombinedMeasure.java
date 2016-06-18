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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class CombinedMeasure extends QualityMeasure {

    private CartogramQualityMeasure cartoMeasure = null;
    private List<Pair<QualityMeasure, Double>> measures;
    private double totalWeight;

    public CombinedMeasure() {
        measures = new ArrayList<Pair<QualityMeasure, Double>>(4);
        totalWeight = 0;
    }

    /**
     * Adds a measure with the specified weight.
     * Note that all measures should be of the same type regarding whether a higher quality is better or not.
     * @param measure
     * @param weight
     * @throws IllegalArgumentException if a measure is added with a different higherIsBetter() result than a previous added measure.
     */
    public void addMeasure(QualityMeasure measure, double weight) {
        if (measures.isEmpty()) {
            setHigherIsBetter(measure.higherIsBetter());

            measures.add(new Pair<QualityMeasure, Double>(measure, weight));
            totalWeight += weight;

            if (measure instanceof CartogramQualityMeasure) {
                cartoMeasure = (CartogramQualityMeasure) measure;
            }
        } else {
            // All measures should be of the same type
            if (measure.higherIsBetter() == higherIsBetter) {
                measures.add(new Pair<QualityMeasure, Double>(measure, weight));
                totalWeight += weight;

                if (measure instanceof CartogramQualityMeasure && ((CartogramQualityMeasure) measure).getnIterations() > cartoMeasure.getnIterations()) {
                    cartoMeasure = (CartogramQualityMeasure) measure;
                }
            } else {
                throw new IllegalArgumentException("All measures should be of the same type regarding whether a higher quality is better or not.");
            }
        }
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        // Compute the cartogram if necessary
        Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> cartogram = null;

        if (cartoMeasure != null) {
            try {
                cartogram = cartoMeasure.makeCartogram(labeling);
            } catch (IncorrectGraphException ex) {
                Logger.getLogger(CombinedMeasure.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        double quality = 0;

        for (Pair<QualityMeasure, Double> pair : measures) {
            QualityMeasure measure = pair.getFirst();
            double measureQuality;

            if (measure instanceof CartogramQualityMeasure) {
                measureQuality = ((CartogramQualityMeasure) measure).getCartogramQuality(cartogram.getFirst(), cartogram.getSecond());
            } else {
                measureQuality = measure.getQuality(labeling);
            }

            quality += pair.getSecond() * measureQuality;
        }

        return quality / totalWeight;
    }
}
