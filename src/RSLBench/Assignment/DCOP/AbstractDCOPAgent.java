/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Assignment.DCOP;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Helpers.Utility.UtilityMatrix;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public abstract class AbstractDCOPAgent implements DCOPAgent {
    protected EntityID agentID;
    protected EntityID targetID;
    protected UtilityMatrix utilities;

    @Override
    public EntityID getAgentID() {
        return agentID;
    }

    @Override
    public EntityID getTargetID() {
        return targetID;
    }

    @Override
    public void initialize(Config config, EntityID agentID, UtilityMatrix utility) {
        utilities = utility;
        this.agentID = agentID;
    }

}
