package RSLBench.Assignment;

import java.util.ArrayList;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;
import RSLBench.Helpers.Stats;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.UtilityMatrix;
import RSLBench.PlatoonAbstractAgent;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.Timestep;
import rescuecore2.standard.score.BuildingDamageScoreFunction;

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
    
    protected final Stats stats = new Stats();
    private StandardWorldModel worldModel;
    protected Config config;
    private BuildingDamageScoreFunction scoreFunction;

    @Override
    public void initialize(StandardWorldModel world, Config config) {
        this.worldModel = world;
        this.config = config;
        scoreFunction = new BuildingDamageScoreFunction();
        scoreFunction.initialise(worldModel, config);

        String file = new StringBuilder()
                .append(config.getValue(Constants.CONF_KEY_RESULTS_PATH))
                .append(getIdentifier()).append(".dat")
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
        keys.add(PlatoonAbstractAgent.KEY_SEARCH_CLASS);
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
    public abstract Assignment compute(UtilityMatrix utility);

    @Override
    public Assignment solve(int time, UtilityMatrix utility) {
        stats.report("time", time);
        Assignment solution = compute(utility);

        // Compute score and utility obtained
        stats.report("score", scoreFunction.score(worldModel, new Timestep(time)));
        stats.report("utility", utility.getUtility(solution));
        stats.report("violations", utility.getViolations(solution));


        stats.reportStep();
        return solution;
    }

}