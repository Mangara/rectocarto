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
        Map<SubdivisionFace, FaceSegments> segments = new HashMap<>(2 * sub.getFaces().size());
        int count = 0;

        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            FaceSegments s = new FaceSegments();
            s.left = new Segment("v" + count);
            s.right = new Segment("v" + (count + 1));
            s.bottom = new Segment("h" + count);
            s.top = new Segment("h" + (count + 1));
            segments.put(face, s);
            count += 2;
        }

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

        // Update the segment mapping
        for (SubdivisionFace face : sub.getTopLevelFaces()) {
            FaceSegments s = segments.get(face);
            s.left = s.left.findRepresentative();
            s.right = s.right.findRepresentative();
            s.bottom = s.bottom.findRepresentative();
            s.top = s.top.findRepresentative();
        }

        return segments;
    }

    static class FaceSegments {

        Segment left, top, right, bottom;
    }

    static class Segment {

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
    }

    private SegmentIdentification() {
    }
}
