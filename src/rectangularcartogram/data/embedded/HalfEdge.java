/*
 * Copyright 2010-2016 Wouter Meulemans and Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectangularcartogram.data.embedded;

public class HalfEdge {

    private Face face;
    private HalfEdge twin;
    private HalfEdge next;
    private HalfEdge previous;
    private EmbeddedVertex origin;
    private boolean directed = false;
    private boolean edgeDirection;

    public Face getFace() {
        return face;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public HalfEdge getNext() {
        return next;
    }

    public void setNext(HalfEdge next) {
        this.next = next;
    }

    public EmbeddedVertex getOrigin() {
        return origin;
    }

    public void setOrigin(EmbeddedVertex origin) {
        this.origin = origin;
    }

    public HalfEdge getPrevious() {
        return previous;
    }

    public void setPrevious(HalfEdge previous) {
        this.previous = previous;
    }

    public HalfEdge getTwin() {
        return twin;
    }

    public void setTwin(HalfEdge twin) {
        this.twin = twin;
    }

    public boolean isDirected() {
        return directed;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    /**
     * If this halfedge is directed, this method returns whether the edge corresponding to the halfedge is directed in the direction of this halfedge, in which case it returns true, or in the opposite direction, in which case it returns false.
     * @return
     */
    public boolean isEdgeDirection() {
        return edgeDirection;
    }

    public void setEdgeDirection(boolean edgeDirection) {
        this.edgeDirection = edgeDirection;
    }

    public EmbeddedVertex getDestination() {
        return twin.getOrigin();
    }

    public double getLength() {
        double dx = getDestination().getX() - origin.getX();
        double dy = getDestination().getY() - origin.getY();

        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return origin + " -> " + getDestination();
    }
}
