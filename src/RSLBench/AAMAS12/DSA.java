package RSLBench.AAMAS12;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DecentralAssignment;
import RSLBench.Comm.Message;
import RSLBench.Comm.AssignmentMessage;
import RSLBench.Comm.ComSimulator;
import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.UtilityMatrix;
import RSLBench.Helpers.SimpleID;
import RSLBench.Params;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

/**
 * Class that implements the DSA algorithm according to the RMASBench
 * specification.
 */
public class DSA implements DecentralAssignment {

    private static final Logger Logger = LogManager.getLogger(DSA.class);
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(DSA.class.getName());
    protected UtilityMatrix _utilityM = null;
    protected EntityID _agentID;
    protected EntityID _targetID;
    protected Collection<Message> _neighborAssignments = null;
    protected TargetScores _targetScores = null;
    protected static Random _random;
    private int _nccc = 0;
    private Set<EntityID> neighbors;

    public DSA() {
        _random = new Random(0);
    }

    @Override
    public void initialize(EntityID agentID, UtilityMatrix utilityM) {
        _agentID = agentID;
        _utilityM = utilityM;
        _targetScores = new TargetScores();
        _targetID = Assignment.UNKNOWN_TARGET_ID;

        Logger.debug(Markers.LIGHT_BLUE, "A [" + SimpleID.conv(agentID) + "] initializing with " + _utilityM.getNumTargets() + " targets.");

        // Find the target with the highest utility and initialize required agents for each target 
        double bestTargetUtility = 0;
        for (EntityID t : _utilityM.getTargets()) {
            _targetScores.initializeTarget(t, _utilityM.getRequiredAgentCount(t));
            double util = _utilityM.getUtility(agentID, t);
            if (bestTargetUtility < util) {
                bestTargetUtility = util;
                _targetID = t;
            }
        }

        Logger.debug(Markers.LIGHT_BLUE, "A [" + SimpleID.conv(agentID) + "] init done!");
        neighbors = new HashSet<>(_utilityM.getAgents());
    }

    @Override
    public boolean improveAssignment() {
        _nccc = 0;

        if (Logger.isDebugEnabled()) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] improveAssignment");
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] received neighbor messages: "
                    + _neighborAssignments.size());
        }

        _targetScores.resetAssignments();
        for (Message message : _neighborAssignments) {
            if (message.getClass() == AssignmentMessage.class) {
                _targetScores.increaseAgentCount(((AssignmentMessage) message).getTargetID());
                _nccc++;
            }
        }
        _neighborAssignments.clear();

        // Find the best target given utilities and constraints
        double bestScore;
        try {
            bestScore = _targetScores.computeScore(_targetID, _utilityM.getUtility(_agentID, _targetID));
            _nccc++;
        } catch (NullPointerException n) {
            bestScore = Double.NEGATIVE_INFINITY;
        }
        EntityID bestTarget = _targetID;
        //Logger.debugColor(Markers.LIGHT_BLUE, "["+ _agentID +"]  BEFORE -> target: " + _targetID +" score: "+bestScore);
        for (EntityID t : _utilityM.getTargets()) {
            double score = _targetScores.computeScore(t, _utilityM.getUtility(_agentID, t));
            if (score > bestScore) {
                bestScore = score;
                bestTarget = t;
            }
            _nccc++;
        }

        if (Logger.isTraceEnabled()) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "]  AFTER -> target: " + bestTarget.getValue()
                    + " score: " + bestScore + " " + bestScore);
        }

        if (bestTarget != _targetID) {
            Logger.trace(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] assignment can be improved");
            // improvement possible
            if (_random.nextDouble() <= Params.DSA_CHANGE_VALUE_PROBABILITY) {
                Logger.debug(Markers.GREEN, "[" + SimpleID.conv(_agentID) + "] assignment improved");
                // change target
                _targetID = bestTarget;
                return true;
            }
        }
        return false;
    }

    @Override
    public long getConstraintChecks() {
        return _nccc;
    }

    @Override
    public EntityID getAgentID() {
        return _agentID;
    }

    @Override
    public EntityID getTargetID() {
        return _targetID;
    }

    @Override
    public Collection<Message> sendMessages(ComSimulator com) {
        Collection<Message> messages = new ArrayList<>();
        Collection<Message> totMessages = new ArrayList<>();
        AssignmentMessage mex = new AssignmentMessage(_agentID, _targetID);
        messages.add(mex);

        for (EntityID neighborID : neighbors) {
            totMessages.add(mex);
            com.send(neighborID, messages);
        }


        return totMessages;
    }

    @Override
    public void receiveMessages(Collection<Message> messages) {
        _neighborAssignments = messages;
    }

    @Override
    public int getNumberOfOtherMessages() {
        return 0;
    }

    @Override
    public long getDimensionOfOtherMessages() {
        return 0;
    }

}
