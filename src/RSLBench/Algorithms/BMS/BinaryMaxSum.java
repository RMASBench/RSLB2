/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.BMS;

import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import rescuecore2.standard.entities.StandardEntityURN;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class BinaryMaxSum extends DCOPSolver {

    /** Configuration key to enable/disable interteam coordination */
    public final static String KEY_INTERTEAM_COORDINATION = "agent.interteam";

    @Override
    protected DCOPAgent buildAgent(StandardEntityURN type) {
        final boolean team = config.getBooleanValue("KEY_INTERTEAM_COORDINATION", false);
        switch(type) {
            case FIRE_BRIGADE:
                return team ? new BMSTeamFireAgent() : new BMSFireAgent();
            case POLICE_FORCE:
                return team ? new BMSTeamPoliceAgent() : new BMSPoliceAgent();
            default:
                throw new UnsupportedOperationException("The Binary Max-Sum solver does not support agents of type " + type);
        }
    }

    @Override
    public String getIdentifier() {
        return "BinaryMaxSum";
    }

}
