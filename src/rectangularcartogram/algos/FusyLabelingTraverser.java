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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.RegularEdgeLabeling;
import rectangularcartogram.data.embedded.EmbeddedGraph;
import rectangularcartogram.data.embedded.EmbeddedVertex;
import rectangularcartogram.data.embedded.HalfEdge;
import rectangularcartogram.data.graph.CycleGraph;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.exceptions.IncorrectGraphException;

public abstract class FusyLabelingTraverser extends LabelingTraverser {

    private Random rand = new Random();
    private EmbeddedGraph dcel;
    private Map<HalfEdge, Edge> edgeMap;
    private Map<Pair<EmbeddedVertex, EmbeddedVertex>, HalfEdge> edgeBetween;
    private HashSet<String> closed;

    public FusyLabelingTraverser(Graph graph) throws IncorrectGraphException {
        super(graph);

        // Initialize fields
        dcel = new EmbeddedGraph(graph);
        edgeMap = new HashMap<HalfEdge, Edge>(graph.getEdges().size() * 4); // 2 half-edges per edge, * 2 for efficient hashing

        for (Map.Entry<Edge, HalfEdge> entry : dcel.getEdgeMap().entrySet()) {
            edgeMap.put(entry.getValue(), entry.getKey());
            edgeMap.put(entry.getValue().getTwin(), entry.getKey());
        }

        edgeBetween = new HashMap<Pair<EmbeddedVertex, EmbeddedVertex>, HalfEdge>(graph.getEdges().size() * 4);

        for (HalfEdge halfEdge : dcel.getDarts()) {
            edgeBetween.put(new Pair<EmbeddedVertex, EmbeddedVertex>(halfEdge.getOrigin(), halfEdge.getDestination()), halfEdge);
        }
    }

    @Override
    protected void traverseLabelings() throws IncorrectGraphException {
        // Initialize all labels
        RegularEdgeLabeling rel = new RegularEdgeLabeling(new CycleGraph(graph));

        for (Edge edge : graph.getEdges()) {
            rel.put(edge, new Pair<Labeling, Edge.Direction>(Labeling.NONE, Edge.Direction.NONE));
        }

        // Color and orient all edges incident to vN
        // Initialize the frontier as the path vE -> ... -> vW of neighbours of vN
        EmbeddedVertex vN = dcel.getVertexMap().get(graph.getVN());
        EmbeddedVertex vE = dcel.getVertexMap().get(graph.getVE());
        HalfEdge vNvE = edgeBetween.get(new Pair<EmbeddedVertex, EmbeddedVertex>(vN, vE));

        ArrayList<HalfEdge> path = new ArrayList<HalfEdge>();
        path.add(vNvE.getTwin().getPrevious().getTwin());

        for (HalfEdge e : vN.getEdges(vNvE)) {
            if (!e.getFace().isOuterFace() && !e.getTwin().getFace().isOuterFace()) {
                label(e.getTwin(), Labeling.RED, rel);
                path.add(e.getTwin().getPrevious().getTwin());
            }
        }

        closed = new HashSet<String>();

        /*/// DEBUG ////
        System.out.println("Edges in the graph:");
        System.out.println(graph.getEdges());
        //// DEBUG ///*/

        // Recursively traverse all labelings
        traverseUniqueLabelings(rel, path);
    }

    private void traverseLabelings(RegularEdgeLabeling rel, ArrayList<HalfEdge> path) {
        /*/// DEBUG ////
        System.out.println("TraverseLabelings with path " + path);
        System.out.println("And current REL:");
        printLabeling(rel);
        System.out.println();
        //// DEBUG ///*/

        if (path.get(0).getFace().isOuterFace()) {
            // We have computed a complete labeling; process it
            processLabeling(rel);

            /*/// DEBUG ////
            System.out.println("This labeling is complete.");
            System.out.println();
            //// DEBUG ///*/
        } else {
            // Our labeling is only partial; extend it by trying every admissable path
            for (int i = 0; i < path.size(); i++) {
                for (int j = i + 1; j < path.size(); j++) {
                    Pair<ArrayList<HalfEdge>, ArrayList<HalfEdge>> pair = getParallelPath(path, i, j);
                    ArrayList<HalfEdge> parallelPath = pair.getFirst();
                    ArrayList<HalfEdge> internalEdges = pair.getSecond();

                    if (isChordFree(path, i, j, parallelPath)) {
                        // Color and orient the edges of the path
                        for (int k = i; k <= j; k++) {
                            label(path.get(k).getTwin(), Labeling.BLUE, rel);
                        }

                        // Color and orient the edges between the path and the parallel path
                        for (HalfEdge e : internalEdges) {
                            label(e.getTwin(), Labeling.RED, rel);
                        }

                        // Update the path
                        ArrayList<HalfEdge> newPath = new ArrayList<HalfEdge>(path.size() + parallelPath.size() - (j - i + 1));
                        newPath.addAll(path.subList(0, i));
                        newPath.addAll(parallelPath);
                        newPath.addAll(path.subList(j + 1, path.size()));

                        // Recurse
                        traverseLabelings(rel, newPath);
                    } else {
                        /*/// DEBUG ////
                        System.out.println("Resulting path is not chord-free.");
                        System.out.println();
                        //// DEBUG ///*/
                    }
                }
            }
        }
    }

