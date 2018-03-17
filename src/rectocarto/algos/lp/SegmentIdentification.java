/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectocarto.algos.lp;

import java.util.HashMap;
import java.util.Map;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public class SegmentIdentification {

    static Map<SubdivisionFace, FaceSegments> identifySegments(Subdivision sub) {
        // Create all segments
        Map<SubdivisionFace, RealFaceSegments> segments = new HashMap<>(2 * sub.getFaces().size());
        int count = 0;

        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            RealFaceSegments s = new RealFaceSegments();
            s.left = new Segment("v" + count);
            s.right = new Segment("v" + (count + 1));
            s.bottom = new Segment("h" + count);
            s.top = new Segment("h" + (count + 1));
            segments.put(face, s);
            count += 2;
        }

        // Add boundary constraints (necessary because these edges are unlabelled)
        RealFaceSegments northSegments = segments.get(sub.getNorthFace());
        RealFaceSegments eastSegments = segments.get(sub.getEastFace());
        RealFaceSegments southSegments = segments.get(sub.getSouthFace());
        RealFaceSegments westSegments = segments.get(sub.getWestFace());

        northSegments.left.merge(westSegments.right);
        northSegments.right.merge(eastSegments.left);
        northSegments.top.merge(westSegments.top);
        northSegments.top.merge(eastSegments.top);

        southSegments.left.merge(westSegments.right);
        southSegments.right.merge(eastSegments.left);
        southSegments.bottom.merge(westSegments.bottom);
        southSegments.bottom.merge(eastSegments.bottom);

        // Run union-find to eliminate duplicates
        RegularEdgeLabeling rel = sub.getDualGraph().getRegularEdgeLabeling();
        for (Edge edge : sub.getDualGraph().getEdges()) {
            Pair<Graph.Labeling, Edge.Direction> label = rel.get(edge);

            if (label.getFirst() == Graph.Labeling.BLUE) { // Horizontal
                SubdivisionFace leftFace = sub.getFace(edge.getOrigin());
                SubdivisionFace rightFace = sub.getFace(edge.getDestination());
                segments.get(leftFace).right.merge(segments.get(rightFace).left);
            } else if (label.getFirst() == Graph.Labeling.RED) { // Vertical
                SubdivisionFace bottomFace = sub.getFace(edge.getOrigin());
                SubdivisionFace topFace = sub.getFace(edge.getDestination());
                segments.get(bottomFace).top.merge(segments.get(topFace).bottom);
            }
        }

        // Build the final segment mapping
        Map<SubdivisionFace, FaceSegments> finalSegments = new HashMap<>(2 * sub.getFaces().size());

        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            finalSegments.put(face, extractNames(segments.get(face)));
        }

        return finalSegments;
    }
    
    public static class FaceSegments {

        String left, top, right, bottom;

        @Override
        public String toString() {
            return "S[" + "left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ']';
        }
    }

    private static FaceSegments extractNames(RealFaceSegments segs) {
        FaceSegments result = new FaceSegments();
        result.left = segs.left.findRepresentative().name;
        result.right = segs.right.findRepresentative().name;
        result.bottom = segs.bottom.findRepresentative().name;
        result.top = segs.top.findRepresentative().name;
        return result;
    }

    private static class RealFaceSegments {

        Segment left, top, right, bottom;

        @Override
        public String toString() {
            return "S[" + "left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ']';
        }
    }

    private static class Segment {

        String name;
        Segment parent = this;
        int rank = 0;

        Segment(String name) {
            this.name = name;
        }

        public Segment findRepresentative() {
            if (parent != this) {
                parent = parent.findRepresentative();
            }

            return parent;
        }

        public void merge(Segment other) {
            Segment rep1 = findRepresentative();
            Segment rep2 = other.findRepresentative();

            if (rep1 == rep2) {
                return;
            }

            if (rep1.rank < rep2.rank) {
                rep1.parent = rep2;
            } else if (rep1.rank > rep2.rank) {
                rep2.parent = rep1;
            } else {
                rep1.parent = rep2;
                rep2.rank++;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private SegmentIdentification() {
    }
}
