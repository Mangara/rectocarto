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
package rectangularcartogram.ipe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import rectangularcartogram.data.graph.Edge;
import rectangularcartogram.data.graph.Graph;
import rectangularcartogram.data.graph.Graph.Labeling;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.gui.SubdivisionDrawPanel;

public class IPEExporter {

    private static final String IPE7HEADER =
            // XML stuff
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n" +
            // We require IPE version 7
            "<ipe version=\"70010\" creator=\"RectangularCartogram\">\n" +
            // Basic IPE style
            "<ipestyle name=\"basic\">\n" +
            "<symbol name=\"arrow/arc(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/farc(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/circle(sx)\" transformations=\"translations\">\n" +
            "<path fill=\"sym-stroke\">\n" +
            "0.6 0 0 0.6 0 0 e\n" +
            "0.4 0 0 0.4 0 0 e\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/disk(sx)\" transformations=\"translations\">\n" +
            "<path fill=\"sym-stroke\">\n" +
            "0.6 0 0 0.6 0 0 e\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/fdisk(sfx)\" transformations=\"translations\">\n" +
            "<group>\n" +
            "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n" +
            "0.6 0 0 0.6 0 0 e\n" +
            "0.4 0 0 0.4 0 0 e\n" +
            "</path>\n" +
            "<path fill=\"sym-fill\">\n" +
            "0.4 0 0 0.4 0 0 e\n" +
            "</path>\n" +
            "</group>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/box(sx)\" transformations=\"translations\">\n" +
            "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n" +
            "-0.6 -0.6 m\n" +
            "0.6 -0.6 l\n" +
            "0.6 0.6 l\n" +
            "-0.6 0.6 l\n" +
            "h\n" +
            "-0.4 -0.4 m\n" +
            "0.4 -0.4 l\n" +
            "0.4 0.4 l\n" +
            "-0.4 0.4 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/square(sx)\" transformations=\"translations\">\n" +
            "<path fill=\"sym-stroke\">\n" +
            "-0.6 -0.6 m\n" +
            "0.6 -0.6 l\n" +
            "0.6 0.6 l\n" +
            "-0.6 0.6 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/fsquare(sfx)\" transformations=\"translations\">\n" +
            "<group>\n" +
            "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n" +
            "-0.6 -0.6 m\n" +
            "0.6 -0.6 l\n" +
            "0.6 0.6 l\n" +
            "-0.6 0.6 l\n" +
            "h\n" +
            "-0.4 -0.4 m\n" +
            "0.4 -0.4 l\n" +
            "0.4 0.4 l\n" +
            "-0.4 0.4 l\n" +
            "h\n" +
            "</path>\n" +
            "<path fill=\"sym-fill\">\n" +
            "-0.4 -0.4 m\n" +
            "0.4 -0.4 l\n" +
            "0.4 0.4 l\n" +
            "-0.4 0.4 l\n" +
            "h\n" +
            "</path>\n" +
            "</group>\n" +
            "</symbol>\n" +
            "<symbol name=\"mark/cross(sx)\" transformations=\"translations\">\n" +
            "<group>\n" +
            "<path fill=\"sym-stroke\">\n" +
            "-0.43 -0.57 m\n" +
            "0.57 0.43 l\n" +
            "0.43 0.57 l\n" +
            "-0.57 -0.43 l\n" +
            "h\n" +
            "</path>\n" +
            "<path fill=\"sym-stroke\">\n" +
            "-0.43 0.57 m\n" +
            "0.57 -0.43 l\n" +
            "0.43 -0.57 l\n" +
            "-0.57 0.43 l\n" +
            "h\n" +
            "</path>\n" +
            "</group>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/fnormal(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/pointed(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-0.8 0 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/fpointed(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-0.8 0 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/linear(spx)\">\n" +
            "<path stroke=\"sym-stroke\" pen=\"sym-pen\">\n" +
            "-1 0.333 m\n" +
            "0 0 l\n" +
            "-1 -0.333 l\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/fdouble(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "-1 0 m\n" +
            "-2 0.333 l\n" +
            "-2 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<symbol name=\"arrow/double(spx)\">\n" +
            "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n" +
            "0 0 m\n" +
            "-1 0.333 l\n" +
            "-1 -0.333 l\n" +
            "h\n" +
            "-1 0 m\n" +
            "-2 0.333 l\n" +
            "-2 -0.333 l\n" +
            "h\n" +
            "</path>\n" +
            "</symbol>\n" +
            "<pen name=\"heavier\" value=\"0.8\"/>\n" +
            "<pen name=\"fat\" value=\"1.2\"/>\n" +
            "<pen name=\"ultrafat\" value=\"2\"/>\n" +
            "<symbolsize name=\"large\" value=\"5\"/>\n" +
            "<symbolsize name=\"small\" value=\"2\"/>\n" +
            "<symbolsize name=\"tiny\" value=\"1.1\"/>\n" +
            "<arrowsize name=\"large\" value=\"10\"/>\n" +
            "<arrowsize name=\"small\" value=\"5\"/>\n" +
            "<arrowsize name=\"tiny\" value=\"3\"/>\n" +
            "<color name=\"red\" value=\"1 0 0\"/>\n" +
            "<color name=\"green\" value=\"0 1 0\"/>\n" +
            "<color name=\"blue\" value=\"0 0 1\"/>\n" +
            "<color name=\"yellow\" value=\"1 1 0\"/>\n" +
            "<color name=\"orange\" value=\"1 0.647 0\"/>\n" +
            "<color name=\"gold\" value=\"1 0.843 0\"/>\n" +
            "<color name=\"purple\" value=\"0.627 0.125 0.941\"/>\n" +
            "<color name=\"gray\" value=\"0.745\"/>\n" +
            "<color name=\"brown\" value=\"0.647 0.165 0.165\"/>\n" +
            "<color name=\"navy\" value=\"0 0 0.502\"/>\n" +
            "<color name=\"pink\" value=\"1 0.753 0.796\"/>\n" +
            "<color name=\"seagreen\" value=\"0.18 0.545 0.341\"/>\n" +
            "<color name=\"turquoise\" value=\"0.251 0.878 0.816\"/>\n" +
            "<color name=\"violet\" value=\"0.933 0.51 0.933\"/>\n" +
            "<color name=\"darkblue\" value=\"0 0 0.545\"/>\n" +
            "<color name=\"darkcyan\" value=\"0 0.545 0.545\"/>\n" +
            "<color name=\"darkgray\" value=\"0.663\"/>\n" +
            "<color name=\"darkgreen\" value=\"0 0.392 0\"/>\n" +
            "<color name=\"darkmagenta\" value=\"0.545 0 0.545\"/>\n" +
            "<color name=\"darkorange\" value=\"1 0.549 0\"/>\n" +
            "<color name=\"darkred\" value=\"0.545 0 0\"/>\n" +
            "<color name=\"lightblue\" value=\"0.678 0.847 0.902\"/>\n" +
            "<color name=\"lightcyan\" value=\"0.878 1 1\"/>\n" +
            "<color name=\"lightgray\" value=\"0.827\"/>\n" +
            "<color name=\"lightgreen\" value=\"0.565 0.933 0.565\"/>\n" +
            "<color name=\"lightyellow\" value=\"1 1 0.878\"/>\n" +
            "<dashstyle name=\"dashed\" value=\"[4] 0\"/>\n" +
            "<dashstyle name=\"dotted\" value=\"[1 3] 0\"/>\n" +
            "<dashstyle name=\"dash dotted\" value=\"[4 2 1 2] 0\"/>\n" +
            "<dashstyle name=\"dash dot dotted\" value=\"[4 2 1 2 1 2] 0\"/>\n" +
            "<textsize name=\"large\" value=\"\\large\"/>\n" +
            "<textsize name=\"small\" value=\"\\small\"/>\n" +
            "<textsize name=\"tiny\" value=\"\\tiny\"/>\n" +
            "<textsize name=\"Large\" value=\"\\Large\"/>\n" +
            "<textsize name=\"LARGE\" value=\"\\LARGE\"/>\n" +
            "<textsize name=\"huge\" value=\"\\huge\"/>\n" +
            "<textsize name=\"Huge\" value=\"\\Huge\"/>\n" +
            "<textsize name=\"footnote\" value=\"\\footnotesize\"/>\n" +
            "<textstyle name=\"center\" begin=\"\\begin{center}\" end=\"\\end{center}\"/>\n" +
            "<textstyle name=\"itemize\" begin=\"\\begin{itemize}\" end=\"\\end{itemize}\"/>\n" +
            "<textstyle name=\"item\" begin=\"\\begin{itemize}\\item{}\" end=\"\\end{itemize}\"/>\n" +
            "<gridsize name=\"4 pts\" value=\"4\"/>\n" +
            "<gridsize name=\"8 pts (~3 mm)\" value=\"8\"/>\n" +
            "<gridsize name=\"16 pts (~6 mm)\" value=\"16\"/>\n" +
            "<gridsize name=\"32 pts (~12 mm)\" value=\"32\"/>\n" +
            "<gridsize name=\"10 pts (~3.5 mm)\" value=\"10\"/>\n" +
            "<gridsize name=\"20 pts (~7 mm)\" value=\"20\"/>\n" +
            "<gridsize name=\"14 pts (~5 mm)\" value=\"14\"/>\n" +
            "<gridsize name=\"28 pts (~10 mm)\" value=\"28\"/>\n" +
            "<gridsize name=\"56 pts (~20 mm)\" value=\"56\"/>\n" +
            "<anglesize name=\"90 deg\" value=\"90\"/>\n" +
            "<anglesize name=\"60 deg\" value=\"60\"/>\n" +
            "<anglesize name=\"45 deg\" value=\"45\"/>\n" +
            "<anglesize name=\"30 deg\" value=\"30\"/>\n" +
            "<anglesize name=\"22.5 deg\" value=\"22.5\"/>\n" +
            "<tiling name=\"falling\" angle=\"-60\" step=\"4\" width=\"1\"/>\n" +
            "<tiling name=\"rising\" angle=\"30\" step=\"4\" width=\"1\"/>\n" +
            "</ipestyle>\n" +
            // Personal IPE style for the labeled edges
            "<ipestyle name=\"REL\">\n" +
            "<symbol name=\"arrow/center(spx)\">\n" +
            "<path stroke=\"sym-stroke\" pen=\"sym-pen\">\n" +
            "-1 0.5 m\n" +
            "0 0 l\n" +
            "-1 -0.5 l\n" +
            "</path>\n" +
            "</symbol>\n" +
            "</ipestyle>\n";
    private static final String IPE6HEADER =
            // We require IPE version 6
            "<ipe version=\"60032\" creator=\"RectangularCartogram\">\n" +
            "<info bbox=\"cropbox\"/>\n" +
            // No custom IPE style, since IPE 6 doesn't allow us to modify the arrows like that
            "<ipestyle>\n" +
            "</ipestyle>\n";
    private static final String layers =
            // Beginning of the IPE page
            "<page>\n" +
            // Layers
            "<layer name=\"Regions\"/>\n" +
            "<layer name=\"OriginalColourRegions\"/>\n" +
            "<layer name=\"SeaRegions\"/>\n" +
            "<layer name=\"RegionLabels\"/>\n" +
            "<layer name=\"DualVertices\"/>\n" +
            "<layer name=\"DualEdges\"/>\n" +
            "<layer name=\"DualLabelingBlue\"/>\n" +
            "<layer name=\"DualLabelingRed\"/>\n" +
            "<view layers=\"Regions SeaRegions\" active=\"Regions\"/>\n"; // Show only the regions by default
    private static final String postTags =
            "</page>\n</ipe>";

