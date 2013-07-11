package RSLBench.Assignment;

import RSLBench.Algorithms.DSA.TargetScores;
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
    private List<Double> utilities;

    public DCOPSolver() {
        utilities = new ArrayList<>();
    }

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
        Assignment finalAssignment = null, bestAssignment = null;
        double bestAssignmentUtility = Double.NEGATIVE_INFINITY;
        while (!done && iterations < MAX_ITERATIONS) {
            finalAssignment = new Assignment();

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

            // try to improve assignment
            done = true;
            long nccc = 0;
            for (DCOPAgent agent : agents) {
                boolean improved = agent.improveAssignment();
                nccc = Math.max(nccc, agent.getConstraintChecks());
                done = done && !improved;

                // Collect assignment
                if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID) {
                    finalAssignment.assign(agent.getAgentID(), agent.getTargetID());
                }
            }

            // Collect the best assignment visited
            double assignmentUtility = utility.getUtility(finalAssignment);
            if (assignmentUtility > bestAssignmentUtility) {
                bestAssignmentUtility = assignmentUtility;
                bestAssignment = finalAssignment;
            }

            totalNccc += nccc;
            iterations++;
            utilities.add(utility.getUtility(finalAssignment));
        }

        // Run sequential value propagation to make the solution consistent
        Assignment finalGreedy = greedyImprovement(utility, finalAssignment, false);
        double finalAssignmentU = utility.getUtility(finalAssignment);
        double finalGreedyU = utility.getUtility(finalGreedy);
        if (finalAssignmentU > finalGreedyU) {
            Logger.error("Final assignment utility went from {} to {}",
                    finalAssignmentU, finalGreedyU);
            greedyImprovement(utility, bestAssignment, true);
        }
        stats.report("final", finalAssignmentU);
        stats.report("final_greedy", finalGreedyU);

        Assignment bestGreedy = greedyImprovement(utility, bestAssignment, false);
        double bestAssignmentU = utility.getUtility(bestAssignment);
        double bestGreedyU = utility.getUtility(bestGreedy);
        if (bestAssignmentU > bestGreedyU) {
            Logger.error("Greedy improvement lowered utility from {} to {}",
                    bestAssignmentU, bestGreedyU);
            greedyImprovement(utility, bestGreedy, true);
        }
        stats.report("best", bestAssignmentU);
        stats.report("best_greedy", bestGreedyU);
        

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
        reportUtilities();

        long time = System.currentTimeMillis() - start;
        Logger.info("{} took {} ms.", getIdentifier(), time);
        Logger.debug("DA Simulator done");

        return bestGreedy;
    }

    private void reportUtilities() {
        StringBuilder buf = new StringBuilder();
        String prefix = "";
        for (double utility : utilities) {
            buf.append(prefix).append(utility);
            prefix = ",";
        }
        stats.report("utilities", buf.toString());
        utilities.clear();
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

    /**
     * Operate on the (sequential) greedy algorithm.
     *
     * This gives the agent an opportunity to orderly reconsider their choices.
     *
     * @param initial current assignment.
     */
    public Assignment greedyImprovement(UtilityMatrix utility, 
            Assignment initial, boolean debug)
    {
        Assignment assignment = new Assignment(initial);
        for (DCOPAgent agent : agents) {
            final EntityID agentID = agent.getAgentID();

            // Consider previous assignments
            TargetScores scores = new TargetScores(agentID, utility);
            for (EntityID a : assignment.getAgents()) {
                if (!a.equals(agentID)) {
                    scores.increaseAgentCount(assignment.getAssignment(a));
                }
            }

            // Compute the best target for ourselves
            EntityID bestTarget = assignment.getAssignment(agentID);
            double bestScore = scores.computeScore(bestTarget);
            for (EntityID t : utility.getTargets()) {
                double score = scores.computeScore(t);
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = t;
                }
            }

            EntityID initialTarget = initial.getAssignment(agentID);
            if (debug && !bestTarget.equals(initialTarget)) {
                Logger.warn("Agent {} switch: {} ({}) -> {} ({})", agentID,
                        initialTarget, scores.computeScore(initialTarget),
                        bestTarget, scores.computeScore(bestTarget));
            }
            
            assignment.assign(agent.getAgentID(), bestTarget);
        }

        if (debug) {
            double initialU = utility.getUtility(initial);
            double finalU   = utility.getUtility(assignment);
            Logger.error("Got {} before greedy, deteriorated to {} afterwards!",
                    initialU, finalU);
        }

        return assignment;
    }
    
}
