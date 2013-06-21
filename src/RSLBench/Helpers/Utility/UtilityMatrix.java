package RSLBench.Helpers;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Params;

/**
 * This class represents a matrix that contains the utility
 * for each agent-target pair that exists in the world model.
 * The utility is calculated only considering the distance between the agent and the target
 * and the burning level of the target (the more recent the fire, the higher the utility).
 *
 */
public class UtilityMatrix

{   
    private double[][] _utilityM = null;
    private ArrayList<EntityID> _agents = null;
    private ArrayList<EntityID> _targets = null;
    private StandardWorldModel _world = null;
    HashMap<EntityID, EntityID> _agentLocations;
    private static final boolean DEBUG_UM = false;

    /**
     * It creates a utility matrix
     * @param agents: a list of agents
     * @param targets: a list of targets
     * @param agentLocations: the agent locations
     * @param world: the model of the world
     */
    public UtilityMatrix(ArrayList<EntityID> agents, ArrayList<EntityID> targets, HashMap<EntityID, EntityID> agentLocations, StandardWorldModel world)
    {
        _agents = agents;
        _targets = targets;
        _world = world;
        _agentLocations = agentLocations;
        computeUtilityMatrix();
        if (DEBUG_UM)
        	Logger.debugColor("UM has been initialized!", Logger.BG_MAGENTA);
    }
   
    /**
     * Reads the utility value for the specified agent and target. 
     * @param agentID 
     * @param targetID
     * @return the utility value for the specified agent and target.
     */
    public double getUtility(EntityID agentID, EntityID targetID)
    {
    	if (_utilityM == null) {
    		Logger.debugColor("Utility matrix has not been initialized!!",Logger.BG_RED);
    		return -1.0;
    	}

    	int agentIdx = id2idx(agentID);
        //System.out.println(agentIdx);
    	int targetIdx = target_id2idx(targetID);
        //System.out.println(targetIdx);
        if (agentIdx >= 0 && agentIdx < _utilityM.length && targetIdx >= 0 && targetIdx < _utilityM[0].length) {
            return _utilityM[agentIdx][targetIdx];
        }
        else
        	Logger.debugColor("Utiliy matrix index out of bounds!", Logger.BG_RED);
        return 0.0;
    }

    
    /**
     * Returns the number of agents in the matrix
     * @return the number of agents considered in the matrix.
     */
    public int getNumAgents() {
        return _agents.size();
    }
    
    /**
     * Returns the entityID of the agent in the idx-th row
     * @param idx: the position of the agent in the matrix
     * @return the entityID of the agent
     */
    public EntityID getAgentID(int idx) {
    	return idx2id(idx);
    }
    
    /**
     * Returns the number of targets in the utility matrix
     * @return the number of targets considered in the matrix.
     */
    public int getNumTargets() {
        return _targets.size();
    }
    
    /**
     * Returns the highest EntityID of all the agents in the matrix
     * @return the highest EntityID
     */
    public EntityID getHighestAgentID() {
    	EntityID best = _agents.get(0);
    	for (EntityID agent: _agents) {
    		if (agent.getValue() > best.getValue()) {
    			best = agent;
    		}
    	}
    	return best;
    }
    
    /**
     * Returns the N targets with the highest utility for the agents
     * @param N: the number of targets to be returned
     * @param agents: the targets are sorted considering, for each target, the utility with the agents in agents 
     * @return a list of EntityID of targets ordered by utility value
     */
    public List<EntityID> getNBestTargets(int N, ArrayList<EntityID> agents)
    {
    	Map<EntityID, Double> map = new HashMap<EntityID, Double>();
    	for (EntityID agent: agents) {
        	for (EntityID target: _targets) {
        	    map.put(target, getUtility(agent, target));
        	}
    	}
    	List<EntityID> res  = sortByValue(map);
    	ArrayList<EntityID> list = new ArrayList<EntityID>();
    	int c = 0;
    	for (EntityID id : res) {
    	    list.add(id);
    	    if (++c>=N) break;
    	}
    	return list;
    }
  
