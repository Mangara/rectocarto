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
import javax.net.ssl.SSLHandshakeException;
import rectangularcartogram.data.subdivision.CompositeFace;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.gui.PropertyDialog;

public class WFBImporter {

    private static Map<Integer, String> dataSets;
    private static final int MAX_TRIES = 10; // Maximum number of connection attempts

    public static void importWeightsFromWFB(int dataSet, Subdivision subdivision) {
        Map<SubdivisionFace, Double> faceValues = null;
        int n = 0;

        while (n < MAX_TRIES && (faceValues == null || faceValues.isEmpty())) {
            try {
                faceValues = getWeightsFromWFB(dataSet, subdivision);
            } catch (SSLHandshakeException e) {
            }

            n++;
        }

        if (faceValues != null && !faceValues.isEmpty()) {
            System.out.println("Import succesful.");

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
        } else {
            System.out.println("Import failed.");
        }
    }

    public static Map<SubdivisionFace, Double> getWeightsFromWFB(int dataSet, Subdivision subdivision) throws SSLHandshakeException {
        HashMap<String, String> alpha = RegionNameDictionary.getISOAlpha2And3();

        HashMap<String, Double> values = new HashMap<String, Double>();

        try {
            // Fetch https://www.cia.gov/library/publications/the-world-factbook/rankorder/rawdata_<number>.txt
            URL url = new URL("https://www.cia.gov/library/publications/the-world-factbook/rankorder/rawdata_" + dataSet + ".txt");
            URLConnection con = url.openConnection();
            con.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                // store the information as name - value pairs
                String[] parts = line.split("\t");

                // parts[0] is the rank, parts[1] is the country name, parts[2] is the value
                String name = parts[1];
                String val = parts[2].replaceAll("[^0-9\\.]", "");

                // parse the value to a double
                double value = Double.parseDouble(val);

                values.put(name, value);
            }

            reader.close();
        } catch (SSLHandshakeException sslex) {
            throw sslex;
        } catch (IOException ex) {
            Logger.getLogger(PropertyDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        HashMap<SubdivisionFace, Double> faceValues = new HashMap<SubdivisionFace, Double>();

        if (!values.isEmpty()) {
            // Assign the values to the regions of the subdivision
            for (SubdivisionFace f : subdivision.getFaces()) {
                if (!f.isSea() && !(f instanceof CompositeFace)) {
                    if (values.containsKey(f.getName())) {
                        faceValues.put(f, values.get(f.getName()));
                    } else if (alpha.containsKey(f.getName()) && values.containsKey(alpha.get(f.getName()))) {
                        faceValues.put(f, values.get(alpha.get(f.getName())));
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

            dataSets.put(2147, "Area");
            dataSets.put(2119, "Population");
            dataSets.put(2002, "Population growth");
            dataSets.put(2054, "Birth rate");
            dataSets.put(2066, "Death rate");
            dataSets.put(2112, "Net migration rate");
            dataSets.put(2223, "Maternal mortality rate");
            dataSets.put(2091, "Infant mortality rate");
            dataSets.put(2102, "Life expectancy at birth");
            dataSets.put(2127, "Total fertility rate");
            dataSets.put(2228, "Obesity - adult prevalence rate");
            dataSets.put(2224, "Children under the age of 5 underweight");
            dataSets.put(2155, "HIV/AIDS - adult prevalence rate");
            dataSets.put(2156, "HIV/AIDS - people living with HIV/AIDS");
            dataSets.put(2157, "HIV/AIDS - deaths");
            dataSets.put(2225, "Health expenditures");
            dataSets.put(2206, "Education expenditures");
            dataSets.put(2001, "GDP (purchasing power parity)");
            dataSets.put(2003, "GDP real growth rate");
            dataSets.put(2004, "GDP - per capita (PPP)");
            dataSets.put(2095, "Labor force");
            dataSets.put(2129, "Unemployment rate");
            dataSets.put(2229, "Unemployment youth ages 15-24");
            dataSets.put(2172, "Distribution of family income - Gini Index");
            dataSets.put(2185, "Investment (gross fixed)");
            dataSets.put(2186, "Public debt");
            dataSets.put(2092, "Inflation rate (consumer prices)");
            dataSets.put(2207, "Central bank discount rate");
            dataSets.put(2208, "Commercial bank prime lending rate");
            dataSets.put(2209, "Stock of money");
            dataSets.put(2210, "Stock of quasi money");
            dataSets.put(2211, "Stock of domestic credit");
            dataSets.put(2200, "Market value of publicly traded shares");
            dataSets.put(2089, "Industrial production growth rate");
            dataSets.put(2038, "Electricity - production");
            dataSets.put(2042, "Electricity - consumption");
            dataSets.put(2173, "Oil - production");
            dataSets.put(2174, "Oil - consumption");
            dataSets.put(2176, "Oil - exports");
            dataSets.put(2175, "Oil - imports");
            dataSets.put(2178, "Oil - proved reserves");
            dataSets.put(2180, "Natural gas - production");
            dataSets.put(2181, "Natural gas - consumption");
            dataSets.put(2183, "Natural gas - exports");
            dataSets.put(2182, "Natural gas - imports");
            dataSets.put(2179, "Natural gas - proved reserves");
            dataSets.put(2187, "Current account balance");
            dataSets.put(2078, "Exports");
            dataSets.put(2087, "Imports");
            dataSets.put(2188, "Reserves of foreign exchange and gold");
            dataSets.put(2079, "Debt - external");
            dataSets.put(2198, "Stock of direct foreign investment - at home");
            dataSets.put(2199, "Stock of direct foreign investment - abroad");
            dataSets.put(2150, "Telephones - main lines in use");
            dataSets.put(2151, "Telephones - mobile cellular");
            dataSets.put(2184, "Internet hosts");
            dataSets.put(2153, "Internet users");
            dataSets.put(2053, "Airports");
            dataSets.put(2121, "Railways");
            dataSets.put(2085, "Roadways");
            dataSets.put(2093, "Waterways");
            dataSets.put(2108, "Merchant marine");
            dataSets.put(2034, "Military expenditures - percent of GDP");
        }

        return dataSets;
    }
}