    public void traverseUniqueLabelings() {
        // Initialize all labels
        RegularEdgeLabeling rel = new RegularEdgeLabeling(new CycleGraph(graph));

        for (Edge edge : graph.getEdges()) {
            rel.put(edge, new Pair<Labeling, Edge.Direction>(Labeling.NONE, Edge.Direction.NONE));
        }

        // Color and orient all edges incident to vN
        // Initialize the frontier as the path vE -> ... -> vW of neighbours of vN
        EmbeddedVertex vN = dcel.getVertexMap().get(graph.getVN());
        EmbeddedVertex vE = dcel.getVertexMap().get(graph.getVE());
        HalfEdge vNvE = edgeBetween.get(new Pair<EmbeddedVertex, EmbeddedVertex>(vN, vE));

        ArrayList<HalfEdge> path = new ArrayList<HalfEdge>();
        path.add(vNvE.getTwin().getPrevious().getTwin());

        for (HalfEdge e : vN.getEdges(vNvE)) {
            if (!e.getFace().isOuterFace() && !e.getTwin().getFace().isOuterFace()) {
                label(e.getTwin(), Labeling.RED, rel);
                path.add(e.getTwin().getPrevious().getTwin());
            }
        }

        closed = new HashSet<String>();

        /*/// DEBUG ////
        System.out.println("Edges in the graph:");
        System.out.println(graph.getEdges());
        //// DEBUG ///*/

        // Recursively traverse all labelings
        traverseUniqueLabelings(rel, path);
    }

    private void traverseUniqueLabelings(RegularEdgeLabeling rel, ArrayList<HalfEdge> path) {
        /*/// DEBUG ////
        System.out.println("TraverseLabelings with path " + path);
        System.out.println("And current REL:");
        printLabeling(rel);
        System.out.println();
        //// DEBUG ///*/

        if (closed.contains(getIdentifier(rel))) {
            /*/// DEBUG ////
            System.out.println("Partial labeling has already been handled.");
            //// DEBUG ///*/
            return;
        }

        if (path.get(0).getFace().isOuterFace()) {
            // We have computed a complete labeling; process it
            processLabeling(rel);
            closed.add(getIdentifier(rel));

            /*/// DEBUG ////
            System.out.println("This labeling is complete.");
            System.out.println();
            //// DEBUG ///*/
        } else {
            // Our labeling is only partial; extend it by trying every admissable path
            for (int i = 0; i < path.size(); i++) {
                for (int j = i + 1; j < path.size(); j++) {
                    Pair<ArrayList<HalfEdge>, ArrayList<HalfEdge>> pair = getParallelPath(path, i, j);
                    ArrayList<HalfEdge> parallelPath = pair.getFirst();
                    ArrayList<HalfEdge> internalEdges = pair.getSecond();

                    if (isChordFree(path, i, j, parallelPath)) {
                        // Color and orient the edges of the path
                        for (int k = i; k <= j; k++) {
                            label(path.get(k).getTwin(), Labeling.BLUE, rel);
                        }

                        // Color and orient the edges between the path and the parallel path
                        for (HalfEdge e : internalEdges) {
                            label(e.getTwin(), Labeling.RED, rel);
                        }

                        // Update the path
                        ArrayList<HalfEdge> newPath = new ArrayList<HalfEdge>(path.size() + parallelPath.size() - (j - i + 1));
                        newPath.addAll(path.subList(0, i));
                        newPath.addAll(parallelPath);
                        newPath.addAll(path.subList(j + 1, path.size()));

                        // Recurse
                        traverseUniqueLabelings(rel, newPath);

                        // Uncolor the edges
                        for (int k = i; k <= j; k++) {
                            rel.put(edgeMap.get(path.get(k)), new Pair<Labeling, Edge.Direction>(Labeling.NONE, Edge.Direction.NONE));
                        }

                        for (HalfEdge e : internalEdges) {
                            rel.put(edgeMap.get(e), new Pair<Labeling, Edge.Direction>(Labeling.NONE, Edge.Direction.NONE));
                        }
                    } else {
                        /*/// DEBUG ////
                        System.out.println("Resulting path is not chord-free.");
                        System.out.println();
                        //// DEBUG ///*/
                    }
                }
            }

            closed.add(getIdentifier(rel));
        }
    }

