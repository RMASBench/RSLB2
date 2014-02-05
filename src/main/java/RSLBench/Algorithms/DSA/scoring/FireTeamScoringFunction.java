/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA.scoring;

import RSLBench.Algorithms.DSA.TargetScores;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class FireTeamScoringFunction implements ScoringFunction {

    @Override
    public double score(EntityID agent, EntityID target, TargetScores scores, ProblemDefinition problem, int nAgents) {

        // Compute the difference in penalty between going to that fire and not going there
        final double penalty = problem.getUtilityPenalty(target, nAgents+1)
                - problem.getUtilityPenalty(target, nAgents);

        // Compute the individual utility of going to that fire
        double utility = problem.getFireUtility(agent, target);

        // Subtract the corresponding penalty if that fire is blocked *and* the blockade is not
        // being attended by any police agent
        if (problem.isFireAgentBlocked(agent, target)) {
            EntityID blockade = problem.getBlockadeBlockingFireAgent(agent, target);
            if (scores.getAgentCount(blockade) == 0) {
                utility -= problem.getConfig().getFloatValue(Constants.KEY_BLOCKED_FIRE_PENALTY);
            }
        }

        // The score is the individual utility gain minus the increase in penalty
        return  utility - penalty;
    }

}
