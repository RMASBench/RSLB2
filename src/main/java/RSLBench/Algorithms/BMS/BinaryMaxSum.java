/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.BMS;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.StandardEntityURN;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class BinaryMaxSum extends DCOPSolver {
    private static final Logger Logger = LogManager.getLogger(BinaryMaxSum.class);

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> result = super.getUsedConfigurationKeys();
        result.add(Constants.KEY_INTERTEAM_COORDINATION);
        result.add(Constants.KEY_BLOCKED_FIRE_PENALTY);
        result.add(Constants.KEY_BLOCKED_POLICE_PENALTY);
        return result;
    }

    @Override
    protected DCOPAgent buildAgent(StandardEntityURN type) {
        final boolean team = config.getBooleanValue(Constants.KEY_INTERTEAM_COORDINATION);

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

    @Override
    public double getUtility(ProblemDefinition problem, Assignment solution) {
        return super.getUtility(problem, solution);
    }



}
