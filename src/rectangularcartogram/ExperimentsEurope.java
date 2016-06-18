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

public class ExperimentsEurope {

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
            /*System.out.println("Creating cartograms based on best labelings for EUR Area");

            Subdivision EURArea = loadSubdivision(new File("Europe Area.sub"));

            makeGoodCartogram(EURArea, new AngleDeviationMeasure(EURArea.getDualGraph(), false), "EUR Area Best Angle Deviation labeling"); // EUR Area Best Angle Deviation labeling
            makeGoodCartogram(EURArea, new AngleDeviationMeasure(EURArea.getDualGraph(), true), "EUR Area Best Maximum Angle Deviation labeling"); // EUR Area Best Maximum Angle Deviation labeling
            makeGoodCartogram(EURArea, new BinaryAngleDeviationMeasure(EURArea.getDualGraph()), "EUR Area Best Binary Angle Deviation labeling"); // EUR Area Best Binary Angle Deviation labeling

            makeGoodCartogram(EURArea, new BoundingBoxSeparationMeasure(EURArea, false), "EUR Area Best Bounding Box Separation Distance labeling"); // EUR Area Best Bounding Box Separation Distance labeling
            makeGoodCartogram(EURArea, new BoundingBoxSeparationMeasure(EURArea, true), "EUR Area Best Maximum Bounding Box Separation Distance labeling"); // EUR Area Best Maximum Bounding Box Separation Distance labeling
            makeGoodCartogram(EURArea, new BinaryBoundingBoxSeparationMeasure(EURArea), "EUR Area Best Binary Bounding Box Separation Distance labeling"); // EUR Area Best Binary Bounding Box Separation Distance labeling

            makeGoodCartogram(EURArea, new CartographicErrorMeasure(EURArea, false, 5), "EUR Area Best Average Cartographic Error labeling"); // EUR Area Best Average Cartographic Error labeling
            makeGoodCartogram(EURArea, new CartographicErrorMeasure(EURArea, true, 5), "EUR Area Best Maximum Cartographic Error Labeling"); // EUR Area Best Maximum Cartographic Error Labeling

            makeGoodCartogram(EURArea, new BoundedCartographicErrorMeasure(EURArea, new AngleDeviationMeasure(EURArea.getDualGraph(), true), 0.05), "EUR Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling"); // EUR Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            makeGoodCartogram(EURArea, new BoundedCartographicErrorMeasure(EURArea, new BoundingBoxSeparationMeasure(EURArea, true), 0.05), "EUR Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling"); // EUR Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling
            //*/

            System.out.println("Creating cartograms based on best labelings for EUR Population");

            Subdivision EURPopulation = loadSubdivision(new File("Europe Population.sub"));

            /*/// Weighted ACE / resulting ad ////
            ResultingAngleDeviationAndCartographicErrorMeasure m1 = new ResultingAngleDeviationAndCartographicErrorMeasure(EURPopulation, cplex);
            makeGoodCartogram(EURPopulation, m1, "Europe Population Weighted ACE and Average Resulting AD");
            ///*/

            /*/// Bounded MCE / maximum resulting ad, using previous measure to steer ////
            ResultingAngleDeviationAndCartographicErrorMeasure m2 = new ResultingAngleDeviationAndCartographicErrorMeasure(EURPopulation, cplex);
            m2.setUseMaximumAngleDeviation(true);
            m2.setUseMaximumCartographicError(true);
            m2.setUseCartographicErrorBound(true);
            m2.setCartographicErrorBound(0.05);
            makeGoodCartogram(EURPopulation, m2, m1, "Europe Population Bounded MCE with Maximum Resulting AD");
            ///*/

            /*/// Bounded MCE / average bbsd ////
            BoundedCartographicErrorMeasure m3 = new BoundedCartographicErrorMeasure(EURPopulation, new BoundingBoxSeparationMeasure(EURPopulation, false), true, 10, 0.05);
            makeGoodCartogram(EURPopulation, m3, "Europe Population Bounded MCE with Average BBSD");
            ///*/

            /*makeGoodCartogram(EURPopulation, new AngleDeviationMeasure(EURPopulation.getDualGraph(), false), "EUR Population Best Angle Deviation labeling"); // EUR Population Best Angle Deviation labeling
            makeGoodCartogram(EURPopulation, new AngleDeviationMeasure(EURPopulation.getDualGraph(), true), "EUR Population Best Maximum Angle Deviation labeling"); // EUR Population Best Maximum Angle Deviation labeling
            makeGoodCartogram(EURPopulation, new BinaryAngleDeviationMeasure(EURPopulation.getDualGraph()), "EUR Population Best Binary Angle Deviation labeling"); // EUR Population Best Binary Angle Deviation labeling

            makeGoodCartogram(EURPopulation, new BoundingBoxSeparationMeasure(EURPopulation, false), "EUR Population Best Bounding Box Separation Distance labeling"); // EUR Population Best Bounding Box Separation Distance labeling
            makeGoodCartogram(EURPopulation, new BoundingBoxSeparationMeasure(EURPopulation, true), "EUR Population Best Maximum Bounding Box Separation Distance labeling"); // EUR Population Best Maximum Bounding Box Separation Distance labeling
            makeGoodCartogram(EURPopulation, new BinaryBoundingBoxSeparationMeasure(EURPopulation), "EUR Population Best Binary Bounding Box Separation Distance labeling"); // EUR Population Best Binary Bounding Box Separation Distance labeling*/

            //makeGoodCartogram(EURPopulation, new CartographicErrorMeasure(EURPopulation, false, 5), "EUR Population Best Average Cartographic Error labeling"); // EUR Population Best Average Cartographic Error labeling
            //makeGoodCartogram(EURPopulation, new CartographicErrorMeasure(EURPopulation, true, 5), "EUR Population Best Maximum Cartographic Error Labeling"); // EUR Population Best Maximum Cartographic Error Labeling

