package RSLBench.AAMAS12;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import rescuecore2.worldmodel.EntityID;
import RSLBench.Params;
import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DecentralAssignment;
import RSLBench.Comm.AbstractMessage;
import RSLBench.Comm.AssignmentMessage;
import RSLBench.Comm.ComSimulator;

import RSLBench.Helpers.Logger;
import RSLBench.Helpers.SimpleID;
import RSLBench.Helpers.UtilityMatrix;

//Com part
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import RSLBench.Helpers.SimpleID;

import RSLBench.Params;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * Class that implements the DSA algorithm according to the RMASBench specification.
 */
public class DSA implements DecentralAssignment
{
    private static final boolean DEBUG_DSA = false;
    
    protected UtilityMatrix _utilityM = null;
    protected EntityID _agentID;
    protected EntityID _targetID;
    protected Collection<AbstractMessage> _neighborAssignments = null;
    protected TargetScores _targetScores = null;
    protected static Random _random;
    private int _nccc = 0;

	//Com
        private int maxCommunicationRange = Params.SIMULATED_COMMUNICATION_RANGE;
	private Map<EntityID, Set<EntityID>> inRange;

    @Override
    public void initialize(EntityID agentID, UtilityMatrix utilityM)
    {
        _agentID = agentID;
        _utilityM = utilityM;
        _targetScores = new TargetScores();
        _targetID = Assignment.UNKNOWN_TARGET_ID;
       
    	if (DEBUG_DSA)
        	Logger.debugColor("A [" + SimpleID.conv(agentID) +"] initializing with "+ _utilityM.getNumTargets() + " targets.", Logger.BG_LIGHTBLUE);
                
        // Find the target with the highest utility and initialize required agents for each target 
        double bestTargetUtility = 0;
        for (EntityID t : _utilityM.getTargets()) {
            _targetScores.initializeTarget(t, _utilityM.getRequiredAgentCount(t));            
            double util = _utilityM.getUtility(agentID, t);
            if (bestTargetUtility < util) {
                bestTargetUtility = util;
                _targetID = t;
            }
        }
        if (DSA._random == null) {
            _random = new Random();
        }
    	if (DEBUG_DSA)
        	Logger.debugColor("A [" + SimpleID.conv(agentID) +"] init done!", Logger.BG_LIGHTBLUE);

	//Com part
    	//Logger.debugColor("initialize Com for "+agents.size()+" agents", Logger.BG_BLUE);
        //this.agents = agents;
        //messageInboxes = new HashMap<EntityID, List<AbstractMessage>>();
        inRange = new HashMap<EntityID, Set<EntityID>>();
       // for (EntityID id: agents)
       // {
           // messageInboxes.put(id, new ArrayList<AbstractMessage>());
            inRange.put(_agentID, new HashSet<EntityID>());
       // }
	this.update();
    
    }

