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

import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rectangularcartogram.data.Pair;
import rectocarto.data.lp.Constraint;
import rectocarto.data.lp.MinimizationProblem;
import rectocarto.data.lp.ObjectiveFunction;
import rectocarto.data.lp.Solution;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class CLPSolverTest {
    
    public CLPSolverTest() {
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
     * Test of solve method, of class CLPSolver.
     */
    @Test
    public void testSolve() {
        System.out.println("solve - basic LP");
        
        /*
        min: x1 + x2;
            x1 >= 1;
            x2 >= 1;
            x1 + x2 >= 2;
        */
        MinimizationProblem linearProgram = new MinimizationProblem();
        linearProgram.setObjective(new ObjectiveFunction.Linear(Arrays.asList(new Pair<>(1d,"x1"), new Pair<>(1d,"x2"))));
        linearProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x1")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 1));
        linearProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x2")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 1));
        linearProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x1"), new Pair<>(1d,"x2")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 2));
        
        Solution expResult = new Solution(2);
        expResult.put("x1", 1d);
        expResult.put("x2", 1d);
        
        CLPSolver instance = new CLPSolver();
        Solution result = instance.solve(linearProgram);
        assertEquals(expResult, result);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSolveQP() {
        System.out.println("solve - quadratic LP");
        
        /*
        min: x1^2 + x2^2;
            x1 >= 1;
            x2 >= 1;
            x1 + 1.5 x2 >= 10;
        */
        MinimizationProblem quadraticProgram = new MinimizationProblem();
        quadraticProgram.setObjective(new ObjectiveFunction.Quadratic(Collections.EMPTY_LIST, Arrays.asList(new Pair<>(1d,"x1"), new Pair<>(1d,"x2"))));
        quadraticProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x1")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 1));
        quadraticProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x2")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 1));
        quadraticProgram.addConstraint(new Constraint.Linear(Arrays.asList(new Pair<>(1d,"x1"), new Pair<>(1.5,"x2")), Constraint.Comparison.GREATER_THAN_OR_EQUAL, 10));
        
        Solution expResult = new Solution(2);
        expResult.put("x1", 4d);
        expResult.put("x2", 4d);
        
        CLPSolver instance = new CLPSolver();
        Solution result = instance.solve(quadraticProgram);
        assertEquals(expResult, result);
    }
    
}