    public void exportIPEFile(File file, Subdivision sub, boolean useIPE6) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            if (useIPE6) {
                out.write(IPE6HEADER);
            } else {
                out.write(IPE7HEADER);
            }
            out.write(layers);

            exportFaces(out, sub, false); // Original colours
            exportFaces(out, sub, true); // Error colours
            exportGraph(out, sub.getDualGraph(), useIPE6);

            out.write(postTags);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void exportIPEFile(File file, Graph graph, boolean useIPE6) throws IOException {
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(file));

            if (useIPE6) {
                out.write(IPE6HEADER);
            } else {
                out.write(IPE7HEADER);
            }
            out.write(layers);

            exportGraph(out, graph, useIPE6);

            out.write(postTags);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void exportFaces(BufferedWriter out, Subdivision sub, boolean showError) throws IOException {
        // Export the sea faces first, so they are at the back
        for (SubdivisionFace face : sub.getFaces()) {
            if (face.isSea()) {
                exportFacePolygon(out, face, showError);
            }
        }

        for (SubdivisionFace face : sub.getFaces()) {
            if (!face.isSea()) {
                exportFacePolygon(out, face, showError);
            }
        }

        for (SubdivisionFace face : sub.getFaces()) {
            exportFaceLabel(out, face);
        }
    }

