/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Constants;
import java.util.List;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class MaxSum extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent() {
        return new MaxSumAgent();
    }

    @Override
    public String getIdentifier() {
        return "MaxSum";
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = super.getUsedConfigurationKeys();
        keys.add(Constants.KEY_MAXSUM_NEIGHBORS);
        return keys;
    }



}
