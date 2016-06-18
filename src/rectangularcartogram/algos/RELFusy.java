
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
 */package rectangularcartogram.algos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Stack;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.ClockwiseOrder;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Vertex;

public class RELFusy {

    private Stack<ArrayList<Vertex>> matchingPaths;
    private ArrayList<Vertex> path; // Vertices on the path from E to W
    private int index; // Pointer to a vertex on the path
    private HashSet<Vertex> markedVertices;
    private Graph graph;

    public void computeREL(Graph graph) throws IncorrectGraphException {
        this.graph = graph;

        GraphChecker.checkGraph(graph);

        initialize();

        while (!path.contains(graph.getExteriorVertices().get(2))) {
            step();

            /*System.out.println("Matching paths after step");
            for (ArrayList<Vertex> mPath : matchingPaths) {
                System.out.println("  " + mPath);
            }
            System.out.println("Pointer: " + path.get(index) + " (index: " + index + ")");*/
        }
    }

    public void doRELStep(Graph graph) {
        if (graph != this.graph) {
            this.graph = graph;
            initialize();
        } else {
            // If the path contains vS, we are done
            if (!path.contains(graph.getExteriorVertices().get(2))) {
                step();
            }
        }

        /*System.out.println("Matching paths after step");
        for (ArrayList<Vertex> mPath : matchingPaths) {
            System.out.println("  " + mPath);
        }
        System.out.println("Pointer: " + path.get(index) + " (index: " + index + ")");*/
    }

    public void initialize(Graph graph) {
        this.graph = graph;
        initialize();
    }

    private void initialize() {
        // Initialization:
        //  all edges incident to N are coloured red and oriented towards N
        //  path contains all neighbours of N, ordered from E to W
        //  pointer to E

        for (Edge edge : graph.getEdges()) {
            graph.setLabel(edge, Graph.Labeling.NONE);
        }

        Vertex vN = graph.getVN();
        Vertex vE = graph.getVE();
        Vertex vS = graph.getVS();
        Vertex vW = graph.getVW();

        matchingPaths = new Stack<ArrayList<Vertex>>();
        path = new ArrayList<Vertex>();
        path.add(vE);
        index = 0;
        markedVertices = new HashSet<Vertex>(2 * graph.getVertices().size());

        ArrayList<Edge> neighbours = new ArrayList<Edge>(vN.getEdges());
        Collections.sort(neighbours, new ClockwiseOrder(vN));

        int start = -1;

        for (int i = 0; i < neighbours.size(); i++) {
            Edge edge = neighbours.get(i);

            if (edge.getVA() == vE || edge.getVB() == vE) {
                start = i;
                break;
            }
        }

        for (int i = 1; i < neighbours.size() - 1; i++) { // -1 to skip vW
            Edge edge = neighbours.get((i + start) % neighbours.size());

            graph.setLabel(edge, Graph.Labeling.RED);

            if (edge.getVA() == vN) {
                edge.setDirection(Edge.Direction.BA);
            } else {
                edge.setDirection(Edge.Direction.AB);
            }

            path.add(edge.getOrigin());
        }

        path.add(vW);

        //// DEBUG ////
        // color all current path edges
        for (int i = 0; i < path.size() - 1; i++) {
            Vertex vertex = path.get(i);
            Vertex nextVertex = path.get(i + 1);

            // Color the correct edges
            for (Edge edge : vertex.getEdges()) {
                if (edge.getVA() == nextVertex || edge.getVB() == nextVertex) {
                    graph.setLabel(edge, Graph.Labeling.PATH);
                }
            }
        }
        //// DEBUG ////
    }

    private void step() {
        if (matchingPaths.isEmpty()) {
            movePointer();
        } else {
            ArrayList<Vertex> rightmostPath = matchingPaths.peek();

            // If v is the left extremity of the path on top of the stack
            if (path.get(index) == rightmostPath.get(rightmostPath.size() - 1)) {
                updateLabeling();
            } else {
                movePointer();
            }
        }

        if (path.contains(graph.getExteriorVertices().get(2))) { // We are done
            // Unmark all path edges
            for (Edge edge : path.get(1).getEdges()) {
                if (edge.getVA() == path.get(0) || edge.getVA() == path.get(2) ||
                        edge.getVB() == path.get(0) || edge.getVB() == path.get(2)) {
                    graph.setLabel(edge, Graph.Labeling.NONE);
                }
            }
        }
    }

    private void movePointer() {
        // Compute the matching path of v and add it if it is valid
        ArrayList<Vertex> matchingPath = getMatchingPath(index);

        if (matchingPath != null) {
            matchingPaths.push(matchingPath);
        }

        // Move the pointer one step to the left
        index++;
    }

