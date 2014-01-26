package RSLBench.Algorithms.DSA;

import RSLBench.Assignment.DCOP.TargetScores;
import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Comm.Message;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.ProblemDefinition;
import RSLBench.Helpers.SimpleID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.config.Config;

import rescuecore2.worldmodel.EntityID;

/**
 * Class that implements the DSA algorithm according to the RMASBench
 * specification.
 */
public class DSAAgent implements DCOPAgent {

    private static final Logger Logger = LogManager.getLogger(DSAAgent.class);
    private ProblemDefinition problem = null;
    private EntityID id;
    private EntityID targetID;
    private Collection<Message> neighborAssignments = null;
    private TargetScores targetScores = null;
    private int nCCCs = 0;
    private Set<EntityID> neighbors;
    private Config config;

    @Override
    public void initialize(Config config, EntityID agentID, ProblemDefinition utilityM) {
        id = agentID;
        problem = utilityM;
        targetScores = new TargetScores(agentID, utilityM);
        targetID = Assignment.UNKNOWN_TARGET_ID;
        this.config = config;

        Logger.debug(Markers.LIGHT_BLUE, "A [" + SimpleID.conv(agentID) + "] initializing with " + problem.getNumFires() + " targets.");

        // Find the target with the highest utility and initialize required agents for each target
        double bestTargetUtility = Double.NEGATIVE_INFINITY;
        for (EntityID t : problem.getFires()) {
            double util = problem.getFireUtility(agentID, t);
            if (bestTargetUtility < util) {
                bestTargetUtility = util;
                targetID = t;
            }
        }

        // The neighbors of this agent are all candidates of all eligible fires
        neighbors = new HashSet<>();
        for (EntityID fire : problem.getFireAgentNeighbors(id)) {
            neighbors.addAll(problem.getFireNeighbors(fire));
        }
        neighbors.remove(id);

        Logger.debug(Markers.LIGHT_BLUE, "A [" + SimpleID.conv(agentID) + "] init done!");
    }

    @Override
    public boolean improveAssignment() {
        nCCCs = 0;

        if (Logger.isDebugEnabled()) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(id) + "] improveAssignment");
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(id) + "] received neighbor messages: "
                    + neighborAssignments.size());
        }

        targetScores.resetAssignments();
        for (Message message : neighborAssignments) {
            if (message.getClass() == AssignmentMessage.class) {
                targetScores.increaseAgentCount(((AssignmentMessage) message).getTargetID());
                nCCCs++;
            }
        }
        neighborAssignments.clear();

        // Find the best target given utilities and constraints
        double bestScore = Double.NEGATIVE_INFINITY;
        EntityID bestTarget = null;
        //Logger.debugColor(Markers.LIGHT_BLUE, "["+ _agentID +"]  BEFORE -> target: " + _targetID +" score: "+bestScore);
        for (EntityID t : problem.getFireAgentNeighbors(id)) {
            double score = targetScores.computeScore(t);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = t;
            }
            nCCCs++;
        }

        if (Logger.isTraceEnabled()) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(id) + "]  AFTER -> target: " + bestTarget.getValue()
                    + " score: " + bestScore + " " + bestScore);
        }
        if (bestTarget == null) {
            bestTarget = problem.getHighestTargetForAgent(id);
        }

        if (bestTarget != targetID) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(id) + "] assignment can be improved");
            // improvement possible
            if (config.getRandom().nextDouble() <= config.getFloatValue(DSA.KEY_DSA_PROBABILITY)) {
                Logger.debug(Markers.GREEN, "[" + SimpleID.conv(id) + "] assignment improved");
                // change target
                targetID = bestTarget;
            }
            return true;
        }
        return false;
    }

    @Override
    public long getConstraintChecks() {
        return nCCCs;
    }

    @Override
    public EntityID getID() {
        return id;
    }

    @Override
    public EntityID getTarget() {
        return targetID;
    }

    @Override
    public Collection<Message> sendMessages(CommunicationLayer com) {
        Collection<Message> sentMessages = new ArrayList<>();
        AssignmentMessage mex = new AssignmentMessage(id, targetID);

        for (EntityID neighborID : neighbors) {
            sentMessages.add(mex);
            com.send(neighborID, mex);
        }

        return sentMessages;
    }

    @Override
    public void receiveMessages(Collection<Message> messages) {
        neighborAssignments = messages;
    }

}
