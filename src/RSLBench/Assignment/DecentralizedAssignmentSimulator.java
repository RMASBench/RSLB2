package RSLBench.Assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import RSLBench.Params;
import RSLBench.Comm.AbstractMessage;
import RSLBench.Comm.ComSimulator;
import RSLBench.Helpers.Logger;
import RSLBench.Helpers.SimpleID;
import RSLBench.Helpers.Stats;
import RSLBench.Helpers.Utility.UtilityMatrix;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import rescuecore2.worldmodel.EntityID;

/**
 * @see AssignmentInterface
 */
public class DecentralizedAssignmentSimulator implements AssignmentInterface
{
    private String _className;
    private List<DecentralAssignment> _simulatedAgents;
    private ComSimulator _com;
    private int _time;
    private long _messagesInBytes;
    private int _averageNccc = 0;
    private int _nMessages;
    private int _nOtherMessages;
    public DecentralizedAssignmentSimulator(String className, ComSimulator com)
    {
        _className = className;
        _com = com;
        _time = 0;
    }

    @Override
    public Assignment compute(UtilityMatrix utility)
    {
        
     	long start = System.currentTimeMillis();
        initializeAgents(utility);        

        Logger.debugColor("starting DA Simulator", Logger.BG_YELLOW);
        boolean done = false;
        int iterations = 0;
        int totalNccc = 0;
        int initialConflicts = 0;
        int finalConflicts = 0;
        int MAX_ITERATIONS = Params.MAX_ITERATIONS;
        long byteMessage = 0;
        int assignmentMessages = 0;
        while (!done && iterations < MAX_ITERATIONS)
        {
            /*
                FileWriter fw2 = null;
                try {
                    fw2 = new FileWriter("assignment.txt", true);
                    fw2.write(iterations+" ");
                    fw2.flush();
                    fw2.close();
                        } catch (IOException i) {
                }*/
            // statistics
            if (iterations == 1)
            {
                initialConflicts = countConflicts(utility);
            }
            
            // send messages
            for (DecentralAssignment agent : _simulatedAgents)
            {   
   
                
            	Collection<AbstractMessage> messages = agent.sendMessages(_com);
                
                //collect the byte size of the messages exchanged between agents
                

                    assignmentMessages = assignmentMessages+messages.size();
                    //byteMessage = byteMessage+MemoryMeasurer.measureBytes(messages);

            }

            // receive messages
            for (DecentralAssignment agent : _simulatedAgents)
            {
                agent.receiveMessages(_com.retrieveMessages(agent.getAgentID()));
            }

            // improve assignment
            done = true;
            int nccc = 0;
            for (DecentralAssignment agent : _simulatedAgents)
            {
                boolean improved = agent.improveAssignment();
                nccc = Math.max(nccc, agent.getNccc());

                done = done && !improved;
            }
            totalNccc += nccc;
            iterations++;
            
            /*try {
                    fw2 = new FileWriter("assignment.txt", true);
                    fw2.write("\n");
                    fw2.flush();
                    fw2.close();
                        } catch (IOException i) {
                }*/
        }
        _averageNccc = totalNccc / iterations;
        
        this._messagesInBytes = byteMessage;
        this._nMessages = assignmentMessages;
        for (DecentralAssignment agent : _simulatedAgents) {
            this._nMessages += agent.getNumberOfOtherMessages();
            this._messagesInBytes+=agent.getDimensionOfOtherMessages();
        }
        
        this._nOtherMessages = this._nMessages-assignmentMessages;
        Logger.debugColor("Done with iterations. Needed: " + iterations , Logger.BG_WHITE);
        
     	long end = System.currentTimeMillis();
     	Logger.debugColor("Total computation time for " + _className + " was "+(end-start)+" ms.", Logger.BG_YELLOW);

        
        finalConflicts = countConflicts(utility);
        Logger.debugColor("DA Simulation complete initial conflicts = " + initialConflicts +
        		" final conflicts = " + finalConflicts, Logger.BG_YELLOW);

        // Combine assignments
        Assignment assignments = new Assignment();
        for (DecentralAssignment agent : _simulatedAgents)
        {
           if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID)
            assignments.assign(agent.getAgentID(), agent.getTargetID());
           
           System.err.println("FILTER | Agent " + agent.getAgentID() + "\t" + agent.getTargetID() + "\t" + 
                   utility.getUtility(agent.getAgentID(), agent.getTargetID()) +
                   "\t" + utility.getWorld().getDistance(agent.getTargetID(), 
                                utility.getAgentLocations().get(agent.getAgentID())
                   ));
        }
        
