package RSLBench.Helpers.Utility;

import RSLBench.Assignment.Assignment;
import RSLBench.Constants;
import RSLBench.Search.DistanceInterface;
import RSLBench.Search.Graph;
import RSLBench.Search.SearchAlgorithm;
import RSLBench.Search.SearchFactory;
import RSLBench.Search.SearchResults;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;

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

    // Indexes entities to indices
    Map<EntityID, Integer> id2idx = new HashMap<>();
    double[][] fireUtilityMatrix;
    double[][] policeUtilityMatrix;

    // Utilities to perform searches
    private SearchAlgorithm search;
    private Graph connectivityGraph;
    private DistanceInterface distanceMatrix;

    /**
     * Creates a problem definition
     *
     * @param fireAgents a list of fire brigade agents
     * @param fires a list of fires
     * @param policeAgents a list of police agents
     * @param blockades a list of blockades
     * @param lastAssignment the assignment computed in the last iteration
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

        // Utilities to check which agents are blocked from reaching wich targets
        search = SearchFactory.buildSearchAlgorithm(config);
        connectivityGraph = new Graph(world);
        distanceMatrix = new DistanceInterface(world);

        long initialTime = System.currentTimeMillis();
        utilityFunction = UtilityFactory.buildFunction();
        utilityFunction.setWorld(world);
        utilityFunction.setConfig(config);

        buildFirefightersUtilityMatrix(lastAssignment);
        buildPoliceUtilityMatrix(lastAssignment);
        computeBlockedFireAgents();
        computeBlockedPoliceAgents();

        // Compute blocked targets... only if there actually are some blockades in the simulation!
        if (blockades.size() > 0) {
            computeBlockedFireAgents();
            computeBlockedPoliceAgents();
        }

        long elapsedTime = System.currentTimeMillis() - initialTime;
        Logger.debug("Problem definition initialized in {}ms.", elapsedTime);
    }

    /**
     * Get the simulator configuration for this run.
     *
     * @return simulator configuration object
     */
    public Config getConfig() {
        return config;
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

                policeUtilityMatrix[i][j] = utility;
            }
        }
    }

    /**
     * Holds the precomputed map from <em>(agent, target)</em> to <em>blockade</em> preventing
     * that agent from reaching that target.
     */
    private HashMap<Pair<EntityID, EntityID>, EntityID> blockedFireAgents = new HashMap<>();
    private HashMap<Pair<EntityID, EntityID>, EntityID> blockedPoliceAgents = new HashMap<>();

    public Collection<Pair<EntityID, EntityID>> getFireAgentsBlockedByBlockade(EntityID blockade) {
        Collection<Pair<EntityID, EntityID>> result = new HashSet<>();

        for (Map.Entry<Pair<EntityID,EntityID>, EntityID> entry : blockedFireAgents.entrySet()) {
            if (!entry.getValue().equals(blockade)) {
                continue;
            }
            result.add(entry.getKey());
        }

        return result;
    }

    private void computeBlockedFireAgents() {
        for (EntityID agent : getFireAgents()) {
            for (EntityID target: getFires()) {
                Human hagent = (Human)world.getEntity(agent);
                EntityID position = hagent.getPosition();
                SearchResults results = search.search(position, target, connectivityGraph, distanceMatrix);
                List<Blockade> pathBlockades = results.getPathBlocks();
                if (!pathBlockades.isEmpty()) {
                    Logger.debug("Firefighter {} blocked from reaching fire {} by {}", agent, target, pathBlockades.get(0).getID());
                    blockedFireAgents.put(new Pair<>(agent, target), pathBlockades.get(0).getID());
                }
            }
        }
    }

    private void computeBlockedPoliceAgents() {
        for (EntityID agent : getPoliceAgents()) {
            for (EntityID target: getBlockades()) {
                Human hagent = (Human)world.getEntity(agent);
                EntityID agentPosition = hagent.getPosition();
                Blockade blockade = (Blockade)world.getEntity(target);
                EntityID targetPosition = blockade.getPosition();
                SearchResults results = search.search(agentPosition, targetPosition, connectivityGraph, distanceMatrix);
                List<Blockade> pathBlockades = results.getPathBlocks();
                if (!pathBlockades.isEmpty() && !pathBlockades.get(0).getID().equals(target)) {
                    Logger.debug("Police agent {} blocked from reaching blockade {} by {}", agent, target, pathBlockades.get(0).getID());
                    blockedPoliceAgents.put(new Pair<>(agent, target), pathBlockades.get(0).getID());
                }
            }
        }
    }

    /**
     * Reads the utility value for the specified fire brigade and target fire.
     *
     * @param firefighter id of the fire brigade
     * @param fire id of the fire
     * @return the utility value for the specified agent and target.
     */
    public double getFireUtility(EntityID firefigher, EntityID fire) {
        final int i = id2idx.get(firefigher);
        final int j = id2idx.get(fire);
        return fireUtilityMatrix[i][j];
    }

    /**
     * Reads the utility value for the specified police agent and blockade.
     *
     * @param police id of the police agent
     * @param blockade id of the blockade
     * @return the utility value for the specified police and blockade.
     */
    public double getPoliceUtility(EntityID police, EntityID blockade) {
        final int i = id2idx.get(police);
        final int j = id2idx.get(blockade);
        return policeUtilityMatrix[i][j];
    }

    /**
     * Check if the given agent is blocked from reaching the given target.
     *
     * @param agent agent trying to reach a target
     * @param target target that the agent wants to reach
     * @return <em>true</em> if there's a blockade in the path, or <em>false</em> otherwise.
     */
    public boolean isFireAgentBlocked(EntityID agent, EntityID target) {
        return blockedFireAgents.containsKey(new Pair<>(agent, target));
    }

    /**
     * Check if the given agent is blocked from reaching the given target.
     *
     * @param agent agent trying to reach a target
     * @param target target that the agent wants to reach
     * @return <em>true</em> if there's a blockade in the path, or <em>false</em> otherwise.
     */
    public boolean isPoliceAgentBlocked(EntityID agent, EntityID target) {
        return blockedPoliceAgents.containsKey(new Pair<>(agent, target));
    }

    /**
     * Get the blockade preventing the given agent from reaching the given target.
     *
     * @param agent agent trying to reach a target
     * @param target target that the agent wants to reach
     * @return <em>true</em> if there's a blockade in the path, or <em>false</em> otherwise.
     */
    public EntityID getBlockadeBlockingFireAgent(EntityID agent, EntityID target) {
        return blockedFireAgents.get(new Pair<>(agent, target));
    }

    /**
     * Get the blockade preventing the given agent from reaching the given target.
     *
     * @param agent agent trying to reach a target
     * @param target target that the agent wants to reach
     * @return <em>true</em> if there's a blockade in the path, or <em>false</em> otherwise.
     */
    public EntityID getBlockadeBlockingPoliceAgent(EntityID agent, EntityID target) {
        return blockedPoliceAgents.get(new Pair<>(agent, target));
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
            map.put(target, getFireUtility(fireAgent, target));
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
            map.put(agent, getFireUtility(agent, fire));
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
            if (getFireUtility(agentID, t) > best) {
                best = getFireUtility(agentID, t);
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
     * Returns a list of fires in the problem.
     * @return list of fires.
     */
    public ArrayList<EntityID> getFires() {
        return fires;
    }

    /**
     * Returns a list of blockades in the problem.
     * @return list of blockades.
     */
    public ArrayList<EntityID> getBlockades() {
        return blockades;
    }

    /**
     * Returns the fire agents.
     * @return the fire agents.
     */
    public ArrayList<EntityID> getFireAgents() {
        return fireAgents;
    }

    /**
     * Returns the police agents.
     * @return the police agents.
     */
    public ArrayList<EntityID> getPoliceAgents() {
        return policeAgents;
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
