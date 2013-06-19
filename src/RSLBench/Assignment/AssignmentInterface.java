package RSLBench.Assignment;

import RSLBench.Helpers.UtilityMatrix;

/**
 * This interface represents the executor of the computation.
 * It executes the initialization, the dispatch and the receiving
 * of the messages and the improvment of the assignment for each agent.
 */
public interface AssignmentInterface
{
    
    /**
     * This method sinchronously executes the computation of the agents
     * (firstable each agent is initialized, then each agent sends messages,
     * then each agent receives messages and eventually each agent computes his new assignment).
     * 
     * @param utility: a matrix that contains all the agent-target utilities
     * (for all the agents and alla the targets).
     * @return a mapping for each agent to a target.
     */
    public Assignment compute(UtilityMatrix utility);
    
    /**
     * This method returns the number of total (assignment+other) messages exchanged between the agents.
     * @return the number of messages exchanged.
     */
    public int getTotalMessages();
    
    /**
     * This method returns the size (in bytes) of all the exchanged messages.
     * @return the size of the exchanged messages.
     */
    public long getTotalMessagesBytes();
    
    /**
     * This method returns the number of messages exchanged between the agents before or after
     * the assignment decision process (i.e. the messages used by the MaxSum algorithm to create
     * the factor graph).
     * @return the number of other messages exchanged.
     */
    public int getOtherMessages();
    
    /**
     * This method returns the number of nccc (non concurrent constraint checks) made by the agents.
     * @return the number of nccc.
     */
    public int getAverageNccc();
}
