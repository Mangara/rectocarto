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
package rectangularcartogram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import rectangularcartogram.algos.LabelingCounter;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.exceptions.IncorrectGraphException;

public class Counter {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, IncorrectGraphException {
        //Subdivision sub = loadSubdivision(new File(args[0]));
        //LabelingCounter counter = new LabelingCounter(sub.getDualGraph());
        //LabelingCounter counter = new LabelingCounter(loadGraph(new File("3x4 Triangulated Grid.grp")));
        LabelingCounter counter = new LabelingCounter(loadSubdivision(new File("World.sub")).getDualGraph());

        System.out.println("Started " + new Date());

        long start = System.currentTimeMillis();

        counter.countLabelings();

        long end = System.currentTimeMillis();

        System.out.println("Total number of labelings: " + counter.getnLabelings());
        System.out.println("Counted for " + (end - start) + " milliseconds");
        System.out.println("Finished " + new Date());
    }
    
    private static Subdivision loadSubdivision(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Subdivision.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static Graph loadGraph(File file) throws IOException {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));

            return Graph.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
