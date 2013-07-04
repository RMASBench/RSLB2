package RSLBench.Assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import RSLBench.Params;
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
public class DecentralizedAssignmentSimulator implements AssignmentInterface {

    private static final Logger Logger = LogManager.getLogger(DecentralizedAssignmentSimulator.class);
    private String _className;
    private List<DecentralAssignment> _simulatedAgents;
    private long _messagesInBytes;
    private int _averageNccc = 0;
    private int _nMessages;
    private int _nOtherMessages;

    public DecentralizedAssignmentSimulator(String className) {
        _className = className;
    }

    @Override
    public Assignment compute(UtilityMatrix utility) {
        CommunicationLayer comLayer = new CommunicationLayer();
        long start = System.currentTimeMillis();
        initializeAgents(utility);

        Logger.debug("starting DA Simulator");
        boolean done = false;
        int iterations = 0;
        int totalNccc = 0;
        int initialConflicts = 0;
        int finalConflicts;
        int MAX_ITERATIONS = Params.MAX_ITERATIONS;
        long byteMessage = 0;
        int assignmentMessages = 0;
        while (!done && iterations < MAX_ITERATIONS) {
            // statistics
            if (iterations == 1) {
                initialConflicts = countConflicts(utility);
            }

            // send messages
            for (DecentralAssignment agent : _simulatedAgents) {
                Collection<Message> messages = agent.sendMessages(comLayer);
                //collect the byte size of the messages exchanged between agents
                assignmentMessages = assignmentMessages + messages.size();
                for (Message msg : messages) {
                    byteMessage += msg.getBytes();
                }
            }

            // receive messages
            for (DecentralAssignment agent : _simulatedAgents) {
                agent.receiveMessages(comLayer.retrieveMessages(agent.getAgentID()));
            }

            // improve assignment
            done = true;
            long nccc = 0;
            for (DecentralAssignment agent : _simulatedAgents) {
                boolean improved = agent.improveAssignment();
                nccc = Math.max(nccc, agent.getConstraintChecks());
                done = done && !improved;
            }
            totalNccc += nccc;
            iterations++;
        }
        _averageNccc = totalNccc / iterations;

        this._messagesInBytes = byteMessage;
        this._nMessages = assignmentMessages;
        for (DecentralAssignment agent : _simulatedAgents) {
            this._nMessages += agent.getNumberOfOtherMessages();
            this._messagesInBytes += agent.getDimensionOfOtherMessages();
        }

        this._nOtherMessages = this._nMessages - assignmentMessages;
        Logger.debug(Markers.WHITE, "Done with iterations. Needed: " + iterations);

        long end = System.currentTimeMillis();
        Logger.info("Total computation time for " + _className + " was " + (end - start) + " ms.");


        finalConflicts = countConflicts(utility);
        Logger.info("DA Simulation complete initial conflicts = " + initialConflicts
                + " final conflicts = " + finalConflicts);

        // Combine assignments
        Assignment assignments = new Assignment();
        for (DecentralAssignment agent : _simulatedAgents) {
            if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID) {
                assignments.assign(agent.getAgentID(), agent.getTargetID());
            }
        }

        Logger.debug("DA Simulator done");

        return assignments;
    }

    /**
     * It counts the violated constraints for all agents. The constraints are
     * violated when a target is assigned to more or less agents then required.
     *
     * @param utilityM: the utility matrix
     * @return the number of total conflicts
     */
    private int countConflicts(UtilityMatrix utilityM) {
        Assignment ass = new Assignment();
        for (DecentralAssignment agent : _simulatedAgents) {
            if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID) {
                ass.assign(agent.getAgentID(), agent.getTargetID());
            }
        }
        int conflicts = 0;
        for (EntityID t : utilityM.getTargets()) {
            conflicts += Math.abs(ass.getTargetSelectionCount(t) - utilityM.getRequiredAgentCount(t));
        }
        return conflicts;
    }

    /**
     * This method initializes the agents for the simulation (it calls the
     * initialize method of the specific DCOP algorithm used for the
     * computation)
     *
     * @param utilityM: the utility matrix
     */
    private void initializeAgents(UtilityMatrix utilityM) {
        // initialize simulated agents
        _simulatedAgents = new ArrayList<>();
        try {
            Class<?> daClass = Class.forName(_className);
            for (EntityID agentID : utilityM.getAgents()) {
                DecentralAssignment agent = (DecentralAssignment) daClass.newInstance();
                // TODO: if required give only local utility matrix to each agent!!!
                agent.initialize(agentID, utilityM);
                _simulatedAgents.add(agent);
            }
        } catch (ClassNotFoundException e) {
            Logger.error(Markers.RED, "SolverClass could not be found: " + _className, e);
        } catch (InstantiationException e) {
            Logger.error(Markers.RED, "SolverClass " + _className + "could not be isntantiated.", e);
        } catch (IllegalAccessException e) {
            Logger.error(Markers.RED, "SolverClass " + _className + " must have an empty constructor.", e);
        }
        Logger.debug(Markers.BLUE, "Initialized " + _simulatedAgents.size() + " agents of class " + _className);
    }

    @Override
    public int getTotalMessages() {
        return this._nMessages;
    }

    @Override
    public long getTotalMessagesBytes() {
        return this._messagesInBytes;
    }

    @Override
    public int getOtherMessages() {
        return this._nOtherMessages;
    }

    @Override
    public int getAverageNccc() {
        return this._averageNccc;
    }
}
