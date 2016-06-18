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
package rectangularcartogram.data.subdivision;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rectangularcartogram.data.graph.ComparableVertex;
import rectangularcartogram.data.graph.Vertex;

public class CompositeFace extends SubdivisionFace {

    private SubdivisionFace face1, face2;

    public CompositeFace(SubdivisionFace face, SubdivisionFace mergeInto) {
        if (face.isSea() || mergeInto.isSea()) {
            throw new IllegalArgumentException("Composite sea regions are not supported.");
        }

        face1 = face;
        face2 = mergeInto;

        setVertices(mergeVertices(face, mergeInto));
        setColor(mergeInto.getColor());
        setName(mergeInto.getName() + " + " + face.getName());
        setWeight(face.getWeight() + mergeInto.getWeight());
        setSea(false);
        updateCorrespondingVertex();
    }

    public CompositeFace(List<Vertex> vertices, Color color, String name, double weight, boolean sea, boolean boundary) {
        super(vertices, color, name, weight, sea, boundary);
    }

    public SubdivisionFace getFace1() {
        return face1;
    }

    public SubdivisionFace getFace2() {
        return face2;
    }

    void setFace1(SubdivisionFace face1) {
        this.face1 = face1;
    }

    void setFace2(SubdivisionFace face2) {
        this.face2 = face2;
    }

    public boolean contains(SubdivisionFace face) {
        if (face == face1 || face == face2) {
            return true;
        }

        if ((face1 instanceof CompositeFace) && ((CompositeFace) face1).contains(face)) {
            return true;
        }

        if ((face2 instanceof CompositeFace) && ((CompositeFace) face2).contains(face)) {
            return true;
        }

        return false;
    }

    private static final boolean DEBUG_VERTICES = false;

    /**
     * Returns the outline of the two regions
     * @param f1
     * @param f2
     * @return
     */
    private List<Vertex> mergeVertices(SubdivisionFace f1, SubdivisionFace f2) {
        boolean empty1 = f1 == null || f1.getVertices() == null || f1.getVertices().isEmpty();
        boolean empty2 = f2 == null || f2.getVertices() == null || f2.getVertices().isEmpty();

        if (DEBUG_VERTICES) System.out.println("empty? " + empty1 + " " + empty2);

        if (empty1 && empty2) {
            return new ArrayList<Vertex>();
        } else if (empty1) {
            return f2.getVertices();
        } else if (empty2) {
            return f1.getVertices();
        } else {
            List<ComparableVertex> list1 = new ArrayList<ComparableVertex>(f1.getVertices().size());
            List<ComparableVertex> list2 = new ArrayList<ComparableVertex>(f2.getVertices().size());

            for (Vertex vertex : f1.getVertices()) {
                list1.add(new ComparableVertex(vertex));
            }

            for (Vertex vertex : f2.getVertices()) {
                list2.add(new ComparableVertex(vertex));
            }

            if (DEBUG_VERTICES) System.out.println("V1: " + list1);
            if (DEBUG_VERTICES) System.out.println("V2: " + list2);

            // Build hash tables to do quick indexOf(vertex) lookups
            Map<ComparableVertex, Integer> index1 = new HashMap<ComparableVertex, Integer>(list1.size() * 2);
            for (int i = 0; i < list1.size(); i++) {
                index1.put(list1.get(i), i);
            }
            Map<ComparableVertex, Integer> index2 = new HashMap<ComparableVertex, Integer>(list2.size() * 2);
            for (int i = 0; i < list2.size(); i++) {
                index2.put(list2.get(i), i);
            }

            List<Vertex> result = new ArrayList<Vertex>(list1.size() + list2.size());

            boolean following1 = true;
            ComparableVertex currentVertex = list1.get(0);
            ComparableVertex prevVertex = null;
            int index = 0; // index of the currentVertex

            // Walk until we reach a vertex that is not on the other face
            while (index < list1.size() && index2.containsKey(currentVertex)) {
                // Move to the next vertex
                prevVertex = currentVertex;
                index++;
                currentVertex = list1.get(index);
            }

            if (index == list1.size()) {
                if (DEBUG_VERTICES) System.err.println("No vertex found that is not on other face!");
                return f2.getVertices();
            }

            if (DEBUG_VERTICES) System.out.println(String.format("f1? %b cur: %s prev: %s i: %d res: %s", following1, currentVertex.toString(), (prevVertex == null ? "null" : prevVertex.toString()), index, result.toString()));

            while (result.isEmpty() || !currentVertex.equals(result.get(0))) {
                // Check if our current vertex is in the other list as well
                Integer otherIndex = (following1 ? index2.get(currentVertex) : index1.get(currentVertex));

                if (otherIndex != null) {
                    index = otherIndex;
                    following1 = !following1;
                    prevVertex = currentVertex;
                } else {
                    if (prevVertex != null) {
                        result.add(prevVertex.getVertex());
                        prevVertex = null;
                    }

                    // Add the current vertex
                    result.add(currentVertex.getVertex());
                }

                // Move to the next vertex
                index = (following1 ? (index + 1) % list1.size() : (index + 1) % list2.size());
                currentVertex = (following1 ? list1.get(index) : list2.get(index));

                if (DEBUG_VERTICES) System.out.println(String.format("f1? %b cur: %s prev: %s i: %d res: %s", following1, currentVertex.toString(), (prevVertex == null ? "null" : prevVertex.toString()), index, result.toString()));
            }

            if (prevVertex != null) {
                result.add(prevVertex.getVertex());
            }

            if (DEBUG_VERTICES) System.out.println(String.format("f1? %b cur: %s prev: %s i: %d res: %s", following1, currentVertex.toString(), (prevVertex == null ? "null" : prevVertex.toString()), index, result.toString()));

            return result;
        }
    }

    void updateWeight() {
        // Recursively update sub-composite regions
        if (face1 instanceof CompositeFace) {
            ((CompositeFace) face1).updateWeight();
        }
        if (face2 instanceof CompositeFace) {
            ((CompositeFace) face2).updateWeight();
        }

        // Update weight
        setWeight(face1.getWeight() + face2.getWeight());
    }

    void updateColor() {
        // Pick the color of face1
        if (face1 instanceof CompositeFace) {
            ((CompositeFace) face1).updateColor();
        }

        setColor(face1.getColor());
    }
}