    /**
     * Dual of the getNBestTargets method
     * @param N: the number of agents to be returned
     * @param targets: the agents are sorted considering, for each agent, the utility with the targets in targets 
     * @return a list of EntityID of agents ordered by utility value
     */
    public List<EntityID> getNBestAgents(int N, ArrayList<EntityID> targets)
    {
    	Map<EntityID, Double> map = new HashMap<EntityID, Double>();
    	for (EntityID target: targets) {
        	for (EntityID agent: _agents) {
        	    map.put(agent, getUtility(agent, target));
        	}
    	}
    	List<EntityID> res  = sortByValue(map);
    	ArrayList<EntityID> list = new ArrayList<EntityID>();
    	int c = 0;
    	for (EntityID id : res) {
    	    list.add(id);
    	    if (++c>=N) break;
    	}
    	return list;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    /**
     * Sorts the keys according to the doubles 
     * @param m - map
     * @return The sorted list
     */
    public static List<EntityID> sortByValue(final Map<EntityID, Double> m) {
        List<EntityID> keys = new ArrayList<EntityID>();
        keys.addAll(m.keySet());
        Collections.sort(keys, new Comparator() {
            public int compare(Object o1, Object o2) {
                Object v1 = m.get(o1);
                Object v2 = m.get(o2);
                if (v1 == null) {
                    return (v2 == null) ? 0 : 1;
                }
                else if (v1 instanceof Comparable) {
                    return -1 * ((Comparable) v1).compareTo(v2);
                }
                else {
                    return 0;
                }
            }
        });
        return keys;
    }


    /**
     * Returns the target with the highest utility for the agent
     * @param agentID: the agentID
     * @return the targetID
     */
    public EntityID getHighestTargetForAgent(EntityID agentID) {
    	double best = - Double.MAX_VALUE;
    	EntityID targetID = _targets.get(0);
    	for (EntityID t : _targets) {
    		if (getUtility(agentID, t) > best ) {
    			best = getUtility(agentID, t);
    			targetID = t;
    		}    			    		
    	}
    	return targetID;
    }
    
    /**
     * Returns an estimate of how many agents are required for a specific
     * target.
     * 
     * @param targetID
     *            the id of the target
     * @return the amount of agents required or zero if targetID is out of
     *         range.
     */
    public int getRequiredAgentCount(EntityID targetID) {
       Building b = (Building) _world.getEntity(targetID);
       if (b == null) return 0;
       double area = (double) b.getTotalArea();
       double neededAgents =  Math.ceil(area / (double) Params.AREA_COVERED_BY_FIRE_BRIGADE);
       double base = neededAgents;
       if (b.getFieryness() == 1)
    	   neededAgents *= 1.5; //1.5 previously
       else if (b.getFieryness() >= 2)
    	   neededAgents *= 3.0; //3.0       
       Logger.debugColor("BASE: " + base + " | FIERYNESS: " + b.getFieryness() + " | NEEEDED AGENTS: " + neededAgents, Logger.BG_RED);

       return (int) Math.round(neededAgents);
    }
    
    /**
     * Returns the whole UtilityMatrix
     * @return the utilitymatrix
     */
    public double[][] getMatrix() {
        return _utilityM;
    }
    
    /**
     * Returns the whole world model
     * @return the world model
     */
    public StandardWorldModel getWorld() {
    	return _world;
    }
    
    /**
     * Returns the location of the agents
     * @return the locations of the agents
     */
    public HashMap<EntityID, EntityID> getAgentLocations() {
    	return _agentLocations;
    }

    /**
     * Returns the considered targets
     * @return the targets
     */
    public ArrayList<EntityID> getTargets() {
    	return _targets;
    }
    
    /**
     * Returns the agents
     * @return the agents
     */
    public ArrayList<EntityID> getAgents() {
    	return _agents;
    }
    
    /**
     *  This function computes a utility matrix 
     *  between agents and targets.
     */
    private void computeUtilityMatrix() {

        if (_agents.size() <= 0 || _targets.size() <= 0) {
        	Logger.debugColor("Cannot compute utility matrix. Either agents or targets are empty!", Logger.BG_RED);
            return;
        }

        // Initialize utilities with -1.0
        double U[][] = new double[_agents.size()][_targets.size()];
        for (int i = 0; i < _agents.size(); i++) {
            for (int j = 0; j < _targets.size(); j++) {
                U[i][j] = -1.0;
            }
        }
        
        int i = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (EntityID t : _targets)
        {    
        	Building b = (Building) _world.getEntity(t);            
            int j = 0;
            for (EntityID a : _agents)
            {
                // Compute distance to target
            	EntityID apos = _agentLocations.get(a);
            	if (!_agentLocations.containsKey(a)) {
            		Logger.debugColor("UM: Cannot find location for agent " + a, Logger.BG_RED);
            		apos = a;
            	}
            	double distance = _world.getDistance(b.getID(), apos);

                // Compute utility of target
                double utility = 0.0;
                double f = b.getFieryness();
                // double u = target.getTotalArea();
                utility = 100.0;
                /*utility = 1.0;
                if (f == 1.0)
                	utility = 1E9;
                else if (f == 2.0)
                	utility = 1E6;
                else if (f == 3.0)
                	utility = 100.0;*/
                // On Kobe map distances are around from 50.000 - 900.000
                U[j][i] = utility / Math.pow(distance * Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY,2.0);
                //Logger.debugColor("Dist: " + distance + " Util: " + utility + " Final: " + U[j][i], Logger.BG_RED);

                if (U[j][i] < min)
                    min = U[j][i];
                if (U[j][i] > max)
                    max = U[j][i];
                j++;
            }
            i++;
        }
        // Normalize
        double divisor = max - min;
        if (divisor == 0)
            divisor = 1.0;
        for (i = 0; i < _agents.size(); i++) {
            for (int j = 0; j < _targets.size(); j++) {
                double val = (U[i][j] - min) / (divisor);
                // Ensure there are no elements with zero
                U[i][j] = (1e-3 + val) * 10.0;
            }
        }
        _utilityM = U;
    }

    /**
     * Given a target EntityID, returns the index of the target in the utilityMatrix 
     * @param targetID
     * @return the index of the target
     */
    public int target_id2idx(EntityID targetID) {
		int idx=0;
		for (EntityID t: _targets) {			
			if (t == targetID) return idx;
			idx++;
		}
		return -1;
	}
    
      /**
       * Given a utilityMatrix index of a target, it returns the EntityID of the target
       * @param targetIdx: index of the target
       * @return the corresponding EntityID
       */  
    public EntityID target_idx2id(int targetIdx)
    {
        if (targetIdx >= 0 && targetIdx < _targets.size()) {
            return _targets.get(targetIdx);
        }
        else Logger.debugColor("Target index out of bounds!", Logger.BG_RED);
        return null;
    }
    
    /**
     * Converts a numeric id into the equivalent EntityId.
     * @param numericID: int value
     * @return EntityID whose value equals to the int value passed as a parameter,
     * if a target with the same id exists, null otherwise
     */
    public EntityID getTargetID(int numericID) {
        for (EntityID t: _targets) {			
			if (t.getValue() == numericID)
                            return t;

		}
		return null;
    }
    
    /**
     * Dual of getTargetID
     * @param numericID: int value 
     * @return EntityID whose value equals to the int value passed as a parameter,
     * if an agent with the same id exists, null otherwise
     */
    public EntityID getAgentIDFromNumericID(int numericID) {
        for (EntityID a: _agents) {			
			if (a.getValue() == numericID)
                            return a;

		}
		return null;
    }
    
    /**
     * Given a utilityMatrix index of an agent, it returns the EntityID of the agent
     * @param agentIdx: index of the agent
     * @return the corresponding EntityID
     */  
    public EntityID idx2id(int agentIdx)
    {
        if (agentIdx >= 0 && agentIdx < _agents.size()) {
            return _agents.get(agentIdx);
        }
        else Logger.debugColor("Agent index out of bounds!", Logger.BG_RED);
        return null;
    }

    /**
     * Given an agent EntityID, returns the index of the agent in the utilityMatrix 
     * @param agentID
     * @return the index of the agent
     */
    public int id2idx(EntityID agentID)
    {
    	int idx=0;
    	for (EntityID a: _agents) {
    		if (a == agentID)
    			return idx;
    		else idx++;
    	}    	
        Logger.debugColor("Cannot find agent " + agentID + " in matrix.", Logger.BG_RED);
        return -1;
    }

/*public List<EntityID> getDistanceFrom(EntityID id){
    HashMap<EntityID, Double> distances = new HashMap<EntityID, Double>();
    EntityID location = _agentLocations.get(id);
    for (EntityID neighbour : _agentLocations.keySet()) {
        if (neighbour != id) {
        EntityID neighLocation = _agentLocations.get(neighbour);
        double dist = _world.getDistance(location, neighLocation);
        distances.put(neighbour, dist);
    }
        
    }
    List<EntityID> result = sortByValue(distances);
    
    return result;
            }*/

    /**
     * Returns the fieryness of a target.
     * @param target: the id of a target
     * @return the fieryness value
     */
    public int getFieryness(EntityID target) {
        Building b = (Building)_world.getEntity(target);
        return b.getFieryness();
    }
}