    private void exportFacePolygon(BufferedWriter out, SubdivisionFace face, boolean showError) throws IOException {
        float[] rgb;

        if (showError && !face.isSea()) {
            rgb = SubdivisionDrawPanel.getErrorColor(face.getCartographicError(true)).getRGBColorComponents(null);
        } else {
            rgb = face.getColor().getRGBColorComponents(null);
        }

        if (face.isSea()) {
            out.write("<path layer=\"SeaRegions\" pen=\"normal\" fill=\"" + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\">\n"); // Different layer and no stroke
        } else if (showError) {
            out.write("<path layer=\"Regions\" pen=\"normal\" stroke=\"black\" fill=\"" + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\">\n");
        } else {
            out.write("<path layer=\"OriginalColourRegions\" pen=\"normal\" stroke=\"black\" fill=\"" + rgb[0] + " " + rgb[1] + " " + rgb[2] + "\">\n");
        }

        List<Vertex> vertices = face.getVertices();
        boolean first = true;

        for (Vertex v : vertices) {
            out.write(v.getX() + " " + v.getY());

            if (first) {
                out.write(" m\n");
                first = false;
            } else {
                out.write(" l\n");
            }
        }

        out.write("h\n");
        out.write("</path>\n");
    }

    private void exportFaceLabel(BufferedWriter out, SubdivisionFace face) throws IOException {
        if (face.getName() != null && !face.getName().isEmpty()) {
            out.write("<text layer=\"RegionLabels\" transformations=\"translations\" stroke=\"black\" type=\"label\" valign=\"baseline\" size=\"normal\" pos=\"" +
                    face.getCorrespondingVertex().getX() + " " + face.getCorrespondingVertex().getY() + "\">" + face.getName() + "</text>\n");
        }
    }

