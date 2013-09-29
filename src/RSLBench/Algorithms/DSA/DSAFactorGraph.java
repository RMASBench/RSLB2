/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Constants;
import java.util.List;
import rescuecore2.standard.entities.StandardEntityURN;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class DSAFactorGraph extends DCOPSolver {

    @Override
    protected DCOPAgent buildAgent(StandardEntityURN type) {
        switch(type) {
            case FIRE_BRIGADE:
                return new DSAFactorgraphAgent();
            default:
                throw new UnsupportedOperationException("The DSAFactorGraph solver does not support agents of type " + type);
        }
    }

    @Override
    public String getIdentifier() {
        return "DSAFactorGraph";
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = super.getUsedConfigurationKeys();
        keys.add(DSA.KEY_DSA_PROBABILITY);
        keys.add(Constants.KEY_MAXSUM_NEIGHBORS);
        return keys;
    }

    

}
