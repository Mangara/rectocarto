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
package rectocarto.algos.lp.solver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.Solution;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class IteratedLinearSolverTest {
    
    public IteratedLinearSolverTest() {
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
     * Test of restrictToLinear method, of class IteratedLinearSolver.
     */
    @Test
    public void testRestrictToLinear() {
        System.out.println("restrictToLinear");
        IteratedLinearSolver solver = new IteratedLinearSolver(null);

        Constraint.Bilinear bilinear = new Constraint.Bilinear(Constraint.Comparison.EQUAL, 2);
        bilinear.addLinearTerm(3, "x");
        bilinear.addLinearTerm(-5, "y");
        bilinear.addLinearTerm(7, "z");
        bilinear.addBilinearTerm(11, "x", "y");

        try {
            solver.restrictToLinear(bilinear, Collections.EMPTY_MAP);
            fail("No exception thrown when not all bilinear terms reduced.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        assertEquals("248.0 y + 7.0 z = -67.0", solver.restrictToLinear(bilinear, Collections.singletonMap("x", 23.0)).toString());
        assertEquals("300.0 x + 7.0 z = 137.0", solver.restrictToLinear(bilinear, Collections.singletonMap("y", 27.0)).toString());
        try {
            solver.restrictToLinear(bilinear, Collections.singletonMap("z", 1.0));
            fail("No exception thrown when not all bilinear terms reduced.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        bilinear.addBilinearTerm(13, "x", "z");
        bilinear.addBilinearTerm(-17, "y", "z");

        assertEquals("-153.0 z = -6763.0", solver.restrictToLinear(bilinear, mapOf("x", 23, "y", 27)).toString());
        assertEquals("-211.0 y = -8329.0", solver.restrictToLinear(bilinear, mapOf("x", 23, "z", 27)).toString());
        assertEquals("607.0 x = 10485.0", solver.restrictToLinear(bilinear, mapOf("y", 23, "z", 27)).toString());

        try {
            solver.restrictToLinear(bilinear, Collections.singletonMap("x", 1.0));
            fail("No exception thrown when not all bilinear terms reduced.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            solver.restrictToLinear(bilinear, Collections.singletonMap("y", 1.0));
            fail("No exception thrown when not all bilinear terms reduced.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            solver.restrictToLinear(bilinear, Collections.singletonMap("z", 1.0));
            fail("No exception thrown when not all bilinear terms reduced.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }
    
    private Map<String, Double> mapOf(String var1, double val1, String var2, double val2) {
        Map<String, Double> map = new HashMap<>();
        map.put(var1, val1);
        map.put(var2, val2);
        return map;
    }
    
}
