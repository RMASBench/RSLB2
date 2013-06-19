package RSLBench.Assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import rescuecore2.worldmodel.EntityID;

/**
 * This class represents the assignment map between agents and targets.
 */
public class Assignment
{
    // Stores the number of targets assigned to an agent
    private HashMap<EntityID, Integer> _targetSelectionCounts;
    // Mapping from Agents to Targets
    private HashMap<EntityID, EntityID> _assignment;
    private ArrayList<EntityID> _agents;
    private ArrayList<EntityID> _targets;
    
    public static final EntityID UNKNOWN_TARGET_ID = new EntityID(-1);
    
    public Assignment()
    {
        _targetSelectionCounts = new HashMap<EntityID, Integer>();
        _assignment = new HashMap<EntityID, EntityID>();
        _agents = new ArrayList<EntityID>();
        _targets = new ArrayList<EntityID>();
    }
    
    /**
     * Computes the assignment process.
     * @param agentID: the agent ID
     * @param targetID: the target ID
     */
    public void assign(EntityID agentID, EntityID targetID)
    {
    	_assignment.put(agentID, targetID);    	
        if (_targetSelectionCounts.containsKey(targetID)) {
           _targetSelectionCounts.put(targetID, _targetSelectionCounts.get(targetID) + 1);
        }
        else {
           _targetSelectionCounts.put(targetID, 1);
        }
        _agents.add(agentID);
        _targets.add(targetID);
    }
    
    /**
     * Returns the assignment of an agent
     * @param agentID: the agent ID
     * @return the entity ID of the target assigned to the agent represented by the agentID
     */
    public EntityID getAssignment(EntityID agentID)
    {
        if (_assignment.containsKey(agentID)) 
        	return _assignment.get(agentID);        
        else 
        	return UNKNOWN_TARGET_ID;
        
    }

    /**
     * Returns the number of targets assigned to an agent
     * @param targetID: the target ID
     * @return the number of targets assigned to an agent
     */
    public int getTargetSelectionCount(EntityID targetID)
    {
        if (_targetSelectionCounts.containsKey(targetID)) {
            return _targetSelectionCounts.get(targetID);
        }
        else return 0;
    }

    /**
     * Returns an arraylist of the entityID's of the agents
     * @return entityID of all the agents
     */
    public ArrayList<EntityID> getAgents() {
    	return _agents;
    }
    
    /**
     * Returns an arraylist of the entityID's of the targets
     * @return entityID of all the targets
     */
    public ArrayList<EntityID> getTargets() {
    	return _targets;
    }
    @Override
    /**
     * @see Object::toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Assignments:\n");
        Iterator<Entry<EntityID,EntityID>> it = _assignment.entrySet().iterator();
        while (it.hasNext()) {
        	Entry<EntityID,EntityID> pair = it.next();        	        	
        	buffer.append("agent ").append(pair.getKey()).append(" > ").append(pair.getValue()).append("\n");        	
        }
        return buffer.toString();
    }
}
