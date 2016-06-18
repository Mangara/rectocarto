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

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.algos.CartogramMaker;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;

public abstract class CartogramQualityMeasure extends QualityMeasure {
    protected Subdivision sub;
    protected IloCplex cplex;
    protected int nIterations;

    public CartogramQualityMeasure(Subdivision sub, IloCplex cplex, int nIterations) {
        this.sub = sub;
        this.cplex = cplex;
        this.nIterations = nIterations;
    }

    public int getnIterations() {
        return nIterations;
    }

    public void setnIterations(int nIterations) {
        this.nIterations = nIterations;
    }

    public Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> makeCartogram(RegularEdgeLabeling labeling) throws IncorrectGraphException {
        sub.getDualGraph().setRegularEdgeLabeling(labeling);
        CartogramMaker carto = new CartogramMaker(sub, cplex);

        try {
            carto.iterate(nIterations);
            return new Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>>(carto.getCartogram(), carto.getFaceMap());
        } catch (IloException ex) {
            Logger.getLogger(CartographicErrorMeasure.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CartographicErrorMeasure.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        try {
            Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> cartogram = makeCartogram(labeling);
            return getCartogramQuality(cartogram.getFirst(), cartogram.getSecond());
        } catch (IncorrectGraphException ex) {
            Logger.getLogger(CartogramQualityMeasure.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (higherIsBetter() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
    }

    public abstract double getCartogramQuality(Subdivision cartogram, Map<SubdivisionFace, SubdivisionFace> faceMap);
}
