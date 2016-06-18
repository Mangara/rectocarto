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
import rectangularcartogram.algos.DataGatherer;
import rectangularcartogram.algos.GoodLabelingFinder;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.AllMeasures;
import rectangularcartogram.measures.AngleDeviationMeasure;
import rectangularcartogram.measures.BinaryAngleDeviationMeasure;
import rectangularcartogram.measures.BinaryBoundingBoxSeparationMeasure;
import rectangularcartogram.measures.BoundedCartographicErrorMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.CartographicErrorMeasure;
import rectangularcartogram.measures.QualityMeasure;

public class ExperimentsNetherlands {

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
            // Gather data on the Netherlands for the scatterplots
            System.out.println("Gathering scatterplot data");

            Subdivision NLArea = loadSubdivision(new File("Netherlands Area.sub"));

            DataGatherer dg = new DataGatherer(NLArea.getDualGraph(), new AllMeasures(NLArea, cplex, new File(resultsDirectory, "Netherlands Area Measurements.txt")));
            dg.gatherData();

            Subdivision NLPopulation = loadSubdivision(new File("Netherlands Population.sub"));

            DataGatherer dg2 = new DataGatherer(NLPopulation.getDualGraph(), new AllMeasures(NLPopulation, cplex, new File(resultsDirectory, "Netherlands Population Measurements.txt")));
            dg2.gatherData();

            Subdivision NLLivestock = loadSubdivision(new File("Netherlands Livestock.sub"));

            DataGatherer dg3 = new DataGatherer(NLLivestock.getDualGraph(), new AllMeasures(NLLivestock, cplex, new File(resultsDirectory, "Netherlands Livestock Measurements.txt")));
            dg3.gatherData();

            /*/// AREA ////
            System.out.println("Creating cartograms based on best labelings for NL Area");

            makeBestCartogram(NLArea, new AngleDeviationMeasure(NLArea, false), "NL Area Best Angle Deviation labeling.sub"); // NL Area Best Angle Deviation labeling
            makeBestCartogram(NLArea, new AngleDeviationMeasure(NLArea, true), "NL Area Best Maximum Angle Deviation labeling.sub"); // NL Area Best Maximum Angle Deviation labeling
            makeBestCartogram(NLArea, new BinaryAngleDeviationMeasure(NLArea.getDualGraph()), "NL Area Best Binary Angle Deviation labeling.sub"); // NL Area Best Binary Angle Deviation labeling

            makeBestCartogram(NLArea, new BoundingBoxSeparationMeasure(NLArea, false), "NL Area Best Bounding Box Separation Distance labeling.sub"); // NL Area Best Bounding Box Separation Distance labeling
            makeBestCartogram(NLArea, new BoundingBoxSeparationMeasure(NLArea, true), "NL Area Best Maximum Bounding Box Separation Distance labeling.sub"); // NL Area Best Maximum Bounding Box Separation Distance labeling
            makeBestCartogram(NLArea, new BinaryBoundingBoxSeparationMeasure(NLArea), "NL Area Best Binary Bounding Box Separation Distance labeling.sub"); // NL Area Best Binary Bounding Box Separation Distance labeling

            ResultingAngleDeviationAndCartographicErrorMeasure resAD = new ResultingAngleDeviationAndCartographicErrorMeasure(NLArea, cplex);
            resAD.setCartographicErrorWeight(0); // Don't consider the cartographic error

            makeBestCartogram(NLArea, resAD, "NL Area Best Resulting Angle Deviation labeling.sub"); // NL Area Best Resulting Angle Deviation labeling

            resAD.setUseMaximumAngleDeviation(true);

            makeBestCartogram(NLArea, resAD, "NL Area Best Maximum Resulting Angle Deviation labeling.sub"); // NL Area Best Maximum Resulting Angle Deviation labeling

            makeBestCartogram(NLArea, new CartographicErrorMeasure(NLArea, cplex, false, 20), "NL Area Best Average Cartographic Error labeling.sub"); // NL Area Best Average Cartographic Error labeling
            makeBestCartogram(NLArea, new CartographicErrorMeasure(NLArea, cplex, true, 20), "NL Area Best Maximum Cartographic Error Labeling.sub"); // NL Area Best Maximum Cartographic Error Labeling

            makeBestCartogram(NLArea, new BoundedCartographicErrorMeasure(NLArea, cplex, new AngleDeviationMeasure(NLArea, true), 0.05), "NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling.sub"); // NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            makeBestCartogram(NLArea, new BoundedCartographicErrorMeasure(NLArea, cplex, new BoundingBoxSeparationMeasure(NLArea, true), 0.05), "NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling.sub"); // NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling

            resAD.setUseCartographicErrorBound(true);
            resAD.setCartographicErrorBound(0.05);
            resAD.setUseMaximumCartographicError(true);

            makeBestCartogram(NLArea, resAD, "NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling.sub"); // NL Area Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling
            //// AREA ///*/

