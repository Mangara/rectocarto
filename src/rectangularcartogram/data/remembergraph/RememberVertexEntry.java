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
package rectangularcartogram.data.remembergraph;

public class RememberVertexEntry {

    RememberVertexEntry prev, next;
    RememberVertexEntry twin;
    RememberVertex myVertex, neighbour;
    boolean edgesRemoved = false; // Were there edges between this entry and the next that have been removed?

    public void remove() {
        if (prev == null) {
            myVertex.head = next;

            // The order on the edges is cyclic, so mark the last edge
            if (next != null) {
                RememberVertexEntry last = next;

                while (last.next != null) {
                    last = last.next;
                }

                last.edgesRemoved = true;
            }
        } else {
            prev.next = next;
            prev.edgesRemoved = true;
        }

        if (next != null) {
            next.prev = prev;
        }
    }

    public boolean hasEdgesRemoved() {
        return edgesRemoved;
    }

    public RememberVertex getMyVertex() {
        return myVertex;
    }

    public RememberVertex getNeighbour() {
        return neighbour;
    }

    public RememberVertexEntry getNext() {
        return next;
    }

    public RememberVertexEntry getPrev() {
        return prev;
    }

    public RememberVertexEntry getTwin() {
        return twin;
    }
}
