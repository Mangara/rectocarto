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
import rectangularcartogram.algos.MinimumLabelingComputer;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class CartogramTest {

    public static void main(String[] args) throws IOException, IncorrectGraphException, IloException {
        // Load the subdivision
        Subdivision sub = loadSubdivision(new File("../Maps/World/World Population.sub"));

        // Get a CPLEX license
        IloCplex cplex = new IloCplex();

        int nIterations = 100;

        try {
            // Compute the minimum labeling of the dual graph
            RegularEdgeLabeling minimum = MinimumLabelingComputer.getMinimalLabeling(sub.getDualGraph());
            sub.getDualGraph().setRegularEdgeLabeling(minimum);

            // Create a cartogram
            CartogramMaker carto = new CartogramMaker(sub, cplex);

            long start = System.nanoTime();
            for (int i = 0; i < 2 * nIterations; i++) {
                //System.out.println("Iteration " + i);
                try {
                    carto.iterate();
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                    System.err.flush();
                    System.err.println(cplex);
                    throw npe;
                }
            }
            long iterations = System.nanoTime() - start;

            Subdivision cartogram = carto.getCartogram();

            double error = cartogram.getAverageCartographicError();

            System.out.println("Error: " + error + " Time per iteration: " + iterations * 10e-9 / (2 * nIterations));
        } finally {
            cplex.end();
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

    private CartogramTest() {
    }
}
