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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegionNameDictionary {

    private static HashMap<String, String> ISOAlpha2To3;
    private static HashMap<String, String> ISOAlpha2And3;
    private static HashMap<String, String> FIPStoAlpha3;
    private static HashMap<String, Integer> censusStateCodes;

    public static void main(String[] args) throws IOException {
        /*getISOAlpha2And3();

        File file = new File("worldmapper_territory_codes.csv");
        BufferedReader in = null;

        try {
        in = new BufferedReader(new FileReader(file));
        List<String> notFound = new ArrayList<String>();

        String line = in.readLine();

        while (line != null) {
        String[] parts = line.split(",");

        String fipsCode = parts[parts.length - 2];
        String motherCountry = parts[parts.length - 1];
        // Strip off quotation marks
        fipsCode = fipsCode.substring(1, fipsCode.length() - 1);
        motherCountry = motherCountry.substring(1, motherCountry.length() - 1);

        boolean found = false;

        for (String code : ISOAlpha2And3.keySet()) {
        if (code.length() == 3 && ISOAlpha2And3.get(code).equals(motherCountry)) {
        System.out.println("FIPStoAlpha3.put(\"" + fipsCode + "\", \"" + code + "\");");
        found = true;
        break;
        }
        }

        if (!found) {
        notFound.add(line);
        }

        line = in.readLine();
        }

        for (String string : notFound) {
        System.out.println("No matching found: " + string);
        }
        } finally {
        if (in != null) {
        in.close();
        }
        }*/
        getFIPStoAlpha3();

        File descriptionFile = new File("worldmapper_colour_codes_to_send.csv");
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(descriptionFile));
            List<String> notFound = new ArrayList<String>();

            String line = in.readLine();

            while (line != null) {
                String code2 = line.substring(0, 2);

                if (FIPStoAlpha3.containsKey(code2)) {
                    System.out.println(FIPStoAlpha3.get(code2) + line.substring(2));
                } else {
                    notFound.add(line);
                }

                line = in.readLine();
            }

            System.out.println("No corresponding codes found for:");

            for (String code : notFound) {
                System.out.println(code);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * A mapping from the abbreviations in the FIPS 10-4 standard to the ISO 3166-1 alpha-3 standard
     * @return
     */
    public static HashMap<String, String> getFIPStoAlpha3() {
        if (FIPStoAlpha3 == null) {
            FIPStoAlpha3 = new HashMap<String, String>(600);

            FIPStoAlpha3.put("AE", "ARE");
            FIPStoAlpha3.put("AF", "AFG");
            FIPStoAlpha3.put("AG", "DZA");
            FIPStoAlpha3.put("AJ", "AZE");
            FIPStoAlpha3.put("AL", "ALB");
            FIPStoAlpha3.put("AM", "ARM");
            FIPStoAlpha3.put("AN", "AND");
            FIPStoAlpha3.put("AO", "AGO");
            FIPStoAlpha3.put("AR", "ARG");
            FIPStoAlpha3.put("AS", "AUS");
            FIPStoAlpha3.put("AU", "AUT");
            FIPStoAlpha3.put("BA", "BHR");
            FIPStoAlpha3.put("BB", "BRB");
            FIPStoAlpha3.put("BC", "BWA");
            FIPStoAlpha3.put("BE", "BEL");
            FIPStoAlpha3.put("BF", "BHS");
            FIPStoAlpha3.put("BG", "BGD");
            FIPStoAlpha3.put("BH", "BLZ");
            FIPStoAlpha3.put("BL", "BOL");
            FIPStoAlpha3.put("BN", "BEN");
            FIPStoAlpha3.put("BO", "BLR");
            FIPStoAlpha3.put("BP", "SLB");
            FIPStoAlpha3.put("BR", "BRA");
            FIPStoAlpha3.put("BT", "BTN");
            FIPStoAlpha3.put("BU", "BGR");
            FIPStoAlpha3.put("BX", "BRN");
            FIPStoAlpha3.put("BY", "BDI");
            FIPStoAlpha3.put("CA", "CAN");
            FIPStoAlpha3.put("CB", "KHM");
            FIPStoAlpha3.put("CD", "TCD");
            FIPStoAlpha3.put("CE", "LKA");
            FIPStoAlpha3.put("CH", "CHN");
            FIPStoAlpha3.put("CI", "CHL");
            FIPStoAlpha3.put("CM", "CMR");
            FIPStoAlpha3.put("CN", "COM");
            FIPStoAlpha3.put("CO", "COL");
            FIPStoAlpha3.put("CS", "CRI");
            FIPStoAlpha3.put("CT", "CAF");
            FIPStoAlpha3.put("CU", "CUB");
            FIPStoAlpha3.put("CV", "CPV");
            FIPStoAlpha3.put("CW", "COK");
            FIPStoAlpha3.put("CY", "CYP");
            FIPStoAlpha3.put("DA", "DNK");
            FIPStoAlpha3.put("DJ", "DJI");
            FIPStoAlpha3.put("DO", "DMA");
            FIPStoAlpha3.put("DR", "DOM");
            FIPStoAlpha3.put("EC", "ECU");
            FIPStoAlpha3.put("EG", "EGY");
            FIPStoAlpha3.put("EI", "IRL");
            FIPStoAlpha3.put("EK", "GNQ");
            FIPStoAlpha3.put("EN", "EST");
            FIPStoAlpha3.put("ER", "ERI");
            FIPStoAlpha3.put("ES", "SLV");
            FIPStoAlpha3.put("ET", "ETH");
            FIPStoAlpha3.put("EZ", "CZE");
            FIPStoAlpha3.put("FI", "FIN");
            FIPStoAlpha3.put("FJ", "FJI");
            FIPStoAlpha3.put("FR", "FRA");
            FIPStoAlpha3.put("GB", "GAB");
            FIPStoAlpha3.put("GG", "GEO");
            FIPStoAlpha3.put("GH", "GHA");
            FIPStoAlpha3.put("GJ", "GRD");
            FIPStoAlpha3.put("GL", "GRL");
            FIPStoAlpha3.put("GM", "DEU");
            FIPStoAlpha3.put("GR", "GRC");
            FIPStoAlpha3.put("GT", "GTM");
            FIPStoAlpha3.put("GV", "GIN");
            FIPStoAlpha3.put("GY", "GUY");
            FIPStoAlpha3.put("HA", "HTI");
            FIPStoAlpha3.put("HO", "HND");
            FIPStoAlpha3.put("HR", "HRV");
            FIPStoAlpha3.put("HU", "HUN");
            FIPStoAlpha3.put("IC", "ISL");
            FIPStoAlpha3.put("ID", "IDN");
            FIPStoAlpha3.put("IN", "IND");
            FIPStoAlpha3.put("IS", "ISR");
            FIPStoAlpha3.put("IT", "ITA");
            FIPStoAlpha3.put("IV", "CIV");
            FIPStoAlpha3.put("IZ", "IRQ");
            FIPStoAlpha3.put("JA", "JPN");
            FIPStoAlpha3.put("JM", "JAM");
            FIPStoAlpha3.put("JO", "JOR");
            FIPStoAlpha3.put("KE", "KEN");
            FIPStoAlpha3.put("KG", "KGZ");
            FIPStoAlpha3.put("KR", "KIR");
            FIPStoAlpha3.put("KU", "KWT");
            FIPStoAlpha3.put("KZ", "KAZ");
            FIPStoAlpha3.put("LE", "LBN");
            FIPStoAlpha3.put("LG", "LVA");
            FIPStoAlpha3.put("LH", "LTU");
            FIPStoAlpha3.put("LI", "LBR");
            FIPStoAlpha3.put("LO", "SVK");
            FIPStoAlpha3.put("LS", "LIE");
            FIPStoAlpha3.put("LT", "LSO");
            FIPStoAlpha3.put("LU", "LUX");
            FIPStoAlpha3.put("MA", "MDG");
            FIPStoAlpha3.put("MG", "MNG");
            FIPStoAlpha3.put("MI", "MWI");
            FIPStoAlpha3.put("ML", "MLI");
            FIPStoAlpha3.put("MN", "MCO");
            FIPStoAlpha3.put("MO", "MAR");
            FIPStoAlpha3.put("MP", "MUS");
            FIPStoAlpha3.put("MR", "MRT");
            FIPStoAlpha3.put("MT", "MLT");
            FIPStoAlpha3.put("MU", "OMN");
            FIPStoAlpha3.put("MV", "MDV");
            FIPStoAlpha3.put("MX", "MEX");
            FIPStoAlpha3.put("MY", "MYS");
            FIPStoAlpha3.put("MZ", "MOZ");
            FIPStoAlpha3.put("NE", "NIU");
            FIPStoAlpha3.put("NG", "NER");
            FIPStoAlpha3.put("NH", "VUT");
            FIPStoAlpha3.put("NI", "NGA");
            FIPStoAlpha3.put("NL", "NLD");
            FIPStoAlpha3.put("NO", "NOR");
            FIPStoAlpha3.put("NP", "NPL");
            FIPStoAlpha3.put("NR", "NRU");
            FIPStoAlpha3.put("NS", "SUR");
            FIPStoAlpha3.put("NU", "NIC");
            FIPStoAlpha3.put("NZ", "NZL");
            FIPStoAlpha3.put("PA", "PRY");
            FIPStoAlpha3.put("PE", "PER");
            FIPStoAlpha3.put("PK", "PAK");
            FIPStoAlpha3.put("PL", "POL");
            FIPStoAlpha3.put("PM", "PAN");
            FIPStoAlpha3.put("PO", "PRT");
            FIPStoAlpha3.put("PP", "PNG");
            FIPStoAlpha3.put("PS", "PLW");
            FIPStoAlpha3.put("PU", "GNB");
            FIPStoAlpha3.put("QA", "QAT");
            FIPStoAlpha3.put("RM", "MHL");
            FIPStoAlpha3.put("RO", "ROU");
            FIPStoAlpha3.put("RP", "PHL");
            FIPStoAlpha3.put("RQ", "PRI");
            FIPStoAlpha3.put("RW", "RWA");
            FIPStoAlpha3.put("SA", "SAU");
            FIPStoAlpha3.put("SE", "SYC");
            FIPStoAlpha3.put("SF", "ZAF");
            FIPStoAlpha3.put("SG", "SEN");
            FIPStoAlpha3.put("SI", "SVN");
            FIPStoAlpha3.put("SL", "SLE");
            FIPStoAlpha3.put("SM", "SMR");
            FIPStoAlpha3.put("SN", "SGP");
            FIPStoAlpha3.put("SO", "SOM");
            FIPStoAlpha3.put("SP", "ESP");
            FIPStoAlpha3.put("ST", "LCA");
            FIPStoAlpha3.put("SU", "SDN");
            FIPStoAlpha3.put("SW", "SWE");
            FIPStoAlpha3.put("SZ", "CHE");
            FIPStoAlpha3.put("TH", "THA");
            FIPStoAlpha3.put("TI", "TJK");
            FIPStoAlpha3.put("TN", "TON");
            FIPStoAlpha3.put("TO", "TGO");
            FIPStoAlpha3.put("TS", "TUN");
            FIPStoAlpha3.put("TU", "TUR");
            FIPStoAlpha3.put("TV", "TUV");
            FIPStoAlpha3.put("TX", "TKM");
            FIPStoAlpha3.put("UG", "UGA");
            FIPStoAlpha3.put("UK", "GBR");
            FIPStoAlpha3.put("UP", "UKR");
            FIPStoAlpha3.put("US", "USA");
            FIPStoAlpha3.put("UV", "BFA");
            FIPStoAlpha3.put("UY", "URY");
            FIPStoAlpha3.put("UZ", "UZB");
            FIPStoAlpha3.put("VE", "VEN");
            FIPStoAlpha3.put("WA", "NAM");
            FIPStoAlpha3.put("WI", "ESH");
            FIPStoAlpha3.put("WS", "WSM");
            FIPStoAlpha3.put("WZ", "SWZ");
            FIPStoAlpha3.put("YM", "YEM");
            FIPStoAlpha3.put("ZA", "ZMB");
            FIPStoAlpha3.put("ZI", "ZWE");
            FIPStoAlpha3.put("TT", "TLS");

            FIPStoAlpha3.put("AC", "ATG");
            FIPStoAlpha3.put("BK", "BIH");
            FIPStoAlpha3.put("BM", "MMR");
            FIPStoAlpha3.put("CF", "COG");
            FIPStoAlpha3.put("CG", "COD");
            FIPStoAlpha3.put("FM", "FSM");
            FIPStoAlpha3.put("GA", "GMB");
            FIPStoAlpha3.put("IR", "IRN");
            FIPStoAlpha3.put("KN", "PRK");
            FIPStoAlpha3.put("KS", "KOR");
            FIPStoAlpha3.put("LA", "LAO");
            FIPStoAlpha3.put("LY", "LBY");
            FIPStoAlpha3.put("MD", "MDA");
            FIPStoAlpha3.put("MK", "MKD");
            FIPStoAlpha3.put("MW", "MNE");
            FIPStoAlpha3.put("RS", "RUS");
            FIPStoAlpha3.put("SR", "SRB");
            FIPStoAlpha3.put("SY", "SYR");
            FIPStoAlpha3.put("TD", "TTO");
            FIPStoAlpha3.put("TW", "TWN");
            FIPStoAlpha3.put("TZ", "TZA");
            FIPStoAlpha3.put("VM", "VNM");
        }

        return FIPStoAlpha3;
    }

    /**
     * A mapping from the abbreviations in the ISO 3166-1 alpha-2 standard to the alpha-3 standard
     * @return
     */
    public static HashMap<String, String> getISOAlpha2To3() {
        if (ISOAlpha2To3 == null) {
            ISOAlpha2To3 = new HashMap<String, String>(600);

            ISOAlpha2To3.put("EC", "ECU");
            ISOAlpha2To3.put("DZ", "DZA");
            ISOAlpha2To3.put("DM", "DMA");
            ISOAlpha2To3.put("DO", "DOM");
            ISOAlpha2To3.put("DE", "DEU");
            ISOAlpha2To3.put("DK", "DNK");
            ISOAlpha2To3.put("DJ", "DJI");
            ISOAlpha2To3.put("ET", "ETH");
            ISOAlpha2To3.put("ES", "ESP");
            ISOAlpha2To3.put("ER", "ERI");
            ISOAlpha2To3.put("EH", "ESH");
            ISOAlpha2To3.put("EG", "EGY");
            ISOAlpha2To3.put("EE", "EST");
            ISOAlpha2To3.put("GD", "GRD");
            ISOAlpha2To3.put("GE", "GEO");
            ISOAlpha2To3.put("GF", "GUF");
            ISOAlpha2To3.put("GA", "GAB");
            ISOAlpha2To3.put("GB", "GBR");
            ISOAlpha2To3.put("FR", "FRA");
            ISOAlpha2To3.put("FO", "FRO");
            ISOAlpha2To3.put("FK", "FLK");
            ISOAlpha2To3.put("FJ", "FJI");
            ISOAlpha2To3.put("FM", "FSM");
            ISOAlpha2To3.put("FI", "FIN");
            ISOAlpha2To3.put("GY", "GUY");
            ISOAlpha2To3.put("GW", "GNB");
            ISOAlpha2To3.put("GU", "GUM");
            ISOAlpha2To3.put("GT", "GTM");
            ISOAlpha2To3.put("GS", "SGS");
            ISOAlpha2To3.put("GR", "GRC");
            ISOAlpha2To3.put("GQ", "GNQ");
            ISOAlpha2To3.put("GP", "GLP");
            ISOAlpha2To3.put("GN", "GIN");
            ISOAlpha2To3.put("GL", "GRL");
            ISOAlpha2To3.put("GI", "GIB");
            ISOAlpha2To3.put("GH", "GHA");
            ISOAlpha2To3.put("GG", "GGY");
            ISOAlpha2To3.put("AT", "AUT");
            ISOAlpha2To3.put("AS", "ASM");
            ISOAlpha2To3.put("AR", "ARG");
            ISOAlpha2To3.put("AQ", "ATA");
            ISOAlpha2To3.put("AX", "ALA");
            ISOAlpha2To3.put("AW", "ABW");
            ISOAlpha2To3.put("AU", "AUS");
            ISOAlpha2To3.put("AZ", "AZE");
            ISOAlpha2To3.put("BA", "BIH");
            ISOAlpha2To3.put("AD", "AND");
            ISOAlpha2To3.put("AG", "ATG");
            ISOAlpha2To3.put("AE", "ARE");
            ISOAlpha2To3.put("AF", "AFG");
            ISOAlpha2To3.put("AL", "ALB");
            ISOAlpha2To3.put("AI", "AIA");
            ISOAlpha2To3.put("AO", "AGO");
            ISOAlpha2To3.put("AM", "ARM");
            ISOAlpha2To3.put("BW", "BWA");
            ISOAlpha2To3.put("BV", "BVT");
            ISOAlpha2To3.put("BY", "BLR");
            ISOAlpha2To3.put("BS", "BHS");
            ISOAlpha2To3.put("BR", "BRA");
            ISOAlpha2To3.put("BT", "BTN");
            ISOAlpha2To3.put("CA", "CAN");
            ISOAlpha2To3.put("BZ", "BLZ");
            ISOAlpha2To3.put("BF", "BFA");
            ISOAlpha2To3.put("BG", "BGR");
            ISOAlpha2To3.put("BH", "BHR");
            ISOAlpha2To3.put("BI", "BDI");
            ISOAlpha2To3.put("BB", "BRB");
            ISOAlpha2To3.put("BD", "BGD");
            ISOAlpha2To3.put("BE", "BEL");
            ISOAlpha2To3.put("BN", "BRN");
            ISOAlpha2To3.put("BQ", "BES");
            ISOAlpha2To3.put("BJ", "BEN");
            ISOAlpha2To3.put("BL", "BLM");
            ISOAlpha2To3.put("BM", "BMU");
            ISOAlpha2To3.put("CZ", "CZE");
            ISOAlpha2To3.put("CY", "CYP");
            ISOAlpha2To3.put("CX", "CXR");
            ISOAlpha2To3.put("CW", "CUW");
            ISOAlpha2To3.put("CV", "CPV");
            ISOAlpha2To3.put("CU", "CUB");
            ISOAlpha2To3.put("CH", "CHE");
            ISOAlpha2To3.put("CF", "CAF");
            ISOAlpha2To3.put("CC", "CCK");
            ISOAlpha2To3.put("CR", "CRI");
            ISOAlpha2To3.put("CO", "COL");
            ISOAlpha2To3.put("CM", "CMR");
            ISOAlpha2To3.put("CN", "CHN");
            ISOAlpha2To3.put("CK", "COK");
            ISOAlpha2To3.put("CL", "CHL");
            ISOAlpha2To3.put("LV", "LVA");
            ISOAlpha2To3.put("LU", "LUX");
            ISOAlpha2To3.put("LT", "LTU");
            ISOAlpha2To3.put("LS", "LSO");
            ISOAlpha2To3.put("LR", "LBR");
            ISOAlpha2To3.put("MG", "MDG");
            ISOAlpha2To3.put("MH", "MHL");
            ISOAlpha2To3.put("ME", "MNE");
            ISOAlpha2To3.put("MF", "MAF");
            ISOAlpha2To3.put("MK", "MKD");
            ISOAlpha2To3.put("ML", "MLI");
            ISOAlpha2To3.put("MC", "MCO");
            ISOAlpha2To3.put("MD", "MDA");
            ISOAlpha2To3.put("MA", "MAR");
            ISOAlpha2To3.put("MV", "MDV");
            ISOAlpha2To3.put("MU", "MUS");
            ISOAlpha2To3.put("MX", "MEX");
            ISOAlpha2To3.put("MW", "MWI");
            ISOAlpha2To3.put("MZ", "MOZ");
            ISOAlpha2To3.put("MY", "MYS");
            ISOAlpha2To3.put("MN", "MNG");
            ISOAlpha2To3.put("MP", "MNP");
            ISOAlpha2To3.put("MO", "MAC");
            ISOAlpha2To3.put("MR", "MRT");
            ISOAlpha2To3.put("MQ", "MTQ");
            ISOAlpha2To3.put("MT", "MLT");
            ISOAlpha2To3.put("MS", "MSR");
            ISOAlpha2To3.put("NF", "NFK");
            ISOAlpha2To3.put("NG", "NGA");
            ISOAlpha2To3.put("NI", "NIC");
            ISOAlpha2To3.put("NL", "NLD");
            ISOAlpha2To3.put("NA", "NAM");
            ISOAlpha2To3.put("NC", "NCL");
            ISOAlpha2To3.put("NE", "NER");
            ISOAlpha2To3.put("NZ", "NZL");
            ISOAlpha2To3.put("NU", "NIU");
            ISOAlpha2To3.put("NR", "NRU");
            ISOAlpha2To3.put("NP", "NPL");
            ISOAlpha2To3.put("NO", "NOR");
            ISOAlpha2To3.put("OM", "OMN");
            ISOAlpha2To3.put("PL", "POL");
            ISOAlpha2To3.put("PM", "SPM");
            ISOAlpha2To3.put("PN", "PCN");
            ISOAlpha2To3.put("PH", "PHL");
            ISOAlpha2To3.put("PK", "PAK");
            ISOAlpha2To3.put("PE", "PER");
            ISOAlpha2To3.put("PF", "PYF");
            ISOAlpha2To3.put("PG", "PNG");
            ISOAlpha2To3.put("PA", "PAN");
            ISOAlpha2To3.put("HK", "HKG");
            ISOAlpha2To3.put("HN", "HND");
            ISOAlpha2To3.put("HM", "HMD");
            ISOAlpha2To3.put("HR", "HRV");
            ISOAlpha2To3.put("HT", "HTI");
            ISOAlpha2To3.put("HU", "HUN");
            ISOAlpha2To3.put("ID", "IDN");
            ISOAlpha2To3.put("IE", "IRL");
            ISOAlpha2To3.put("IL", "ISR");
            ISOAlpha2To3.put("IM", "IMN");
            ISOAlpha2To3.put("IN", "IND");
            ISOAlpha2To3.put("IO", "IOT");
            ISOAlpha2To3.put("IQ", "IRQ");
            ISOAlpha2To3.put("IS", "ISL");
            ISOAlpha2To3.put("IT", "ITA");
            ISOAlpha2To3.put("JE", "JEY");
            ISOAlpha2To3.put("JP", "JPN");
            ISOAlpha2To3.put("JO", "JOR");
            ISOAlpha2To3.put("JM", "JAM");
            ISOAlpha2To3.put("KI", "KIR");
            ISOAlpha2To3.put("KH", "KHM");
            ISOAlpha2To3.put("KG", "KGZ");
            ISOAlpha2To3.put("KE", "KEN");
            ISOAlpha2To3.put("KO", "KOS");
            ISOAlpha2To3.put("KM", "COM");
            ISOAlpha2To3.put("KN", "KNA");
            ISOAlpha2To3.put("KW", "KWT");
            ISOAlpha2To3.put("KY", "CYM");
            ISOAlpha2To3.put("KZ", "KAZ");
            ISOAlpha2To3.put("LC", "LCA");
            ISOAlpha2To3.put("LB", "LBN");
            ISOAlpha2To3.put("LI", "LIE");
            ISOAlpha2To3.put("LK", "LKA");
            ISOAlpha2To3.put("VU", "VUT");
            ISOAlpha2To3.put("VI", "VIR");
            ISOAlpha2To3.put("VG", "VGB");
            ISOAlpha2To3.put("VC", "VCT");
            ISOAlpha2To3.put("VA", "VAT");
            ISOAlpha2To3.put("UZ", "UZB");
            ISOAlpha2To3.put("UY", "URY");
            ISOAlpha2To3.put("US", "USA");
            ISOAlpha2To3.put("UM", "UMI");
            ISOAlpha2To3.put("UK", "GBR");
            ISOAlpha2To3.put("UG", "UGA");
            ISOAlpha2To3.put("UA", "UKR");
            ISOAlpha2To3.put("TT", "TTO");
            ISOAlpha2To3.put("TW", "TWN");
            ISOAlpha2To3.put("TV", "TUV");
            ISOAlpha2To3.put("WS", "WSM");
            ISOAlpha2To3.put("WF", "WLF");
            ISOAlpha2To3.put("RE", "REU");
            ISOAlpha2To3.put("RO", "ROU");
            ISOAlpha2To3.put("QA", "QAT");
            ISOAlpha2To3.put("PT", "PRT");
            ISOAlpha2To3.put("PW", "PLW");
            ISOAlpha2To3.put("PR", "PRI");
            ISOAlpha2To3.put("PS", "PSE");
            ISOAlpha2To3.put("PY", "PRY");
            ISOAlpha2To3.put("TG", "TGO");
            ISOAlpha2To3.put("TF", "ATF");
            ISOAlpha2To3.put("TD", "TCD");
            ISOAlpha2To3.put("TK", "TKL");
            ISOAlpha2To3.put("TJ", "TJK");
            ISOAlpha2To3.put("TH", "THA");
            ISOAlpha2To3.put("TO", "TON");
            ISOAlpha2To3.put("TN", "TUN");
            ISOAlpha2To3.put("TM", "TKM");
            ISOAlpha2To3.put("TL", "TLS");
            ISOAlpha2To3.put("TR", "TUR");
            ISOAlpha2To3.put("SV", "SLV");
            ISOAlpha2To3.put("SS", "SSD");
            ISOAlpha2To3.put("ST", "STP");
            ISOAlpha2To3.put("SZ", "SWZ");
            ISOAlpha2To3.put("SX", "SXM");
            ISOAlpha2To3.put("TC", "TCA");
            ISOAlpha2To3.put("SD", "SDN");
            ISOAlpha2To3.put("SC", "SYC");
            ISOAlpha2To3.put("SE", "SWE");
            ISOAlpha2To3.put("SH", "SHN");
            ISOAlpha2To3.put("SG", "SGP");
            ISOAlpha2To3.put("SJ", "SJM");
            ISOAlpha2To3.put("SI", "SVN");
            ISOAlpha2To3.put("SL", "SLE");
            ISOAlpha2To3.put("SK", "SVK");
            ISOAlpha2To3.put("SN", "SEN");
            ISOAlpha2To3.put("SM", "SMR");
            ISOAlpha2To3.put("SO", "SOM");
            ISOAlpha2To3.put("SR", "SUR");
            ISOAlpha2To3.put("RS", "SRB");
            ISOAlpha2To3.put("RU", "RUS");
            ISOAlpha2To3.put("RW", "RWA");
            ISOAlpha2To3.put("SA", "SAU");
            ISOAlpha2To3.put("SB", "SLB");
            ISOAlpha2To3.put("ZA", "ZAF");
            ISOAlpha2To3.put("ZM", "ZMB");
            ISOAlpha2To3.put("ZW", "ZWE");
            ISOAlpha2To3.put("YE", "YEM");
            ISOAlpha2To3.put("YT", "MYT");
        }

        return ISOAlpha2To3;
    }

    /**
     * A mapping from the abbreviations in the ISO 3166-1 alpha-2 and alpha-3 standards to the names used by the CIA World Fact Book
     * @return
     */
    public static HashMap<String, String> getISOAlpha2And3() {
        if (ISOAlpha2And3 == null) {
            ISOAlpha2And3 = new HashMap<String, String>(600);

            // ISO 3166-1 alpha-2
            ISOAlpha2And3.put("AD", "Andorra");
            ISOAlpha2And3.put("AE", "United Arab Emirates");
            ISOAlpha2And3.put("AF", "Afghanistan");
            ISOAlpha2And3.put("AG", "Antigua and Barbuda");
            ISOAlpha2And3.put("AI", "Anguilla");
            ISOAlpha2And3.put("AL", "Albania");
            ISOAlpha2And3.put("AM", "Armenia");
            ISOAlpha2And3.put("AO", "Angola");
            ISOAlpha2And3.put("AQ", "Antarctica");
            ISOAlpha2And3.put("AR", "Argentina");
            ISOAlpha2And3.put("AS", "American Samoa");
            ISOAlpha2And3.put("AT", "Austria");
            ISOAlpha2And3.put("AU", "Australia");
            ISOAlpha2And3.put("AW", "Aruba");
            ISOAlpha2And3.put("AX", "Åland Islands");
            ISOAlpha2And3.put("AZ", "Azerbaijan");
            ISOAlpha2And3.put("BA", "Bosnia and Herzegovina");
            ISOAlpha2And3.put("BB", "Barbados");
            ISOAlpha2And3.put("BD", "Bangladesh");
            ISOAlpha2And3.put("BE", "Belgium");
            ISOAlpha2And3.put("BF", "Burkina Faso");
            ISOAlpha2And3.put("BG", "Bulgaria");
            ISOAlpha2And3.put("BH", "Bahrain");
            ISOAlpha2And3.put("BI", "Burundi");
            ISOAlpha2And3.put("BJ", "Benin");
            ISOAlpha2And3.put("BL", "Saint Barthélemy");
            ISOAlpha2And3.put("BM", "Bermuda");
            ISOAlpha2And3.put("BN", "Brunei Darussalam");
            ISOAlpha2And3.put("BO", "Bolivia, Plurinational State of");
            ISOAlpha2And3.put("BQ", "Bonaire, Sint Eustatius and Saba");
            ISOAlpha2And3.put("BR", "Brazil");
            ISOAlpha2And3.put("BS", "Bahamas");
            ISOAlpha2And3.put("BT", "Bhutan");
            ISOAlpha2And3.put("BV", "Bouvet Island");
            ISOAlpha2And3.put("BW", "Botswana");
            ISOAlpha2And3.put("BY", "Belarus");
            ISOAlpha2And3.put("BZ", "Belize");
            ISOAlpha2And3.put("CA", "Canada");
            ISOAlpha2And3.put("CC", "Cocos (Keeling) Islands");
            ISOAlpha2And3.put("CD", "Congo, the Democratic Republic of the");
            ISOAlpha2And3.put("CF", "Central African Republic");
            ISOAlpha2And3.put("CG", "Congo");
            ISOAlpha2And3.put("CH", "Switzerland");
            ISOAlpha2And3.put("CI", "Côte d'Ivoire");
            ISOAlpha2And3.put("CK", "Cook Islands");
            ISOAlpha2And3.put("CL", "Chile");
            ISOAlpha2And3.put("CM", "Cameroon");
            ISOAlpha2And3.put("CN", "China");
            ISOAlpha2And3.put("CO", "Colombia");
            ISOAlpha2And3.put("CR", "Costa Rica");
            ISOAlpha2And3.put("CU", "Cuba");
            ISOAlpha2And3.put("CV", "Cape Verde");
            ISOAlpha2And3.put("CW", "Curaçao");
            ISOAlpha2And3.put("CX", "Christmas Island");
            ISOAlpha2And3.put("CY", "Cyprus");
            ISOAlpha2And3.put("CZ", "Czech Republic");
            ISOAlpha2And3.put("DE", "Germany");
            ISOAlpha2And3.put("DJ", "Djibouti");
            ISOAlpha2And3.put("DK", "Denmark");
            ISOAlpha2And3.put("DM", "Dominica");
            ISOAlpha2And3.put("DO", "Dominican Republic");
            ISOAlpha2And3.put("DZ", "Algeria");
            ISOAlpha2And3.put("EC", "Ecuador");
            ISOAlpha2And3.put("EE", "Estonia");
            ISOAlpha2And3.put("EG", "Egypt");
            ISOAlpha2And3.put("EH", "Western Sahara");
            ISOAlpha2And3.put("ER", "Eritrea");
            ISOAlpha2And3.put("ES", "Spain");
            ISOAlpha2And3.put("ET", "Ethiopia");
            ISOAlpha2And3.put("FI", "Finland");
            ISOAlpha2And3.put("FJ", "Fiji");
            ISOAlpha2And3.put("FK", "Falkland Islands (Malvinas)");
            ISOAlpha2And3.put("FM", "Micronesia, Federated States of");
            ISOAlpha2And3.put("FO", "Faroe Islands");
            ISOAlpha2And3.put("FR", "France");
            ISOAlpha2And3.put("GA", "Gabon");
            ISOAlpha2And3.put("GB", "United Kingdom");
            ISOAlpha2And3.put("GD", "Grenada");
            ISOAlpha2And3.put("GE", "Georgia");
            ISOAlpha2And3.put("GF", "French Guiana");
            ISOAlpha2And3.put("GG", "Guernsey");
            ISOAlpha2And3.put("GH", "Ghana");
            ISOAlpha2And3.put("GI", "Gibraltar");
            ISOAlpha2And3.put("GL", "Greenland");
            ISOAlpha2And3.put("GM", "Gambia");
            ISOAlpha2And3.put("GN", "Guinea");
            ISOAlpha2And3.put("GP", "Guadeloupe");
            ISOAlpha2And3.put("GQ", "Equatorial Guinea");
            ISOAlpha2And3.put("GR", "Greece");
            ISOAlpha2And3.put("GS", "South Georgia and the South Sandwich Islands");
            ISOAlpha2And3.put("GT", "Guatemala");
            ISOAlpha2And3.put("GU", "Guam");
            ISOAlpha2And3.put("GW", "Guinea-Bissau");
            ISOAlpha2And3.put("GY", "Guyana");
            ISOAlpha2And3.put("HK", "Hong Kong");
            ISOAlpha2And3.put("HM", "Heard Island and McDonald Islands");
            ISOAlpha2And3.put("HN", "Honduras");
            ISOAlpha2And3.put("HR", "Croatia");
            ISOAlpha2And3.put("HT", "Haiti");
            ISOAlpha2And3.put("HU", "Hungary");
            ISOAlpha2And3.put("ID", "Indonesia");
            ISOAlpha2And3.put("IE", "Ireland");
            ISOAlpha2And3.put("IL", "Israel");
            ISOAlpha2And3.put("IM", "Isle of Man");
            ISOAlpha2And3.put("IN", "India");
            ISOAlpha2And3.put("IO", "British Indian Ocean Territory");
            ISOAlpha2And3.put("IQ", "Iraq");
            ISOAlpha2And3.put("IR", "Iran, Islamic Republic of");
            ISOAlpha2And3.put("IS", "Iceland");
            ISOAlpha2And3.put("IT", "Italy");
            ISOAlpha2And3.put("JE", "Jersey");
            ISOAlpha2And3.put("JM", "Jamaica");
            ISOAlpha2And3.put("JO", "Jordan");
            ISOAlpha2And3.put("JP", "Japan");
            ISOAlpha2And3.put("KE", "Kenya");
            ISOAlpha2And3.put("KG", "Kyrgyzstan");
            ISOAlpha2And3.put("KH", "Cambodia");
            ISOAlpha2And3.put("KI", "Kiribati");
            ISOAlpha2And3.put("KM", "Comoros");
            ISOAlpha2And3.put("KN", "Saint Kitts and Nevis");
            ISOAlpha2And3.put("KO", "Kosovo");
            ISOAlpha2And3.put("KP", "Korea, Democratic People's Republic of");
            ISOAlpha2And3.put("KR", "Korea, Republic of");
            ISOAlpha2And3.put("KW", "Kuwait");
            ISOAlpha2And3.put("KY", "Cayman Islands");
            ISOAlpha2And3.put("KZ", "Kazakhstan");
            ISOAlpha2And3.put("LA", "Lao People's Democratic Republic");
            ISOAlpha2And3.put("LB", "Lebanon");
            ISOAlpha2And3.put("LC", "Saint Lucia");
            ISOAlpha2And3.put("LI", "Liechtenstein");
            ISOAlpha2And3.put("LK", "Sri Lanka");
            ISOAlpha2And3.put("LR", "Liberia");
            ISOAlpha2And3.put("LS", "Lesotho");
            ISOAlpha2And3.put("LT", "Lithuania");
            ISOAlpha2And3.put("LU", "Luxembourg");
            ISOAlpha2And3.put("LV", "Latvia");
            ISOAlpha2And3.put("LY", "Libyan Arab Jamahiriya");
            ISOAlpha2And3.put("MA", "Morocco");
            ISOAlpha2And3.put("MC", "Monaco");
            ISOAlpha2And3.put("MD", "Moldova");
            ISOAlpha2And3.put("ME", "Montenegro");
            ISOAlpha2And3.put("MF", "Saint Martin (French part)");
            ISOAlpha2And3.put("MG", "Madagascar");
            ISOAlpha2And3.put("MH", "Marshall Islands");
            ISOAlpha2And3.put("MK", "Macedonia"); // Originally "Macedonia, the former Yugoslav Republic of"
            ISOAlpha2And3.put("ML", "Mali");
            ISOAlpha2And3.put("MM", "Myanmar");
            ISOAlpha2And3.put("MN", "Mongolia");
            ISOAlpha2And3.put("MO", "Macao");
            ISOAlpha2And3.put("MP", "Northern Mariana Islands");
            ISOAlpha2And3.put("MQ", "Martinique");
            ISOAlpha2And3.put("MR", "Mauritania");
            ISOAlpha2And3.put("MS", "Montserrat");
            ISOAlpha2And3.put("MT", "Malta");
            ISOAlpha2And3.put("MU", "Mauritius");
            ISOAlpha2And3.put("MV", "Maldives");
            ISOAlpha2And3.put("MW", "Malawi");
            ISOAlpha2And3.put("MX", "Mexico");
            ISOAlpha2And3.put("MY", "Malaysia");
            ISOAlpha2And3.put("MZ", "Mozambique");
            ISOAlpha2And3.put("NA", "Namibia");
            ISOAlpha2And3.put("NC", "New Caledonia");
            ISOAlpha2And3.put("NE", "Niger");
            ISOAlpha2And3.put("NF", "Norfolk Island");
            ISOAlpha2And3.put("NG", "Nigeria");
            ISOAlpha2And3.put("NI", "Nicaragua");
            ISOAlpha2And3.put("NL", "Netherlands");
            ISOAlpha2And3.put("NO", "Norway");
            ISOAlpha2And3.put("NP", "Nepal");
            ISOAlpha2And3.put("NR", "Nauru");
            ISOAlpha2And3.put("NU", "Niue");
            ISOAlpha2And3.put("NZ", "New Zealand");
            ISOAlpha2And3.put("OM", "Oman");
            ISOAlpha2And3.put("PA", "Panama");
            ISOAlpha2And3.put("PE", "Peru");
            ISOAlpha2And3.put("PF", "French Polynesia");
            ISOAlpha2And3.put("PG", "Papua New Guinea");
            ISOAlpha2And3.put("PH", "Philippines");
            ISOAlpha2And3.put("PK", "Pakistan");
            ISOAlpha2And3.put("PL", "Poland");
            ISOAlpha2And3.put("PM", "Saint Pierre and Miquelon");
            ISOAlpha2And3.put("PN", "Pitcairn");
            ISOAlpha2And3.put("PR", "Puerto Rico");
            ISOAlpha2And3.put("PS", "Palestinian Territory, Occupied");
            ISOAlpha2And3.put("PT", "Portugal");
            ISOAlpha2And3.put("PW", "Palau");
            ISOAlpha2And3.put("PY", "Paraguay");
            ISOAlpha2And3.put("QA", "Qatar");
            ISOAlpha2And3.put("RE", "Réunion");
            ISOAlpha2And3.put("RO", "Romania");
            ISOAlpha2And3.put("RS", "Serbia");
            ISOAlpha2And3.put("RU", "Russia"); // originally "Russian Federation"
            ISOAlpha2And3.put("RW", "Rwanda");
            ISOAlpha2And3.put("SA", "Saudi Arabia");
            ISOAlpha2And3.put("SB", "Solomon Islands");
            ISOAlpha2And3.put("SC", "Seychelles");
            ISOAlpha2And3.put("SD", "Sudan");
            ISOAlpha2And3.put("SE", "Sweden");
            ISOAlpha2And3.put("SG", "Singapore");
            ISOAlpha2And3.put("SH", "Saint Helena, Ascension and Tristan da Cunha");
            ISOAlpha2And3.put("SI", "Slovenia");
            ISOAlpha2And3.put("SJ", "Svalbard and Jan Mayen");
            ISOAlpha2And3.put("SK", "Slovakia");
            ISOAlpha2And3.put("SL", "Sierra Leone");
            ISOAlpha2And3.put("SM", "San Marino");
            ISOAlpha2And3.put("SN", "Senegal");
            ISOAlpha2And3.put("SO", "Somalia");
            ISOAlpha2And3.put("SR", "Suriname");
            ISOAlpha2And3.put("SS", "South Sudan");
            ISOAlpha2And3.put("ST", "Sao Tome and Principe");
            ISOAlpha2And3.put("SV", "El Salvador");
            ISOAlpha2And3.put("SX", "Sint Maarten (Dutch part)");
            ISOAlpha2And3.put("SY", "Syrian Arab Republic");
            ISOAlpha2And3.put("SZ", "Swaziland");
            ISOAlpha2And3.put("TC", "Turks and Caicos Islands");
            ISOAlpha2And3.put("TD", "Chad");
            ISOAlpha2And3.put("TF", "French Southern Territories");
            ISOAlpha2And3.put("TG", "Togo");
            ISOAlpha2And3.put("TH", "Thailand");
            ISOAlpha2And3.put("TJ", "Tajikistan");
            ISOAlpha2And3.put("TK", "Tokelau");
            ISOAlpha2And3.put("TL", "Timor-Leste");
            ISOAlpha2And3.put("TM", "Turkmenistan");
            ISOAlpha2And3.put("TN", "Tunisia");
            ISOAlpha2And3.put("TO", "Tonga");
            ISOAlpha2And3.put("TR", "Turkey");
            ISOAlpha2And3.put("TT", "Trinidad and Tobago");
            ISOAlpha2And3.put("TV", "Tuvalu");
            ISOAlpha2And3.put("TW", "Taiwan, Province of China");
            ISOAlpha2And3.put("TZ", "Tanzania, United Republic of");
            ISOAlpha2And3.put("UA", "Ukraine");
            ISOAlpha2And3.put("UG", "Uganda");
            ISOAlpha2And3.put("UK", "United Kingdom");
            ISOAlpha2And3.put("UM", "United States Minor Outlying Islands");
            ISOAlpha2And3.put("US", "United States");
            ISOAlpha2And3.put("UY", "Uruguay");
            ISOAlpha2And3.put("UZ", "Uzbekistan");
            ISOAlpha2And3.put("VA", "Holy See (Vatican City State)");
            ISOAlpha2And3.put("VC", "Saint Vincent and the Grenadines");
            ISOAlpha2And3.put("VE", "Venezuela, Bolivarian Republic of");
            ISOAlpha2And3.put("VG", "Virgin Islands, British");
            ISOAlpha2And3.put("VI", "Virgin Islands, U.S.");
            ISOAlpha2And3.put("VN", "Viet Nam");
            ISOAlpha2And3.put("VU", "Vanuatu");
            ISOAlpha2And3.put("WF", "Wallis and Futuna");
            ISOAlpha2And3.put("WS", "Samoa");
            ISOAlpha2And3.put("YE", "Yemen");
            ISOAlpha2And3.put("YT", "Mayotte");
            ISOAlpha2And3.put("ZA", "South Africa");
            ISOAlpha2And3.put("ZM", "Zambia");
            ISOAlpha2And3.put("ZW", "Zimbabwe");

            // ISO 3166-1 alpha-3
            ISOAlpha2And3.put("ABW", "Aruba");
            ISOAlpha2And3.put("AFG", "Afghanistan");
            ISOAlpha2And3.put("AGO", "Angola");
            ISOAlpha2And3.put("AIA", "Anguilla");
            ISOAlpha2And3.put("ALA", "Åland Islands");
            ISOAlpha2And3.put("ALB", "Albania");
            ISOAlpha2And3.put("AND", "Andorra");
            ISOAlpha2And3.put("ARE", "United Arab Emirates");
            ISOAlpha2And3.put("ARG", "Argentina");
            ISOAlpha2And3.put("ARM", "Armenia");
            ISOAlpha2And3.put("ASM", "American Samoa");
            ISOAlpha2And3.put("ATA", "Antarctica");
            ISOAlpha2And3.put("ATF", "French Southern Territories");
            ISOAlpha2And3.put("ATG", "Antigua and Barbuda");
            ISOAlpha2And3.put("AUS", "Australia");
            ISOAlpha2And3.put("AUT", "Austria");
            ISOAlpha2And3.put("AZE", "Azerbaijan");
            ISOAlpha2And3.put("BDI", "Burundi");
            ISOAlpha2And3.put("BEL", "Belgium");
            ISOAlpha2And3.put("BEN", "Benin");
            ISOAlpha2And3.put("BES", "Bonaire, Sint Eustatius and Saba");
            ISOAlpha2And3.put("BFA", "Burkina Faso");
            ISOAlpha2And3.put("BGD", "Bangladesh");
            ISOAlpha2And3.put("BGR", "Bulgaria");
            ISOAlpha2And3.put("BHR", "Bahrain");
            ISOAlpha2And3.put("BHS", "Bahamas");
            ISOAlpha2And3.put("BIH", "Bosnia and Herzegovina");
            ISOAlpha2And3.put("BLM", "Saint Barthélemy");
            ISOAlpha2And3.put("BLR", "Belarus");
            ISOAlpha2And3.put("BLZ", "Belize");
            ISOAlpha2And3.put("BMU", "Bermuda");
            ISOAlpha2And3.put("BOL", "Bolivia");
            ISOAlpha2And3.put("BRA", "Brazil");
            ISOAlpha2And3.put("BRB", "Barbados");
            ISOAlpha2And3.put("BRN", "Brunei Darussalam");
            ISOAlpha2And3.put("BTN", "Bhutan");
            ISOAlpha2And3.put("BVT", "Bouvet Island");
            ISOAlpha2And3.put("BWA", "Botswana");
            ISOAlpha2And3.put("CAF", "Central African Republic");
            ISOAlpha2And3.put("CAN", "Canada");
            ISOAlpha2And3.put("CCK", "Cocos (Keeling) Islands");
            ISOAlpha2And3.put("CHE", "Switzerland");
            ISOAlpha2And3.put("CHL", "Chile");
            ISOAlpha2And3.put("CHN", "China");
            ISOAlpha2And3.put("CIV", "Cote d'Ivoire");
            ISOAlpha2And3.put("CMR", "Cameroon");
            ISOAlpha2And3.put("COD", "Congo, Democratic Republic of the");
            ISOAlpha2And3.put("COG", "Congo, Republic of the");
            ISOAlpha2And3.put("COK", "Cook Islands");
            ISOAlpha2And3.put("COL", "Colombia");
            ISOAlpha2And3.put("COM", "Comoros");
            ISOAlpha2And3.put("CPV", "Cape Verde");
            ISOAlpha2And3.put("CRI", "Costa Rica");
            ISOAlpha2And3.put("CUB", "Cuba");
            ISOAlpha2And3.put("CUW", "Curaçao");
            ISOAlpha2And3.put("CXR", "Christmas Island");
            ISOAlpha2And3.put("CYM", "Cayman Islands");
            ISOAlpha2And3.put("CYP", "Cyprus");
            ISOAlpha2And3.put("CZE", "Czech Republic");
            ISOAlpha2And3.put("DEU", "Germany");
            ISOAlpha2And3.put("DJI", "Djibouti");
            ISOAlpha2And3.put("DMA", "Dominica");
            ISOAlpha2And3.put("DNK", "Denmark");
            ISOAlpha2And3.put("DOM", "Dominican Republic");
            ISOAlpha2And3.put("DZA", "Algeria");
            ISOAlpha2And3.put("ECU", "Ecuador");
            ISOAlpha2And3.put("EGY", "Egypt");
            ISOAlpha2And3.put("ERI", "Eritrea");
            ISOAlpha2And3.put("ESH", "Western Sahara");
            ISOAlpha2And3.put("ESP", "Spain");
            ISOAlpha2And3.put("EST", "Estonia");
            ISOAlpha2And3.put("ETH", "Ethiopia");
            ISOAlpha2And3.put("FIN", "Finland");
            ISOAlpha2And3.put("FJI", "Fiji");
            ISOAlpha2And3.put("FLK", "Falkland Islands (Malvinas)");
            ISOAlpha2And3.put("FRA", "France");
            ISOAlpha2And3.put("FRO", "Faroe Islands");
            ISOAlpha2And3.put("FSM", "Micronesia, Federated States of");
            ISOAlpha2And3.put("GAB", "Gabon");
            ISOAlpha2And3.put("GBR", "United Kingdom");
            ISOAlpha2And3.put("GEO", "Georgia");
            ISOAlpha2And3.put("GGY", "Guernsey");
            ISOAlpha2And3.put("GHA", "Ghana");
            ISOAlpha2And3.put("GIB", "Gibraltar");
            ISOAlpha2And3.put("GIN", "Guinea");
            ISOAlpha2And3.put("GLP", "Guadeloupe");
            ISOAlpha2And3.put("GMB", "Gambia, The");
            ISOAlpha2And3.put("GNB", "Guinea-Bissau");
            ISOAlpha2And3.put("GNQ", "Equatorial Guinea");
            ISOAlpha2And3.put("GRC", "Greece");
            ISOAlpha2And3.put("GRD", "Grenada");
            ISOAlpha2And3.put("GRL", "Greenland");
            ISOAlpha2And3.put("GTM", "Guatemala");
            ISOAlpha2And3.put("GUF", "French Guiana");
            ISOAlpha2And3.put("GUM", "Guam");
            ISOAlpha2And3.put("GUY", "Guyana");
            ISOAlpha2And3.put("HKG", "Hong Kong");
            ISOAlpha2And3.put("HMD", "Heard Island and McDonald Islands");
            ISOAlpha2And3.put("HND", "Honduras");
            ISOAlpha2And3.put("HRV", "Croatia");
            ISOAlpha2And3.put("HTI", "Haiti");
            ISOAlpha2And3.put("HUN", "Hungary");
            ISOAlpha2And3.put("IDN", "Indonesia");
            ISOAlpha2And3.put("IMN", "Isle of Man");
            ISOAlpha2And3.put("IND", "India");
            ISOAlpha2And3.put("IOT", "British Indian Ocean Territory");
            ISOAlpha2And3.put("IRL", "Ireland");
            ISOAlpha2And3.put("IRN", "Iran");
            ISOAlpha2And3.put("IRQ", "Iraq");
            ISOAlpha2And3.put("ISL", "Iceland");
            ISOAlpha2And3.put("ISR", "Israel");
            ISOAlpha2And3.put("ITA", "Italy");
            ISOAlpha2And3.put("JAM", "Jamaica");
            ISOAlpha2And3.put("JEY", "Jersey");
            ISOAlpha2And3.put("JOR", "Jordan");
            ISOAlpha2And3.put("JPN", "Japan");
            ISOAlpha2And3.put("KAZ", "Kazakhstan");
            ISOAlpha2And3.put("KEN", "Kenya");
            ISOAlpha2And3.put("KGZ", "Kyrgyzstan");
            ISOAlpha2And3.put("KHM", "Cambodia");
            ISOAlpha2And3.put("KIR", "Kiribati");
            ISOAlpha2And3.put("KNA", "Saint Kitts and Nevis");
            ISOAlpha2And3.put("KOR", "Korea, South");
            ISOAlpha2And3.put("KOS", "Kosovo");
            ISOAlpha2And3.put("KWT", "Kuwait");
            ISOAlpha2And3.put("LAO", "Laos");
            ISOAlpha2And3.put("LBN", "Lebanon");
            ISOAlpha2And3.put("LBR", "Liberia");
            ISOAlpha2And3.put("LBY", "Libya");
            ISOAlpha2And3.put("LCA", "Saint Lucia");
            ISOAlpha2And3.put("LIE", "Liechtenstein");
            ISOAlpha2And3.put("LKA", "Sri Lanka");
            ISOAlpha2And3.put("LSO", "Lesotho");
            ISOAlpha2And3.put("LTU", "Lithuania");
            ISOAlpha2And3.put("LUX", "Luxembourg");
            ISOAlpha2And3.put("LVA", "Latvia");
            ISOAlpha2And3.put("MAC", "Macao");
            ISOAlpha2And3.put("MAF", "Saint Martin (French part)");
            ISOAlpha2And3.put("MAR", "Morocco");
            ISOAlpha2And3.put("MCO", "Monaco");
            ISOAlpha2And3.put("MDA", "Moldova");
            ISOAlpha2And3.put("MDG", "Madagascar");
            ISOAlpha2And3.put("MDV", "Maldives");
            ISOAlpha2And3.put("MEX", "Mexico");
            ISOAlpha2And3.put("MHL", "Marshall Islands");
            ISOAlpha2And3.put("MKD", "Macedonia");
            ISOAlpha2And3.put("MLI", "Mali");
            ISOAlpha2And3.put("MLT", "Malta");
            ISOAlpha2And3.put("MMR", "Burma");
            ISOAlpha2And3.put("MNE", "Montenegro");
            ISOAlpha2And3.put("MNG", "Mongolia");
            ISOAlpha2And3.put("MNP", "Northern Mariana Islands");
            ISOAlpha2And3.put("MOZ", "Mozambique");
            ISOAlpha2And3.put("MRT", "Mauritania");
            ISOAlpha2And3.put("MSR", "Montserrat");
            ISOAlpha2And3.put("MTQ", "Martinique");
            ISOAlpha2And3.put("MUS", "Mauritius");
            ISOAlpha2And3.put("MWI", "Malawi");
            ISOAlpha2And3.put("MYS", "Malaysia");
            ISOAlpha2And3.put("MYT", "Mayotte");
            ISOAlpha2And3.put("NAM", "Namibia");
            ISOAlpha2And3.put("NCL", "New Caledonia");
            ISOAlpha2And3.put("NER", "Niger");
            ISOAlpha2And3.put("NFK", "Norfolk Island");
            ISOAlpha2And3.put("NGA", "Nigeria");
            ISOAlpha2And3.put("NIC", "Nicaragua");
            ISOAlpha2And3.put("NIU", "Niue");
            ISOAlpha2And3.put("NLD", "Netherlands");
            ISOAlpha2And3.put("NOR", "Norway");
            ISOAlpha2And3.put("NPL", "Nepal");
            ISOAlpha2And3.put("NRU", "Nauru");
            ISOAlpha2And3.put("NZL", "New Zealand");
            ISOAlpha2And3.put("OMN", "Oman");
            ISOAlpha2And3.put("PAK", "Pakistan");
            ISOAlpha2And3.put("PAN", "Panama");
            ISOAlpha2And3.put("PCN", "Pitcairn");
            ISOAlpha2And3.put("PER", "Peru");
            ISOAlpha2And3.put("PHL", "Philippines");
            ISOAlpha2And3.put("PLW", "Palau");
            ISOAlpha2And3.put("PNG", "Papua New Guinea");
            ISOAlpha2And3.put("POL", "Poland");
            ISOAlpha2And3.put("PRI", "Puerto Rico");
            ISOAlpha2And3.put("PRK", "Korea, North");
            ISOAlpha2And3.put("PRT", "Portugal");
            ISOAlpha2And3.put("PRY", "Paraguay");
            ISOAlpha2And3.put("PSE", "Palestinian Territory, Occupied");
            ISOAlpha2And3.put("PYF", "French Polynesia");
            ISOAlpha2And3.put("QAT", "Qatar");
            ISOAlpha2And3.put("REU", "Réunion");
            ISOAlpha2And3.put("ROU", "Romania");
            ISOAlpha2And3.put("RUS", "Russia");
            ISOAlpha2And3.put("RWA", "Rwanda");
            ISOAlpha2And3.put("SAU", "Saudi Arabia");
            ISOAlpha2And3.put("SDN", "Sudan");
            ISOAlpha2And3.put("SEN", "Senegal");
            ISOAlpha2And3.put("SGP", "Singapore");
            ISOAlpha2And3.put("SGS", "South Georgia and the South Sandwich Islands");
            ISOAlpha2And3.put("SHN", "Saint Helena, Ascension and Tristan da Cunha");
            ISOAlpha2And3.put("SJM", "Svalbard and Jan Mayen");
            ISOAlpha2And3.put("SLB", "Solomon Islands");
            ISOAlpha2And3.put("SLE", "Sierra Leone");
            ISOAlpha2And3.put("SLV", "El Salvador");
            ISOAlpha2And3.put("SMR", "San Marino");
            ISOAlpha2And3.put("SOM", "Somalia");
            ISOAlpha2And3.put("SPM", "Saint Pierre and Miquelon");
            ISOAlpha2And3.put("SRB", "Serbia");
            ISOAlpha2And3.put("SSD", "South Sudan");
            ISOAlpha2And3.put("STP", "Sao Tome and Principe");
            ISOAlpha2And3.put("SUR", "Suriname");
            ISOAlpha2And3.put("SVK", "Slovakia");
            ISOAlpha2And3.put("SVN", "Slovenia");
            ISOAlpha2And3.put("SWE", "Sweden");
            ISOAlpha2And3.put("SWZ", "Swaziland");
            ISOAlpha2And3.put("SXM", "Sint Maarten (Dutch part)");
            ISOAlpha2And3.put("SYC", "Seychelles");
            ISOAlpha2And3.put("SYR", "Syria");
            ISOAlpha2And3.put("TCA", "Turks and Caicos Islands");
            ISOAlpha2And3.put("TCD", "Chad");
            ISOAlpha2And3.put("TGO", "Togo");
            ISOAlpha2And3.put("THA", "Thailand");
            ISOAlpha2And3.put("TJK", "Tajikistan");
            ISOAlpha2And3.put("TKL", "Tokelau");
            ISOAlpha2And3.put("TKM", "Turkmenistan");
            ISOAlpha2And3.put("TLS", "Timor-Leste");
            ISOAlpha2And3.put("TON", "Tonga");
            ISOAlpha2And3.put("TTO", "Trinidad and Tobago");
            ISOAlpha2And3.put("TUN", "Tunisia");
            ISOAlpha2And3.put("TUR", "Turkey");
            ISOAlpha2And3.put("TUV", "Tuvalu");
            ISOAlpha2And3.put("TWN", "Taiwan, Province of China");
            ISOAlpha2And3.put("TZA", "Tanzania");
            ISOAlpha2And3.put("UGA", "Uganda");
            ISOAlpha2And3.put("UKR", "Ukraine");
            ISOAlpha2And3.put("UMI", "United States Minor Outlying Islands");
            ISOAlpha2And3.put("URY", "Uruguay");
            ISOAlpha2And3.put("USA", "United States");
            ISOAlpha2And3.put("UZB", "Uzbekistan");
            ISOAlpha2And3.put("VAT", "Holy See (Vatican City State)");
            ISOAlpha2And3.put("VCT", "Saint Vincent and the Grenadines");
            ISOAlpha2And3.put("VEN", "Venezuela");
            ISOAlpha2And3.put("VGB", "Virgin Islands, British");
            ISOAlpha2And3.put("VIR", "Virgin Islands, U.S.");
            ISOAlpha2And3.put("VNM", "Vietnam");
            ISOAlpha2And3.put("VUT", "Vanuatu");
            ISOAlpha2And3.put("WLF", "Wallis and Futuna");
            ISOAlpha2And3.put("WSM", "Samoa");
            ISOAlpha2And3.put("YEM", "Yemen");
            ISOAlpha2And3.put("ZAF", "South Africa");
            ISOAlpha2And3.put("ZMB", "Zambia");
            ISOAlpha2And3.put("ZWE", "Zimbabwe");
        }

        return ISOAlpha2And3;
    }

    public static HashMap<String, Integer> getCensusStateCodes() {
        if (censusStateCodes == null) {
            censusStateCodes = new HashMap<String, Integer>();

            censusStateCodes.put("AL", 1000);
            censusStateCodes.put("AK", 2000);
            censusStateCodes.put("AZ", 4000);
            censusStateCodes.put("AR", 5000);
            censusStateCodes.put("CA", 6000);
            censusStateCodes.put("CO", 8000);
            censusStateCodes.put("CT", 9000);
            censusStateCodes.put("DE", 10000);
            censusStateCodes.put("DC", 11000);
            censusStateCodes.put("FL", 12000);
            censusStateCodes.put("GA", 13000);
            censusStateCodes.put("HI", 15000);
            censusStateCodes.put("ID", 16000);
            censusStateCodes.put("IL", 17000);
            censusStateCodes.put("IN", 18000);
            censusStateCodes.put("IA", 19000);
            censusStateCodes.put("KS", 20000);
            censusStateCodes.put("KY", 21000);
            censusStateCodes.put("LA", 22000);
            censusStateCodes.put("ME", 23000);
            censusStateCodes.put("MD", 24000);
            censusStateCodes.put("MA", 25000);
            censusStateCodes.put("MI", 26000);
            censusStateCodes.put("MN", 27000);
            censusStateCodes.put("MS", 28000);
            censusStateCodes.put("MO", 29000);
            censusStateCodes.put("MT", 30000);
            censusStateCodes.put("NE", 31000);
            censusStateCodes.put("NV", 32000);
            censusStateCodes.put("NH", 33000);
            censusStateCodes.put("NJ", 34000);
            censusStateCodes.put("NM", 35000);
            censusStateCodes.put("NY", 36000);
            censusStateCodes.put("NC", 37000);
            censusStateCodes.put("ND", 38000);
            censusStateCodes.put("OH", 39000);
            censusStateCodes.put("OK", 40000);
            censusStateCodes.put("OR", 41000);
            censusStateCodes.put("PA", 42000);
            censusStateCodes.put("RI", 44000);
            censusStateCodes.put("SC", 45000);
            censusStateCodes.put("SD", 46000);
            censusStateCodes.put("TN", 47000);
            censusStateCodes.put("TX", 48000);
            censusStateCodes.put("UT", 49000);
            censusStateCodes.put("VT", 50000);
            censusStateCodes.put("VA", 51000);
            censusStateCodes.put("WA", 53000);
            censusStateCodes.put("WV", 54000);
            censusStateCodes.put("WI", 55000);
            censusStateCodes.put("WY", 56000);
        }

        return censusStateCodes;
    }
}
