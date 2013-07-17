/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.Random;

import RSLBench.Assignment.DCOP.DefaultDCOPAgent;
import RSLBench.Helpers.Utility.UtilityMatrix;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

/**
 * Agent that chooses its target randomly.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class RandomAgent extends DefaultDCOPAgent {
    
    private java.util.Random random;

    @Override
    public void initialize(Config config, EntityID agentID, UtilityMatrix utility) {
        super.initialize(config, agentID, utility);
        random = config.getRandom();
    }



    @Override
    public boolean improveAssignment() {
        int choice = random.nextInt(utilities.getNumTargets());
        targetID = utilities.getTargets().get(choice);
        return true;
    }

}