/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.AAMAS12;
import RSLBench.Assignment.DCOP.DCOPAgent;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Helpers.Utility.UtilityMatrix;
import RSLBench.Comm.Message;
import RSLBench.Assignment.Assignment;
import RSLBench.Comm.CommunicationLayer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import rescuecore2.config.Config;

/**
 *
 * @author riccardo
 */
public class Dummy implements DCOPAgent{
    private EntityID _agentID;
    private EntityID _targetID;
    private UtilityMatrix _utility;
     /**
     * Initialized this simulated agent.
     * @param agentID: the ID of the agent (as defined in the world model).
     * @param utility: the utility matrix for all agents that are within communication range 
     */
    @Override
    public void initialize(Config config, EntityID agentID, UtilityMatrix utility){
        _agentID = agentID;
        _utility = utility;   
        _targetID = Assignment.UNKNOWN_TARGET_ID;
    }
    
    /**
     * Improves the assignment for the specific agent.
     * This function is called unless any agent in the world return true.
     * @return true, if the assignment of this agent changed, false otherwise.
     */
    @Override
    public boolean improveAssignment() {
        List<EntityID> bestTarget = _utility.getNBestTargets(1, _agentID);
        _targetID = bestTarget.get(0);
        return true;
    }
    
    /**
     * Returns the ID of the agent.
     * @return the ID of the agent. 
     */
    @Override
    public EntityID getAgentID(){
        return _agentID;
    }

    /**
     * Returns the ID  of the currently selected target. 
     * @return the ID of the target. 
     */
    @Override
    public EntityID getTargetID(){
        return _targetID;
    }

    /**
     * Send a set of messages to all neighboring agents 
     * (i.e. those which are within communication range).
     * @return The set of messages to be send.
     */
    @Override
    public Collection<Message> sendMessages(CommunicationLayer com){
       return new LinkedList<>();   
    }
    
    /**
     * Receive messages of the neighbor agents.
     * @param collection of messages received from other agents near by.
     */
    @Override
    public void receiveMessages(Collection<Message> messages){}

    /**
     * Return constraint checked
     * 
     * @return 
     */
    @Override
    public long getConstraintChecks(){
        return 0;
    }
    
    @Override
    public int getNumberOfOtherMessages() {
        return 0;
    }

    @Override
    public long getDimensionOfOtherMessages() {
        return 0;
    }
}
