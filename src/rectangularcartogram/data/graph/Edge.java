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
package rectangularcartogram.data.graph;

public class Edge {

    public static enum Direction {
        AB, BA, NONE;
    }

    private Vertex vA, vB;
    private boolean visible;
    private Direction direction = Direction.NONE; // TODO: remove ?

    /**
     * Creates a new undirected, visible edge with the given endpoints.
     * @param vA
     * @param vB
     */
    public Edge(final Vertex vA, final Vertex vB) {
        this(vA, vB, true);
    }

    /**
     * Creates a new edge with the given endpoints and specified visibility.
     * @param vA
     * @param vB
     * @param visible
     */
    public Edge(Vertex vA, Vertex vB, boolean visible) {
        this.vA = vA;
        this.vB = vB;
        this.visible = visible;
    }

    public Vertex getVA() {
        return vA;
    }

    public void setVA(final Vertex vA) {
        this.vA = vA;
    }

    public Vertex getVB() {
        return vB;
    }

    public void setVB(final Vertex vB) {
        this.vB = vB;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isDirected() {
        return direction != Direction.NONE;
    }

    /**
     * Returns the origin of this edge if it is directed and null otherwise.
     * @return
     */
    public Vertex getOrigin() {
        switch (direction) {
            case AB: return vA;
            case BA: return vB;
            default: return null;
        }
    }

    /**
     * Precondition: v == getVA() || v == getVB()
     * Sets the direction such that v is the origin of this edge.
     * @param v
     */
    public void setOrigin(Vertex v) {
        if (v == vA) {
            direction = Direction.AB;
        } else if (v == vB) {
            direction = Direction.BA;
        } else {
            throw new IllegalArgumentException("Origin must be one of the edge endpoints.");
        }
    }

    /**
     * Returns the destination of this edge if it is directed and null otherwise.
     * @return
     */
    public Vertex getDestination() {
        switch (direction) {
            case AB: return vB;
            case BA: return vA;
            default: return null;
        }
    }

    /**
     * Precondition: v == getVA() || v == getVB()
     * Sets the direction such that v is the destination of this edge.
     * @param v
     */
    public void setDestination(Vertex v) {
        if (v == vA) {
            direction = Direction.BA;
        } else if (v == vB) {
            direction = Direction.AB;
        } else {
            throw new IllegalArgumentException("Destination must already be one of the edge endpoints.");
        }
    }

    public boolean isNear(double x, double y, double precision) {
        double vecX = vB.getX() - vA.getX();
        double vecY = vB.getY() - vA.getY();
        double len = Math.sqrt(vecX * vecX + vecY * vecY);
        double unitX = vecX / len;
        double unitY = vecY / len;
        double offsetX = x - vA.getX();
        double offsetY = y - vA.getY();
        double dot_offset_unit = offsetX * unitX + offsetY * unitY;

        if (0 <= dot_offset_unit && dot_offset_unit <= len) {
            double rotatedUnitX = -unitY;
            double rotatedUnitY = unitX;
            double dot_offset_rotatedUnit = offsetX * rotatedUnitX + offsetY * rotatedUnitY;

            return Math.abs(dot_offset_rotatedUnit) <= precision;
        } else {
            return false;
        }
    }

    ////DEBUG////
    @Override
    public String toString() {
        return "E[" + vA + ", " + vB + "]";
    }
    ////DEBUG////
}