    private Pair<ArrayList<HalfEdge>, ArrayList<HalfEdge>> getParallelPath(ArrayList<HalfEdge> path, int i, int j) {
        // Find an admissable path connecting the origin of path[i] to the destination of path[j]
        // To be admissable, the path can only use neighbours of vertices on the path
        // And it must use the next counter-clockwise edge out of the origin of path[i]
        // And the next clockwise edge into the destination of path[j]
        ArrayList<HalfEdge> parallelPath = new ArrayList<HalfEdge>();
        ArrayList<HalfEdge> internalEdges = new ArrayList<HalfEdge>();

        int k = i + 1; // The next edge on the path to be encountered
        HalfEdge walkEdge = path.get(i).getNext(); // Pointing from the current vertex on path to the current vertex on parallelPath
        parallelPath.add(walkEdge.getNext().getTwin());

        while (walkEdge != path.get(j).getNext()) {
            internalEdges.add(walkEdge);

            if (walkEdge.getTwin().getNext() == path.get(k)) {
                // Move the pointer on the path
                walkEdge = walkEdge.getTwin().getPrevious();
                k++;
            } else {
                // Move the pointer on the parallelPath
                walkEdge = walkEdge.getTwin().getNext();
                parallelPath.add(walkEdge.getNext().getTwin());
            }
        }

        parallelPath.add(walkEdge.getTwin());

        /*/// DEBUG ////
        System.out.println("ParallelPath between " + i + " and " + j + " in " + path);
        System.out.println(parallelPath);
        System.out.println("Internal edges: " + internalEdges);
        //// DEBUG ///*/

        return new Pair<ArrayList<HalfEdge>, ArrayList<HalfEdge>>(parallelPath, internalEdges);
    }

    private boolean isChordFree(ArrayList<HalfEdge> path, int i, int j, ArrayList<HalfEdge> parallelPath) {
        // Check if the path we obtain if we replace path[i .. j] with the parallel path is chord-free
        // We already know that the current path is chord-free, so we only have to check for edges
        // between the remaining parts of the path and the parallel-path or between two vertices of the parallel path
        ArrayList<EmbeddedVertex> parallelVertices = new ArrayList<EmbeddedVertex>(parallelPath.size() + 1);

        parallelVertices.add(parallelPath.get(0).getOrigin());
        for (HalfEdge halfEdge : parallelPath) {
            parallelVertices.add(halfEdge.getDestination());
        }

        // Check for edges between the existing path and the new vertices
        for (int k = 0; k < path.size(); k++) {
            EmbeddedVertex pathVertex;

            if (k < i) {
                pathVertex = path.get(k).getOrigin();
            } else if (k > j) {
                pathVertex = path.get(k).getDestination();
            } else {
                continue;
            }

            for (int m = 1; m < parallelVertices.size() - 1; m++) { // exclude the first and last vertex
                if (edgeBetween.containsKey(new Pair<EmbeddedVertex, EmbeddedVertex>(pathVertex, parallelVertices.get(m)))) {
                    return false;
                }
            }
        }

        // Check for internal chords
        for (int k = 0; k < parallelVertices.size(); k++) {
            EmbeddedVertex parallelVertex = parallelVertices.get(k);

            for (int m = k + 2; m < parallelVertices.size(); m++) {
                if (edgeBetween.containsKey(new Pair<EmbeddedVertex, EmbeddedVertex>(parallelVertex, parallelVertices.get(m)))) {
                    return false;
                }
            }
        }

        return true;
    }