            //makeGoodCartogram(EURPopulation, new BoundedCartographicErrorMeasure(EURPopulation, new AngleDeviationMeasure(EURPopulation.getDualGraph(), true), 0.05), "EUR Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling"); // EUR Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            //makeGoodCartogram(EURPopulation, new BoundedCartographicErrorMeasure(EURPopulation, new BoundingBoxSeparationMeasure(EURPopulation, true), 0.05), "EUR Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling"); // EUR Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling
            //*/

            /*System.out.println("Creating cartograms based on best labelings for EUR Livestock");

            Subdivision EURLivestock = loadSubdivision(new File("Europe Livestock.sub"));

            makeGoodCartogram(EURLivestock, new AngleDeviationMeasure(EURLivestock.getDualGraph(), false), "EUR Livestock Best Angle Deviation labeling"); // EUR Livestock Best Angle Deviation labeling
            makeGoodCartogram(EURLivestock, new AngleDeviationMeasure(EURLivestock.getDualGraph(), true), "EUR Livestock Best Maximum Angle Deviation labeling"); // EUR Livestock Best Maximum Angle Deviation labeling
            makeGoodCartogram(EURLivestock, new BinaryAngleDeviationMeasure(EURLivestock.getDualGraph()), "EUR Livestock Best Binary Angle Deviation labeling"); // EUR Livestock Best Binary Angle Deviation labeling

            makeGoodCartogram(EURLivestock, new BoundingBoxSeparationMeasure(EURLivestock, false), "EUR Livestock Best Bounding Box Separation Distance labeling"); // EUR Livestock Best Bounding Box Separation Distance labeling
            makeGoodCartogram(EURLivestock, new BoundingBoxSeparationMeasure(EURLivestock, true), "EUR Livestock Best Maximum Bounding Box Separation Distance labeling"); // EUR Livestock Best Maximum Bounding Box Separation Distance labeling
            makeGoodCartogram(EURLivestock, new BinaryBoundingBoxSeparationMeasure(EURLivestock), "EUR Livestock Best Binary Bounding Box Separation Distance labeling"); // EUR Livestock Best Binary Bounding Box Separation Distance labeling

            makeGoodCartogram(EURLivestock, new CartographicErrorMeasure(EURLivestock, false, 5), "EUR Livestock Best Average Cartographic Error labeling"); // EUR Livestock Best Average Cartographic Error labeling
            makeGoodCartogram(EURLivestock, new CartographicErrorMeasure(EURLivestock, true, 5), "EUR Livestock Best Maximum Cartographic Error Labeling"); // EUR Livestock Best Maximum Cartographic Error Labeling

            makeGoodCartogram(EURLivestock, new BoundedCartographicErrorMeasure(EURLivestock, new AngleDeviationMeasure(EURLivestock.getDualGraph(), true), 0.05), "EUR Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling"); // EUR Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            makeGoodCartogram(EURLivestock, new BoundedCartographicErrorMeasure(EURLivestock, new BoundingBoxSeparationMeasure(EURLivestock, true), 0.05), "EUR Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling"); // EUR Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling
            //*/

            /*Subdivision EURUnemplyment = loadSubdivision(new File("Europe Unemployment.sub"));

            //makeGoodCartogram(EURUnemplyment, new BoundedCartographicErrorMeasure(EURUnemplyment, new AngleDeviationMeasure(EURUnemplyment.getDualGraph(), true), 0.05), "EUR Unemployment Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling"); // EUR Unemployment Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            //makeGoodCartogram(EURUnemplyment, new BoundedCartographicErrorMeasure(EURUnemplyment, new BoundingBoxSeparationMeasure(EURUnemplyment, true), 0.05), "EUR Unemployment Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling"); // EUR Unemployment Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling

            CombinedMeasure avg = new CombinedMeasure();
            avg.addMeasure(new CartographicErrorMeasure(EURUnemplyment, false, 10), 7);
            avg.addMeasure(new BoundingBoxSeparationMeasure(EURUnemplyment, false), 3);

            makeGoodCartogram(EURUnemplyment, avg, "Europe Unemployment Combined MCE and BBSD");*/

            Subdivision EURHighway = loadSubdivision(new File("Europe Highway.sub"));

            /*/// Weighted ACE / resulting ad ////
            ResultingAngleDeviationAndCartographicErrorMeasure m21 = new ResultingAngleDeviationAndCartographicErrorMeasure(EURHighway, cplex);
            makeGoodCartogram(EURHighway, m21, "Europe Highway Weighted ACE and Average Resulting AD");
            ///*/

            /*/// Bounded MCE / maximum resulting ad, using previous measure to steer ////
            ResultingAngleDeviationAndCartographicErrorMeasure m22 = new ResultingAngleDeviationAndCartographicErrorMeasure(EURHighway, cplex);
            m22.setUseMaximumAngleDeviation(true);
            m22.setUseMaximumCartographicError(true);
            m22.setUseCartographicErrorBound(true);
            m22.setCartographicErrorBound(0.05);
            makeGoodCartogram(EURHighway, m22, m21, "Europe Highway Bounded MCE with Maximum Resulting AD");
            ///*/

            /*/// Bounded MCE / average bbsd ////
            BoundedCartographicErrorMeasure m23 = new BoundedCartographicErrorMeasure(EURHighway, cplex, new BoundingBoxSeparationMeasure(EURHighway, false), true, 10, 0.05);
            makeGoodCartogram(EURHighway, m23, "Europe Highway Bounded MCE with Average BBSD");
            ///*/

            System.out.println("Experiments for Europe finished.");
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

    private ExperimentsEurope() {
    }
}
