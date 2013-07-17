package RSLBench.Helpers.Utility;

import RSLBench.Assignment.Assignment;
import RSLBench.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.config.Config;

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
    private Config config;
    HashMap<EntityID, EntityID> _agentLocations;

    // Indexes and matrix of the utility noises
    Map<EntityID, Integer> agent2idx;
    Map<EntityID, Integer> target2idx;
    double[][] utilityMatrix;

    /**
     * It creates a utility matrix
     *
     * @param agents a list of agents
     * @param targets a list of targets
     * @param lastAssignment the assignment computed in the last iteration
     * @param agentLocations the agent locations
     * @param world the model of the world
     */
    public UtilityMatrix(Config config, ArrayList<EntityID> agents, ArrayList<EntityID> targets, Assignment lastAssignment, StandardWorldModel world) {
        _agents = agents;
        _targets = targets;
        _world = world;
        this.config = config;

        utilityFunction = UtilityFactory.buildFunction();
        utilityFunction.setWorld(world);
        utilityFunction.setConfig(config);
        Logger.debug("UM has been initialized!");

        // Build the utility matrix. This is necessary because utility functions
        // may not be consistent (they may introduce a small random noise to
        // break ties)
        final int nAgents = agents.size();
        final int nTargets = targets.size();
        agent2idx = new HashMap<>(nAgents);
        target2idx = new HashMap<>(nTargets);
        utilityMatrix = new double[nAgents][nTargets];
        for (int i=0; i<nAgents; i++) {
            final EntityID agent = agents.get(i);
            agent2idx.put(agent, i);

            for (int j=0; j<nTargets; j++) {
                final EntityID target = targets.get(j);
                if (i == 0) {
                    target2idx.put(target, j);
                }

                double utility = utilityFunction.getUtility(agent, target);

                // Apply hysteresis factor if configured
                if (lastAssignment.getAssignment(agent).equals(target)) {
                    utility *= config.getFloatValue(Constants.KEY_UTIL_HYSTERESIS);
                }
                // Set a cap on max utility
                if (Double.isInfinite(utility)) {
                    utility = 1e15;
                }
                
                utilityMatrix[i][j] = utility;
            }
        }
    }

    /**
     * Reads the utility value for the specified agent and target.
     *
     * @param agentID
     * @param targetID
     * @return the utility value for the specified agent and target.
     */
    public double getUtility(EntityID agentID, EntityID targetID) {
        final int i = agent2idx.get(agentID);
        final int j = target2idx.get(targetID);
        return utilityMatrix[i][j];
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
    public List<EntityID> getNBestTargets(int N, EntityID agent) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID target : _targets) {
            map.put(target, getUtility(agent, target));
        }
        List<EntityID> res = sortByValue(map);
        ArrayList<EntityID> list = new ArrayList<>();
        for (int i=0, len=res.size(); i<N && i<len; i++) {
            list.add(res.get(i));
        }
        return list;
    }

    /**
     * Dual of the getNBestTargets method
     *
     * @param N: the number of agents to be returned
     * @param targets: the agents are sorted considering, for each agent, the
     * utility with the targets in targets.
     * @TODO Actually single target: refactor!
     * @return a list of EntityID of agents ordered by utility value
     */
    public List<EntityID> getNBestAgents(int N, EntityID target) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID agent : _agents) {
            map.put(agent, getUtility(agent, target));
        }
        List<EntityID> res = sortByValue(map);
        ArrayList<EntityID> list = new ArrayList<>();
        for (int i=0, len=res.size(); i<N && i<len; i++) {
            list.add(res.get(i));
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
     * Returns the utility penalty incurred when the given number of agents
     * are assigned to the given target.
     *
     * @param target target assigned to some agents
     * @param nAgents number of agents assigned to that target
     * @return utility penalty incurred by this assignment
     */
    public double getUtilityPenalty(EntityID target, int nAgents) {
        int maxAgents = getRequiredAgentCount(target);
        if (maxAgents >= nAgents) {
            return 0;
        }
        return config.getFloatValue(Constants.KEY_UTIL_K) *
                Math.pow(nAgents-maxAgents, config.getFloatValue(Constants.KEY_UTIL_ALPHA));
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

    /**
     * Get the utility obtained by the given solution.
     * 
     * @param solution solution to evaluate.
     * @return utility obtained by this solution.
     */
    public double getUtility(Assignment solution) {
        if (solution == null) {
            return Double.NaN;
        }

        double utility = 0;

        HashMap<EntityID, Integer> nAgentsPerTarget = new HashMap<>();
        for (EntityID agent : _agents) {
            EntityID target = solution.getAssignment(agent);
            utility += getUtility(agent, target);

            // Add 1 to the target count
            int nAgents = nAgentsPerTarget.containsKey(target)
                    ? nAgentsPerTarget.get(target) : 0;
            nAgentsPerTarget.put(target, nAgents+1);
        }

        // Check violated constraints
        for (EntityID target : nAgentsPerTarget.keySet()) {
            int assigned = nAgentsPerTarget.get(target);
            utility -= getUtilityPenalty(target, assigned);
        }

        return utility;
    }

    /**
     * Get the number of violated constraints in this solution.
     *
     * @param solution solution to evaluate.
     * @return number of violated constraints.
     */
    public int getViolations(Assignment solution) {
        int count = 0;
        
        HashMap<EntityID, Integer> nAgentsPerTarget = new HashMap<>();
        for (EntityID agent : _agents) {
            EntityID target = solution.getAssignment(agent);
            int nAgents = nAgentsPerTarget.containsKey(target)
                    ? nAgentsPerTarget.get(target) : 0;
            nAgentsPerTarget.put(target, nAgents+1);
        }

        // Check violated constraints
        for (EntityID target : nAgentsPerTarget.keySet()) {
            int assigned = nAgentsPerTarget.get(target);
            int max = getRequiredAgentCount(target);
            if (assigned > max) {
                Logger.warn("Violation! Target {} needs {} agents, got {}", target, max, assigned);
                count += assigned - max;
            }
        }

        return count;
    }

    /**
     * Get the total maximum number of agents allocable to targets.
     * 
     * This is used as a check to see if a problem can or can't be solved
     * without violating any constraints
     * @return total number of agents that can be allocated without conflicts.
     */
    public int getTotalMaxAgents() {
        int count = 0;
        for (EntityID target : _targets) {
            count += getRequiredAgentCount(target);
        }
        return count;
    }
    
}
