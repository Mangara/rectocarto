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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.exceptions.IncorrectGraphException;
import rectangularcartogram.data.graph.Graph;

public class LabelingTraverserTest {

    public static void main(String[] args) throws IOException {
        Graph graph = loadGraph(new File("3x3 Triangulated Grid.grp"));

        for (int i = 0; i < 10; i++) {
            try {
                long start = System.currentTimeMillis();
                LabelingCounter lc = new LabelingCounter(graph);
                lc.traverseLabelings();
                long duration = System.currentTimeMillis() - start;
                System.out.println(lc.getnLabelings() + " Labelings. Duration: " + duration);
            } catch (IncorrectGraphException ex) {
                Logger.getLogger(LabelingTraverserTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static Graph loadGraph(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            StringBuilder text = new StringBuilder();
            String line = in.readLine();

            while (line != null) {
                text.append(line);
                text.append("\n");

                line = in.readLine();
            }

            // Read the data
            return Graph.fromSaveString(text.toString());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
