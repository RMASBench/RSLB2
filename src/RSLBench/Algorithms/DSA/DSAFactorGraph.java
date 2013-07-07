/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA;

import RSLBench.Assignment.DCOPAgent;
import RSLBench.Assignment.DCOPSolver;
import RSLBench.Constants;
import java.util.List;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class DSAFactorGraph extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent() {
        return new DSAFactorgraphAgent();
    }

    @Override
    public String getIdentifier() {
        return "DSAFactorGraph";
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = super.getUsedConfigurationKeys();
        keys.add(Constants.KEY_DSA_PROBABILITY);
        keys.add(Constants.KEY_MAXSUM_NEIGHBORS);
        return keys;
    }

    

}