        _time++;
        Logger.debugColor("Combin Ass done", Logger.BG_MAGENTA);

        return assignments;
    }
    
    /**
     * It counts the violated constraints for all agents.
     * The constraints are violated when a target is assigned to more or less agents then required.  
     * @param utilityM: the utility matrix
     * @return the number of total conflicts
     */
    private int countConflicts(UtilityMatrix utilityM)
    {    	
        Assignment ass = new Assignment();
        for (DecentralAssignment agent : _simulatedAgents) {
        	if (agent.getTargetID() != Assignment.UNKNOWN_TARGET_ID)
        		ass.assign(agent.getAgentID(), agent.getTargetID());
        }    	
        int conflicts = 0;
        for (EntityID t : utilityM.getTargets()) {
        	conflicts += Math.abs(ass.getTargetSelectionCount(t) - utilityM.getRequiredAgentCount(t));
        }        	
        return conflicts;
    }
    
    /**
     * This method initializes the agents for the simulation (it calls the initialize method
     * of the specific DCOP algorithm used for the computation)
     * @param utilityM: the utility matrix
     */
    private void initializeAgents(UtilityMatrix utilityM)
    {
        // initialize simulated agents
        _simulatedAgents = new ArrayList<DecentralAssignment>();
        try
        {
            Class<?> daClass = Class.forName(_className);
            for (EntityID agentID : utilityM.getAgents()) 
            {
                DecentralAssignment agent = (DecentralAssignment) daClass.newInstance();
                UtilityMatrix localUM;
                
                // TODO: if required give only local utility matrix to each agent!!!
                
                //if (Params.LOCAL_UTILITY_MATRIX_LENGTH == -1)
                    localUM = utilityM;
                /*else 
                    convertToLocal(agentID, utilityM);*/
                    
                    
                Logger.debugColor("Agent " + SimpleID.conv(agentID) + " has local UM with " + localUM.getNumAgents() + " agents and " + 
                			       localUM.getNumTargets() + " targets. Agents: ", Logger.BG_MAGENTA);
                //for (EntityID a : localUM.getAgents()) {
                //	System.out.println(SimpleID.conv(a) + "," );
                //}
                //System.out.println();
                	
                //agent.resetStructures();
                agent.initialize(agentID, localUM);
                _simulatedAgents.add(agent);
            }
        } catch (ClassNotFoundException e)
        {
            Logger.debugColor("SolverClass could not be found: " + _className, Logger.BG_RED);
            e.printStackTrace();
        } catch (InstantiationException e)
        {
            Logger.debugColor("SolverClass " + _className + " could not be instantiated. (abstract?!)", Logger.BG_RED);
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            Logger.debugColor("SolverClass " + _className + " must have an empty constructor.", Logger.BG_RED);
            e.printStackTrace();
        }
        Logger.debugColor("Initialized " + _simulatedAgents.size() + " agents of class " + _className, Logger.BG_BLUE);
    }
    
    /**
     * This function computes from a utility matrix over all agents and targets
     * a local utility matrix for a single agent (agentID) and all its neighbors 
     * within communication range (comm_range).
     * 
     * @param agentID
     * @param comm_range
     * @return
     */
/*
    public UtilityMatrix convertToLocal(EntityID agentID, UtilityMatrix um)  
    {
    	Set<EntityID> dummy = _com.getNeighbors(agentID);
    	ArrayList<EntityID> neighbors = new ArrayList<EntityID> ();
    	for (EntityID a : dummy) {
            if (a.getValue() != agentID.getValue()) {
    		neighbors.add(a);
            }
	}
    	neighbors.add(agentID); // add ourself !!
        System.out.println("converttolocal");
    	// Now lets add only high value targets up to a limited number !!
    	int N = Params.LOCAL_UTILITY_MATRIX_LENGTH;
    	ArrayList<EntityID> targets = (ArrayList<EntityID>) um.getNBestTargets(N, neighbors);
    	return new UtilityMatrix(neighbors, targets, um.getAgentLocations(), um.getWorld());    	    	
    }
*/
    @Override
    public int getTotalMessages() {
        return this._nMessages;
    }
    
    @Override
    public long getTotalMessagesBytes() {
        return this._messagesInBytes;
    }
    
    @Override
    public int getOtherMessages() {
        return this._nOtherMessages;
    }
    
    @Override
    public int getAverageNccc() {
        return this._averageNccc;
    }
}