            /*/// POPULATION ////
            System.out.println("Creating cartograms based on best labelings for NL Population");

            Subdivision NLPopulation = loadSubdivision(new File("Netherlands Population.sub"));

            makeBestCartogram(NLPopulation, new AngleDeviationMeasure(NLPopulation, false), "NL Population Best Angle Deviation labeling.sub"); // NL Population Best Angle Deviation labeling
            makeBestCartogram(NLPopulation, new AngleDeviationMeasure(NLPopulation, true), "NL Population Best Maximum Angle Deviation labeling.sub"); // NL Population Best Maximum Angle Deviation labeling
            makeBestCartogram(NLPopulation, new BinaryAngleDeviationMeasure(NLPopulation.getDualGraph()), "NL Population Best Binary Angle Deviation labeling.sub"); // NL Population Best Binary Angle Deviation labeling

            makeBestCartogram(NLPopulation, new BoundingBoxSeparationMeasure(NLPopulation, false), "NL Population Best Bounding Box Separation Distance labeling.sub"); // NL Population Best Bounding Box Separation Distance labeling
            makeBestCartogram(NLPopulation, new BoundingBoxSeparationMeasure(NLPopulation, true), "NL Population Best Maximum Bounding Box Separation Distance labeling.sub"); // NL Population Best Maximum Bounding Box Separation Distance labeling
            makeBestCartogram(NLPopulation, new BinaryBoundingBoxSeparationMeasure(NLPopulation), "NL Population Best Binary Bounding Box Separation Distance labeling.sub"); // NL Population Best Binary Bounding Box Separation Distance labeling

            ResultingAngleDeviationAndCartographicErrorMeasure resAD = new ResultingAngleDeviationAndCartographicErrorMeasure(NLPopulation, cplex);
            resAD.setCartographicErrorWeight(0); // Don't consider the cartographic error

            makeBestCartogram(NLPopulation, resAD, "NL Population Best Resulting Angle Deviation labeling.sub"); // NL Population Best Resulting Angle Deviation labeling

            resAD.setUseMaximumAngleDeviation(true);

            makeBestCartogram(NLPopulation, resAD, "NL Population Best Maximum Resulting Angle Deviation labeling.sub"); // NL Population Best Maximum Resulting Angle Deviation labeling

            makeBestCartogram(NLPopulation, new CartographicErrorMeasure(NLPopulation, cplex, false, 20), "NL Population Best Average Cartographic Error labeling.sub"); // NL Population Best Average Cartographic Error labeling
            makeBestCartogram(NLPopulation, new CartographicErrorMeasure(NLPopulation, cplex, true, 20), "NL Population Best Maximum Cartographic Error Labeling.sub"); // NL Population Best Maximum Cartographic Error Labeling

            makeBestCartogram(NLPopulation, new BoundedCartographicErrorMeasure(NLPopulation, cplex, new AngleDeviationMeasure(NLPopulation, true), 0.05), "NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling.sub"); // NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            makeBestCartogram(NLPopulation, new BoundedCartographicErrorMeasure(NLPopulation, cplex, new BoundingBoxSeparationMeasure(NLPopulation, true), 0.05), "NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling.sub"); // NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling

            resAD.setUseCartographicErrorBound(true);
            resAD.setCartographicErrorBound(0.05);
            resAD.setUseMaximumCartographicError(true);

            makeBestCartogram(NLPopulation, resAD, "NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling.sub"); // NL Population Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling
            //// POPULATION ///*/

