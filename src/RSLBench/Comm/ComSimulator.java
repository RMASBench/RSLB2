package RSLBench.Comm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

/**
 * This class represents a communication layer that the agents use to communicate between themselves
 * during the execution of each timestep, ignoring the messages limitations of the usual
 * communication layer between agents and kernel.
 * In this way agents can exchange as many messages as they want as expected in a DCOP algorithm.
 * When a message is sent to an agent, it is added in the messageInbox of the recipient
 * (represented by an EntityID), so that the recipient can read all the messages sent to him
 * when the time comes.
 */
public class ComSimulator
{
    private static final Logger Logger = LogManager.getLogger(ComSimulator.class);
   
    private List<EntityID> agents;

     Map<EntityID, List<AbstractMessage>> messageInboxes;

    
    private boolean initialized;
    
    /**
     * It builds a ComSimulator.
     * @param maxCommunicationRange: the range of communication of the agents.
     */
    public ComSimulator(int maxCommunicationRange)
    {
        //this.maxCommunicationRange = maxCommunicationRange;
        initialized = false;
    }
    
    /**
     * This method initializes the communicator for each agent.
     * @param agents: all the agents in the simulation
     */
    public void initialize(List<EntityID> agents)
    {
        Logger.debug("initialize Com for "+agents.size()+" agents");
        this.agents = agents;
        messageInboxes = new HashMap<EntityID, List<AbstractMessage>>();
       // inRange = new HashMap<EntityID, Set<EntityID>>();
        for (EntityID id: agents)
        {
            messageInboxes.put(id, new ArrayList<AbstractMessage>());
            //inRange.put(id, new HashSet<EntityID>());
        }
        initialized = true;
    }
    
  /**  
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
    **/
    
    /**
     * This method resets all the messageInboxes.
     */
    public void update()
    {
        // delete old messages
        for (List<AbstractMessage> inbox: messageInboxes.values()) {
            inbox.clear();
        }
        /**
        boolean USE_DISJOINT_AGENT_TEAMS=true;
        if (USE_DISJOINT_AGENT_TEAMS) { 
        	// Initialize Clusters with minimal distance
        	ArrayList<ArrayList<EntityID>> clusters = buildAgentPairs(agents, agentLocations, world);         
        	for (EntityID id: agents) {
        		// delete old assignments
        		inRange.get(id).clear();
        		// find cluster
        		boolean found = false;
        		for (ArrayList<EntityID> c : clusters) {
        			if (c.contains(id)) {
        				inRange.get(id).addAll(c);
        				found = true;
        			}      				
        		}
        		if (!found) {
        			Logger.debugColor("ERROR could not find cluster of agent " + id, Logger.BG_RED);
        		}      		
        	Logger.debugColor("Agent "+SimpleID.conv(id)+" has the following group of " + inRange.get(id).size() + " : ", Logger.BG_GREEN);
        	for (EntityID a : inRange.get(id)) {
        		System.out.print(SimpleID.conv(a) + ",");
        	}
        	System.out.println(SimpleID.conv(id)+",");
        	}
        }
        else {      	        
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
      		}    	
      	}
*/
    }
    
    /**
     * This method memorizes a message in the messageInbox of the recipient.
     * @param agentID: the id of the recipient
     * @param message: the message
     */
    public void send(EntityID agentID, AbstractMessage message)
    {
        //Set<EntityID>neighbors = inRange.get(agentID);
        //for (EntityID neighborID: neighbors)
        //{
            messageInboxes.get(agentID).add(message);
        //}
    }
    /**
     * This method memorizes a series of messages in the messageInbox of the recipient 
     * @param agentID: the id of the recipient
     */
    public void send(EntityID agentID, Collection<AbstractMessage> messages)
    {
        //Set<EntityID>neighbors = inRange.get(agentID);
    	//for (EntityID neighborID: neighbors) {
        	for (AbstractMessage message : messages) {
    			messageInboxes.get(agentID).add(message);
    		}
    	//}
    }    
    
    /**
     * This method retrieves the messages from the inbox of an agent
     * @param agentID: the id of the recipient
     * @return a list of alla the messages received
     */
    public List<AbstractMessage> retrieveMessages(EntityID agentID)
    {
        List<AbstractMessage> mesageInbox = messageInboxes.get(agentID);
        messageInboxes.put(agentID, new ArrayList<AbstractMessage>());
        return mesageInbox; 
    }
    
    /**
     * Returns if the ComSimulator is initialized.
     * @return true if the ComSimulator is initialized, false otherwise
     */
    public boolean isInitialized()
    {
        return initialized;
    }
    /**
    public Set<EntityID> getNeighbors(EntityID agentID){
    	return inRange.get(agentID);
    }**/
}
