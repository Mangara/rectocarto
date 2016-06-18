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
package rectangularcartogram.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.exceptions.SeparatingTriangleException;

public class RegularEdgeColoring extends HashMap<Edge, Labeling> {

    private static final Random rand = new Random();
    private CycleGraph graph;

    public RegularEdgeColoring(RegularEdgeColoring rec) {
        super(rec);
        this.graph = rec.graph;
    }

    public RegularEdgeColoring(CycleGraph graph) {
        super(graph.getEdges().size() * 2);
        this.graph = graph;
    }

    public RegularEdgeColoring(RegularEdgeLabeling rel) {
        super(rel.size() * 2);
        this.graph = rel.getGraph();

        for (Edge edge : graph.getEdges()) {
            put(edge, rel.get(edge).getFirst());
        }
    }

    public CycleGraph getGraph() {
        return graph;
    }

    public int getNumFourCycles() {
        return graph.getFourCycles().size();
    }

    /**
     * Returns a list of all neighbours of this regular edge coloring.
     * @return
     */
    public List<RegularEdgeColoring> getNeighbours() {
        List<RegularEdgeColoring> neighbours = new ArrayList<RegularEdgeColoring>();

        for (int i = 0; i < getNumFourCycles(); i++) {
            RegularEdgeColoring neighbour = getNeighbour(i);

            if (neighbour != null) {
                neighbours.add(neighbour);
            }
        }

        return neighbours;
    }

    /**
     * Returns the i-th neighbour of this regular edge coloring.
     * The i-th neighbour is null if the i-th 4-cycle is not colored alternatingly, otherwise it is the result of flipping the colors of all edges inside this alternating 4-cycle.
     * @param i
     * @return
     */
    public RegularEdgeColoring getNeighbour(int i) {
        Edge[] cycle = graph.getFourCycles().get(i);

        if (isAlternating(cycle)) {
            return flip4Cycle(cycle);
        } else {
            return null;
        }
    }

