/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.AAMAS12;

import RSLBench.Assignment.Assignment;
import RSLBench.Helpers.SimpleID;
import RSLBench.Helpers.UtilityMatrix;
import RSLBench.Params;
import RSLBench.maxSumAdapters.RMASNodeFunctionUtility;
import java.util.*;
import rescuecore2.worldmodel.EntityID;
/**
 * Class that implements the DSA algorithm according to RMASBench specification,
 * but, instead of considering all the targets, it considers the targets that MaxSum
 * would consider after the creation of the factorgraph.
 */
public class DSAFactorgraph extends DSA {
        private ArrayList<EntityID> myFunctions= new ArrayList<EntityID>();
        private static ArrayList<DSAFactorgraph> agents = new ArrayList<DSAFactorgraph>();
        private static HashMap<EntityID, EntityID> _targetController = new HashMap<EntityID, EntityID>();
        private static HashMap<EntityID, ArrayList<EntityID>> _agentNeighbours= new HashMap<EntityID, ArrayList<EntityID>>();
        private static HashMap<EntityID, ArrayList<EntityID>> _targetNeighbours= new HashMap<EntityID, ArrayList<EntityID>>();
        private static ArrayList<EntityID> _assignedFunctions = new ArrayList<EntityID>();
        private static HashMap<EntityID, ArrayList<EntityID>> _consideredVariables = new HashMap<EntityID, ArrayList<EntityID>>();
        private int _funPerAgent=4;
        private int _dependencies=Params.MaxSum_NUMBER_OF_NEIGHBOURS;
        private UtilityMatrix _oldUtilityMatrix = null;
        private static boolean toReset= false;
        //private static int _number = 1;
        
        @Override
        public void initialize(EntityID agentID, UtilityMatrix utilityM) {
            //_number++;
            this.resetStructures();
            
            _agentID = agentID;
            _utilityM = utilityM;
            _oldUtilityMatrix = utilityM;
            _targetScores = new TargetScores();
            _targetID = Assignment.UNKNOWN_TARGET_ID;
            
            
            agents.add(this);
            
            ArrayList<EntityID> me = new ArrayList<EntityID>();
            me.add(_agentID);
            ArrayList<EntityID> targets = (ArrayList<EntityID>)utilityM.getNBestTargets(_utilityM.getNumTargets(), me);
            
            //assegno le funzioni agli agenti
            int i = 0;
            for (EntityID target: targets) {
            if ((!_assignedFunctions.contains(target)) && i < _funPerAgent) {
                _assignedFunctions.add(target);
                myFunctions.add(target);
                _consideredVariables.put(target, new ArrayList<EntityID>());
                _targetController.put(target, _agentID);
                i++;
            }           
        }
            
            
        for (EntityID function: _assignedFunctions) {
                if (!_targetNeighbours.containsKey(function)) {
                    _targetNeighbours.put(function, new ArrayList<EntityID>());
                }
            }
        
        for(EntityID function: myFunctions) {
            int count = 0;
            ArrayList<EntityID> func = new ArrayList<EntityID>();
            func.add(function);
            ArrayList<EntityID> bestAgents = (ArrayList<EntityID>)_utilityM.getNBestAgents(_utilityM.getNumTargets(), func);
            this.buildNeighborhood(function, bestAgents, count);
        }
        
        if (agents.size() == _utilityM.getNumAgents()) {
            ArrayList<EntityID> agentsIDs = new ArrayList<EntityID>();
            for (DSAFactorgraph agent: agents) {
                agentsIDs.add(agent.getAgentID());
            }
            for (DSAFactorgraph agent: agents) {
                /*try {
                   agent._utilityM = new UtilityMatrix(agentsIDs, _agentNeighbours.get(agent.getAgentID()), _utilityM.getAgentLocations(), _utilityM.getWorld());
                } catch (NullPointerException n) {
                    agent._utilityM = null;
                }*/
                
                /*System.out.println("Sono l'agente "+agent.getAgentID().getValue()+" e i miei vicini sono");
                for (EntityID target: agent._utilityM.getTargets()) {
                    System.out.print(" "+target.getValue());
                }
                System.out.println();*/
        // Find the target with the highest utility and initialize required agents for each target 
                if (agent._utilityM != null) {
                    double bestTargetUtility = 0;
                    for (EntityID t : agent._utilityM.getTargets()) {
                        agent._targetScores.initializeTarget(t, agent._utilityM.getRequiredAgentCount(t));            
                        double util = agent._utilityM.getUtility(agentID, t);
                        if (bestTargetUtility < util) {
                            bestTargetUtility = util;
                            agent._targetID = t;
                        }
                    }
                }
           
            }
                    
            if (DSA._random == null) {
             _random = new Random();
            }
        }
        /*if (_number == _oldUtilityMatrix.getNumAgents())
            this.reassignFunctions();*/
        }

