package RSLBench.Assignment;

import java.util.Collection;

import rescuecore2.worldmodel.EntityID;

import RSLBench.Comm.AbstractMessage;
import RSLBench.Helpers.UtilityMatrix;
import RSLBench.Comm.ComSimulator;
/**
 * This interface implements the actions that a single agent can perform in a DCOP algorithm.
 * The implementations of this interface are executed
 * by the implementations of the AssignmentInterface interface.
 */
public interface DecentralAssignment
{
    /**
     * This method initializes the agent.
     * @param agentID: the ID of the agent (as defined in the world model).
     * @param utility: a matrix that contains all the agent-target utilities
     * (for all the agents and alla the targets). 
     */
    public void initialize(EntityID agentID, UtilityMatrix utility);
    
    /**
     * Considering all the messages received from other agents, tries to find
     * an improvement over the previous assignment of the agent.
     * @return true, if the assignment of this agent changed, false otherwise.
     */
    public boolean improveAssignment();
    
    /**
     * Returns the ID of the agent.
     * @return the ID of the agent. 
     */
    public EntityID getAgentID();

    /**
     * Returns the ID  of the currently assigned target. 
     * @return the ID of the target.
     */
    public EntityID getTargetID();

    /**
     * Sends a set of messages from an agent to all the recipients.
     * @param com: a communication simulator.
     * @return The set of messages that have been sent.
     */
    public Collection<AbstractMessage> sendMessages(ComSimulator com);
    
    /**
     * Receives a set of messages sent by some other agents.
     * @param messages: colletcion of messages received from other agents.
     */
    public void receiveMessages(Collection<AbstractMessage> messages);
    
    /**
     * Returns the number of nccc (non concurrent constraint checks).
     * @return the number of nccc 
     */
    public int getNccc();
    
    /**
     * Returns the number of messages exchanged by the agents
     * before or after the assignment process
     * (i.e. in MaxSum the messages exchanged for the building of the factor graph).
     * @return the number of messages exchanged outside of the assignment process
     */
    public int getNumberOfOtherMessages();
    
    /**
     * Returns the dimension (in bytes) of the messages exchanged by the agents
     * before or after the assignment process.
     * @return the dimension of the messages excanged outside of the assignment process
     */
    public long getDimensionOfOtherMessages();
}
