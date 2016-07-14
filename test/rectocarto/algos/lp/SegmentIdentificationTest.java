/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package rectocarto.algos.lp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rectangularcartogram.algos.RELFusy;
import rectangularcartogram.algos.RectangularDualDrawer;
import rectangularcartogram.data.Pair;
import rectangularcartogram.data.graph.Vertex;
import rectangularcartogram.data.subdivision.Subdivision;
import rectangularcartogram.data.subdivision.SubdivisionFace;
import rectangularcartogram.exceptions.IncorrectGraphException;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class SegmentIdentificationTest {

    public SegmentIdentificationTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of identifySegments method, of class SegmentIdentification.
     */
    @Test
    public void testIdentifySegments() throws IOException, IncorrectGraphException {
        System.out.println("identifySegments");

        testSubdivision("exampleData/Subdivisions/Netherlands Area.sub");
        testSubdivision("exampleData/Subdivisions/World.sub");
        testSubdivision("exampleData/Subdivisions/Europe.sub");
    }

    private void testSubdivision(String name) throws IOException, IncorrectGraphException {
        try (BufferedReader in = Files.newBufferedReader(Paths.get(name))) {
            // Load a subdivision
            Subdivision sub = Subdivision.load(in);
            (new RELFusy()).computeREL(sub.getDualGraph());

            // Compute the segments both ways
            Map<SubdivisionFace, SegmentIdentification.FaceSegments> segments = SegmentIdentification.identifySegments(sub);
            Pair<Subdivision, Map<SubdivisionFace, SubdivisionFace>> dual = (new RectangularDualDrawer()).drawSubdivision(sub, false);

            long expectedNHorizontalSegments = dual.getSecond().values().stream()
                    .flatMap(f -> f.getVertices().stream())
                    .mapToDouble(v -> v.getX())
                    .distinct()
                    .count();

            long expectedNVerticalSegments = dual.getSecond().values().stream()
                    .flatMap(f -> f.getVertices().stream())
                    .mapToDouble(v -> v.getY())
                    .distinct()
                    .count();

            long actualHor = segments.values().stream()
                    .flatMap(s -> Arrays.asList(s.left.name, s.right.name).stream())
                    .distinct()
                    .count();

            long actualVert = segments.values().stream()
                    .flatMap(s -> Arrays.asList(s.top.name, s.bottom.name).stream())
                    .distinct()
                    .count();

            // Test whether the number of segments are equal
            assertEquals("Number of horizontal segments does not match.", expectedNHorizontalSegments, actualHor);
            assertEquals("Number of vertical segments does not match.", expectedNVerticalSegments, actualVert);

            // Test whether there is a bijection between the segments
            Map<String, String> rectDualToSegment = new HashMap<>();

            for (SubdivisionFace f : sub.getTopLevelFaces()) {
                SegmentIdentification.FaceSegments s = segments.get(f);

                double minFaceX = Double.POSITIVE_INFINITY;
                double minFaceY = Double.POSITIVE_INFINITY;
                double maxFaceX = Double.NEGATIVE_INFINITY;
                double maxFaceY = Double.NEGATIVE_INFINITY;

                for (Vertex v : dual.getSecond().get(f).getVertices()) {
                    minFaceX = Math.min(minFaceX, v.getX());
                    minFaceY = Math.min(minFaceY, v.getY());
                    maxFaceX = Math.max(maxFaceX, v.getX());
                    maxFaceY = Math.max(maxFaceY, v.getY());
                }

                String left = "x_" + minFaceX;
                String right = "x_" + maxFaceX;
                String top = "y_" + maxFaceY;
                String bottom = "y_" + minFaceY;

                if (rectDualToSegment.containsKey(left)) {
                    assertEquals(f.getName() + " left: " + left, rectDualToSegment.get(left), s.left.name);
                } else {
                    rectDualToSegment.put(left, s.left.name);
                }

                if (rectDualToSegment.containsKey(right)) {
                    assertEquals(f.getName() + " right: " + right, rectDualToSegment.get(right), s.right.name);
                } else {
                    rectDualToSegment.put(right, s.right.name);
                }

                if (rectDualToSegment.containsKey(top)) {
                    assertEquals(f.getName() + " top: " + top, rectDualToSegment.get(top), s.top.name);
                } else {
                    rectDualToSegment.put(top, s.top.name);
                }

                if (rectDualToSegment.containsKey(bottom)) {
                    assertEquals(f.getName() + " bottom: " + bottom, rectDualToSegment.get(bottom), s.bottom.name);
                } else {
                    rectDualToSegment.put(bottom, s.bottom.name);
                }
            }
        }
    }

}
