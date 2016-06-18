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

import ilog.cplex.IloCplex;
import java.util.Map;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class CartographicErrorMeasure extends CartogramQualityMeasure {

    public CartographicErrorMeasure(Subdivision sub, IloCplex cplex) {
        this(sub, cplex, Fold.AVERAGE_SQUARED, 5);
    }

    public CartographicErrorMeasure(Subdivision sub, IloCplex cplex, Fold fold) {
        this(sub, cplex, fold, 5);
    }

    public CartographicErrorMeasure(Subdivision sub, IloCplex cplex, Fold fold, int nIterations) {
        super(sub, cplex, nIterations);
        this.fold = fold;

        setHigherIsBetter(false);
    }

    @Override
    public double getCartogramQuality(Subdivision cartogram, Map<SubdivisionFace, SubdivisionFace> faceMap) {
        switch (fold) {
            case MAXIMUM:
                return cartogram.getMaximumCartographicError();
            case AVERAGE:
                return cartogram.getAverageCartographicError();
            case AVERAGE_SQUARED:
                return cartogram.getAverageSquaredCartographicError();
            default:
                throw new AssertionError("Unrecognized fold type: " + fold);
        }
    }
}
