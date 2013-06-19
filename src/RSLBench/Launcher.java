package RSLBench;

import RSLBench.Helpers.Logger;
import java.io.IOException;

import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.Constants;

import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;
/**
 * Launcher for the agents. 
 * This will launch as many instances of each of the 
 * agents as possible, all using one connection.
 */
public final class Launcher {
    private static final String FIRE_BRIGADE_FLAG = "-fb";
    private static final String POLICE_FORCE_FLAG = "-pf";
    private static final String AMBULANCE_TEAM_FLAG = "-at";
    private static final String COMM_RANGE = "-ra";
    private static final String START_TIME = "-st";
    private static final String COST_TRADE_OFF_FACTOR = "-cf";
  //  private static final String CIVILIAN_FLAG = "-cv";

    private Launcher() {}

    private static int comm_range = -1;
    private static int start_time = -1;
    private static double cost_trade_off = -1;
    private static boolean overwriteFromCommandLine = false;

    
    /**
     *  Launches the instances of the agents.
     *  @param args The following arguments are understood: -p <port>, -h <hostname>, -fb <fire brigades>, -pf <police forces>, -at <ambulance teams>
     */
    public static void main(String[] args) {

        Logger.setLogContext("RSLBench");
        Logger.info("Started!");
        try {
            Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
            Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
            Config config = new Config();
            args = CommandLineOptions.processArgs(args, config);
            int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
            String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);
            int fb = -1;
            int pf = -1;
            int at = -1;
            // CHECKSTYLE:OFF:ModifiedControlVariable
            
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals(FIRE_BRIGADE_FLAG)) {
                    fb = Integer.parseInt(args[++i]);
                    Logger.debugColor("fb="+fb, Logger.BG_GREEN);
                }
                else if (args[i].equals(POLICE_FORCE_FLAG)) {
                    //pf = Integer.parseInt(args[++i]);
                	Logger.warn("Police forces are not supported yet!");
                }
                else if (args[i].equals(AMBULANCE_TEAM_FLAG)) {
                    //at = Integer.parseInt(args[++i]);
                	Logger.warn("Ambulances are not supported yet!");
                }
                else if (args[i].equals(COMM_RANGE)) {
                    comm_range = Integer.parseInt(args[++i]);
                	Logger.debugColor("Will use range="+comm_range, Logger.BG_GREEN);
                	overwriteFromCommandLine = true;
                }
                else if (args[i].equals(START_TIME)) {
                    start_time = Integer.parseInt(args[++i]);
                	Logger.debugColor("Will use start_time="+start_time, Logger.BG_GREEN);
                	overwriteFromCommandLine = true;
                }
                else if (args[i].equals(COST_TRADE_OFF_FACTOR)) {
                    cost_trade_off = Double.parseDouble(args[++i]);
                	Logger.debugColor("Will use cost_trade_off_fac=" + cost_trade_off, Logger.BG_GREEN);
                	overwriteFromCommandLine = true;
                }
                else {
                    Logger.warn("Unrecognised option: " + args[i]);
                }
            }
            
            // CHECKSTYLE:ON:ModifiedControlVariable
            ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
            connect(launcher, fb, pf, at, config);
        }
        catch (IOException e) {
            Logger.error("Error connecting agents", e);
        }
        catch (ConfigException e) {
            Logger.error("Configuration error", e); 
        }
        catch (ConnectionException e) {
            Logger.error("Error connecting agents", e);
        }
        catch (InterruptedException e) {
            Logger.error("Error connecting agents", e);
        }
    }
    
    /**
     * It connects the agents to the kernel.
     * @param launcher: the launcher
     * @param fb: number of fire brigades
     * @param pf: number of police forces
     * @param at: number of ambulances
     * @param config: configuration file
     * @throws InterruptedException
     * @throws ConnectionException 
     */
    private static void connect(ComponentLauncher launcher, int fb, int pf, int at, Config config) throws InterruptedException, ConnectionException {
        int i = 0;
        try {
            while (fb-- != 0) {
                Logger.info("Connecting fire brigade " + (i++) + "...");
                if (overwriteFromCommandLine) 
                	launcher.connect(new PlatoonFireAgent(true, start_time));
                else
                	launcher.connect(new PlatoonFireAgent(false, 0)); 
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
/*        try {
            while (pf-- != 0) {
                Logger.info("Connecting police force " + (i++) + "...");
                launcher.connect(new SamplePoliceForce());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (at-- != 0) {
                Logger.info("Connecting ambulance team " + (i++) + "...");
                launcher.connect(new SampleAmbulanceTeam());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
*/
        try {
            while (true) {
                Logger.info("Connecting center " + (i++) + "...");
                if (overwriteFromCommandLine) 
                	launcher.connect(new CenterAgent(true, comm_range, start_time, cost_trade_off));
                else
                	launcher.connect(new CenterAgent(false, 0, 0, 0 ));              
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
        try {
            while (true) {
                Logger.info("Connecting dummy agent " + (i++) + "...");
                launcher.connect(new DummyAgent());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
    }
}
