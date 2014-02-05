/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA.scoring;

import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class FireScoringFunction implements ScoringFunction {

    @Override
    public double score(EntityID agent, EntityID target, ProblemDefinition problem, int nAgents) {

        // Compute the difference in penalty between going to that fire and not going there
        final double penalty = problem.getUtilityPenalty(target, nAgents+1)
                - problem.getUtilityPenalty(target, nAgents);

        // Compute the individual utility of going to that fire
        double utility = problem.getFireUtility(agent, target);

        // Subtract the corresponding penalty if that fire is blocked
        if (problem.isFireAgentBlocked(agent, target)) {
                utility -= problem.getConfig().getFloatValue(Constants.KEY_BLOCKED_FIRE_PENALTY);
        }

        // The score is the individual utility gain minus the increase in penalty
        return  utility - penalty;
    }

}
