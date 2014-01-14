/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import java.util.List;
import rescuecore2.standard.entities.StandardEntityURN;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class DSA extends DCOPSolver {

    /**
     * The probability that an agent changes his assigned target.
     */
    public static final String KEY_DSA_PROBABILITY = "dsa.probability";

    @Override
    public String getIdentifier() {
        return "DSA";
    }

    @Override
    protected DCOPAgent buildAgent(StandardEntityURN type) {
        switch(type) {
            case FIRE_BRIGADE:
                return new DSAAgent();
            default:
                throw new UnsupportedOperationException("The DSA solver does not support agents of type " + type);
        }
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = super.getUsedConfigurationKeys();
        keys.add(KEY_DSA_PROBABILITY);
        return keys;
    }



}