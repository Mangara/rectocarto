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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Edge.Direction;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.exceptions.IncorrectDirectionException;
import rectangularcartogram.measures.IncorrectDirectionsMeasure;

public class RegularEdgeLabeling extends HashMap<Edge, Pair<Labeling, Direction>> {

    private static final Random rand = new Random();

    private CycleGraph graph;

    public RegularEdgeLabeling(RegularEdgeLabeling rel) {
        super(rel);
        this.graph = rel.graph;
    }

    public RegularEdgeLabeling(CycleGraph graph) {
        super(graph.getEdges().size() * 2);
        this.graph = graph;
    }

    public RegularEdgeLabeling(RegularEdgeColoring coloring) throws IncorrectDirectionException {
        super(coloring.getGraph().getEdges().size() * 2);
        graph = coloring.getGraph();

        // Assign the right colors
        for (Edge edge : graph.getEdges()) {
            put(edge, new Pair<Labeling, Direction>(coloring.get(edge), Direction.NONE));
        }

        // Use a DFS starting from the South exterior vertex to assign all directions
        Stack<Vertex> frontier = new Stack<Vertex>();
        HashSet<Vertex> visited = new HashSet<Vertex>(graph.getVertices().size());

        // Direct the edges incident to VS away from it and add all interior neighbours of VS to the frontier
        for (Edge edge : graph.getVS().getEdges()) {
            if (coloring.get(edge) != Labeling.NONE) {
                if (edge.getVA() == graph.getVS()) {
                    get(edge).setSecond(Direction.AB);

                    if (edge.getVB() != graph.getVN()) {
                        frontier.push(edge.getVB());
                    }
                } else {
                    assert edge.getVB() == graph.getVS();

                    get(edge).setSecond(Direction.BA);

                    if (edge.getVA() != graph.getVN()) {
                        frontier.push(edge.getVA());
                    }
                }
            }
        }

        while (!frontier.empty()) {
            Vertex current = frontier.pop();
            visited.add(current);

            assignDirections(current);

            for (Vertex neighbour : current.getNeighbours()) {
                if (!visited.contains(neighbour) && !frontier.contains(neighbour) && !graph.getExteriorVertices().contains(neighbour)) {
                    frontier.push(neighbour);
                }
            }
        }

        // Check whether all directions are correct
        IncorrectDirectionsMeasure.checkDirections(graph, this);
    }

    public CycleGraph getGraph() {
        return graph;
    }

    public int getNumFourCycles() {
        return graph.getFourCycles().size();
    }

    /**
     * Returns a list of all neighbours of this regular edge labeling.
     * @return
     */
    public List<RegularEdgeLabeling> getNeighbours() {
        List<RegularEdgeLabeling> neighbours = new ArrayList<RegularEdgeLabeling>();

        for (int i = 0; i < getNumFourCycles(); i++) {
            RegularEdgeLabeling neighbour = getNeighbour(i);

            if (neighbour != null) {
                neighbours.add(neighbour);
            }
        }

        return neighbours;
    }

    /**
     * Returns the i-th neighbour of this regular edge labeling.
     * The i-th neighbour is null if the i-th 4-cycle is not colored alternatingly, otherwise it is the result of flipping the colors of all edges inside this alternating 4-cycle.
     * @param i
     * @return
     */
    public RegularEdgeLabeling getNeighbour(int i) {
        Edge[] cycle = graph.getFourCycles().get(i);

        if (isAlternating(cycle)) {
            return flip4Cycle(cycle);
        } else {
            return null;
        }
    }

