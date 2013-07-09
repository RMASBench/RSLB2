package RSLBench.Assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import RSLBench.Constants;
import RSLBench.Comm.Message;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.UtilityMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

/**
 * @see AssignmentInterface
 */
public abstract class DCOPSolver extends AbstractSolver {

    private static final Logger Logger = LogManager.getLogger(DCOPSolver.class);
    private List<DCOPAgent> agents;

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = super.getUsedConfigurationKeys();
        keys.add(Constants.KEY_DCOP_ITERATIONS);
        return keys;
    }

    @Override
    public Assignment compute(UtilityMatrix utility) {
        CommunicationLayer comLayer = new CommunicationLayer();
        long start = System.currentTimeMillis();
        initializeAgents(utility);

        Logger.debug("Starting {} solver.", getIdentifier());
        int totalNccc = 0;
        long bMessages = 0;
        int nMessages = 0;

        int MAX_ITERATIONS = getConfig().getIntValue(Constants.KEY_DCOP_ITERATIONS);
        boolean done = false;
        int iterations = 0;
        while (!done && iterations < MAX_ITERATIONS) {

            // send messages
            for (DCOPAgent agent : agents) {
                Collection<? extends Message> messages = agent.sendMessages(comLayer);
                //collect the byte size of the messages exchanged between agents
                nMessages = nMessages + messages.size();
                for (Message msg : messages) {
                    bMessages += msg.getBytes();
                }
            }

            // receive messages
            for (DCOPAgent agent : agents) {
                agent.receiveMessages(comLayer.retrieveMessages(agent.getAgentID()));
            }

            // improve assignment
            done = true;
            long nccc = 0;
            for (DCOPAgent agent : agents) {
                boolean improved = agent.improveAssignment();
                nccc = Math.max(nccc, agent.getConstraintChecks());
                done = done && !improved;
            }
            totalNccc += nccc;
            iterations++;
        }

        long algBMessages = bMessages;
        int  algNMessages = nMessages;
        for (DCOPAgent agent : agents) {
            nMessages += agent.getNumberOfOtherMessages();
            bMessages += agent.getDimensionOfOtherMessages();
        }

        int  nOtherMessages = nMessages - algNMessages;
        long bOtherMessages = bMessages - algBMessages;
        Logger.debug(Markers.WHITE, "Done with iterations. Needed: " + iterations);

        stats.report("iterations", iterations);
        stats.report("NCCCs", totalNccc);
        stats.report("MessageNum", nMessages);
        stats.report("MessageBytes", bMessages);
        stats.report("OtherNum", nOtherMessages);
        stats.report("OtherBytes", bOtherMessages);

        long time = System.currentTimeMillis() - start;
        Logger.info("{} took {} ms.", getIdentifier(), time);

        // Combine assignments
        Assignment assignments = new Assignment();
        for (DCOPAgent agent : agents) {
            if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID) {
                assignments.assign(agent.getAgentID(), agent.getTargetID());
            }
        }

        Logger.debug("DA Simulator done");

        return assignments;
    }

    /**
     * This method initializes the agents for the simulation (it calls the
     * initialize method of the specific DCOP algorithm used for the
     * computation)
     *
     * @param utilityM: the utility matrix
     */
    private void initializeAgents(UtilityMatrix utilityM) {
        agents = new ArrayList<>();
        for (EntityID agentID : utilityM.getAgents()) {
            DCOPAgent agent = buildAgent();
            // TODO: if required give only local utility matrix to each agent!!!
            agent.initialize(config, agentID, utilityM);
            agents.add(agent);
        }

        Logger.debug(Markers.BLUE, "Initialized " + agents.size() + " agents in " + getIdentifier());
    }

    protected abstract DCOPAgent buildAgent();
}
