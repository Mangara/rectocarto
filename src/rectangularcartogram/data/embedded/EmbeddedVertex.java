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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmbeddedVertex {

    private HalfEdge dart;
    private double x, y;

    public EmbeddedVertex(final double x, final double y) {
        this.x = x;
        this.y = y;
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

    public HalfEdge getDart() {
        return dart;
    }

    public void setDart(final HalfEdge dart) {
        this.dart = dart;
    }

    /**
     * Returns the outgoing darts of this vertex in clockwise order, starting with the HalfEdge that is stored with this vertex.
     * @return
     */
    public List<HalfEdge> getEdges() {
        return getEdges(dart);
    }

    /**
     * Returns the outgoing darts of this vertex in clockwise order, starting with the specified HalfEdge.
     * @param from
     * @return
     */
    public List<HalfEdge> getEdges(HalfEdge from) {
        List<HalfEdge> result = new ArrayList<HalfEdge>();

        HalfEdge e = from;

        do {
            result.add(e);
            e = e.getTwin().getNext();
        } while (e != from);

        return result;
    }

    public Set<Face> getFaces() {
        Set<Face> set = new HashSet<Face>();

        HalfEdge drt = dart;

        do {
            set.add(drt.getFace());
            drt = drt.getPrevious().getTwin();
        } while (drt != dart);

        return set;
    }

    public static final Comparator<EmbeddedVertex> increasingX = new Comparator<EmbeddedVertex>() {

        public int compare(EmbeddedVertex v1, EmbeddedVertex v2) {
            int compX = java.lang.Double.compare(v1.getX(), v2.getX());

            if (compX != 0) {
                return compX;
            } else {
                return java.lang.Double.compare(v1.getY(), v2.getY());
            }
        }
    };

    public static final Comparator<EmbeddedVertex> increasingY = new Comparator<EmbeddedVertex>() {

        public int compare(EmbeddedVertex v1, EmbeddedVertex v2) {
            int compY = java.lang.Double.compare(v1.getY(), v2.getY());

            if (compY != 0) {
                return compY;
            } else {
                return java.lang.Double.compare(v1.getX(), v2.getX());
            }
        }
    };

    @Override
    public String toString() {
        return "V[" + x + ", " + y + "]";
    }
}
