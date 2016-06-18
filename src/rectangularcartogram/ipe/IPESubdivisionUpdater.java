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
package rectangularcartogram.ipe;

import java.awt.geom.Path2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class IPESubdivisionUpdater {

    public static void updateSubdivision(Subdivision sub, File file) throws IOException {
        BufferedReader in = null;
        HashMap<SubdivisionFace, Path2D> polygons = new HashMap<SubdivisionFace, Path2D>(sub.getFaces().size() * 2);
        HashSet<SubdivisionFace> unlabeled = new HashSet<SubdivisionFace>(sub.getFaces());

        for (SubdivisionFace f : sub.getFaces()) {
            Path2D polygon = new Path2D.Double(Path2D.WIND_EVEN_ODD);

            boolean start = true;

            for (Vertex v : f.getVertices()) {
                if (start) {
                    polygon.moveTo(v.getX(), v.getY());
                    start = false;
                } else {
                    polygon.lineTo(v.getX(), v.getY());
                }
            }

            polygon.closePath();

            polygons.put(f, polygon);
        }

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

                if (line.contains("<text")) {
                    // This is a label; see if it belongs to a region
                    Vertex anchor = getAnchor(line);

                    SubdivisionFace f = getContainingFace(anchor, polygons);
                    f.setName(line.substring(line.indexOf(">") + 1, line.indexOf("</")));
                    unlabeled.remove(f);
                }

                line = in.readLine();
            }

            for (SubdivisionFace f : unlabeled) {
                if (!f.isSea()) {
                    f.setSea(true);
                    f.setWeight(SubdivisionFace.SEA_WEIGHT);
                    f.setColor(SubdivisionFace.SEA_COLOR);
                    f.setName("");
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static Vertex getAnchor(String line) {
        if (line.contains("pos=")) {
            // Isolate the substring that contains the position
            int startIndex = line.indexOf("pos=") + "pos=\"".length();
            String pos = line.substring(startIndex, line.indexOf("\"", startIndex));

            return getVertex(pos, getMatrix(line));
        } else {
            return null;
        }
    }

    private static Vertex getVertex(String line, double[] transform) {
        String[] coords = line.split(" ");

        assert coords.length >= 2 : "Wrong input for importVertex. Expecting \"x y\", received \"" + line + "\"";

        double x = Double.parseDouble(coords[0]);
        double y = Double.parseDouble(coords[1]);

        // Apply the transformation
        x = transform[0] * x + transform[1] * y + transform[4];
        y = transform[2] * x + transform[3] * y + transform[5];

        return new Vertex(x, y);
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

    private static SubdivisionFace getContainingFace(Vertex anchor, HashMap<SubdivisionFace, Path2D> polygons) {
        for (Entry<SubdivisionFace, Path2D> entry : polygons.entrySet()) {
            if (entry.getValue().contains(anchor.getX(), anchor.getY())) {
                return entry.getKey();
            }
        }

        return null;
    }

    private IPESubdivisionUpdater() {
    }
}
