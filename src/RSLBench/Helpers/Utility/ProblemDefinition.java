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
 * This class represents the current world status as utilities.
 * 
 * Utilities are calculated using the configured UtilityFunction.
 */
public class ProblemDefinition {
    private static final Logger Logger = LogManager.getLogger(ProblemDefinition.class);

    private UtilityFunction utilityFunction;
    private ArrayList<EntityID> fireAgents;
    private ArrayList<EntityID> policeAgents;
    private ArrayList<EntityID> fires;
    private ArrayList<EntityID> blockades;
    private StandardWorldModel world;
    private Config config;
    HashMap<EntityID, EntityID> _agentLocations;

    // Indexes entities to indices
    Map<EntityID, Integer> id2idx;
    double[][] fireUtilityMatrix;
    double[][] policeUtilityMatrix;

    /**
     * Creates a problem definition
     *
     * @param fireAgents a list of agents
     * @param fires a list of targets
     * @param lastAssignment the assignment computed in the last iteration
     * @param agentLocations the agent locations
     * @param world the model of the world
     */
    public ProblemDefinition(Config config, ArrayList<EntityID> fireAgents,
            ArrayList<EntityID> fires, ArrayList<EntityID> policeAgents,
            ArrayList<EntityID> blockades, Assignment lastAssignment,
            StandardWorldModel world) {
        this.fireAgents = fireAgents;
        this.fires = fires;
        this.policeAgents = policeAgents;
        this.blockades = blockades;
        
        this.world = world;
        this.config = config;

        long initialTime = System.currentTimeMillis();
        utilityFunction = UtilityFactory.buildFunction();
        utilityFunction.setWorld(world);
        utilityFunction.setConfig(config);

        buildFirefightersUtilityMatrix(lastAssignment);
        buildPoliceUtilityMatrix(lastAssignment);

        long elapsedTime = System.currentTimeMillis() - initialTime;
        Logger.debug("Problem definition initialized in {}ms.", elapsedTime);
    }

    /**
     * Build the firefighters (fire brigades to fires) utility matrix.
     *
     * This is necessary because utility functions may not be consistent
     * (they may introduce a small random noise to break ties), whereas the
     * problem repoted utilities must stay consistent.
     */
    private void buildFirefightersUtilityMatrix(Assignment lastAssignment) {
        final int nAgents = fireAgents.size();
        final int nTargets = fires.size();
        id2idx = new HashMap<>(nAgents+nTargets);
        fireUtilityMatrix = new double[nAgents][nTargets];
        for (int i=0; i<nAgents; i++) {
            final EntityID agent = fireAgents.get(i);
            id2idx.put(agent, i);

            for (int j=0; j<nTargets; j++) {
                final EntityID target = fires.get(j);
                if (i == 0) {
                    id2idx.put(target, j);
                }

                double utility = utilityFunction.getFireUtility(agent, target);

                // Apply hysteresis factor if configured
                if (lastAssignment.getAssignment(agent).equals(target)) {
                    utility *= config.getFloatValue(Constants.KEY_UTIL_HYSTERESIS);
                }

                // Set a cap on max utility
                if (Double.isInfinite(utility)) {
                    utility = 1e15;
                }

                fireUtilityMatrix[i][j] = utility;
            }
        }
    }

    private void buildPoliceUtilityMatrix(Assignment lastAssignment) {
        final int nAgents = policeAgents.size();
        final int nTargets = blockades.size();
        id2idx = new HashMap<>(nAgents+nTargets);
        policeUtilityMatrix = new double[nAgents][nTargets];
        for (int i=0; i<nAgents; i++) {
            final EntityID agent = policeAgents.get(i);
            id2idx.put(agent, i);

            for (int j=0; j<nTargets; j++) {
                final EntityID target = blockades.get(j);
                if (i == 0) {
                    id2idx.put(target, j);
                }

                double utility = utilityFunction.getPoliceUtility(agent, target);

                // Apply hysteresis factor if configured
                if (lastAssignment.getAssignment(agent).equals(target)) {
                    utility *= config.getFloatValue(Constants.KEY_UTIL_HYSTERESIS);
                }

                // Set a cap on max utility
                if (Double.isInfinite(utility)) {
                    utility = 1e15;
                }

                fireUtilityMatrix[i][j] = utility;
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
        final int i = id2idx.get(agentID);
        final int j = id2idx.get(targetID);
        return fireUtilityMatrix[i][j];
    }

    /**
     * Returns the number of fire brigade agents in the matrix
     *
     * @return the number of firie brigade agents considered in the matrix.
     */
    public int getNumFireAgents() {
        return fireAgents.size();
    }

    /**
     * Returns the number of fires in the problem.
     *
     * @return the number of fires.
     */
    public int getNumFires() {
        return fires.size();
    }

    /**
     * Returns the N fires with the highest utility for the given agent.
     *
     * @param N: the number of targets to be returned
     * @param fireAgent: the agent considered
     * @return a list of EntityID of targets ordered by utility value
     */
    public List<EntityID> getNBestFires(int N, EntityID fireAgent) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID target : fires) {
            map.put(target, getUtility(fireAgent, target));
        }
        List<EntityID> res = sortByValue(map);
        ArrayList<EntityID> list = new ArrayList<>();
        for (int i=0, len=res.size(); i<N && i<len; i++) {
            list.add(res.get(i));
        }
        return list;
    }

    /**
     * Dual of the getNBestFires method
     *
     * @param N: the number of fire agents to be returned.
     * @param fire: the fire being considered.
     * @return a list of fire agents EntityIDs ordered by utility value
     */
    public List<EntityID> getNBestFireAgents(int N, EntityID fire) {
        Map<EntityID, Double> map = new HashMap<>();
        for (EntityID agent : fireAgents) {
            map.put(agent, getUtility(agent, fire));
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
        EntityID targetID = fires.get(0);
        for (EntityID t : fires) {
            if (getUtility(agentID, t) > best) {
                best = getUtility(agentID, t);
                targetID = t;
            }
        }
        return targetID;
    }

    /**
     * Returns an estimate of how many agents are required for a specific
     * fire.
     *
     * @param fire the id of the target
     * @return the amount of agents required or zero if targetID is out of
     * range.
     */
    public int getRequiredAgentCount(EntityID fire) {
        if (utilityFunction == null) {
            Logger.error("Utility matrix has not been initialized!!");
            System.exit(1);
        }

        return utilityFunction.getRequiredAgentCount(fire);
    }

    /**
     * Returns the utility penalty incurred when the given number of agents
     * are assigned to the given fire.
     *
     * @param fire target assigned to some agents
     * @param nAgents number of agents assigned to that target
     * @return utility penalty incurred by this assignment
     */
    public double getUtilityPenalty(EntityID fire, int nAgents) {
        int maxAgents = getRequiredAgentCount(fire);
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
        return world;
    }

    /**
     * Returns the location of the agents
     *
     * @return the locations of the agents
     */
    public HashMap<EntityID, EntityID> getFireAgentLocations() {
        return _agentLocations;
    }

    /**
     * Returns the considered targets
     *
     * @return the targets
     */
    public ArrayList<EntityID> getFires() {
        return fires;
    }

    /**
     * Returns the agents
     *
     * @return the agents
     */
    public ArrayList<EntityID> getFireAgents() {
        return fireAgents;
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
        for (EntityID agent : fireAgents) {
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
        for (EntityID agent : fireAgents) {
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
        for (EntityID target : fires) {
            count += getRequiredAgentCount(target);
        }
        return count;
    }

}
