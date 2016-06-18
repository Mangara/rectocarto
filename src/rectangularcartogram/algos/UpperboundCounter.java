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
package rectangularcartogram.algos;

import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;

public class UpperboundCounter {
    public static double computeUpperBoundForGraph(Graph g) {
        double bound = 1;

        for (Vertex v : g.getVertices()) {
            int d = v.getDegree();

            // bound *= 2^5 * (d choose 4) = 32 * d! / (d - 4)!4! = 4 * d! / 3(d - 4)!
            // (d choose 4) = d! / (d - 4)!4!

            bound *= Math.pow(4 * faculty(d) / (3 * faculty(d - 4)), 0.25);

            System.out.println("bound: " + bound);
        }

        return bound;
    }

    private static long faculty(int i) {
        long result = 1;

        for (int j = 2; j <= i; j++) {
            result *= j;
        }

        return result;
    }
}
