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
package rectangularcartogram;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import rectangularcartogram.algos.CartogramMaker;
import rectangularcartogram.algos.SimulatedAnnealingTraverser;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.BoundedCartographicErrorMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.QualityMeasure;

public class ExperimentsUS {

    private static File resultsDirectory;
    private static IloCplex cplex;

    public static void main(String[] args) throws IOException, IncorrectGraphException, IloException {
        // Create a new directpry for the results if it doesn't exist yet
        resultsDirectory = new File("Results");

        if (!resultsDirectory.exists()) {
            resultsDirectory.mkdir();
        }

        // Obtain a CPLEX license
        cplex = new IloCplex();

        try {
            System.out.println("Optimizing US");

            Subdivision USPopulation = loadSubdivision(new File("United States Population.sub"));

            /*/// Weighted ACE / resulting ad ////
            ResultingAngleDeviationAndCartographicErrorMeasure m1 = new ResultingAngleDeviationAndCartographicErrorMeasure(USPopulation, cplex);
            //makeGoodCartogram(USPopulation, m1, "US Population Weighted ACE and Average Resulting AD");
            ///*/

            /*/// Bounded ACE / maximum resulting ad, using previous measure to steer ////
            ResultingAngleDeviationAndCartographicErrorMeasure m2 = new ResultingAngleDeviationAndCartographicErrorMeasure(USPopulation, cplex);
            m2.setUseMaximumAngleDeviation(true);
            m2.setUseMaximumCartographicError(false);
            m2.setUseCartographicErrorBound(true);
            m2.setCartographicErrorBound(0.05);
            makeGoodCartogram(USPopulation, m2, m1, "US Population Bounded ACE with Maximum Resulting AD");
            ///*/

            /*/// Bounded MCE / average bbsd ////
            BoundedCartographicErrorMeasure m3 = new BoundedCartographicErrorMeasure(USPopulation, cplex, new BoundingBoxSeparationMeasure(USPopulation, false), true, 10, 0.25);
            makeGoodCartogram(USPopulation, m3, "US Population Bounded MCE with Average BBSD");
            ///*/

            Subdivision USNatives = loadSubdivision(new File("United States Native Population.sub"));

            /*/// Weighted ACE / resulting ad ////
            ResultingAngleDeviationAndCartographicErrorMeasure m21 = new ResultingAngleDeviationAndCartographicErrorMeasure(USNatives, cplex);
            makeGoodCartogram(USNatives, m21, "US Native Population Weighted ACE and Average Resulting AD");
            ///*/

            /*/// Bounded ACE / maximum resulting ad, using previous measure to steer ////
            ResultingAngleDeviationAndCartographicErrorMeasure m22 = new ResultingAngleDeviationAndCartographicErrorMeasure(USNatives, cplex);
            m2.setUseMaximumAngleDeviation(true);
            m2.setUseMaximumCartographicError(false);
            m2.setUseCartographicErrorBound(true);
            m2.setCartographicErrorBound(0.05);
            makeGoodCartogram(USNatives, m22, m21, "US Native Population Bounded ACE with Maximum Resulting AD");
            ///*/

            /*/// Bounded MCE / average bbsd ////
            BoundedCartographicErrorMeasure m23 = new BoundedCartographicErrorMeasure(USNatives, cplex, new BoundingBoxSeparationMeasure(USNatives, false), true, 10, 0.25);
            makeGoodCartogram(USNatives, m23, "US Native Population Bounded MCE with Average BBSD");
            ///*/
        } finally {
            // Release the CPLEX license
            cplex.end();
        }
    }

    private static void makeGoodCartogram(Subdivision sub, QualityMeasure measure, String fileName) throws IncorrectGraphException, IloException, IOException {
        makeGoodCartogram(sub, measure, measure, fileName);
    }

    private static void makeGoodCartogram(Subdivision sub, QualityMeasure measure, QualityMeasure steering, String fileName) throws IncorrectGraphException, IloException, IOException {
        for (int j = 0; j < 10; j++) { // Try 10 times
            // Start by finding a recognizable labeling
            SimulatedAnnealingTraverser sim = new SimulatedAnnealingTraverser(sub.getDualGraph(), new BoundingBoxSeparationMeasure(sub), 1000000);
            RegularEdgeLabeling start = sim.findBestLabeling();

            // Use this as starting point for our real search
            SimulatedAnnealingTraverser glf = new SimulatedAnnealingTraverser(sub.getDualGraph(), measure, steering, 2000);
            glf.setInitialLabeling(start);
            glf.findBestLabeling();
            sub.getDualGraph().setRegularEdgeLabeling(glf.getBestLabeling());

            CartogramMaker carto = new CartogramMaker(sub, cplex);

            for (int i = 0; i < 50; i++) {
                carto.iterate();
                carto.iterate();
            }

            Subdivision cartogram = carto.getCartogram();
            saveSubdivision(cartogram, new File(resultsDirectory, cartogram.getAverageCartographicError() + " " + cartogram.getMaximumCartographicError() + " " + fileName + "_" + j + ".sub"));
        }
    }

    private static Subdivision loadSubdivision(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Subdivision.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static void saveSubdivision(Subdivision sub, File file) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            // Write the data
            sub.save(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private ExperimentsUS() {
    }
}