    /**
     * Returns the regular edge labeling that results from taking a step down the lattice (towards the minimum labeling), by flipping the color of all edges inside the first right alternating 4-cycle, or null if no such cycle exists.
     * @return
     */
    public RegularEdgeLabeling moveDown() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && isRight(cycle)) {
                return flip4Cycle(cycle);
            }
        }

        // We are at the minimum labeling
        return null;
    }

    /**
     * Changes this labeling to the regular edge labeling that results from taking a step down the lattice (towards the minimum labeling), by flipping the color of all edges inside the first right alternating 4-cycle, or doesn't change this labeling if no such cycle exists.
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
     * Returns the index of the first right alternating 4-cycle, or -1 if no such cycle exists (in which case this is the minimum labeling).
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
     * Returns the regular edge labeling that results from taking a step down the lattice (towards the minimum labeling) by flipping the color of all edges inside a random right alternating 4-cycle, or this labeling if no such cycle exists.
     * @return
     */
    public RegularEdgeLabeling moveDownRandomly() {
        RegularEdgeLabeling result = new RegularEdgeLabeling(this);

        result.moveDownRandomlyLocal();
        
        return result;
    }

    /**
     * Changes this labeling by taking a step down the lattice (towards the minimum labeling) by flipping the color of all edges inside a random right alternating 4-cycle, or doesn't change this labeling if no such cycle exists.
     */
    public void moveDownRandomlyLocal() {
        List<Edge[]> rightAlternating = new ArrayList<Edge[]>();

        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && isRight(cycle)) {
                rightAlternating.add(cycle);
            }
        }

        if (rightAlternating.isEmpty()) {
            // No right alternating 4-cycle, we are at the minimal labeling
        } else {
            flip4CycleLocal(rightAlternating.get(rand.nextInt(rightAlternating.size())));
        }
    }

    /**
     * Returns the regular edge labeling that results from taking a step up the lattice (towards the maximum labeling), by flipping the color of all edges inside the first left alternating 4-cycle, or null if no such cycle exists.
     * @return
     */
    public RegularEdgeLabeling moveUp() {
        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && !isRight(cycle)) {
                return flip4Cycle(cycle);
            }
        }

        // We are at the maximal labeling
        return null;
    }

    /**
     * Changes this labeling to the regular edge labeling that results from taking a step up the lattice (towards the maximum labeling), by flipping the color of all edges inside the first left alternating 4-cycle, or doesn't change this labeling if no such cycle exists.
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
     * Returns the regular edge labeling that results from taking a step up the lattice (towards the maximum labeling) by flipping the color of all edges inside a random left alternating 4-cycle, or this labeling if no such cycle exists.
     * @return
     */
    public RegularEdgeLabeling moveUpRandomly() {
        RegularEdgeLabeling result = new RegularEdgeLabeling(this);

        result.moveUpRandomlyLocal();

        return result;
    }

    /**
     * Changes this labeling by taking a step up the lattice (towards the maximum labeling) by flipping the color of all edges inside a random left alternating 4-cycle, or doesn't change this labeling if no such cycle exists.
     */
    public void moveUpRandomlyLocal() {
        List<Edge[]> leftAlternating = new ArrayList<Edge[]>();

        for (Edge[] cycle : graph.getFourCycles()) {
            if (isAlternating(cycle) && !isRight(cycle)) {
                leftAlternating.add(cycle);
            }
        }

        if (leftAlternating.isEmpty()) {
            // No left alternating 4-cycle, we are at the maximal labeling
        } else {
            flip4CycleLocal(leftAlternating.get(rand.nextInt(leftAlternating.size())));
        }
    }

    /**
     * Returns a random neighbour of this regular edge labeling by flipping the color of all edges inside a random alternating 4-cycle.
     * @return
     */
    public RegularEdgeLabeling moveRandomly() {
        RegularEdgeLabeling result = new RegularEdgeLabeling(this);

        result.moveRandomlyLocal();

        return result;
    }

    /**
     * Changes this labeling to a random neighbour by flipping the color of all edges inside a random alternating 4-cycle.
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
        Labeling prevLabel = get(fourCycle[0]).getFirst();

        for (int i = 1; i < 4; i++) {
            Labeling label = get(fourCycle[i]).getFirst();

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
    private RegularEdgeLabeling flip4Cycle(Edge[] fourCycle) {
        RegularEdgeLabeling result = new RegularEdgeLabeling(this);

        result.flip4CycleLocal(fourCycle);

        return result;
    }

    /**
     * Flips the color of all edges inside the given alternating 4-cycle in this RegularEdgeLabeling.
     * Precondition: isAlternating(fourCycle)
     * @param fourCycle
     */
    private void flip4CycleLocal(Edge[] fourCycle) {
        Set<Edge> edgesInside = graph.getEdgesInside(fourCycle);

        Labeling switchLabeling = getSwitchLabeling(fourCycle);

        for (Edge edge : edgesInside) {
            Labeling label = get(edge).getFirst();
            Labeling newLabel = (label == Labeling.RED ? Labeling.BLUE : Labeling.RED);
            Direction newDirection;

            if (label == switchLabeling) {
                // This edge needs to switch its direction
                newDirection = (get(edge).getSecond() == Direction.AB ? Direction.BA : Direction.AB);
            } else {
                newDirection = get(edge).getSecond();
            }

            put(edge, new Pair<Labeling, Direction>(newLabel, newDirection));
        }
    }

    /**
     * Checks whether the given alternating 4-cycle is right-alternating.
     * Precondition: isAlternating(cycle)
     * @param cycle
     * @return true if the given alternating 4-cycle is right-alternating, false otherwise.
     */
    private boolean isRight(Edge[] cycle) {
        // We only need to check one vertex that has incoming edges. Each edge has has at least one such corner.
        List<Edge> edgesBetween = graph.getEdgesBetween(cycle[0], cycle[1]);
        Labeling rightLabeling = get(cycle[0]).getFirst();

        if (edgesBetween.isEmpty()) {
            edgesBetween = graph.getEdgesBetween(cycle[1], cycle[2]);
            rightLabeling = get(cycle[1]).getFirst();
        }

        return (get(edgesBetween.get(0)).getFirst() == rightLabeling);
    }

    /**
     * Assigns directions consistent with the coloring and existing directions to all edges around this vertex.
     * Precondition: at least one edge around this vertex is directed and these directions are not conflicting.
     * @param vertex
     */
    private void assignDirections(Vertex vertex) {
        List<Edge> edges = vertex.getEdges(); // All edges incident to this vertex, in clockwise order around it

        // Find the first directed edge
        int firstDirected = -1;

        for (int i = 0; i < edges.size(); i++) {
            if (get(edges.get(i)).getSecond() != Direction.NONE) {
                firstDirected = i;
                break;
            }
        }

        Labeling currentLabel = get(edges.get(firstDirected)).getFirst();
        boolean currentlyOutgoing = isOutgoing(edges.get(firstDirected), vertex, get(edges.get(firstDirected)).getSecond());

        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get((i + firstDirected) % edges.size());
            Pair<Labeling, Direction> label = get(edge);

            if (label.getFirst() != currentLabel) {
                currentLabel = label.getFirst();

                if (currentLabel == Labeling.RED) {
                    currentlyOutgoing = !currentlyOutgoing;
                }
            }

            if (currentlyOutgoing) {
                if (edge.getVA() == vertex) {
                    label.setSecond(Direction.AB);
                } else {
                    label.setSecond(Direction.BA);
                }
            } else {
                if (edge.getVA() == vertex) {
                    label.setSecond(Direction.BA);
                } else {
                    label.setSecond(Direction.AB);
                }
            }
        }
    }

    /**
     * Checks if the given edge is outgoing from the given vertex if it has the given direction.
     * Precondition: v == e.getVA() || v == e.getVB()
     * @param e
     * @param v
     * @param d
     * @return
     */
    private boolean isOutgoing(Edge e, Vertex v, Direction d) {
        if (e.getVA() == v) {
            return d == Direction.AB;
        } else {
            assert e.getVB() == v : "Vertex should be one of the edge endpoints";
            return d == Direction.BA;
        }
    }

    private Labeling getSwitchLabeling(Edge[] fourCycle) {
        Labeling switchLabeling;

        // Shared vertex between the first and second edge
        Vertex v = (fourCycle[0].getVA() == fourCycle[1].getVA() || fourCycle[0].getVA() == fourCycle[1].getVB() ? fourCycle[0].getVA() : fourCycle[0].getVB());

        if (isOutgoing(fourCycle[0], v, get(fourCycle[0]).getSecond()) == isOutgoing(fourCycle[1], v, get(fourCycle[1]).getSecond())) {
            // This shared vertex is a sink or source
            // if we are switching from right alternating to left alternating, the edges that are labeled the same as the left edge of this vertex (the second cycle edge) switch directions
            switchLabeling = get(fourCycle[1]).getFirst();
        } else {
            switchLabeling = get(fourCycle[0]).getFirst();
        }

        if (!isRight(fourCycle)) {
            // the other edges should switch direction
            switchLabeling = (switchLabeling == Labeling.RED ? Labeling.BLUE : Labeling.RED);
        }

        return switchLabeling;
    }
}
