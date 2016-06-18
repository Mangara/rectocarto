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
import rectangularcartogram.algos.ga.LabelingGA;
import rectangularcartogram.algos.ga.selection.RankSelection;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.measures.BoundedCartographicErrorMeasure;
import rectangularcartogram.measures.BoundingBoxSeparationMeasure;
import rectangularcartogram.measures.CartographicErrorMeasure;
import rectangularcartogram.measures.CombinedMeasure;
import rectangularcartogram.measures.QualityMeasure;
import rectangularcartogram.measures.ResultingAngleDeviationMeasure;

public class ExperimentsLondon {

    private static File resultsDirectory;
    private static IloCplex cplex;

    public static void main(String[] args) throws IOException, IncorrectGraphException, IloException {
        // Create a new directory for the results if it doesn't exist yet
        resultsDirectory = new File("Results");

        if (!resultsDirectory.exists()) {
            resultsDirectory.mkdir();
        }

        // Obtain a CPLEX license
        cplex = new IloCplex();

        try {
            System.out.println("Optimizing London");
            loop();
        } finally {
            // Release the CPLEX license
            cplex.end();
        }
    }

    private static void loop() throws IOException, IloException, IncorrectGraphException {
        Subdivision sub = loadSubdivision(new File("London_Boroughs_Subdivision_Weighted.sub"));

        // Weighted ACE / resulting ad
        CombinedMeasure m1 = new CombinedMeasure();
        m1.addMeasure(new CartographicErrorMeasure(sub, cplex), 7);
        m1.addMeasure(new ResultingAngleDeviationMeasure(sub, cplex), 3);

        // Weighted MCE / average bbsd
        CombinedMeasure m2 = new CombinedMeasure();
        m2.addMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), 7);
        m2.addMeasure(new BoundingBoxSeparationMeasure(sub), 3);

        // Bounded ACE / maximum resulting ad
        BoundedCartographicErrorMeasure m3 = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex), new ResultingAngleDeviationMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), 0.05);

        // Bounded MCE / average bbsd
        BoundedCartographicErrorMeasure m4 = new BoundedCartographicErrorMeasure(new CartographicErrorMeasure(sub, cplex, QualityMeasure.Fold.MAXIMUM), new BoundingBoxSeparationMeasure(sub), 0.25);

        // Weighted ACE / average bbsd
        CombinedMeasure m5 = new CombinedMeasure();
        m5.addMeasure(new CartographicErrorMeasure(sub, cplex), 7);
        m5.addMeasure(new BoundingBoxSeparationMeasure(sub), 3);

        QualityMeasure[] measures = {m1, m2, m3, m4, m5};
        String[] measureNames = {"Weighted ACE - resulting ad", "Weighted MCE - average bbsd", "Bounded ACE - maximum resulting ad", "Bounded MCE - average bbsd", "Weighted ACE - average bbsd"};

        LabelingGA ga = new LabelingGA(sub.getDualGraph(), null);
        ga.setElitistFraction(0.04);
        ga.setCrossoverChance(0.05);
        ga.setMutationChance(0.9);
        ga.setSelection(new RankSelection(0.9));

        int maxGenerations = 50;
        int populationSize = 50;

        int m = 0;

        while (true) {
            ga.setMeasure(measures[m]);
            m = (m + 1) % measures.length;

            ga.initialize(populationSize);

            Pair<RegularEdgeLabeling, Double> best = ga.getBestAfter(maxGenerations);

            sub.getDualGraph().setRegularEdgeLabeling(best.getFirst());

            CartogramMaker carto = new CartogramMaker(sub, cplex);

            for (int i = 0; i < 50; i++) {
                carto.iterate();
                carto.iterate();
            }

            Subdivision cartogram = carto.getCartogram();

            saveSubdivision(cartogram, new File(resultsDirectory, cartogram.getAverageCartographicError() + " " + cartogram.getMaximumCartographicError() + " " + measureNames[(m - 1 + measures.length) % measures.length] + ".sub"));
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

    private ExperimentsLondon() {
    }
}
