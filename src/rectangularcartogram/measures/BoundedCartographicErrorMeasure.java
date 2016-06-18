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

import rectangularcartogram.data.RegularEdgeLabeling;

public class BoundedCartographicErrorMeasure extends QualityMeasure {

    private CartographicErrorMeasure cartoError;
    private QualityMeasure measure;
    private double maxError;

    /**
     * Assumption: 0 <= measure.getQuality(..) <= 1
     * @param sub
     * @param measure
     * @param maxError
     */
    public BoundedCartographicErrorMeasure(CartographicErrorMeasure cartoError, QualityMeasure measure, double maxError) {
        this.cartoError = cartoError;
        this.measure = measure;
        this.maxError = maxError;

        setHigherIsBetter(measure.higherIsBetter());
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        double error = cartoError.getQuality(labeling);
        double quality = measure.getQuality(labeling);

        if (higherIsBetter) {
            if (error < maxError) {
                // The cartographic error bound is satisfied: we don't care any more
                return 0.66 + quality / 3;
            } else {
                // The bound is not satisfied
                return quality / 9 + 2 * (1 - Math.min(error, 1)) / 9;
            }
        } else {
            if (error < maxError) {
                // The cartographic error bound is satisfied: we don't care any more
                return quality / 3;
            } else {
                // The bound is not satisfied
                return 0.66 + quality / 9 + 2 * error / 9;
            }
        }
    }

    @Override
    public int compare(RegularEdgeLabeling labeling1, RegularEdgeLabeling labeling2) {
        int directResult = super.compare(labeling1, labeling2);

        if (directResult == 0) {
            // The two labelings have the same (maximum or average) cartographic error and the same initial quality for our measure, use our measure's compare function
            return measure.compare(labeling1, labeling2);
        } else {
            return directResult;
        }
    }
}