    private void updateLabeling() {
        // Color blue and orient the edges of the matching path from left to right
        // Color red and orient from the matching path to the path,
        // the edges inside the cycle formed by the matching path and the path.
        ArrayList<Vertex> matchingPath = matchingPaths.pop();
        Vertex v = path.get(index);
        Vertex w = matchingPath.get(0);

        int i = index;
        Vertex vertex = path.get(i);
        Vertex nextVertex = path.get(i - 1);

        while (vertex != w) {
            // Color and orient the correct edges
            for (Edge edge : vertex.getEdges()) {
                if (edge.getVA() == nextVertex) {
                    graph.setLabel(edge, Graph.Labeling.BLUE);
                    edge.setDirection(Edge.Direction.BA);
                } else if (edge.getVB() == nextVertex) {
                    graph.setLabel(edge, Graph.Labeling.BLUE);
                    edge.setDirection(Edge.Direction.AB);
                } else if (vertex != v && edge.getVA() != v && edge.getVA() != w && matchingPath.contains(edge.getVA())) {
                    graph.setLabel(edge, Graph.Labeling.RED);
                    edge.setDirection(Edge.Direction.AB);
                } else if (vertex != v && edge.getVB() != v && edge.getVB() != w && matchingPath.contains(edge.getVB())) {
                    graph.setLabel(edge, Graph.Labeling.RED);
                    edge.setDirection(Edge.Direction.BA);
                }
            }

            vertex = nextVertex;
            i--;

            if (i > 0) { // this should always hold, unless we are at w, so the loop will stop anyway
                nextVertex = path.get(i - 1);
            } else {
                assert vertex == w;
            }
        }

        // Replace [v, w] by the matching path
        // v = path.get(index), w = path.get(i), i < index

        //System.out.println("Path before: " + path);
        //System.out.println("Matching path: " + matchingPath);

        // Remove all vertices between v and w from the path
        path.subList(i + 1, index).clear();

        // Insert the new vertices in the correct order
        path.addAll(i + 1, matchingPath.subList(1, matchingPath.size() - 1));

        //System.out.println("Path after: " + path);

        index = i;

        //// DEBUG ////
        // color all current path edges
        for (i = 0; i < path.size() - 1; i++) {
            vertex = path.get(i);
            nextVertex = path.get(i + 1);

            // Color and orient the correct edges
            for (Edge edge : vertex.getEdges()) {
                if (edge.getVA() == nextVertex || edge.getVB() == nextVertex) {
                    graph.setLabel(edge, Graph.Labeling.PATH);
                }
            }
        }
        //// DEBUG ////
    }

    private ArrayList<Vertex> getMatchingPath(int pathIndex) {
        if (pathIndex > path.size() - 3) {
            //System.out.println("No path exists, because index is too high");
            return null;
        }

        Vertex v = path.get(pathIndex);
        Vertex w = path.get(pathIndex + 1);
        Vertex ww = path.get(pathIndex + 2);

        // Get the neighbours of w inside the cycle until one of them is connected to a vertex on the path
        ArrayList<Edge> neighbourEdges = new ArrayList<Edge>(w.getEdges());
        Collections.sort(neighbourEdges, new ClockwiseOrder(w));

        // Find v, all further vertices until ww are neighbours inside the cycle
        boolean foundV = false;
        ArrayList<Vertex> neighbours = new ArrayList<Vertex>();

        for (int i = 0; i < 2 * neighbourEdges.size(); i++) {
            Edge e = neighbourEdges.get(i % neighbourEdges.size());
            Vertex neighbour = (e.getVA() == w ? e.getVB() : e.getVA());

            if (neighbour == v) {
                foundV = true;
            } else if (foundV) { // If we already encountered v
                // And did not encounter ww yet
                if (neighbour == ww) {
                    break;
                }

                if (markedVertices.contains(neighbour)) {
                    //System.out.println("Encountered marked vertex: " + neighbour);
                    return null;
                }

                neighbours.add(neighbour);
            }
        }

        /*/// DEBUG ////
        System.out.println("v : " + v);
        System.out.println("w : " + w);
        System.out.println("ww: " + ww);
        System.out.println("Neighbours of w inside the cycle: " + neighbours);
        /*/// DEBUG ////

        // Find the first neighbour that is connected to a vertex on the path to the left of w
        int firstNeighbour = -1;
        int lastPathIndex = -1;

        for (int i = 0; i < neighbours.size(); i++) {
            Vertex neighbour = neighbours.get(i);

            for (int j = path.size() - 1; j > pathIndex + 1; j--) {
                for (Edge e : neighbour.getEdges()) {
                    Vertex neighbour2 = (e.getVA() == neighbour ? e.getVB() : e.getVA());

                    if (neighbour2 == path.get(j)) {
                        firstNeighbour = i;
                        lastPathIndex = j;
                        break;
                    }
                }

                if (firstNeighbour >= 0) {
                    break;
                }
            }

            if (firstNeighbour >= 0) {
                break;
            }
        }

        if (firstNeighbour < 0) { // Something went wrong, probably
            System.err.println("First Neighbour is null!!");
            return null;
        }

        // The matching path is [v, neighbours[0], ..., neighbours[firstNeighbour], path[lastPathIndex]]
        ArrayList<Vertex> matchingPath = new ArrayList<Vertex>(firstNeighbour + 2);
        matchingPath.add(v);

        for (int i = 0; i <= firstNeighbour; i++) {
            Vertex n = neighbours.get(i);
            matchingPath.add(n);
            markedVertices.add(n);
        }

        matchingPath.add(path.get(lastPathIndex));

        return matchingPath;
    }
}
