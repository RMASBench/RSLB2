/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.Random;

import RSLBench.Assignment.DCOP.DefaultDCOPAgent;
import RSLBench.Helpers.Utility.ProblemDefinition;
import java.util.List;
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
    public void initialize(Config config, EntityID agentID, ProblemDefinition utility) {
        super.initialize(config, agentID, utility);
        random = config.getRandom();
    }



    @Override
    public boolean improveAssignment() {
        final EntityID id = getID();

        List<EntityID> targets = getProblem().getFireAgentNeighbors(id);
        int choice = random.nextInt(targets.size());
        setTarget(targets.get(choice));
        
        return true;
    }

}