    public RegularEdgeLabeling generateRandomLabeling() {
        // Initialize all labels
        RegularEdgeLabeling rel = new RegularEdgeLabeling(new CycleGraph(graph));

        for (Edge edge : graph.getEdges()) {
            rel.put(edge, new Pair<Labeling, Edge.Direction>(Labeling.NONE, Edge.Direction.NONE));
        }

        // Color and orient all edges incident to vN
        // Initialize the frontier as the path vE -> ... -> vW of neighbours of vN
        EmbeddedVertex vN = dcel.getVertexMap().get(graph.getVN());
        EmbeddedVertex vE = dcel.getVertexMap().get(graph.getVE());
        HalfEdge vNvE = edgeBetween.get(new Pair<EmbeddedVertex, EmbeddedVertex>(vN, vE));

        ArrayList<HalfEdge> path = new ArrayList<HalfEdge>();
        path.add(vNvE.getTwin().getPrevious().getTwin());

        for (HalfEdge e : vN.getEdges(vNvE)) {
            if (!e.getFace().isOuterFace() && !e.getTwin().getFace().isOuterFace()) {
                label(e.getTwin(), Labeling.RED, rel);
                path.add(e.getTwin().getPrevious().getTwin());
            }
        }

        // Recursively generate a random labeling
        return generateRandomLabeling(rel, path);
    }

    private RegularEdgeLabeling generateRandomLabeling(RegularEdgeLabeling rel, ArrayList<HalfEdge> path) {
        /*/// DEBUG ////
        System.out.println("TraverseLabelings with path " + path);
        System.out.println("And current REL:");
        printLabeling(rel);
        System.out.println();
        //// DEBUG ///*/

        if (path.get(0).getFace().isOuterFace()) {
            // We have computed a complete labeling; return it
            return rel;

            /*/// DEBUG ////
            System.out.println("This labeling is complete.");
            System.out.println();
            //// DEBUG ///*/
        } else {
            // Our labeling is only partial; extend it by a random admissable path
            int x = 0;

            while (true) {
                x++;

                int a = rand.nextInt(path.size());
                int b = rand.nextInt(path.size());

                while (a == b) {
                    b = rand.nextInt(path.size());
                }

                int i = Math.min(a, b);
                int j = Math.max(a, b);

                //System.out.println("Try " + x + " at finding an admissable path. (i=" + i + ",j=" + j+")");

                Pair<ArrayList<HalfEdge>, ArrayList<HalfEdge>> pair = getParallelPath(path, i, j);
                ArrayList<HalfEdge> parallelPath = pair.getFirst();
                ArrayList<HalfEdge> internalEdges = pair.getSecond();

                if (isChordFree(path, i, j, parallelPath)) {
                    // Color and orient the edges of the path
                    for (int k = i; k <= j; k++) {
                        label(path.get(k).getTwin(), Labeling.BLUE, rel);
                    }

                    // Color and orient the edges between the path and the parallel path
                    for (HalfEdge e : internalEdges) {
                        label(e.getTwin(), Labeling.RED, rel);
                    }

                    // Update the path
                    ArrayList<HalfEdge> newPath = new ArrayList<HalfEdge>(path.size() + parallelPath.size() - (j - i + 1));
                    newPath.addAll(path.subList(0, i));
                    newPath.addAll(parallelPath);
                    newPath.addAll(path.subList(j + 1, path.size()));

                    // Recurse
                    return generateRandomLabeling(rel, newPath);
                }
            }
        }
    }

    protected void traverseColorings(Graph graph) throws IncorrectGraphException {
    }

    /**
     * Labels the edge corresponding to the given halfedge with the specified color and the direction of the halfedge.
     * @param rel
     * @param twin
     * @param labeling
     */
    private void label(HalfEdge halfedge, Labeling labeling, RegularEdgeLabeling rel) {
        Edge e = edgeMap.get(halfedge);

        if (dcel.getVertexMap().get(e.getVA()) == halfedge.getOrigin()) {
            rel.put(e, new Pair<Labeling, Edge.Direction>(labeling, Edge.Direction.AB));
        } else {
            rel.put(e, new Pair<Labeling, Edge.Direction>(labeling, Edge.Direction.BA));
        }
    }

    private void printLabeling(RegularEdgeLabeling rel) {
        System.out.println(getIdentifier(rel));
    }

    private String getIdentifier(RegularEdgeLabeling rel) {
        StringBuilder sb = new StringBuilder();

        for (Edge edge : graph.getEdges()) {
            Pair<Graph.Labeling, Edge.Direction> label = rel.get(edge);

            if (label == null) {
                sb.append("-");
            } else {
                switch (label.getFirst()) {
                    case BLUE:
                        sb.append("B");
                        break;
                    case RED:
                        sb.append("R");
                        break;
                    case PATH:
                        sb.append("$");
                        break;
                    case NONE:
                        sb.append("-");
                        break;
                }
            }
        }

        return sb.toString();
    }
}
