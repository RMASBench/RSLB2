package RSLBench.Assignment;

import java.io.File;
import java.util.ArrayList;

import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Params;
import RSLBench.Comm.ComSimulator;
//import RSLBench.Comm.ComSimulatorDSAFactorgraph;
import RSLBench.Comm.SimpleProtocolToServer;
import RSLBench.Helpers.Logger;
import RSLBench.Helpers.Stats;
import RSLBench.Helpers.UtilityMatrix;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class represents a sort of "collection point" and acts as a layer of communication
 * between the agents and the kernel.
 * It starts the computation and it collects all the assignments of all the agents when ready,
 * then it builds the AssignmentMessage that will be sent to the kernel.
 * It also collects and writes all the metrics used by the benchmark.
 *
 */
public class AssignmentSolver
{
    private String _assignmentSolverClassName = "";
    private String _logFileName = "no_logfile_name.dat";
    private AssignmentInterface _solver = null;;
    private ComSimulator _com = null;
    
    /**
     * Creates the assignment solver and initializes some of the simulation parameters
     * @param world: the model of the world
     * @param config: the configuration file
     */
    public AssignmentSolver(StandardWorldModel world, Config config)
    {
        
        // Initialize mandatory parameters
    	/*if (!Params.OVERWRITE_FROM_COMMANDLINE) {
    		Params.START_EXPERIMENT_TIME = config.getIntValue("experiment_start_time");               
    		Params.simulatedCommunicationRange = (int) config.getFloatValue("simulated_com_range") * 1000; // convert to mm         
    		Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY = config.getFloatValue("trade_off_factor_travel_cost_and_utility");         		
    	}*/

		String basePackage = config.getValue("base_package");
		String className = config.getValue("assignment_class");
		String groupName = config.getValue("assignment_group");
                
                Params.START_EXPERIMENT_TIME = config.getIntValue("experiment_start_time", 25);
                Params.END_EXPERIMENT_TIME = config.getIntValue("experiment_end_time", 300);
                Params.IGNORE_AGENT_COMMANDS_KEY_UNTIL = config.getIntValue("ignore_agents_commands_until", 3);
                Params.SIMULATED_COMMUNICATION_RANGE = config.getIntValue("simulated_communication_range", 10000) * 1000;
                Params.ONLY_ACT_ON_ASSIGNED_TARGETS = config.getBooleanValue("only_assigned_targets", false);
                Params.OPTIMIZE_ASSIGNMENT = config.getBooleanValue("optimize_assignment", true);
                Params.AREA_COVERED_BY_FIRE_BRIGADE = config.getFloatValue("area_covered_by_fire_brigade", 100.0);
                Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY = config.getFloatValue("trade_off_factor_travel_cost_and_utility", 1.0);
                Params.AGENT_SELECT_IDLE_TARGET = config.getBooleanValue("select_idle_target", true);
                //Params.LOCAL_UTILITY_MATRIX_LENGTH = config.getIntValue("number_of_considered_targets", -1);
                Params.MAX_ITERATIONS = config.getIntValue("max_iterations", 100);
                
                Params.setLocalParams(config, className);

        _assignmentSolverClassName = basePackage + "." + groupName + "." + className;
        _logFileName = "logs/" + basePackage + "_" + groupName + "_" + className + ".dat";
        System.out.println("Writing ouput to log: " +  _logFileName);
        // Initialize Assignment
        /*Logger.debugColor("Starting decentralized solver with com_range: " 
        		  + Params.simulatedCommunicationRange + " startTime: "  
        		  + Params.START_EXPERIMENT_TIME + " cost_trade_off: " 
        		  + Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY, Logger.BG_GREEN);*/
        //TEMPORARY
        _com = new ComSimulator(Params.SIMULATED_COMMUNICATION_RANGE);
        _solver = new DecentralizedAssignmentSimulator(_assignmentSolverClassName, _com);

        // Delete old log file
        File f1 = new File(_logFileName);
        f1.delete();
    }

    /**
     * It executes the simulation and it collects the metrics used by the benchmark.
     * @param time the actual timestep of the simulation
     * @param agents a list of the agents in the simulation
     * @param targets a list of the targets
     * @param agentLocations the location of the agents
     * @param world the model of the world
     * @return an array of bytes containing the assignment message sent to the kernel
     */
    public byte[] act(int time, ArrayList<EntityID> agents, ArrayList<EntityID> targets, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world)
    {
    	if (world == null) {	
    		Logger.error("Got empty StandardWorldModel !!!");
    		System.exit(-1);
    	}

        // Write statistics header to file
        if (time == 7) {
            Stats.writeHeader(_logFileName);
        }

        // Check whether there is something to do at all
        if (targets.size() == 0 || agents.size() == 0) {
            Logger.debugColor("No agents or targets for assignment! targets=" + targets.size() + " agents:" + agents.size(), Logger.BG_YELLOW);
            return null;
        }

        // Initialize simulated communication
        if (_com != null) {
        	if(!_com.isInitialized()) {
        		_com.initialize(agents);
        	}
        	_com.update();
        }

        Logger.debugColor(_assignmentSolverClassName + ": assigning " + agents.size() + " agents to " + targets.size() + " targets", Logger.BG_GREEN);
        UtilityMatrix utility = new UtilityMatrix(agents, targets, agentLocations, world);
        long start = System.currentTimeMillis();
        Assignment A = _solver.compute(utility);
        long end = System.currentTimeMillis();
        long computationTime = end-start;
        long messagesInBytes = _solver.getTotalMessagesBytes();
        int totalMessages = _solver.getTotalMessages();
        int averageNccc = _solver.getAverageNccc();
        int notAssignmentMessages = _solver.getOtherMessages();
        if (A != null) {
        	// Count violated constraints
        	int violatedConstraints = 0;
        	for (EntityID a: agents) {
        		EntityID targetID = A.getAssignment(a);
        		violatedConstraints += Math.abs(A.getTargetSelectionCount(targetID) - utility.getRequiredAgentCount(targetID));         		
        	}
                
        	Stats.writeStatsToFile(_logFileName, time, world, violatedConstraints, computationTime, messagesInBytes, averageNccc, totalMessages, notAssignmentMessages);
        	return SimpleProtocolToServer.buildAssignmentMessage(A, true);
        }
        else
        {
        	Stats.writeStatsToFile(_logFileName, time, world, -1, computationTime, messagesInBytes, averageNccc, totalMessages, notAssignmentMessages);
        	return null;
        }
    }
}