    /**
     * Returns the regular edge coloring that results from taking a step down the lattice (towards the minimum coloring), by flipping the color of all edges inside the first right alternating 4-cycle, or null if no such cycle exists.
     * @return
     */
    public RegularEdgeColoring moveDown() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && isRight(cycle)) {
                return flip4Cycle(cycle);
            }
        }

        // We are at the minimum coloring
        return null;
    }

    /**
     * Changes this coloring to the regular edge coloring that results from taking a step down the lattice (towards the minimum coloring), by flipping the color of all edges inside the first right alternating 4-cycle, or doesn't change this coloring if no such cycle exists.
     * @return
     */
    public void moveDownLocal() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && isRight(cycle)) {
                flip4CycleLocal(cycle);
                return;
            }
        }
    }

    /**
     * Returns the index of the first right alternating 4-cycle, or -1 if no such cycle exists (in which case this is the minimum coloring).
     * @return
     */
    public int getMoveDownCycleIndex() {
        for (int i = 0; i < graph.getFourCycles().size(); i++) {
            Edge[] cycle = graph.getFourCycles().get(i);

            if (isAlternating(cycle) && isRight(cycle)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the regular edge coloring that results from taking a step down the lattice (towards the minimum coloring) by flipping the color of all edges inside a random right alternating 4-cycle, or this coloring if no such cycle exists.
     * @return
     */
    public RegularEdgeColoring moveDownRandomly() {
        RegularEdgeColoring result = new RegularEdgeColoring(this);

        result.moveDownRandomlyLocal();

        return result;
    }

    /**
     * Changes this coloring by taking a step down the lattice (towards the minimum coloring) by flipping the color of all edges inside a random right alternating 4-cycle, or doesn't change this coloring if no such cycle exists.
     */
    public void moveDownRandomlyLocal() {
        List<Edge[]> rightAlternating = new ArrayList<Edge[]>();

        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && isRight(cycle)) {
                rightAlternating.add(cycle);
            }
        }

        if (rightAlternating.isEmpty()) {
            // No right alternating 4-cycle, we are at the minimal coloring
        } else {
            flip4CycleLocal(rightAlternating.get(rand.nextInt(rightAlternating.size())));
        }
    }

    /**
     * Returns the regular edge coloring that results from taking a step up the lattice (towards the maximum coloring), by flipping the color of all edges inside the first left alternating 4-cycle, or null if no such cycle exists.
     * @return
     */
    public RegularEdgeColoring moveUp() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && !isRight(cycle)) {
                return flip4Cycle(cycle);
            }
        }

        // We are at the maximal coloring
        return null;
    }

    /**
     * Changes this coloring to the regular edge coloring that results from taking a step up the lattice (towards the maximum coloring), by flipping the color of all edges inside the first left alternating 4-cycle, or doesn't change this coloring if no such cycle exists.
     * @return
     */
    public void moveUpLocal() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && !isRight(cycle)) {
                flip4CycleLocal(cycle);
                return;
            }
        }
    }

    /**
     * Returns the regular edge coloring that results from taking a step up the lattice (towards the maximum coloring) by flipping the color of all edges inside a random left alternating 4-cycle, or this coloring if no such cycle exists.
     * @return
     */
    public RegularEdgeColoring moveUpRandomly() {
        RegularEdgeColoring result = new RegularEdgeColoring(this);

        result.moveUpRandomlyLocal();

        return result;
    }

    /**
     * Changes this coloring by taking a step up the lattice (towards the maximum coloring) by flipping the color of all edges inside a random left alternating 4-cycle, or doesn't change this coloring if no such cycle exists.
     */
    public void moveUpRandomlyLocal() {
        List<Edge[]> leftAlternating = new ArrayList<Edge[]>();

        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && !isRight(cycle)) {
                leftAlternating.add(cycle);
            }
        }

        if (leftAlternating.isEmpty()) {
            // No left alternating 4-cycle, we are at the maximal coloring
        } else {
            flip4CycleLocal(leftAlternating.get(rand.nextInt(leftAlternating.size())));
        }
    }

    /**
     * Returns a random neighbour of this regular edge coloring by flipping the color of all edges inside a random alternating 4-cycle.
     * @return
     */
    public RegularEdgeColoring moveRandomly() {
        RegularEdgeColoring result = new RegularEdgeColoring(this);

        result.moveRandomlyLocal();

        return result;
    }

    /**
     * Changes this coloring to a random neighbour by flipping the color of all edges inside a random alternating 4-cycle.
     */
    public void moveRandomlyLocal() {
        boolean changed = false;

        while (!changed) {
            Edge[] fourCycle = graph.getFourCycles().get(rand.nextInt(graph.getFourCycles().size()));

            if (isAlternating(fourCycle)) {
                flip4CycleLocal(fourCycle);
                changed = true;
            }
        }
    }

    /**
     * Checks whether the given 4-cycle is alternatingly colored by this regular edge coloring.
     * @param fourCycle
     * @return true if the given 4-cycle is alternatingly colored by this regular edge coloring, false otherwise.
     */
    private boolean isAlternating(Edge[] fourCycle) {
        Labeling prevLabel = get(fourCycle[0]);

        for (int i = 1; i < 4; i++) {
            Labeling label = get(fourCycle[i]);

            if (label == prevLabel) {
                return false;
            }

            prevLabel = label;
        }

        return true;
    }

    /**
     * Flips the color of all edges inside the given alternating 4-cycle.
     * Precondition: isAlternating(fourCycle)
     * @param fourCycle
     * @return a new regular edge coloring where all edges have the same color as in this coloring, except for the edges inside the given alternating 4-cycle.
     */
    private RegularEdgeColoring flip4Cycle(Edge[] fourCycle) {
        RegularEdgeColoring result = new RegularEdgeColoring(this);

        result.flip4CycleLocal(fourCycle);

        return result;
    }

    /**
     * Flips the color of all edges inside the given alternating 4-cycle in this RegularEdgeColoring.
     * Precondition: isAlternating(fourCycle)
     * @param fourCycle
     */
    private void flip4CycleLocal(Edge[] fourCycle) {
        Set<Edge> edgesInside = graph.getEdgesInside(fourCycle);

        for (Edge edge : edgesInside) {
            Labeling newLabel = (get(edge) == Labeling.RED ? Labeling.BLUE : Labeling.RED);
            put(edge, newLabel);
        }
    }

    /**
     * Checks whether the given alternating 4-cycle is right-alternating.
     * Precondition: isAlternating(cycle)
     * @param cycle
     * @return true if the given alternating 4-cycle is right-alternating, false otherwise.
     */
    protected boolean isRight(Edge[] cycle) {
        // We only need to check one vertex that has incoming edges. Each edge has has at least one such corner.
        List<Edge> edgesBetween = graph.getEdgesBetween(cycle[0], cycle[1]);
        Labeling rightLabeling = get(cycle[0]);

        if (edgesBetween.isEmpty()) {
            edgesBetween = graph.getEdgesBetween(cycle[1], cycle[2]);
            rightLabeling = get(cycle[1]);
        }

        return (get(edgesBetween.get(0)) == rightLabeling);
    }

    public boolean isValid() {
        return isValid(false);
    }

    public boolean isValid(boolean allowUnlabeledEdges) {
        // All edges incident to the North exterior vertex should be red
        for (Edge edge : graph.getVN().getEdges()) {
            Labeling l = get(edge);

            if (allowUnlabeledEdges) {
                if (l == Labeling.BLUE) {
                    return false;
                }
            } else {
                if (l != Labeling.RED && (!graph.getExteriorVertices().contains(edge.getVA()) || !graph.getExteriorVertices().contains(edge.getVB()))) {
                    return false;
                }
            }
        }

        // All edges incident to the East exterior vertex should be blue
        for (Edge edge : graph.getVE().getEdges()) {
            Labeling l = get(edge);

            if (allowUnlabeledEdges) {
                if (l == Labeling.RED) {
                    return false;
                }
            } else {
                if (l != Labeling.BLUE && (!graph.getExteriorVertices().contains(edge.getVA()) || !graph.getExteriorVertices().contains(edge.getVB()))) {
                    return false;
                }
            }
        }

        // All edges incident to the South exterior vertex should be red
        for (Edge edge : graph.getVS().getEdges()) {
            Labeling l = get(edge);

            if (allowUnlabeledEdges) {
                if (l == Labeling.BLUE) {
                    return false;
                }
            } else {
                if (l != Labeling.RED && (!graph.getExteriorVertices().contains(edge.getVA()) || !graph.getExteriorVertices().contains(edge.getVB()))) {
                    return false;
                }
            }
        }

        // All edges incident to the West exterior vertex should be blue
        for (Edge edge : graph.getVW().getEdges()) {
            Labeling l = get(edge);

            if (allowUnlabeledEdges) {
                if (l == Labeling.RED) {
                    return false;
                }
            } else {
                if (l != Labeling.BLUE && (!graph.getExteriorVertices().contains(edge.getVA()) || !graph.getExteriorVertices().contains(edge.getVB()))) {
                    return false;
                }
            }
        }

        // All interior vertices should have four intervals of colored edges
        for (Vertex vertex : graph.getVertices()) {
            if (!graph.getExteriorVertices().contains(vertex)) {
                if (allowUnlabeledEdges) {
                    if (!checkUnfinishedVertexLabeling(vertex)) {
                        return false;
                    }
                } else {
                    if (!checkFinishedVertexLabeling(vertex)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * A vertex should have blue incoming edges, red outgoing edges, blue outgoing edges and red incoming edges, in clockwise direction around itself.
     * This method only checks the colors, ignoring the directions. If there are unlabeled edges, this method checks whether it is possible to label the unlabeled edges such that this vertex satisfies the local coloring constraint.
     * @param v
     * @return true if it is possible to label the unlabeled edges around this vertex such that this vertex satisfies constraint C1 with respect to the edge colors (not the orientations)
     */
    private boolean checkUnfinishedVertexLabeling(Vertex vertex) {
        List<Edge> edges = vertex.getEdges();

        // If there are less than 4 edges, this is never going to work
        if (edges.size() < 4) {
            return false;
        }

        // See what intervals we have
        List<Labeling> intervals = new ArrayList<Labeling>();
        int redIntervals = 0;
        int blueIntervals = 0;
        int unlabeledIntervals = 0;

        for (Edge edge : edges) {
            Labeling label = get(edge);

            // Skip this label if it fits into the current interval
            if (intervals.isEmpty() || label != intervals.get(intervals.size() - 1)) {
                intervals.add(label);

                switch (label) {
                    case RED:
                        redIntervals++;
                        break;
                    case BLUE:
                        blueIntervals++;
                        break;
                    case NONE:
                        unlabeledIntervals++;
                        break;
                }
            }
        }

        // If the first and last interval are the same, merge them
        if (intervals.size() > 1 && intervals.get(0) == intervals.get(intervals.size() - 1)) {
            intervals.remove(intervals.size() - 1);

            switch (intervals.get(0)) {
                case RED:
                    redIntervals--;
                    break;
                case BLUE:
                    blueIntervals--;
                    break;
                case NONE:
                    unlabeledIntervals--;
                    break;
            }
        }

        if (redIntervals == 0 && blueIntervals == 0) {
            // All unlabeled edges, so they can still be labeled correctly
            return true;
        } else if (blueIntervals == 0) {
            // If there is only one interval of red edges, we need at least three unlabeled edges
            if (redIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    if (get(edge) == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                return nUnlabeled >= 3;
            } else {
                // If there are more, there must also be at least two unlabeled intervals separating them
                // so these can be coloured blue, which satiesfies the vertex constraint
                return true;
            }
        } else if (redIntervals == 0) {
            // If there is only one interval of blue edges, we need at least three unlabeled edges
            if (blueIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    if (get(edge) == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                return nUnlabeled >= 3;
            } else {
                // If there are more, there must also be at least two unlabeled intervals separating them
                // so these can be coloured red, which satiesfies the vertex constraint
                return true;
            }
        } else {
            // We have both red and blue intervals
            if (unlabeledIntervals == 0) {
                return redIntervals == 2 && blueIntervals == 2;
            } else if (redIntervals == 1 && blueIntervals == 1) {
                int nUnlabeled = 0;

                for (Edge edge : edges) {
                    if (get(edge) == Labeling.NONE) {
                        nUnlabeled++;
                    }
                }

                if (unlabeledIntervals == 1) {
                    // if there are at least two unlabeled edges between the red and blue intervals, it can be labeled, otherwise it's impossible
                    return nUnlabeled >= 2;
                } else {
                    // there are two unlabeled intervals between the red and blue intervals,
                    // if one of those has at least 2 edges, we can separate the intervals and create a valid coloring
                    // this is the case iff there are at least 3 unlabeled edges
                    return nUnlabeled >= 3;
                }
            } else if (redIntervals == 1 || blueIntervals == 1) {
                // the multiple intervals of the other colour must be separated on one side by a NONE interval. This interval can be given the opposite colour, to produce a valid coloring
                return true;
            } else {
                // let's see what we have left if we omit the unlabeled edges
                List<Labeling> realIntervals = new ArrayList<Labeling>();
                int realRedIntervals = 0;
                int realBlueIntervals = 0;

                for (Edge edge : edges) {
                    Labeling label = get(edge);

                    // Skip this label if it fits into the current interval or if its NONE
                    if (label != Labeling.NONE && (realIntervals.isEmpty() || label != realIntervals.get(realIntervals.size() - 1))) {
                        realIntervals.add(label);

                        switch (label) {
                            case RED:
                                realRedIntervals++;
                                break;
                            case BLUE:
                                realBlueIntervals++;
                                break;
                        }
                    }
                }

                // If the first and last interval are the same, merge them
                if (realIntervals.size() > 1 && realIntervals.get(0) == realIntervals.get(realIntervals.size() - 1)) {
                    realIntervals.remove(realIntervals.size() - 1);

                    switch (realIntervals.get(0)) {
                        case RED:
                            realRedIntervals--;
                            break;
                        case BLUE:
                            realBlueIntervals--;
                            break;
                    }
                }

                if (realRedIntervals <= 2 && realBlueIntervals <= 2) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private boolean checkFinishedVertexLabeling(Vertex vertex) {
        List<Edge> edges = vertex.getEdges();

        // If there are less than 4 edges, this is never going to work
        if (edges.size() < 4) {
            return false;
        }

        // See what intervals we have
        Labeling firstInterval = get(edges.get(0));
        Labeling lastInterval = firstInterval;
        int redIntervals = (firstInterval == Labeling.RED ? 1 : 0);
        int blueIntervals = (firstInterval == Labeling.BLUE ? 1 : 0);

        for (int i = 1; i < edges.size(); i++) {
            Labeling label = get(edges.get(i));

            // Skip this label if it fits into the current interval
            if (label != lastInterval) {
                lastInterval = label;

                switch (label) {
                    case RED:
                        redIntervals++;
                        break;
                    case BLUE:
                        blueIntervals++;
                        break;
                    default:
                        // This edge isn't labeled correctly
                        return false;
                }
            }
        }

        // If the first and last interval are the same, merge them
        if (firstInterval == lastInterval) {
            switch (firstInterval) {
                case RED:
                    if (redIntervals > 1) {
                        redIntervals--;
                    }
                    break;
                case BLUE:
                    if (blueIntervals > 1) {
                        blueIntervals--;
                    }
                    break;
            }
        }

        return redIntervals == 2 && blueIntervals == 2;
    }
}
