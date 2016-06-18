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
import java.util.List;

public class Face {

    private HalfEdge dart;
    private boolean outerFace;

    public Face() {
        this(false);
    }

    public Face(boolean outerFace) {
        this.outerFace = outerFace;
    }

    public HalfEdge getDart() {
        return dart;
    }

    public void setDart(final HalfEdge dart) {
        this.dart = dart;
    }

    public boolean isOuterFace() {
        return outerFace;
    }

    public void setOuterFace(boolean outerFace) {
        this.outerFace = outerFace;
    }

    public int getVertexCount() {
        int count = 1;
        HalfEdge walkDart = dart.getNext();

        while (walkDart != dart) {
            walkDart = walkDart.getNext();
            count++;
        }
        
        return count;
    }

    public ArrayList<EmbeddedVertex> getVertices() {
        ArrayList<EmbeddedVertex> result = new ArrayList<EmbeddedVertex>();
        HalfEdge walkDart = dart;

        do {
            result.add(walkDart.getOrigin());
            walkDart = walkDart.getNext();
        } while (walkDart != dart);

        return result;
    }

    public List<HalfEdge> getDarts() {
        List<HalfEdge> darts = new ArrayList<HalfEdge>();
        HalfEdge walkDart = dart;

        do {
            darts.add(walkDart);
            walkDart = walkDart.getNext();
        } while (walkDart != dart);
        
        return darts;
    }
}
