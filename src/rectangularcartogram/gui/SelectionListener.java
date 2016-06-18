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
package rectangularcartogram.gui;

import javax.swing.JPanel;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.SubdivisionFace;

public interface SelectionListener {

    public void edgeSelected(JPanel source, Edge edge);
    public void vertexSelected(JPanel source, Vertex vertex);
    public void faceSelected(SubdivisionDrawPanel source, SubdivisionFace face);

}
