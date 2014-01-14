/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Assignment.DCOP;

import RSLBench.Helpers.Utility.ProblemDefinition;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public abstract class AbstractDCOPAgent implements DCOPAgent {
    protected EntityID agentID;
    protected EntityID targetID;
    protected ProblemDefinition utilities;

    @Override
    public EntityID getAgentID() {
        return agentID;
    }

    @Override
    public EntityID getTargetID() {
        return targetID;
    }

    @Override
    public void initialize(Config config, EntityID agentID, ProblemDefinition utility) {
        utilities = utility;
        this.agentID = agentID;
    }

}
