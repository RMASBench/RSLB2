package RSLBench;

public class Constants {
    public final static int STATION_CHANNEL = 1;
    public final static int PLATOON_CHANNEL = 1;
    
    /** The time at which the experiment starts */
    public static final String KEY_START_EXPERIMENT_TIME = "experiment.start_time";

    /** The time at which the experiment ends */
    public static final String KEY_END_EXPERIMENT_TIME = "experiment.end_time";

    /** Fully qualified class of the utility function to employ */
    public static final String KEY_UTILITY_CLASS = "util.class";

    /** Amount of area covered by a single fire brigade. */
    public static final String KEY_AREA_COVERED_BY_FIRE_BRIGADE = "util.fire_brigade_area";
    
    /**
     * The number of iterations max that an algorithm can perform before the
     * agents take a definitive decision for each timestep.
     */
    public static final String KEY_DCOP_ITERATIONS = "dcop.iterations";
    
    /**
     * This factor controls the influence of travel costs on the utility for
     * targets. As bigger as the factor as bigger the influence.
     */
    public static final String KEY_UTIL_TRADEOFF = "util.trade_off";

    /**
     * when true and there is no target assigned by the station, the agent
     * selects a target on his own (rather than doing nothing)
     */
    public static final String KEY_AGENT_ONLY_ASSIGNED = "agent.only_assigned";

    /** Hysteresis factor to prevent target switching due to pathing isssues. */
    public static final String KEY_UTIL_HYSTERESIS = "util.hysteresis";

    /** Map name */
    public static final String KEY_MAP_NAME = "map.name";

    /** Scenario name */
    public static final String KEY_MAP_SCENARIO = "map.scenario";

    /**
     * The probability that an agent changes his assigned target.
     */
    public static final String KEY_DSA_PROBABILITY = "dsa.probability";

    /**
     * The number of neighbours of an agent in the factograph (the number of
     * considered targets).
     */
    public static final String KEY_MAXSUM_NEIGHBORS = "maxsum.neighbors";

    /** Config key to the results path */
    public static final String CONF_KEY_RESULTS_PATH = "results.path";

    /** Config key to the results filename */
    public static final String CONF_KEY_RESULTS_PREFIX = "results.prefix";

    /** Key to identify the ID of the experiment run */
    public static final String KEY_RUN_ID = "run";

    /** Key to identify the main solver being run */
    public static final String KEY_MAIN_SOLVER = "main_solver";

}