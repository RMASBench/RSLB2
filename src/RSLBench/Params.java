package RSLBench;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import rescuecore2.config.Config;

public class Params {
    
    private static HashMap<String, Object> params = new HashMap<String, Object>();

    
    public static boolean OVERWRITE_FROM_COMMANDLINE;
    
    /**
     * The agents will ignore every command until a specific timestep specified by this parameter
     */
    public static int IGNORE_AGENT_COMMANDS_KEY_UNTIL;
    
    /**
     * The timestep in which the agents will start taking action
     */
    public static int START_EXPERIMENT_TIME;
    
    /**
     * The time in which the experiment ends
     */
    public static int END_EXPERIMENT_TIME;

    public final static int STATION_CHANNEL = 1;
    public final static int PLATOON_CHANNEL = 1;
    public static int SIMULATED_COMMUNICATION_RANGE;

    /**
     * When this is true, agents will only approach 
     * targets selected by the station (which simulates the decentralize assignment)
     * Otherwise they search for targets on their own.
     */
    
    public static double AREA_COVERED_BY_FIRE_BRIGADE;
    
    /**
     * Used in the utility calculation in the UtilityMatrix
     */
    public static double TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY;
    public static boolean OPTIMIZE_ASSIGNMENT;
    public static boolean ONLY_ACT_ON_ASSIGNED_TARGETS;
    public static boolean AGENT_SELECT_IDLE_TARGET;
    
    //public static int LOCAL_UTILITY_MATRIX_LENGTH;
    
    /**
     * The number of iterations max that an algorithm can perform before the agents
     * take a definitive decision for each timestep.
     */
    public static int MAX_ITERATIONS = 100;
    
    /** 
     * Parameters for simulated communication
     */
    
    
    /**
     * This factor controls the influence of travel costs on the
     * utility for targets. As bigger as the factor as bigger the
     * influence.
     */
    
    
    /**
     *  prioritize recently ignited buildings (should always be on) 
     */

    /** 
     * when true and there is no target assigned by the station, 
     * the agent selects a target on his own (rather than doing nothing)
     */

    /** Hysteresis factor to prevent target switching due to pathing isssues. */
    public static double HYSTERESIS_FACTOR = 1.2;
     
     /**
      * The probability that an agent changes his assigned target.
      */
     public static double DSA_CHANGE_VALUE_PROBABILITY = 0.6;
     /**
      * The number of neighbours of an agent in the factograph (the number of considered targets).
      */
     public static int MaxSum_NUMBER_OF_NEIGHBOURS = 3;
     
     /**
      * It sets (some of) the params of the Params class according to the specifications in config.
      * @param config: the config file in which the params to set are specified.
      * @param alg: the used algorithm, used to set the specific algorithm params. 
      */
     public static void setLocalParams(Config config, String alg) {
         Set<String> allParams = (Set<String>)config.getAllKeys();
         for (String param: allParams) {
             if (param.startsWith(alg + "_")) {
                 setParam(param, config.getValue(param));
             }
         }
     }
        /**
         * Returns the value of a specific parameter
         * @param name the name of the parameter
         * @return the value of the parameter
         */
        public static Object get(String name) {
            return params.get(name);
        }

        /**
         * It sets a parameter of the Params class
         * @param name: the name of the parameter to set
         * @param value: the value of the parameter
         */
        private static void setParam(String name, String value) {
        try {
            Field field = Params.class.getField(name);
            Class<?> cla = field.getType();
            if (cla.getName().equals("Integer") || cla.getName().equals("int")){
                field.setInt(null, Integer.parseInt(value));
            }
            else if (cla.getName().equals("Double") || cla.getName().equals("double")) {
                field.setDouble(null, Double.parseDouble(value));
            }
            else if (cla.getName().equals("Float") || cla.getName().equals("float")) {
                field.setFloat(null, Float.parseFloat(value));
            }
            else if (cla.getName().equals("Long") || cla.getName().equals("long")) {
                field.setLong(null, Long.parseLong(value));
            }
            else if (cla.getName().equals("Short") || cla.getName().equals("short")) {
                field.setShort(null, Short.parseShort(value));
            }
            else if (cla.getName().equals("Boolean") || cla.getName().equals("boolean")) {
                field.setBoolean(null, Boolean.parseBoolean(value));
            }
            else if (cla.getName().equals("Byte") || cla.getName().equals("byte")) {
                field.setByte(null, Byte.parseByte(value));
            }
            else if (cla.getName().equals("Character") || cla.getName().equals("char")) {
                field.setChar(null, value.charAt(0));
            }
            else if (cla.getName().equals("String")) {
                field.set(null, (String)value);
            }
            /*else {
                field.set(null, cla.cast(value));
            }*/
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.exit(0);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.exit(0);
        } catch (SecurityException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.exit(0);
        }
        }
     
}