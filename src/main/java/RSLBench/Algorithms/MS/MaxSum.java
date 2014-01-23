/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import java.util.List;
import rescuecore2.standard.entities.StandardEntityURN;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class MaxSum extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent(StandardEntityURN type) {
        switch(type) {
            case FIRE_BRIGADE:
                return new MaxSumAgent();
            default:
                throw new UnsupportedOperationException("The Max-Sum solver does not support agents of type " + type);
        }
    }

    @Override
    public String getIdentifier() {
        return "MaxSum";
    }

    @Override
    protected void initializeAgents(ProblemDefinition problem) {
        MaxSumAgent.reset();
        super.initializeAgents(problem);
        MaxSumAgent.finishInitialization();
    }

}
