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
public class BlockadeScoringFunction implements ScoringFunction {

    @Override
    public double score(EntityID agent, EntityID target, ProblemDefinition problem, int nAgents) {
        // If there is already another police agent attending that blockade, the utility is -inf
        if (nAgents > 0) {
            return Double.NEGATIVE_INFINITY;
        }

        // Otherwise we can pick it, and the value is given by the unary utility
        double utility = problem.getPoliceUtility(agent, target);
        if (problem.isPoliceAgentBlocked(agent, target)) {
            utility -= problem.getConfig().getFloatValue(Constants.KEY_BLOCKED_POLICE_PENALTY);
        }

        return utility;
    }

}