        /*public void reassignFunctions() {
            for (DSAFactorgraph agent: agents) {
                (agent.myFunctions).clear();
            }
            _targetController.clear();
            for (EntityID function: _assignedFunctions) {
                    ArrayList<EntityID> tn = _targetNeighbours.get(function);
                    if (tn.isEmpty()) {
                        myFunctions.add(function);
                        _targetController.put(function, _agentID);
                    } else {
                        Iterator agentIterator = agents.iterator();
                        while (agentIterator.hasNext()) {
                            DSAFactorgraph agent = (DSAFactorgraph)agentIterator.next();
                            if (tn.contains(agent._agentID)) {
                                _targetController.put(function, agent._agentID);
                                agent.myFunctions.add(function);
                                break;
                            }
                        }
                    }
            }
        }*/
        public void buildNeighborhood(EntityID function, ArrayList<EntityID> bestAgents, int count) {
            
            for (EntityID bestAgent: bestAgents) {
                if (!_agentNeighbours.containsKey(bestAgent)) {
                    _agentNeighbours.put(bestAgent, new ArrayList<EntityID>());
                }
            }
            
            ArrayList<EntityID> tempVars = new ArrayList<EntityID>();
            ArrayList<EntityID> tempFunc;
            ArrayList<EntityID> tempAgents;
            Iterator agentIterator = bestAgents.iterator();
            while (agentIterator.hasNext() && count < _dependencies) {
                EntityID agent = (EntityID)agentIterator.next();
                ArrayList<EntityID> consideredVariables = _consideredVariables.get(function);
                consideredVariables.add(agent);
                _consideredVariables.put(function, consideredVariables);
                if (_agentNeighbours.get(agent).size() < _dependencies) {
                    count++;
                    tempVars.add(agent);
                        tempFunc = _agentNeighbours.get(agent);
                        tempFunc.add(function);
                        tempAgents = _targetNeighbours.get(function);
                        tempAgents.add(agent);
                        
                        //System.out.println(agent.getValue()+" "+function.getValue());
                        _agentNeighbours.put(agent, tempFunc);
                        _targetNeighbours.put(function, tempAgents);
                }
                else {
                    ArrayList<EntityID> myTargets = _agentNeighbours.get(agent);
                    EntityID worstTarget = function;
                    double targetUtility = _utilityM.getUtility(agent, worstTarget);
                    double worstUtility = targetUtility;
                    for (EntityID target: myTargets) {
                        double oldUtility = _utilityM.getUtility(agent, target);
                        if (oldUtility < worstUtility) {
                            worstUtility = oldUtility;
                            worstTarget = target;
                        }
                    }
                    
                    if (worstUtility != targetUtility) {
                        count++;
                        myTargets.remove(worstTarget);
                        ArrayList<EntityID> myAgents = _targetNeighbours.get(worstTarget);
                        myAgents.remove(agent);
                        _targetNeighbours.put(worstTarget, myAgents);
                        myAgents = _targetNeighbours.get(function);
                        myAgents.add(agent);
                        _targetNeighbours.put(function, myAgents);
                        myTargets.add(function);
                        _agentNeighbours.put(agent, myTargets);
                        this.newNeighbour(worstTarget, agent);
                        }
                        
                    }
            }
     
    }
        
    public void newNeighbour(EntityID function, EntityID removedVariable) {
        ArrayList<EntityID> possibleNewNeighbours = new ArrayList<EntityID>();
        ArrayList<EntityID> me = new ArrayList<EntityID>();
        me.add(function);
        ArrayList<EntityID> best = (ArrayList<EntityID>)_utilityM.getNBestAgents(_utilityM.getNumAgents(), me);
        ArrayList alreadyConsidered = _consideredVariables.get(function);
        for (EntityID possibleNewNeighbour: best) {
            if (!alreadyConsidered.contains(possibleNewNeighbour))  {
                possibleNewNeighbours.add(possibleNewNeighbour);
            }
        }
        if (!possibleNewNeighbours.isEmpty()) {
        this.buildNeighborhood(function, possibleNewNeighbours, _dependencies-1);
        }
    }
    
    public boolean improveAssignment() {
        toReset = true;
        //System.out.println(_utilityM.getNumTargets());
        if (_utilityM == null) {
            _targetID = Assignment.UNKNOWN_TARGET_ID;
            return true;
        }
        else return super.improveAssignment();
    }
    
    
    public static ArrayList<EntityID> getNeighbours (EntityID agentID) {
        ArrayList<EntityID> targets = _agentNeighbours.get(agentID);
        ArrayList<EntityID> neighbours = new ArrayList<EntityID>();
                
        try{
        for (EntityID target: targets) {
            EntityID neighbour = _targetController.get(target);
            if (!neighbours.contains(neighbour))
                    neighbours.add(neighbour);
        }
        for (DSAFactorgraph agent: agents) {
            
        if (agent.getAgentID().getValue() == agentID.getValue()) {
        for (EntityID function: agent.myFunctions) {
            ArrayList<EntityID> neighbourhood = _targetNeighbours.get(function);
            for (EntityID neigh: neighbourhood) {
                if (!neighbours.contains(neigh)) {
                    neighbours.add(neigh);
                }
            }
        }
        }
         }
        }catch (NullPointerException n) {
            return new ArrayList<EntityID>();
        }
        //System.out.println("-----------------");
        return neighbours;
        
    }
    
    private void resetStructures() {
        if (toReset) {
            toReset = false;
        agents = new ArrayList<DSAFactorgraph>();
        _targetController = new HashMap<EntityID, EntityID>();
        _agentNeighbours = new HashMap<EntityID, ArrayList<EntityID>>();
        _assignedFunctions = new ArrayList<EntityID>();
        _consideredVariables = new HashMap<EntityID, ArrayList<EntityID>>();
        _targetNeighbours = new HashMap<EntityID, ArrayList<EntityID>>();
        _utilityM = _oldUtilityMatrix;
        //_number = 1;
    }
    }

}
