/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.BMS;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class BinaryMaxSum extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent() {
        return new BMSAgent();
    }

    @Override
    public String getIdentifier() {
        return "BinaryMaxSum";
    }

}
