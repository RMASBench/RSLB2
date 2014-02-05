package RSLBench.Algorithms.DSA;

import RSLBench.Algorithms.DSA.scoring.ScoringFunction;
import RSLBench.Assignment.Assignment;
import RSLBench.Helpers.Utility.ProblemDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;

/**
 * Computes the utility for an agent to pick a specific target given the targets
 * chosen by the other agents.
 */
public class TargetScores {
    private HashMap<EntityID, Integer> nAssignedAgents;
    private ScoringFunction scoringFunction;
    private ProblemDefinition problem;
    private EntityID agent;

    /**
     * Build a new score tracker.
     * @param agent agent that is evaluating different target options.
     * @param utilities problem scenario in terms of utility.
     */
    public TargetScores(EntityID agent, ProblemDefinition problem) {
        this.agent = agent;
        this.problem = problem;
        nAssignedAgents = new HashMap<>();
    }

    /**
     * Set the scoring function to use when evaluating targets.
     *
     * @param function
     */
    public void setScoringFunction(ScoringFunction function) {
        scoringFunction = function;
    }

    /**
     * Increases the count of agents that have chosen the specified target.
     * @param target target chosen by some other agent.
     */
    public void increaseAgentCount(EntityID target) {
        Integer count = nAssignedAgents.get(target);
        if (count == null) {
            nAssignedAgents.put(target, 1);
        } else {
            nAssignedAgents.put(target, count+1);
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
        if (nAssignedAgents.containsKey(target)) {
            nAgents = nAssignedAgents.get(target);
        }

        return scoringFunction.score(agent, target, problem, nAgents);
    }

    /**
     * Computes the best target among the given candidates.
     *
     * @param candidates candidate targets
     * @return target that maximizes the obtained utility from the point of view of this agent.
     */
    public EntityID getBestTarget(List<EntityID> candidates) {
        EntityID bestTarget = Assignment.UNKNOWN_TARGET_ID;
        double bestUtility = Double.NEGATIVE_INFINITY;
        for (EntityID target : candidates) {
            final double utility = computeScore(target);
            if (utility > bestUtility) {
                bestUtility = utility;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    /**
     * Resets this object (clears all choices of the neighboring agents).
     */
    public void resetAssignments() {
        nAssignedAgents.clear();
    }
}