/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.Greedy;

import RSLBench.Assignment.DCOP.DefaultDCOPAgent;
import rescuecore2.worldmodel.EntityID;

/**
 * Agent that picks whatever fire is best for him, disregarding any others.
 * <p/>
 * Keep in mind that this initial assignment can be optimized by the sequential
 * greedy deconflicting procedure.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class GreedyAgent extends DefaultDCOPAgent {

    @Override
    public boolean improveAssignment() {
        double best = Double.NEGATIVE_INFINITY;
        final EntityID id = getID();

        for (EntityID target : getProblem().getFireAgentNeighbors(getID())) {
            double value = getProblem().getFireUtility(id, target);
            if (value > best) {
                best = value;
                setTarget(target);
            }
        }

        return false;
    }

}
