/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.Random;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;

/**
 * Pure greedy solver.
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class Random extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent() {
        return new RandomAgent();
    }

    @Override
    public String getIdentifier() {
        return "Random";
    }

}
