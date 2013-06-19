package RSLBench.Comm;

import rescuecore2.worldmodel.EntityID;

/**
 * This class represents the messages sent by some DCOP algorithms.
 * It contains two EntityID's: the agent id and the id of the target assigned to the agent. 
 */
public class AssignmentMessage extends AbstractMessage
{
    private EntityID _agentID;
    private EntityID _targetID;
    
    /**
     * It builds the AssignmentMessage.
     * @param agentID: the agent id
     * @param targetID: the target id
     */
    public AssignmentMessage(EntityID agentID, EntityID targetID)
    {
        _agentID = agentID;
        _targetID = targetID;
    }

    /**
     * Returns the agent id.
     * @return the agent id
     */
    public EntityID getAgentID()
    {
        return _agentID;
    }
    
    /**
     * Returns the target id.
     * @return the target id
     */
    public EntityID getTargetID()
    {
        return _targetID;
    }
}