    @Override
    public boolean improveAssignment()
    {
        if (DEBUG_DSA)
            Logger.debugColor("["+ SimpleID.conv(_agentID) +"] improveAssignment", Logger.BG_LIGHTBLUE);        
        if (DEBUG_DSA)
            Logger.debugColor("[" + SimpleID.conv(_agentID) +"]  received neighbor messages: " + 
            			      _neighborAssignments.size(), Logger.BG_LIGHTBLUE);

        _targetScores.resetAssignments();        
        for (AbstractMessage message: _neighborAssignments) {
        	if (message.getClass() == AssignmentMessage.class) {        		
        		_targetScores.increaseAgentCount(((AssignmentMessage)message).getTargetID());
        	}
        }
        _neighborAssignments.clear();
        
        // Find the best target given utilities and constraints
        double bestScore;
        try {
        bestScore = _targetScores.computeScore(_targetID, _utilityM.getUtility(_agentID, _targetID));
        } catch (NullPointerException n) {
            bestScore = Double.NEGATIVE_INFINITY;
        }
        EntityID bestTarget = _targetID;
        //Logger.debugColor("["+ _agentID +"]  BEFORE -> target: " + _targetID +" score: "+bestScore, Logger.BG_LIGHTBLUE);
        _nccc = 0;
        for (EntityID t : _utilityM.getTargets()) {
        	double score = _targetScores.computeScore(t, _utilityM.getUtility(_agentID, t));
            if (score > bestScore) {
                bestScore = score;
                bestTarget = t;
            }
            _nccc++;
        }        
        if (DEBUG_DSA)
            Logger.debugColor("["+ SimpleID.conv(_agentID) +"]  AFTER -> target: "+bestTarget.getValue() + 
            			      " score: "+ bestScore + " " + bestScore, Logger.BG_LIGHTBLUE);
        
        if (bestTarget != _targetID) {
            if (DEBUG_DSA) Logger.debugColor("[" + SimpleID.conv(_agentID) +"] assignment can be improved", Logger.BG_LIGHTBLUE);
            // improvement possible
            if (_random.nextDouble() <= Params.DSA_CHANGE_VALUE_PROBABILITY) {
                if (DEBUG_DSA) Logger.debugColor("["+ SimpleID.conv(_agentID) +"] assignment improved", Logger.BG_GREEN);
                // change target
                _targetID = bestTarget;
                return true;
            }
        }
        return false;
    }

    public int getNccc(){
        return _nccc;
    }
    @Override
    public EntityID getAgentID()
    {
        return _agentID;
    }

    @Override
    public EntityID getTargetID()
    {
        return _targetID;
    }

    @Override
    public Collection<AbstractMessage> sendMessages(ComSimulator com)
    {
    	Collection<AbstractMessage> messages = new ArrayList<AbstractMessage>();
        Collection<AbstractMessage> totMessages = new ArrayList<AbstractMessage>();
    	AssignmentMessage mex = new AssignmentMessage(_agentID, _targetID);
        messages.add(mex);     
        
        Set<EntityID>neighbors = inRange.get(_agentID);
        for (EntityID neighborID: neighbors)
        {
            totMessages.add(mex);
            com.send(neighborID, messages);
        }
	

        return totMessages;
    }

    @Override
    public void receiveMessages(Collection<AbstractMessage> messages)
    {
        _neighborAssignments = messages;
    }
    
    public int getNumberOfOtherMessages() {
        return 0;
    }
    public long getDimensionOfOtherMessages() {
        return 0;
    }
//Com function

public void update()
    {
        // delete old messages
       // for (List<AbstractMessage> inbox: messageInboxes.values()) {
         //   inbox.clear();
       // }
        
        boolean USE_DISJOINT_AGENT_TEAMS=true;
        if (USE_DISJOINT_AGENT_TEAMS) { 
        	// Initialize Clusters with minimal distance
		List<EntityID> agents = new ArrayList<EntityID>();
		//agents.add(_agentID);
		agents.addAll(_utilityM.getAgents());
        	ArrayList<ArrayList<EntityID>> clusters = buildAgentPairs(agents, _utilityM.getAgentLocations(), _utilityM.getWorld());         
        	for (EntityID id: agents) {
        		// delete old assignments
        		inRange.get(_agentID).clear();
        		// find cluster
        		boolean found = false;
        		for (ArrayList<EntityID> c : clusters) {
        			if (c.contains(_agentID)) {
        				inRange.get(_agentID).addAll(c);
        				found = true;
        			}      				
        		}
        		if (!found) {
        			Logger.debugColor("ERROR could not find cluster of agent " + _agentID, Logger.BG_RED);
        		}      		
        	//Logger.debugColor("Agent "+SimpleID.conv(id)+" has the following group of " + inRange.get(id).size() + " : ", Logger.BG_GREEN);
        	//for (EntityID a : inRange.get(id)) {
        	//	System.out.print(SimpleID.conv(a) + ",");
        //	}
        //	System.out.println(SimpleID.conv(id)+",");
        	}
        }
        else {  /*    	        
      		for (EntityID id1: agents) {
      			// delete old assignments
      			inRange.get(id1).clear();
      			for (EntityID id2: agents) {
      				if (id1.getValue() != id2.getValue()) {
      					int distance = world.getDistance(agentLocations.get(id1), agentLocations.get(id2));
      					if (distance <= maxCommunicationRange) {
      						inRange.get(id1).add(id2);
      					}
      				}
      			}
      		}    */	
      	}
    }
    private Set<EntityID> getNeighbors(EntityID agentID){
    	return inRange.get(agentID);
    }

