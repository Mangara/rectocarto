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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.subdivision.Subdivision;

public class AllMeasures extends QualityMeasure {

    private BufferedWriter output;
    
    private AngleDeviationMeasure ad;
    private AngleDeviationMeasure maxAd;
    private BinaryAngleDeviationMeasure binaryAd;

    private BoundingBoxSeparationMeasure bb;
    private BoundingBoxSeparationMeasure maxBb;
    private BinaryBoundingBoxSeparationMeasure binaryBb;

    private ResultingAngleDeviationMeasure resAD;
    private ResultingAngleDeviationMeasure maxResAD;

    private CartographicErrorMeasure error;
    private CartographicErrorMeasure maxError;

    public AllMeasures(Subdivision sub, IloCplex cplex, File outputFie) throws IOException {
        setHigherIsBetter(false);

        CycleGraph cg = new CycleGraph(sub.getDualGraph());

        output = new BufferedWriter(new FileWriter(outputFie));

        ad = new AngleDeviationMeasure(sub);
        maxAd = new AngleDeviationMeasure(sub, Fold.MAXIMUM);
        binaryAd = new BinaryAngleDeviationMeasure(cg);

        bb = new BoundingBoxSeparationMeasure(sub);
        maxBb = new BoundingBoxSeparationMeasure(sub, Fold.MAXIMUM);
        binaryBb = new BinaryBoundingBoxSeparationMeasure(sub, cg);

        resAD = new ResultingAngleDeviationMeasure(sub, cplex);
        resAD.setnIterations(20);

        maxResAD = new ResultingAngleDeviationMeasure(sub, cplex, Fold.MAXIMUM, 20, true, false);

        error = new CartographicErrorMeasure(sub, cplex, Fold.AVERAGE_SQUARED, 20);
        maxError = new CartographicErrorMeasure(sub, cplex, Fold.MAXIMUM, 20);

        output.write("Average Angle Deviation, Maximum Angle Deviation, Binary Angle Deviation, Average Bounding Box Separation Distance, Maximum Bounding Box Separation Distance, Binary Bounding Box Separation Distance, Average Resulting Angle Deviation after 20 iterations, Maximum Resulting Angle Deviation after 20 iterations, Average Cartographic Error after 20 iterations, Maximum Cartographic Error after 20 iterations");
        output.newLine();
        output.flush();
    }

    @Override
    public double getQuality(RegularEdgeLabeling labeling) {
        double adQ = ad.getQuality(labeling);
        double maxAdQ = maxAd.getQuality(labeling);
        double binaryAdQ = binaryAd.getQuality(labeling);

        double bbQ = bb.getQuality(labeling);
        double maxBbQ = maxBb.getQuality(labeling);
        double binaryBbQ = binaryBb.getQuality(labeling);

        double resADQ = resAD.getQuality(labeling);
        double maxResADQ = maxResAD.getQuality(labeling);

        double errorQ = error.getQuality(labeling);
        double maxErrorQ = maxError.getQuality(labeling);

        try {
            output.write(String.format("%f %f %f %f %f %f %f %f %f %f", adQ, maxAdQ, binaryAdQ, bbQ, maxBbQ, binaryBbQ, resADQ, maxResADQ, errorQ, maxErrorQ));
            output.newLine();
            output.flush();
        } catch (IOException ex) {
            Logger.getLogger(AllMeasures.class.getName()).log(Level.SEVERE, null, ex);
        }

        return 1;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        output.close();
    }
}
