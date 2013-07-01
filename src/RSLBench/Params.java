package RSLBench;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.config.Config;

public class Params {

    private static final Logger Logger = LogManager.getLogger(Params.class);
    
    /**
     * The agents will ignore every command until a specific timestep specified
     * by this parameter.
     *
     * <strong>Warning:</strong> This can't be lower than <em>3</em>, for some
     * reason I can't fathom.
     */
    public static int IGNORE_AGENT_COMMANDS_KEY_UNTIL;
    
    /**
     * The timestep in which the agents will start taking action
     */
    public static int START_EXPERIMENT_TIME;
    
    /**
     * The time at which the experiment ends
     */
    public static int END_EXPERIMENT_TIME;
    public final static int STATION_CHANNEL = 1;
    public final static int PLATOON_CHANNEL = 1;
    public static int SIMULATED_COMMUNICATION_RANGE;
    
    /**
     * When this is true, agents will only approach targets selected by the
     * station (which simulates the decentralize assignment) Otherwise they
     * search for targets on their own.
     */
    public static double AREA_COVERED_BY_FIRE_BRIGADE;
    
    /**
     * The number of iterations max that an algorithm can perform before the
     * agents take a definitive decision for each timestep.
     */
    public static int MAX_ITERATIONS = 100;
    
    /**
     * This factor controls the influence of travel costs on the utility for
     * targets. As bigger as the factor as bigger the influence.
     */
    public static double TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY;

    /**
     * when true and there is no target assigned by the station, the agent
     * selects a target on his own (rather than doing nothing)
     */
    public static boolean ONLY_ACT_ON_ASSIGNED_TARGETS;
    /**
     * Hysteresis factor to prevent target switching due to pathing isssues.
     */
    public static double HYSTERESIS_FACTOR = 1.2;
    /**
     * The probability that an agent changes his assigned target.
     */
    public static double DSA_CHANGE_VALUE_PROBABILITY = 0.6;
    /**
     * The number of neighbours of an agent in the factograph (the number of
     * considered targets).
     */
    public static int MaxSum_NUMBER_OF_NEIGHBOURS = 3;

    /**
     * It sets (some of) the params of the Params class according to the
     * specifications in config.
     *
     * @param config: the config file in which the params to set are specified.
     * @param alg: the used algorithm, used to set the specific algorithm
     * params.
     */
    public static void setLocalParams(Config config, String alg) {
        Set<String> allParams = (Set<String>) config.getAllKeys();
        for (String param : allParams) {
            if (param.startsWith(alg + "_")) {
                setParam(param, config.getValue(param));
            }
        }
    }

    /**
     * It sets a parameter of the Params class
     *
     * @param name: the name of the parameter to set
     * @param value: the value of the parameter
     */
    private static void setParam(String name, String value) {
        try {
            Field field = Params.class.getField(name);
            Class<?> cla = field.getType();
            switch (cla.getName()) {
                case "Integer":
                case "int":
                    field.setInt(null, Integer.parseInt(value));
                    break;
                case "Double":
                case "double":
                    field.setDouble(null, Double.parseDouble(value));
                    break;
                case "Float":
                case "float":
                    field.setFloat(null, Float.parseFloat(value));
                    break;
                case "Long":
                case "long":
                    field.setLong(null, Long.parseLong(value));
                    break;
                case "Short":
                case "short":
                    field.setShort(null, Short.parseShort(value));
                    break;
                case "Boolean":
                case "boolean":
                    field.setBoolean(null, Boolean.parseBoolean(value));
                    break;
                case "Byte":
                case "byte":
                    field.setByte(null, Byte.parseByte(value));
                    break;
                case "Character":
                case "char":
                    field.setChar(null, value.charAt(0));
                    break;
                case "String":
                    field.set(null, (String) value);
                    break;
            }
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            Logger.error(ex.getLocalizedMessage(), ex);
            System.exit(0);
        }
    }
}