 private ArrayList<ArrayList<EntityID>> buildAgentPairs(List<EntityID> agents, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world) 
    {
    	ArrayList<ArrayList<EntityID>> clusters = new ArrayList<ArrayList<EntityID>>();
    	Set<EntityID> assigned = new HashSet<EntityID>();
    	
    	Logger.debugColor("CLUSTERING START", Logger.BG_GREEN);
    	while (true) {    		
    		int minDist = Integer.MAX_VALUE;
    		EntityID bestA = new EntityID(-1), bestB=new EntityID(-1);
        	boolean found = false;
    		for (EntityID id1: agents) {
    			if (assigned.contains(id1)) continue;
    			for (EntityID id2: agents) {
    				if (assigned.contains(id2)) continue;
    				if (id1 == id2) continue;
    				int distance = world.getDistance(agentLocations.get(id1), agentLocations.get(id2));
    				if (distance <= maxCommunicationRange && distance < minDist) {
    					minDist = distance;
    					bestA = id1;
    					bestB= id2;
    					found = true;
    				}
    			}
    		}
    		if (found) {
    			ArrayList<EntityID> cl = new ArrayList<EntityID>();
    			cl.add(bestA);cl.add(bestB);
    			assigned.add(bestA);assigned.add(bestB);
    			clusters.add(cl);
    			//Logger.debugColor("Adding pair " + SimpleID.conv(bestA) + " --- " + SimpleID.conv(bestB) + " d= " + minDist, Logger.BG_RED);
    		}
    		else 
    			break;
    	}
    	
    	// Merge Clusters
    	while (clusters.size() > 1) {
    		boolean merged = false;
    		for (int i=0; i<clusters.size(); i++) {
        		for (int j=0; j<clusters.size(); j++)  {
        			if (i==j) continue;
        			ArrayList<EntityID> c1 = clusters.get(i);
        			ArrayList<EntityID> c2 = clusters.get(j);
    				if (clustersInRange(c1,c2, agentLocations, world)) {
    					ArrayList<EntityID> newC = clustersMerge(c1, c2);
    					clusters.remove(i);
        			    clusters.remove(--j);
        			    clusters.add(newC);
        			    merged = true;
        			    break;
    				}
        		}
        		if (merged) break;
    		}
      	  if (!merged) break;	
    	}
    			        	        		
        		
    	// Show Clusters
    	int count = 0;
    	for (ArrayList<EntityID> c: clusters) {
    		Logger.debugColor("Cluster " + count++ + ":", Logger.BG_GREEN);
    		for (EntityID id : c) {
    			System.out.print(id + ",");
    		}
    		System.out.println();
    	}    	    	
    	Logger.debugColor("CLUSTERING END", Logger.BG_GREEN);
    	
    	
    	return clusters;
    }
    	
    
    private boolean clustersInRange(ArrayList<EntityID> c1, ArrayList<EntityID> c2, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world) {
    	for (EntityID id1 : c1 ) {
        	for (EntityID id2 : c2 ) {
				int distance = world.getDistance(agentLocations.get(id1), agentLocations.get(id2));
				if (distance > maxCommunicationRange) {
					return false;
				}					
        	}        		
    	}    	
    	return true;
    }

    private ArrayList<EntityID> clustersMerge(ArrayList<EntityID> c1, ArrayList<EntityID> c2) {
    	ArrayList<EntityID> result = new ArrayList<EntityID>();
    	for (EntityID id1 : c1 ) {
    		result.add(id1);
    	}
        for (EntityID id2 : c2 ) {
        	result.add(id2);
        }    	
    	return result;
    }  
}
