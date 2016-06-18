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
package rectangularcartogram.exceptions;

import rectangularcartogram.data.graph.Edge;

public class IntersectingEdgesException extends IncorrectGraphException {
    private Edge edge1;
    private Edge edge2;

    public IntersectingEdgesException(Edge edge1, Edge edge2) {
        super("Intersecting edges!");
        this.edge1 = edge1;
        this.edge2 = edge2;
    }

    public Edge getEdge1() {
        return edge1;
    }

    public Edge getEdge2() {
        return edge2;
    }

    
}