            /*/// LIVESTOCK ////
            System.out.println("Creating cartograms based on best labelings for NL Livestock");

            Subdivision NLLivestock = loadSubdivision(new File("Netherlands Livestock.sub"));

            makeBestCartogram(NLLivestock, new AngleDeviationMeasure(NLLivestock, false), "NL Livestock Best Angle Deviation labeling.sub"); // NL Livestock Best Angle Deviation labeling
            makeBestCartogram(NLLivestock, new AngleDeviationMeasure(NLLivestock, true), "NL Livestock Best Maximum Angle Deviation labeling.sub"); // NL Livestock Best Maximum Angle Deviation labeling
            makeBestCartogram(NLLivestock, new BinaryAngleDeviationMeasure(NLLivestock.getDualGraph()), "NL Livestock Best Binary Angle Deviation labeling.sub"); // NL Livestock Best Binary Angle Deviation labeling

            makeBestCartogram(NLLivestock, new BoundingBoxSeparationMeasure(NLLivestock, false), "NL Livestock Best Bounding Box Separation Distance labeling.sub"); // NL Livestock Best Bounding Box Separation Distance labeling
            makeBestCartogram(NLLivestock, new BoundingBoxSeparationMeasure(NLLivestock, true), "NL Livestock Best Maximum Bounding Box Separation Distance labeling.sub"); // NL Livestock Best Maximum Bounding Box Separation Distance labeling
            makeBestCartogram(NLLivestock, new BinaryBoundingBoxSeparationMeasure(NLLivestock), "NL Livestock Best Binary Bounding Box Separation Distance labeling.sub"); // NL Livestock Best Binary Bounding Box Separation Distance labeling

            resAD = new ResultingAngleDeviationAndCartographicErrorMeasure(NLLivestock, cplex);
            resAD.setCartographicErrorWeight(0); // Don't consider the cartographic error

            makeBestCartogram(NLLivestock, resAD, "NL Livestock Best Resulting Angle Deviation labeling.sub"); // NL Livestock Best Resulting Angle Deviation labeling

            resAD.setUseMaximumAngleDeviation(true);

            makeBestCartogram(NLLivestock, resAD, "NL Livestock Best Maximum Resulting Angle Deviation labeling.sub"); // NL Livestock Best Maximum Resulting Angle Deviation labeling

            makeBestCartogram(NLLivestock, new CartographicErrorMeasure(NLLivestock, cplex, false, 20), "NL Livestock Best Average Cartographic Error labeling.sub"); // NL Livestock Best Average Cartographic Error labeling
            makeBestCartogram(NLLivestock, new CartographicErrorMeasure(NLLivestock, cplex, true, 20), "NL Livestock Best Maximum Cartographic Error Labeling.sub"); // NL Livestock Best Maximum Cartographic Error Labeling

            makeBestCartogram(NLLivestock, new BoundedCartographicErrorMeasure(NLLivestock, cplex, new AngleDeviationMeasure(NLLivestock, true), 0.05), "NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling.sub"); // NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Angle Deviation labeling
            makeBestCartogram(NLLivestock, new BoundedCartographicErrorMeasure(NLLivestock, cplex, new BoundingBoxSeparationMeasure(NLLivestock, true), 0.05), "NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling.sub"); // NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Bounding Box Separation Distance labeling

            resAD.setUseCartographicErrorBound(true);
            resAD.setCartographicErrorBound(0.05);
            resAD.setUseMaximumCartographicError(true);

            makeBestCartogram(NLLivestock, resAD, "NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling.sub"); // NL Livestock Best Bounded Maximum Cartographic Error 0.05 with Maximum Resulting Angle Deviation labeling
            //// LIVESTOCK ///*/
        } finally {
            // Release the CPLEX license
            cplex.end();
        }
    }

    private static void makeBestCartogram(Subdivision sub, QualityMeasure measure, String fileName) throws IncorrectGraphException, IloException, IOException {
        GoodLabelingFinder glf = new GoodLabelingFinder(sub.getDualGraph(), measure);
        glf.findBestLabeling();
        sub.getDualGraph().setRegularEdgeLabeling(glf.getBestLabeling());

        CartogramMaker carto = new CartogramMaker(sub, cplex);

        for (int i = 0; i < 50; i++) {
            carto.iterate();
            carto.iterate();
        }

        saveSubdivision(carto.getCartogram(), new File(resultsDirectory, fileName));
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

    private ExperimentsNetherlands() {
    }
}
