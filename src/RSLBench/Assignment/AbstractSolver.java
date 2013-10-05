package RSLBench.Assignment;

import java.util.ArrayList;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;
import RSLBench.Helpers.Stats;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import RSLBench.PlatoonPoliceAgent;
import RSLBench.Search.SearchFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.Timestep;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.score.BuildingDamageScoreFunction;
import rescuecore2.worldmodel.EntityID;

/**
 * This class represents a sort of "collection point" and acts as a layer of communication
 * between the agents and the kernel.
 * It starts the computation and it collects all the assignments of all the agents when ready,
 * then it builds the AssignmentMessage that will be sent to the kernel.
 * It also collects and writes all the metrics used by the benchmark.
 *
 */
public abstract class AbstractSolver implements Solver
{
    private static final Logger Logger = LogManager.getLogger(AbstractSolver.class);

    protected long maxTime;
    protected final Stats stats = new Stats();
    private StandardWorldModel worldModel;
    protected Config config;
    private BuildingDamageScoreFunction scoreFunction;

    @Override
    public void setMaxTime(int maxTime) {
        this.maxTime = maxTime;
    }

    @Override
    public int getMaxTime() {
        return (int)maxTime;
    }

    @Override
    public void initialize(StandardWorldModel world, Config config) {
        this.worldModel = world;
        this.config = config;
        scoreFunction = new BuildingDamageScoreFunction();
        scoreFunction.initialise(worldModel, config);

        String file = new StringBuilder()
                .append(config.getValue(Constants.CONF_KEY_RESULTS_PATH))
                .append(config.getValue(Constants.KEY_RUN_ID))
                .append("-").append(getIdentifier()).append(".dat")
                .toString();
        stats.initialize(config, this, file);
        Logger.info("Solver {} initialized. Results file: {}.", getIdentifier(), file);
    }

    public StandardWorldModel getWorldModel() {
        return worldModel;
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(Constants.KEY_MAIN_SOLVER);
        keys.add(rescuecore2.Constants.RANDOM_SEED_KEY);
        keys.add(Constants.KEY_RUN_ID);
        keys.add(Constants.KEY_START_EXPERIMENT_TIME);
        keys.add(Constants.KEY_END_EXPERIMENT_TIME);
        keys.add(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
        keys.add(Constants.KEY_AGENT_ONLY_ASSIGNED);
        keys.add(Constants.KEY_UTILITY_CLASS);
        keys.add(Constants.KEY_AREA_COVERED_BY_FIRE_BRIGADE);
        keys.add(Constants.KEY_UTIL_TRADEOFF);
        keys.add(Constants.KEY_UTIL_HYSTERESIS);
        keys.add(Constants.KEY_MAP_NAME);
        keys.add(Constants.KEY_MAP_SCENARIO);
        keys.add(SearchFactory.KEY_SEARCH_CLASS);
        return keys;
    }

    /**
     * This method sinchronously executes the computation of the agents
     * (first each agent is initialized, then each agent sends messages,
     * then each agent receives messages and eventually each agent computes
     * his new assignment).
     *
     * @param utility: a matrix that contains all the agent-target utilities
     * (for all the agents and alla the targets).
     * @return a mapping for each agent to a target.
     */
    public abstract Assignment compute(ProblemDefinition utility);

    @Override
    public Assignment solve(int time, ProblemDefinition problem) {
        final long start = System.currentTimeMillis();
        stats.report("time", time);
        Logger.debug("Starting {} solver.", getIdentifier());

        // Report number of burning and once burned buildings
        int nOnceBurned = 0;
        int nBurning = 0;
        for (StandardEntity entity : worldModel.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            Building building = (Building) entity;

            if (building.getFierynessEnum() != StandardEntityConstants.Fieryness.UNBURNT) {
                nOnceBurned++;
            }
            if (building.isOnFire()) {
                nBurning++;
            }
        }
        stats.report("nOnceBurned", nOnceBurned);
        stats.report("nBurning", nBurning);

        Assignment solution = compute(problem);
        long cputime = System.currentTimeMillis() - start;
        Logger.info("{} took {} ms.", getIdentifier(), cputime);

        // Compute score and utility obtained
        stats.report("score", scoreFunction.score(worldModel, new Timestep(time)));
        stats.report("utility", getUtility(problem, solution));
        stats.report("violations", problem.getViolations(solution));
        stats.report("solvable", problem.getTotalMaxAgents() >= problem.getNumFireAgents());

        Logger.debug("DA Simulator done");
        stats.report("cpu_time", cputime);

        stats.reportStep();
        return solution;
    }

    /**
     * Get the utility obtained by the given solution.
     *
     * @param solution solution to evaluate.
     * @return utility obtained by this solution.
     */
    public double getUtility(ProblemDefinition problem, Assignment solution) {
        if (solution == null) {
            return Double.NaN;
        }
        double utility = 0;
        boolean POLICE_CLEAR_PATHBLOCKS = problem.getConfig().getBooleanValue(PlatoonPoliceAgent.KEY_CLEAR_PATHBLOCKS);

        HashSet<EntityID> blockadesAttended = new HashSet<>();
        // Add individual police utilities
        for (EntityID agent : problem.getPoliceAgents()) {
            EntityID target = solution.getAssignment(agent);
            if (target == Assignment.UNKNOWN_TARGET_ID) {
                continue;
            }

            utility += problem.getPoliceUtility(agent, target);
            if (problem.isPoliceAgentBlocked(agent, target)
                    && !POLICE_CLEAR_PATHBLOCKS) {
                double p = problem.getConfig().getFloatValue(Constants.KEY_BLOCKED_PENALTY);
                utility -= POLICE_CLEAR_PATHBLOCKS ? p/2 : p;
            }

            // Track assignments and violations
            if (blockadesAttended.contains(target)) {
                return Double.NEGATIVE_INFINITY;
            }
            blockadesAttended.add(target);
        }

        // Track individual utilities and count how many firefighters have chosen each fire
        HashMap<EntityID, Integer> nAgentsPerTarget = new HashMap<>();
        final boolean interteam = config.getBooleanValue(Constants.KEY_INTERTEAM_COORDINATION);
        for (EntityID agent : problem.getFireAgents()) {
            EntityID target = solution.getAssignment(agent);

            // Individual utility (possibly penalized if blocked and not attended)
            utility += problem.getFireUtility(agent, target);
            if (problem.isFireAgentBlocked(agent, target)) {
                if (!(interteam && blockadesAttended.contains(problem.getBlockadeBlockingFireAgent(agent, target)))) {
                    utility -= problem.getConfig().getFloatValue(Constants.KEY_BLOCKED_PENALTY);
                }
            }

            // Add 1 to the target count
            int nAgents = nAgentsPerTarget.containsKey(target)
                    ? nAgentsPerTarget.get(target) : 0;
            nAgentsPerTarget.put(target, nAgents+1);
        }

        // Penalize overassignments of agents to fires
        for (EntityID target : nAgentsPerTarget.keySet()) {
            int assigned = nAgentsPerTarget.get(target);
            utility -= problem.getUtilityPenalty(target, assigned);
        }

        return utility;
    }

}