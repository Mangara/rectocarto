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
package rectangularcartogram.webimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import rectangularcartogram.data.subdivision.CompositeFace;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.gui.PropertyDialog;

public class CensusImporter {

    private static Map<Integer, String> dataSets;

    public static void importWeightsFromCensus(int dataSet, Subdivision subdivision) {
        Map<SubdivisionFace, Double> faceValues = getWeightsFromCensus(dataSet, subdivision);

        for (Map.Entry<SubdivisionFace, Double> entry : faceValues.entrySet()) {
            if (entry.getValue() <= 0) {
                System.err.println("Region \"" + entry.getKey().getName() + "\" has weight " + entry.getValue() + " - impossible to construct a cartogram.");
                System.err.flush();
                throw new IllegalArgumentException("Region \"" + entry.getKey().getName() + "\" has weight " + entry.getValue() + " - impossible to construct a cartogram.");
            } else {
                entry.getKey().setWeight(entry.getValue());
            }
        }

        subdivision.updateCompositeFaces();
    }

    public static Map<SubdivisionFace, Double> getWeightsFromCensus(int dataSet, Subdivision subdivision) {
        HashMap<Integer, Double> values = new HashMap<Integer, Double>();

        try {
            // Fetch http://quickfacts.census.gov/qfd/download/DataSet.txt
            URL url = new URL("http://quickfacts.census.gov/qfd/download/DataSet.txt");
            URLConnection con = url.openConnection();
            con.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;

            // Skip the first line
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                // store the information as code - value pairs
                String[] parts = line.split(",");

                // parts[0] is the region code, parts[dataSet] is the value
                int code = Integer.parseInt(parts[0]);

                // parse the value to a double
                double value = Double.parseDouble(parts[dataSet]);

                values.put(code, value);
            }

            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(PropertyDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        HashMap<SubdivisionFace, Double> faceValues = new HashMap<SubdivisionFace, Double>();

        if (!values.isEmpty()) {
            HashMap<String, Integer> states = RegionNameDictionary.getCensusStateCodes();

            // Assign the values to the regions of the subdivision
            for (SubdivisionFace f : subdivision.getFaces()) {
                if (!f.isSea() && !(f instanceof CompositeFace)) {
                    if (states.containsKey(f.getName()) && values.containsKey(states.get(f.getName()))) {
                        faceValues.put(f, values.get(states.get(f.getName())));
                    } else {
                        System.err.println("No value found for region \"" + f.getName() + "\"");
                        System.err.flush();
                        throw new IllegalArgumentException("Given data set does not have a value for each region in the subdivision.");
                    }
                }
            }
        }

        return faceValues;
    }

    public static Map<Integer, String> getDataSets() {
        if (dataSets == null) {
            dataSets = new LinkedHashMap<Integer, String>();

            try {
                // Fetch and parse from http://quickfacts.census.gov/qfd/download/DataDict.txt
                URL url = new URL("http://quickfacts.census.gov/qfd/download/DataDict.txt");
                URLConnection con = url.openConnection();
                con.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                int i = 1;

                // Skip first two lines
                reader.readLine();
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    dataSets.put(i, line.substring(10, 115).trim());
                    i++;
                }

                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(PropertyDialog.class.getName()).log(Level.SEVERE, null, ex);
                dataSets = null;
            }
        }

        return dataSets;
    }
}
