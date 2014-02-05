/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.DSA.scoring;

import RSLBench.Helpers.Utility.ProblemDefinition;
import rescuecore2.worldmodel.EntityID;

/**
 * Interface of a function that specifies the penalty of assigning a given number of
 * agents to a target.
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public interface ScoringFunction {

    public double score(EntityID agent, EntityID target, ProblemDefinition problem, int nAgents);

}