    private void exportGraph(BufferedWriter out, Graph graph, boolean useIPE6) throws IOException {
        for (Edge e : graph.getEdges()) {
            exportEdge(out, e);
        }

        if (!useIPE6) { // Only export the labeling to IPE 7
            for (Edge e : graph.getEdges()) {
                Labeling label = graph.getEdgeLabel(e);

                if (label != null && label != Labeling.NONE) {
                    exportLabeledEdge(out, e, label);
                }
            }
        }

        // Vertices last, so they appear on top
        for (Vertex v : graph.getVertices()) {
            if (useIPE6) {
                exportVertexIPE6(out, v);
            } else {
                exportVertexIPE7(out, v);
            }
        }
    }

    private void exportVertexIPE6(BufferedWriter out, Vertex v) throws IOException {
        out.write("<mark layer=\"DualVertices\" type=\"1\" pos=\"" + v.getX() + " " + v.getY() + "\" size=\"large\" stroke=\"black\" fill=\"blue\"/>\n");
    }

    private void exportVertexIPE7(BufferedWriter out, Vertex v) throws IOException {
        out.write("<use layer=\"DualVertices\" name=\"mark/fdisk(sfx)\" pos=\"" + v.getX() + " " + v.getY() + "\" size=\"large\" stroke=\"black\" fill=\"blue\"/>\n");
    }

    private void exportEdge(BufferedWriter out, Edge e) throws IOException {
        out.write("<path layer=\"DualEdges\" pen=\"normal\" stroke=\"black\">\n");
        out.write(e.getVA().getX() + " " + e.getVA().getY() + " m\n");
        out.write(e.getVB().getX() + " " + e.getVB().getY() + " l\n");
        out.write("</path>\n");
    }

    private void exportLabeledEdge(BufferedWriter out, Edge e, Labeling labeling) throws IOException {
        String edgeColour = (labeling == Labeling.RED ? "red" : "blue");

        double halfWayX = (e.getVA().getX() + e.getVB().getX()) / 2;
        double halfWayY = (e.getVA().getY() + e.getVB().getY()) / 2;

        if (labeling == Labeling.RED) {
            out.write("<path layer=\"DualLabelingRed\" stroke=\"" + edgeColour + "\" pen=\"fat\" arrow=\"center/large\">\n");
        } else {
            out.write("<path layer=\"DualLabelingBlue\" stroke=\"" + edgeColour + "\" pen=\"fat\" arrow=\"center/large\">\n");
        }
        out.write(e.getOrigin().getX() + " " + e.getOrigin().getY() + " m\n");
        out.write(halfWayX + " " + halfWayY + " l\n");
        out.write("</path>\n");

        out.write("<path stroke=\"" + edgeColour + "\" pen=\"fat\">\n");
        out.write(halfWayX + " " + halfWayY + " m\n");
        out.write(e.getDestination().getX() + " " + e.getDestination().getY() + " l\n");
        out.write("</path>\n");
    }
}
