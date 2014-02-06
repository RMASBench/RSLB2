/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.NewMS;

import RSLBench.Algorithms.MS.MSAgent;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Comm.Message;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class MSAgentTest {



    public MSAgentTest() {
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
     * Test of improveAssignment method, of class MSAgent.
     */
    @Test
    public void testImproveAssignment() {
        System.out.println("improveAssignment");
        MSAgent instance = new MSAgent();
        boolean expResult = false;
        boolean result = instance.improveAssignment();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sendMessages method, of class MSAgent.
     */
    @Test
    public void testSendMessages() {
        System.out.println("sendMessages");
        CommunicationLayer com = null;
        MSAgent instance = new MSAgent();
        Collection expResult = null;
        Collection result = instance.sendMessages(com);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of receiveMessages method, of class MSAgent.
     */
    @Test
    public void testReceiveMessages() {
        System.out.println("receiveMessages");
        Collection<Message> messages = null;
        MSAgent instance = new MSAgent();
        instance.receiveMessages(messages);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getConstraintChecks method, of class MSAgent.
     */
    @Test
    public void testGetConstraintChecks() {
        System.out.println("getConstraintChecks");
        MSAgent instance = new MSAgent();
        long expResult = 0L;
        long result = instance.getConstraintChecks();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}