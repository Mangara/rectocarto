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

public abstract class QualityMeasure {

    public enum Fold {
        MAXIMUM, AVERAGE, AVERAGE_SQUARED
    }

    protected Fold fold = Fold.AVERAGE_SQUARED;
    protected boolean higherIsBetter = true;

    public abstract double getQuality(RegularEdgeLabeling labeling);

    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    public void setHigherIsBetter(boolean b) {
        higherIsBetter = b;
    }

    public Fold getFold() {
        return fold;
    }

    public void setFold(Fold fold) {
        this.fold = fold;
    }

    /**
     * Compares two labelings, returning the value <code>0</code> if <code>labeling1</code> and <code>labeling2</code> are of equal quality;
     * a value less than <code>0</code> if <code>labeling1</code> is worse than <code>labeling2</code>;
     * and a value greater than <code>0</code> if <code>labeling1</code> is better than <code>labeling2</code>.
     *
     * If the getQuality method returns different qualities for labeling1 and labeling2, this method should return Double.compare(getQuality(labeling1), getQuality(labeling2)) if higherIsBetter() returns true, or -Double.compare(getQuality(labeling1), getQuality(labeling2)) otherwise.
     * If the qualities are the same, this method may return something other than 0, based on other quality criteria.
     * @param labeling1
     * @param labeling2
     * @return
     */
    public int compare(RegularEdgeLabeling labeling1, RegularEdgeLabeling labeling2) {
        double quality1 = getQuality(labeling1);
        double quality2 = getQuality(labeling2);

        if (higherIsBetter) {
            return Double.compare(quality1, quality2);
        } else {
            return -Double.compare(quality1, quality2);
        }
    }
}
