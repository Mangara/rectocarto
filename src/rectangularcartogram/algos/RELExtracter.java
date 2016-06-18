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

import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class RELExtracter {

    /**
     * Computes the regular edge labeling that was used to build this rectangular cartogram.
     * @param rectangularCartogram
     * @return
     */
    public static RegularEdgeLabeling findRegularEdgeLabeling(Subdivision rectangularCartogram) {
        RegularEdgeLabeling result = new RegularEdgeLabeling(new CycleGraph(rectangularCartogram.getDualGraph()));

        for (Edge edge : rectangularCartogram.getDualGraph().getEdges()) {
            result.put(edge, findLabeling(edge, rectangularCartogram));
        }

        return result;
    }

    private static Pair<Labeling, Direction> findLabeling(Edge edge, Subdivision rectangularCartogram) {
        // Get the corresponding faces
        SubdivisionFace f1 = rectangularCartogram.getFaceMap().get(edge.getVA());
        SubdivisionFace f2 = rectangularCartogram.getFaceMap().get(edge.getVB());

        if (f1.isSea() || f2.isSea()) {
            // Land-Sea adjacencies can be messed up by the LP, so we can't say anything about this.
            return new Pair<Labeling, Direction>(Labeling.NONE, Direction.NONE);
        }

        double maxX = Double.NEGATIVE_INFINITY;
        double minX = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;

        for (Vertex v : f1.getVertices()) {
            maxX = Math.max(maxX, v.getX());
            minX = Math.min(minX, v.getX());
            maxY = Math.max(maxY, v.getY());
            minY = Math.min(minY, v.getY());
        }

        boolean left = true;
        boolean right = true;
        boolean above = true;
        boolean below = true;

        for (Vertex v : f2.getVertices()) {
            left = left && v.getX() <= minX;
            right = right && v.getX() >= maxX;
            above = above && v.getY() >= maxY;
            below = below && v.getY() <= minY;
        }

        if (left && !right && !above && !below) {
            return new Pair<Labeling, Direction>(Labeling.BLUE, Direction.BA);
        } else if (!left && right && !above && !below) {
            return new Pair<Labeling, Direction>(Labeling.BLUE, Direction.AB);
        } else if (!left && !right && above && !below) {
            return new Pair<Labeling, Direction>(Labeling.RED, Direction.AB);
        } else if (!left && !right && !above && below) {
            return new Pair<Labeling, Direction>(Labeling.RED, Direction.BA);
        } else {
            System.out.println("STRANGE!");
            System.out.println("Face 1: " + f1.getName() + " = " + f1.getVertices());
            System.out.println("Face 2: " + f2.getName() + " = " + f2.getVertices());
            System.out.println("* left:  " + left);
            System.out.println("* right: " + right);
            System.out.println("* above: " + above);
            System.out.println("* below: " + below);
            System.out.println("/STRANGE!");
            return new Pair<Labeling, Direction>(Labeling.NONE, Direction.NONE);
        }
    }
}
