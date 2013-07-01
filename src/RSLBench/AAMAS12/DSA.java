package RSLBench.AAMAS12;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DecentralAssignment;
import RSLBench.Comm.AbstractMessage;
import RSLBench.Comm.AssignmentMessage;
import RSLBench.Comm.ComSimulator;
import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.UtilityMatrix;
import RSLBench.Helpers.SimpleID;
import RSLBench.Params;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.standard.entities.StandardWorldModel;
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
    protected Collection<AbstractMessage> _neighborAssignments = null;
    protected TargetScores _targetScores = null;
    protected static Random _random;
    private int _nccc = 0;
    // Communication
    private int maxCommunicationRange = Params.SIMULATED_COMMUNICATION_RANGE;
    private Map<EntityID, Set<EntityID>> inRange;

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

        inRange = new HashMap<>();
        inRange.put(_agentID, new HashSet<EntityID>());
        this.update();
    }

    @Override
    public boolean improveAssignment() {

        if (Logger.isDebugEnabled()) {
            Logger.debug(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] improveAssignment");
            Logger.debug(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] received neighbor messages: "
                    + _neighborAssignments.size());
        }

        _targetScores.resetAssignments();
        for (AbstractMessage message : _neighborAssignments) {
            if (message.getClass() == AssignmentMessage.class) {
                _targetScores.increaseAgentCount(((AssignmentMessage) message).getTargetID());
            }
        }
        _neighborAssignments.clear();

        // Find the best target given utilities and constraints
        double bestScore;
        try {
            bestScore = _targetScores.computeScore(_targetID, _utilityM.getUtility(_agentID, _targetID));
        } catch (NullPointerException n) {
            bestScore = Double.NEGATIVE_INFINITY;
        }
        EntityID bestTarget = _targetID;
        //Logger.debugColor(Markers.LIGHT_BLUE, "["+ _agentID +"]  BEFORE -> target: " + _targetID +" score: "+bestScore);
        _nccc = 0;
        for (EntityID t : _utilityM.getTargets()) {
            double score = _targetScores.computeScore(t, _utilityM.getUtility(_agentID, t));
            if (score > bestScore) {
                bestScore = score;
                bestTarget = t;
            }
            _nccc++;
        }

        if (Logger.isDebugEnabled()) {
            Logger.debug(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "]  AFTER -> target: " + bestTarget.getValue()
                    + " score: " + bestScore + " " + bestScore);
        }

        if (bestTarget != _targetID) {
            Logger.debug(Markers.LIGHT_BLUE, "[" + SimpleID.conv(_agentID) + "] assignment can be improved");
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
    public int getNccc() {
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
    public Collection<AbstractMessage> sendMessages(ComSimulator com) {
        Collection<AbstractMessage> messages = new ArrayList<AbstractMessage>();
        Collection<AbstractMessage> totMessages = new ArrayList<AbstractMessage>();
        AssignmentMessage mex = new AssignmentMessage(_agentID, _targetID);
        messages.add(mex);

        Set<EntityID> neighbors = inRange.get(_agentID);
        for (EntityID neighborID : neighbors) {
            totMessages.add(mex);
            com.send(neighborID, messages);
        }


        return totMessages;
    }

    @Override
    public void receiveMessages(Collection<AbstractMessage> messages) {
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

    public void update() {

        // Initialize Clusters with minimal distance
        List<EntityID> agents = new ArrayList<>();
        //agents.add(_agentID);
        agents.addAll(_utilityM.getAgents());
        ArrayList<ArrayList<EntityID>> clusters = buildAgentPairs(agents, _utilityM.getAgentLocations(), _utilityM.getWorld());
        for (EntityID id : agents) {
            // delete old assignments
            inRange.get(_agentID).clear();
            // find cluster
            boolean found = false;
            for (ArrayList<EntityID> c : clusters) {
                if (c.contains(_agentID)) {
                    inRange.get(_agentID).addAll(c);
                    found = true;
                }
            }
            if (!found) {
                Logger.debug(Markers.RED, "ERROR could not find cluster of agent " + _agentID);
            }

            // Just trace information
            if (Logger.isTraceEnabled()) {
                StringBuilder buf = new StringBuilder();
                buf.append("Agent ").append(SimpleID.conv(_agentID))
                        .append("\" has the following group of \"")
                        .append(inRange.get(_agentID).size())
                        .append(" neighbors : \n");
                for (EntityID a : inRange.get(_agentID)) {
                    buf.append(SimpleID.conv(a)).append(",");
                }
                Logger.trace(Markers.GREEN, buf.toString());
            }
        }
    }

    private ArrayList<ArrayList<EntityID>> buildAgentPairs(List<EntityID> agents, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world) {
        ArrayList<ArrayList<EntityID>> clusters = new ArrayList<ArrayList<EntityID>>();
        Set<EntityID> assigned = new HashSet<EntityID>();

        Logger.debug(Markers.GREEN, "CLUSTERING START");
        while (true) {
            int minDist = Integer.MAX_VALUE;
            EntityID bestA = new EntityID(-1), bestB = new EntityID(-1);
            boolean found = false;
            for (EntityID id1 : agents) {
                if (assigned.contains(id1)) {
                    continue;
                }
                for (EntityID id2 : agents) {
                    if (assigned.contains(id2)) {
                        continue;
                    }
                    if (id1 == id2) {
                        continue;
                    }
                    int distance = world.getDistance(agentLocations.get(id1), agentLocations.get(id2));
                    if (distance <= maxCommunicationRange && distance < minDist) {
                        minDist = distance;
                        bestA = id1;
                        bestB = id2;
                        found = true;
                    }
                }
            }
            if (found) {
                ArrayList<EntityID> cl = new ArrayList<EntityID>();
                cl.add(bestA);
                cl.add(bestB);
                assigned.add(bestA);
                assigned.add(bestB);
                clusters.add(cl);
                //Logger.debugColor("Adding pair " + SimpleID.conv(bestA) + " --- " + SimpleID.conv(bestB) + " d= " + minDist, Logger.BG_RED);
            } else {
                break;
            }
        }

        // Merge Clusters
        while (clusters.size() > 1) {
            boolean merged = false;
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = 0; j < clusters.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    ArrayList<EntityID> c1 = clusters.get(i);
                    ArrayList<EntityID> c2 = clusters.get(j);
                    if (clustersInRange(c1, c2, agentLocations, world)) {
                        ArrayList<EntityID> newC = clustersMerge(c1, c2);
                        clusters.remove(i);
                        clusters.remove(--j);
                        clusters.add(newC);
                        merged = true;
                        break;
                    }
                }
                if (merged) {
                    break;
                }
            }
            if (!merged) {
                break;
            }
        }


        // Show Clusters
        if (Logger.isDebugEnabled()) {
            int count = 0;
            for (ArrayList<EntityID> c : clusters) {
                StringBuilder buf = new StringBuilder("Cluster ");
                String prefix = "";
                buf.append(count++).append(": ");
                for (EntityID id : c) {
                    buf.append(prefix);
                    prefix = ", ";
                    buf.append(id);
                }
                Logger.debug(buf.toString());
            }
            Logger.debug(Markers.GREEN, "CLUSTERING END");
        }


        return clusters;
    }

    private boolean clustersInRange(ArrayList<EntityID> c1, ArrayList<EntityID> c2, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world) {
        for (EntityID id1 : c1) {
            for (EntityID id2 : c2) {
                int distance = world.getDistance(agentLocations.get(id1), agentLocations.get(id2));
                if (distance > maxCommunicationRange) {
                    return false;
                }
            }
        }
        return true;
    }

    private ArrayList<EntityID> clustersMerge(ArrayList<EntityID> c1, ArrayList<EntityID> c2) {
        ArrayList<EntityID> result = new ArrayList<>();
        for (EntityID id1 : c1) {
            result.add(id1);
        }
        for (EntityID id2 : c2) {
            result.add(id2);
        }
        return result;
    }
}
