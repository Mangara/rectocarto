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
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.ipe.IPEExporter;

public class IPEConverter {

    public static void main(String[] args) throws IOException {
        // Convert the file specified by the first commandline argument into an IPE 6 file
        File file;
        File ipeFile;

        if (args.length == 1) {
            file = new File(args[0]);
            ipeFile = new File(args[0] + ".ipe");
        } else if (args.length == 2) {
            file = new File(args[0]);
            ipeFile = new File(args[1]);
        } else {
            throw new IllegalArgumentException("Either one or two arguments expected.");
        }

        Subdivision sub = loadSubdivision(file);

        IPEExporter ipe = new IPEExporter();
        ipe.exportIPEFile(ipeFile, sub, true);
    }

    private IPEConverter() {
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
}
