/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.Greedy;

import RSLBench.Assignment.DCOP.DefaultDCOPAgent;
import RSLBench.Assignment.DCOP.AbstractDCOPAgent;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Comm.Message;
import java.util.ArrayList;
import java.util.Collection;
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
        for (EntityID target : utilities.getTargets()) {
            double value = utilities.getUtility(agentID, target);
            if (value > best) {
                targetID = target;
                best = value;
            }
        }

        return false;
    }

}
