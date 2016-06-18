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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RememberVertex {

    private double x, y; // The coordinates of this vertex
    RememberVertexEntry head; // Pointer to the first element in the double-linked adjacency list
    int degree; // The degree of this vertex

    /**
     * Creates a new RememberVertex with the given coordinates.
     * Runs in O(1) time.
     * @param x
     * @param y
     */
    public RememberVertex(double x, double y) {
        this.x = x;
        this.y = y;
        head = null;
        degree = 0;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     * Returns the degree of this vertex.
     * Runs in O(1) time.
     * @return
     */
    public int getDegree() {
        return degree;
    }

    /**
     * Returns true if this vertex has an outgoing or undirected edge to the given vertex, false otherwise.
     * Runs in O(degree) time.
     * @return
     */
    public boolean isAdjacentTo(RememberVertex v) {
        RememberVertexEntry e = head;

        while (e != null && e.neighbour != v) {
            e = e.next;
        }

        if (e == null) {
            return false;
        } else {
            assert e.neighbour == v;
            return true;
        }
    }

    /**
     * Returns a collection of all neighbours of this vertex.
     * Runs in O(degree) time.
     * @return
     */
    public List<RememberVertex> getNeighbours() {
        ArrayList<RememberVertex> ns = new ArrayList<RememberVertex>(degree);

        RememberVertexEntry e = head;

        while (e != null) {
            ns.add(e.neighbour);
            e = e.next;
        }

        return ns;
    }

    /**
     * Returns a collection of all entries of neighbours of this vertex.
     * Runs in O(degree) time.
     * @return
     */
    public List<RememberVertexEntry> getNeighbourEntries() {
        ArrayList<RememberVertexEntry> ns = new ArrayList<RememberVertexEntry>(degree);

        RememberVertexEntry e = head;

        while (e != null) {
            ns.add(e);
            e = e.next;
        }

        return ns;
    }

    /**
     * Sorts the edges around this vertex in the order specified by the given comparator.
     * Runs in O(d log d * x) time, where d is the degree of this vertex and x is the time required for a comparison.
     * @param comp
     */
    public void sortEdges(Comparator<RememberVertexEntry> comp) {
        if (degree > 0) {
            List<RememberVertexEntry> neighbours = getNeighbourEntries();
            Collections.sort(neighbours, comp);

            head = neighbours.get(0);

            for (int i = 0; i < neighbours.size(); i++) {
                RememberVertexEntry e = neighbours.get(i);

                if (i == 0) {
                    e.prev = null;
                } else {
                    e.prev = neighbours.get(i - 1);
                }

                if (i == neighbours.size() - 1) {
                    e.next = null;
                } else {
                    e.next = neighbours.get(i + 1);
                }
            }
        }
    }

    /**
     * Adds a neighbour to the head of the neighbour list and returns the new entry.
     * Runs in O(1) time.
     * @param v
     * @return
     */
    RememberVertexEntry addNeighbour(RememberVertex v) {
        RememberVertexEntry e = new RememberVertexEntry();

        // Initialize the new entry's fields
        e.myVertex = this;
        e.neighbour = v;
        e.next = head;
        e.prev = null;

        // Add the new entry to the head of the list
        if (head != null) {
            head.prev = e;
        }

        head = e;

        // Increment degree
        degree++;

        return e;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
