package RSLBench.Helpers.Utility;

import RSLBench.Assignment.Assignment;
import RSLBench.Params;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;


/**
 * This class represents a matrix that contains the utility for each
 * agent-target pair that exists in the world model. The utility is calculated
 * only considering the distance between the agent and the target and the
 * burning level of the target (the more recent the fire, the higher the
 * utility).
 *
 */
public class UtilityMatrix {
    private static final Logger Logger = LogManager.getLogger(UtilityMatrix.class);

    private UtilityFunction utilityFunction;
    private ArrayList<EntityID> _agents;
    private ArrayList<EntityID> _targets;
    private StandardWorldModel _world;
    private Assignment lastAssignment;
    HashMap<EntityID, EntityID> _agentLocations;

    /**
     * It creates a utility matrix
     *
     * @param agents a list of agents
     * @param targets a list of targets
     * @param lastAssignment the assignment computed in the last iteration
     * @param agentLocations the agent locations
     * @param world the model of the world
     */
    public UtilityMatrix(ArrayList<EntityID> agents, ArrayList<EntityID> targets, Assignment lastAssignment, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world) {
        _agents = agents;
        _targets = targets;
        _world = world;
        _agentLocations = agentLocations;
        this.lastAssignment = lastAssignment;

        utilityFunction = UtilityFactory.buildFunction();
        utilityFunction.setAgentLocations(agentLocations);
        utilityFunction.setWorld(world);
        Logger.debug("UM has been initialized!");
    }

    /**
     * Reads the utility value for the specified agent and target.
     *
     * @param agentID
     * @param targetID
     * @return the utility value for the specified agent and target.
     */
    public double getUtility(EntityID agentID, EntityID targetID) {
        if (utilityFunction == null) {
            Logger.error("Utility matrix has not been initialized!!");
            System.exit(1);
        }

        double utility = utilityFunction.getUtility(agentID, targetID);
        if (lastAssignment.getAssignment(agentID) == targetID) {
            utility *= Params.HYSTERESIS_FACTOR;
        }
        return utility;
    }

    /**
     * Returns the number of agents in the matrix
     *
     * @return the number of agents considered in the matrix.
     */
    public int getNumAgents() {
        return _agents.size();
    }

    /**
     * Returns the number of targets in the utility matrix
     *
     * @return the number of targets considered in the matrix.
     */
    public int getNumTargets() {
        return _targets.size();
    }

    /**
     * Returns the N targets with the highest utility for the agents
     *
     * @param N: the number of targets to be returned
     * @param agents: the targets are sorted considering, for each target, the
     * utility with the agents in agents
     * @return a list of EntityID of targets ordered by utility value
     */
    public List<EntityID> getNBestTargets(int N, ArrayList<EntityID> agents) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID agent : agents) {
            for (EntityID target : _targets) {
                map.put(target, getUtility(agent, target));
            }
        }
        List<EntityID> res = sortByValue(map);
        ArrayList<EntityID> list = new ArrayList<>();
        int c = 0;
        for (EntityID id : res) {
            list.add(id);
            if (++c >= N) {
                break;
            }
        }
        return list;
    }

    /**
     * Dual of the getNBestTargets method
     *
     * @param N: the number of agents to be returned
     * @param targets: the agents are sorted considering, for each agent, the
     * utility with the targets in targets
     * @return a list of EntityID of agents ordered by utility value
     */
    public List<EntityID> getNBestAgents(int N, ArrayList<EntityID> targets) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID target : targets) {
            for (EntityID agent : _agents) {
                map.put(agent, getUtility(agent, target));
            }
        }
        List<EntityID> res = sortByValue(map);
        ArrayList<EntityID> list = new ArrayList<>();
        int c = 0;
        for (EntityID id : res) {
            list.add(id);
            if (++c >= N) {
                break;
            }
        }
        return list;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /**
     * Sorts the keys according to the doubles
     *
     * @param m - map
     * @return The sorted list
     */
    public static List<EntityID> sortByValue(final Map<EntityID, Double> m) {
        List<EntityID> keys = new ArrayList<>();
        keys.addAll(m.keySet());
        Collections.sort(keys, new Comparator<EntityID>() {
            @Override
            public int compare(EntityID o1, EntityID o2) {
                Double v1 = m.get(o1);
                Double v2 = m.get(o2);
                if (v1 == null) {
                    return (v2 == null) ? 0 : 1;
                } else {
                    return -1 * v1.compareTo(v2);
                }
            }
        });
        return keys;
    }

    /**
     * Returns the target with the highest utility for the agent
     *
     * @param agentID: the agentID
     * @return the targetID
     */
    public EntityID getHighestTargetForAgent(EntityID agentID) {
        double best = -Double.MAX_VALUE;
        EntityID targetID = _targets.get(0);
        for (EntityID t : _targets) {
            if (getUtility(agentID, t) > best) {
                best = getUtility(agentID, t);
                targetID = t;
            }
        }
        return targetID;
    }

    /**
     * Returns an estimate of how many agents are required for a specific
     * target.
     *
     * @param targetID the id of the target
     * @return the amount of agents required or zero if targetID is out of
     * range.
     */
    public int getRequiredAgentCount(EntityID targetID) {
        if (utilityFunction == null) {
            Logger.error("Utility matrix has not been initialized!!");
            System.exit(1);
        }
        
        return utilityFunction.getRequiredAgentCount(targetID);
    }

    /**
     * Returns the whole world model
     *
     * @return the world model
     */
    public StandardWorldModel getWorld() {
        return _world;
    }

    /**
     * Returns the location of the agents
     *
     * @return the locations of the agents
     */
    public HashMap<EntityID, EntityID> getAgentLocations() {
        return _agentLocations;
    }

    /**
     * Returns the considered targets
     *
     * @return the targets
     */
    public ArrayList<EntityID> getTargets() {
        return _targets;
    }

    /**
     * Returns the agents
     *
     * @return the agents
     */
    public ArrayList<EntityID> getAgents() {
        return _agents;
    }
    
}
