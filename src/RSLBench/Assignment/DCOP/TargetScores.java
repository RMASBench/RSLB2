package RSLBench.Assignment.DCOP;

import RSLBench.Helpers.Utility.UtilityMatrix;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import rescuecore2.worldmodel.EntityID;

/**
 * Computes the utility for an agent to pick a specific target given the targets
 * chosen by the other agents.
 */
public class TargetScores {
    private HashMap<EntityID, Integer> _numAssignedAgents;
    private UtilityMatrix utilities;
    private EntityID agent;

    /**
     * Build a new score tracker.
     * @param agent agent that is evaluating different target options.
     * @param utilities problem scenario in terms of utility.
     */
    public TargetScores(EntityID agent, UtilityMatrix utilities) {
        _numAssignedAgents = new HashMap<>();
        this.utilities = utilities;
        this.agent = agent;
    }

    /**
     * Increases the count of agents that have chosen the specified target.
     * @param target target chosen by some other agent.
     */
    public void increaseAgentCount(EntityID target) {
        Integer count = _numAssignedAgents.get(target);
        if (count == null) {
            _numAssignedAgents.put(target, 1);
        } else {
            _numAssignedAgents.put(target, count+1);
        }
    }

    /**
     * Get the utility of chosing a target given the targets chosen by other
     * agents.
     *
     * This function should only be used <strong>after</strong> all the other
     * agents' choices have been set through the
     * {@link #increaseAgentCount(EntityID)} method.
     *
     * @param target target to evaluate.
     * @return utility for this agent to pick the given target.
     */
    public double computeScore(EntityID target) {
        // Get the number of *other* agents that have already chosen this target
        int nAgents = 0;
        if (_numAssignedAgents.containsKey(target)) {
            nAgents = _numAssignedAgents.get(target);
        }

        // Now get the utility penalty if this agent *also* chooses that target
        double penalty = utilities.getUtilityPenalty(target, nAgents+1);

        // Finally, the score for this agent is its utility minus the penalty
        return utilities.getUtility(agent, target) - penalty;
    }

    /**
     * Resets this object (clears all choices of the neighboring agents).
     */
    public void resetAssignments() {
        Iterator<Entry<EntityID, Integer>> it = _numAssignedAgents.entrySet().iterator();
        while (it.hasNext()) {
            Entry<EntityID, Integer> pair = it.next();
            pair.setValue(0);
        }
    }
}