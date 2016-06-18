/*
 * Copyright 2010-2016 Wouter Meulemans and Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectangularcartogram.ipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;

public class IPEImporter {

    public static Graph importGraph(File file) throws IOException {
        BufferedReader in = null;
        Graph graph = new Graph();

        try {
            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            while (line != null) {
                // Skip the ipestyle
                if (line.contains("<ipestyle")) {
                    while (!line.contains("</ipestyle>")) {
                        line = in.readLine();
                    }
                }

                if (line.contains("<path")) {
                    // This is a path consisting of one or more edges
                    importEdges(graph, in, getMatrix(line));
                } else if (line.contains("<mark") || (line.contains("<use") && line.contains("name=\"mark"))) {
                    // This is a vertex
                    if (line.contains("pos=")) {
                        // Isolate the substring that contains the position
                        int startIndex = line.indexOf("pos=") + "pos=\"".length();
                        String pos = line.substring(startIndex, line.indexOf("\"", startIndex));

                        importVertex(graph, pos, getMatrix(line));
                    }
                }

                line = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return graph;
    }

    private static void importEdges(Graph graph, BufferedReader in, double[] transform) throws IOException {
        String line = in.readLine();
        Vertex prev = null;
        Vertex first = null;

        while (!line.contains("</path>")) {
            Vertex v;

            if (line.equals("h")) {
                // Return to the start
                v = first;
            } else {
                v = importVertex(graph, line, transform);
            }

            if (prev != null) {
                graph.addEdge(prev, v);
            } else {
                first = v;
            }

            prev = v;

            line = in.readLine();
        }
    }

    private static Vertex importVertex(Graph graph, String line, double[] transform) {
        String[] coords = line.split(" ");

        assert coords.length >= 2 : "Wrong input for importVertex. Expecting \"x y\", received \"" + line + "\"";

        double x = Double.parseDouble(coords[0]);
        double y = Double.parseDouble(coords[1]);

        // Apply the transformation
        x = transform[0] * x + transform[1] * y + transform[4];
        y = transform[2] * x + transform[3] * y + transform[5];

        Vertex v = graph.getVertexAt(x, y, 0.01); // Low accuracy due to rounding errors in transformation =(

        if (v == null) {
            v = new Vertex(x, y);
            graph.addVertex(v);
        }

        return v;
    }

    /**
     * Parses and returns the transformation matrix of this element.
     *
     * The matrix consists of 6 double values: {a, b, c, d, e, f}
     *
     * It has to be read as
     *
     * | a  b  e |
     * | c  d  f |
     * | 0  0  1 |
     *
     * Every point (x, y) is treated as a column vector
     *
     * | x |
     * | y |
     * | 1 |
     *
     * The resulting point is the matrix multiplication of the two:
     *
     * | a  b  e |   | x |   |ax + by + e|
     * | c  d  f | x | y | = |cx + dy + f|
     * | 0  0  1 |   | 1 |   |     1     |
     *
     * @param line
     * @return
     */
    private static double[] getMatrix(String line) {
        double[] transform = new double[]{1, 0, 0, 1, 0, 0}; // Identity matrix

        if (line.contains("matrix=")) {
            // Get the substring that contains the matrix
            int startIndex = line.indexOf("matrix") + "matrix=\"".length();
            String matrix = line.substring(startIndex, line.indexOf("\"", startIndex));

            // Split to obtain the 6 double values
            String[] parts = matrix.split(" ");

            assert parts.length == 6;

            for (int i = 0; i < parts.length; i++) {
                transform[i] = Double.parseDouble(parts[i]);
            }
        }

        return transform;
    }

    private IPEImporter() {
    }
}
