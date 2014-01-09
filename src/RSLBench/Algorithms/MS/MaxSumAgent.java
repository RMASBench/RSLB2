/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPAgent;
import RSLBench.Comm.Message;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Helpers.Utility.ProblemDefinition;
import RSLBench.Constants;

import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exception.PostServiceNotSetException;
import messages.MailMan;
import messages.MessageFactory;
import messages.MessageFactoryArrayDouble;
import factorgraph.NodeVariable;
import factorgraph.NodeFunction;
import factorgraph.NodeArgument;
import operation.OPlus;
import operation.OPlus_MaxSum;
import operation.OTimes;
import operation.OTimes_MaxSum;

/**
 * This class implements the MaxSum algorithm according to RMASBench specification.
 */
public class MaxSumAgent implements DCOPAgent {
    private static final Logger Logger = LogManager.getLogger(MaxSumAgent.class);

    /* Variables to setup the jMaxSum library */
    private static final MessageFactory JMS_MSG_FACTORY = new MessageFactoryArrayDouble();
    private static final OTimes JMS_OTIMES = new OTimes_MaxSum(JMS_MSG_FACTORY);
    private static final OPlus JMS_OPLUS = new OPlus_MaxSum(JMS_MSG_FACTORY);
    private static final MSumOperator_Sync JMS_OPERATOR = new MSumOperator_Sync(JMS_OTIMES, JMS_OPLUS);

    /* Class-wide variables (seriously...) */
    private static MailMan JMSMailMan = new MailMan();
    private static HashSet<NodeVariable> AllJMSVariables = new HashSet<>();
    private static HashSet<NodeFunction> AllJMSFunctions = new HashSet<>();
    private static ArrayList<MaxSumAgent> AllMaxSumAgents = new ArrayList<>();

    private static int _initializedAgents = 0;
    private static int _localNumberOfTargets = 70;
    private static final int _targetPerAgent = 4;//numero di funzioni per agente

    private static ProblemDefinition problemDefinition = null;

    private EntityID agentID;
    private EntityID targetID = Assignment.UNKNOWN_TARGET_ID;
    private AgentMS_Sync JMSAgent;
    private int _dependencies;
    private int _nMexForFG;
    private long _FGMexBytes;

    private static boolean toReset = false;
    private static HashMap<EntityID, ArrayList<EntityID>> _consideredVariables = new HashMap<>();

    @Override
    public void initialize(Config config, EntityID agentID, ProblemDefinition definition) {
        this.resetStructures();

        _nMexForFG = 0;
        _FGMexBytes = 0;
        _initializedAgents++;
        AllMaxSumAgents.add(this);
        problemDefinition = definition;
        _dependencies = config.getIntValue(Constants.KEY_MAXSUM_NEIGHBORS);

        JMSAgent = AgentMS_Sync.getAgent(agentID.getValue());//Agent.getAgent(_agentID.getValue());
        JMSAgent.setPostservice(JMSMailMan);
        JMSAgent.setOp(JMS_OPERATOR);

        // Each agent controls only one variable, so we can associate it with the agentid
        NodeVariable nodevariable = NodeVariable.getNodeVariable(agentID.getValue());
        AllJMSVariables.add(nodevariable);
        JMSAgent.addNodeVariable(nodevariable);

        // Assegnamento delle funzioni agli agenti
        List<EntityID> targets = problemDefinition.getNBestFires(_localNumberOfTargets, agentID);
        Iterator<EntityID> targetIterator = targets.iterator();
        int nAssignedFunctions = 0;
        while (nAssignedFunctions < _targetPerAgent && targetIterator.hasNext()) {
            EntityID nextTargetID = targetIterator.next();

            boolean alreadyAssigned = false;
            for (NodeFunction function : AllJMSFunctions) {
                if (function.getId() == nextTargetID.getValue()) {
                    alreadyAssigned = true;
                    break;
                }
            }

            if (!alreadyAssigned) {
                nAssignedFunctions++;
                NodeFunction target = NodeFunction.putNodeFunction(nextTargetID.getValue(), new RMASTabularFunction());
                AllJMSFunctions.add(target);
                JMSAgent.addNodeFunction(target);
                _consideredVariables.put(nextTargetID, new ArrayList<EntityID>());
            }
        }

        for (NodeFunction nodeTarget : JMSAgent.getFunctions()) {
            int count = 0;
            EntityID target = new EntityID(nodeTarget.getId());
            List<EntityID> bestAgents = problemDefinition.getNBestFireAgents(problemDefinition.getNumFireAgents(), target);
            this.buildNeighborhood(target, bestAgents, count);
        }

        if (_initializedAgents == problemDefinition.getNumFireAgents()) {
            tupleBuilder();
            reassignFunctions();
        }
    }

    public void reassignFunctions() {
        ArrayList<EntityID> agents = problemDefinition.getFireAgents();
        for (EntityID agent : agents) {
            AgentMS_Sync maxSumAgent = AgentMS_Sync.getAgent(agent.getValue());
            maxSumAgent.resetNodeFunction();
        }

        for (NodeFunction function : AllJMSFunctions) {
            HashSet<NodeVariable> neighbour = function.getNeighbour();
            Iterator<NodeVariable> it = neighbour.iterator();
            if (it.hasNext()) {
                NodeVariable controller = it.next();
                AgentMS_Sync agent = AgentMS_Sync.getAgent(controller.getId());
                agent.addNodeFunction(function);
            } else {
                JMSAgent.addNodeFunction(function);
            }
        }
    }

    private NodeFunction fetchFunctionNode(Integer id) {
        try {
            return NodeFunction.getNodeFunction(id);
        } catch (exception.FunctionNotPresentException e) {
            Logger.fatal("Unable to fetch function node.", e);
            System.exit(0);
        }
        return null;
    }

    public void buildNeighborhood(EntityID target, List<EntityID> bestAgents, int count) {
        NodeFunction nodeTarget = fetchFunctionNode(target.getValue());

        int tarID = target.getValue();
        Iterator<EntityID> agentIterator = bestAgents.iterator();
        while (agentIterator.hasNext() && count < _dependencies) {
            EntityID agent = agentIterator.next();
            ArrayList<EntityID> consideredVariables = _consideredVariables.get(target);
            consideredVariables.add(agent);
            _consideredVariables.put(target, consideredVariables);
            NodeVariable tempVar = NodeVariable.getNodeVariable(agent.getValue());
            if (tempVar.getNeighbour().size() < _dependencies) {
                _nMexForFG += 2;
                _FGMexBytes += 2*4;
                count++;

                nodeTarget.addNeighbour(NodeVariable.getNodeVariable(agent.getValue()));
                NodeVariable.getNodeVariable(agent.getValue()).addNeighbour(nodeTarget);
                NodeVariable.getNodeVariable(agent.getValue()).addValue(NodeArgument.getNodeArgument(nodeTarget.getId()));

            } else {
                _FGMexBytes += 2*4;
                _nMexForFG += 2;
                HashSet<NodeFunction> assignedToMe = tempVar.getNeighbour();
                EntityID worstTarget = new EntityID(tarID);
                double targetUtility = problemDefinition.getFireUtility(agent, worstTarget);
                double worstUtility = targetUtility;
                for (NodeFunction assigned : assignedToMe) {
                    double oldUtility = problemDefinition.getFireUtility(agent, new EntityID(assigned.getId()));
                    if (oldUtility < worstUtility) {
                        worstUtility = oldUtility;
                        worstTarget = new EntityID(assigned.getId());
                    }
                }

                if (worstUtility != targetUtility) {
                    _FGMexBytes += 4;
                    _nMexForFG++;
                    count++;
                    nodeTarget.addNeighbour(tempVar);

                    NodeFunction oldTarget = fetchFunctionNode(worstTarget.getValue());
                    RMASNodeFunctionUtility.removeNeighbourBeforeTuples(oldTarget, tempVar);
                    tempVar.changeNeighbour(oldTarget, nodeTarget);
                    this.newNeighbour(worstTarget);
                    tempVar.changeValue(NodeArgument.getNodeArgument(worstTarget.getValue()), NodeArgument.getNodeArgument(nodeTarget.getId()));

                }
            }

        }
    }

    private void tupleBuilder() {
        for (NodeFunction function : AllJMSFunctions) {
            double cost = 0;
            int countAgent = 0;
            int target = function.getId();
            int[] possibleValues = {0, target};
            int[][] combinations = createCombinations(possibleValues);
            for (int[] arguments : combinations) {
                NodeArgument[] arg = new NodeArgument[function.size()];

                for (int i = 0; i < function.size(); i++) {
                    arg[i] = NodeArgument.getNodeArgument(arguments[i]);
                    Iterator<NodeVariable> prova = function.getNeighbour().iterator();
                    if (((Integer) arg[i].getValue()).intValue() == target) {
                        countAgent++;
                        NodeVariable var = prova.next();
                        cost = cost + problemDefinition.getFireUtility(new EntityID(var.getId()), new EntityID(target));
                    }
                }
                cost -= problemDefinition.getUtilityPenalty(new EntityID(target), countAgent);

                function.getFunction().addParametersCost(arg, cost);
            }
        }
    }

    @Override
    public boolean improveAssignment() {
        //this.printNMex();
        Set<NodeVariable> vars = JMSAgent.getVariables();
        Iterator<NodeVariable> it = vars.iterator();
        NodeVariable var = it.next();
        HashSet<NodeFunction> func = var.getNeighbour();
        if (!func.isEmpty()) {
            JMSAgent.updateVariableValue();
        }

        toReset = true;
        for (NodeVariable variable : JMSAgent.getVariables()) {
            try {
                String target = variable.getStateArgument().getValue().toString();
                targetID = new EntityID(Integer.parseInt(target));
            } catch (exception.VariableNotSetException e) {
                targetID = problemDefinition.getHighestTargetForAgent(agentID);
            }
        }

        return true;
    }

    @Override
    public EntityID getAgentID() {
        return agentID;
    }

    @Override
    public EntityID getTargetID() {
        return targetID;
    }

    @Override
    public Collection<Message> sendMessages(CommunicationLayer com) {
        Collection<Message> mexQ = new ArrayList<>();
        Collection<Message> mexR = new ArrayList<>();
        Collection<Message> allmex = new ArrayList<>();
        try {
            //System.out.println("Stampa messaggi 1");
            mexQ = JMSAgent.sendQMessages();

            Iterator<Message> iteratorm = mexQ.iterator();
            while(iteratorm.hasNext()){
                //usare com per i Q per ogni mex devo recuperare destinatario
                MS_MessageQ messageQ = (MS_MessageQ)iteratorm.next();

                /* PRovvisorio..ricavo funzioni ciclando INEFFICIENTE*/
                Iterator<MaxSumAgent> agentiter = AllMaxSumAgents.iterator();
                MaxSumAgent agent;
                while(agentiter.hasNext()){
                    agent = agentiter.next();
                    if(agent.isLocalFunction(messageQ.getFunction())){ // se la funzione è di proprietà dell'agente X il destinatario è LUI
                        com.send(agent.getAgentID(), messageQ); // Se trovo il destinatario invio
                        break;
                    }
                }
            }

            //System.out.println("Stampa messaggi 2");
            mexR = JMSAgent.sendRMessages();
            iteratorm = mexR.iterator();
            MS_MessageR messageR;
            while(iteratorm.hasNext()){
                //usare com per i R per ogni mex devo recuperare destinatario
                messageR = (MS_MessageR)iteratorm.next();
                com.send(new EntityID(messageR.getVariable().getId()), messageR); //Ricavo destinatario dall'id della variabile
            }

            JMSAgent.sendZMessages(); // controllare sendZ
        } catch (PostServiceNotSetException p) {
            Logger.fatal("Unconfigured max-sum library", p);
            System.exit(0);
        }

        allmex.addAll(mexQ);
        allmex.addAll(mexR);
        return allmex;
    }

    private boolean isLocalFunction(NodeFunction f) {
        return JMSAgent.getFunctions().contains(f);
    }

    @Override
    public void receiveMessages(Collection<Message> messages) {
        Collection<Message> mexQ = new ArrayList<>();
        Collection<Message> mexR = new ArrayList<>();

        for (Message msg : messages) {
            MS_Message mex = (MS_Message)msg;
            if (mex.getMessageType().compareTo("Q") == 0) {
                mexQ.add(mex);
            } else if (mex.getMessageType().compareTo("R") == 0) {
                mexR.add(mex);
            }
        }

        try {
            JMSAgent.readQMessages(mexQ);
            JMSAgent.readRMessages(mexR);
        } catch (PostServiceNotSetException ex) {
            Logger.fatal("Uninitialized max-sum library", ex);
        }

    }

    private int[][] createCombinations(int[] possibleValues) {
        int totalCombinations = (int) Math.pow(2, _dependencies);

        int[][] combinationsMatrix = new int[totalCombinations][_dependencies];
        int changeIndex = 1;

        for (int i = 0; i < _dependencies; i++) {
            int index = 0;
            int count = 1;

            changeIndex = changeIndex * possibleValues.length;
            for (int j = 0; j < totalCombinations; j++) {
                combinationsMatrix[j][i] = possibleValues[index];
                if (count == (totalCombinations / changeIndex)) {
                    count = 1;
                    index = (index + 1) % (possibleValues.length);
                } else {
                    count++;
                }

            }
        }
        return combinationsMatrix;
    }

    private void resetStructures() {
        if (toReset) {
            toReset = false;
            AgentMS_Sync.resetIds();
            NodeVariable.resetIds();
            NodeFunction.resetIds();
            NodeArgument.resetIds();

            //_op = new MSumOperator(_otimes, _oplus);
            JMSMailMan = new MailMan();
            AllJMSVariables = new HashSet<>();
            AllJMSFunctions = new HashSet<>();
            _initializedAgents = 0;
            _consideredVariables = new HashMap<>();
        }

    }

    @Override
    public long getConstraintChecks() {
        int totalnccc = 0;
        for (NodeFunction function : AllJMSFunctions) {
            totalnccc += ((RMASTabularFunction) function.getFunction()).getNCCC();
        }
        return totalnccc;
    }

    public void printFG() {
        try (FileWriter fw = new FileWriter("factor_graph.txt", true)) {
            for (NodeVariable var : AllJMSVariables) {
                int agent_id = var.getId();
                AgentMS_Sync agent = AgentMS_Sync.getAgent(agent_id);
                Set<NodeFunction> agent_fun = agent.getFunctions();

                fw.write("Agent: " + agent_id + "\n");
                fw.write("\t functions: " + agent_fun.toString() + "\n");
                fw.write("\t var connected to: " + NodeVariable.getNodeVariable(agent_id).getNeighbour().toString() + "\n\n");
                fw.flush();
            }
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    public void printDimTuples() {
        for (NodeVariable var : AllJMSVariables) {
            int agent_id = var.getId();
            AgentMS_Sync agent = AgentMS_Sync.getAgent(agent_id);
            Set<NodeFunction> agent_fun = agent.getFunctions();

            try (FileWriter fw = new FileWriter("tuples_dim.txt", true)) {
                fw.write("Agent: " + agent_id + "\n");

                for (NodeFunction f : agent_fun) {
                    int num_tup = 1;
                    int num_real = 1;
                    for (NodeVariable v : f.getNeighbour()) {
                        num_tup *= v.getNeighbour().size();
                        num_real *= 2;
                    }
                    num_tup = (num_tup == 1) ? 0 : num_tup;
                    num_real = (num_real == 1) ? 0 : num_real;
                    fw.write("\t Funtion: " + f.id() + " dim: " + num_tup + "\n");
                    fw.write("\t Funtion: " + f.id() + " dim_real: " + num_real + "\n");
                }

                fw.write("----------------------------------------------------------------\n");
                fw.flush();
            } catch (IOException ex) {
                Logger.error(ex);
            }
        }
    }

    public void printNMex() {
        try (FileWriter fw = new FileWriter("tables.stats", true)) {
            for (NodeFunction function : (HashSet<NodeFunction>) JMSAgent.getFunctions()) {
                fw.write("Number of tuples tried for function " + function.getId() + ": " + ((RMASTabularFunction) function.getFunction()).getNCCC() + "\n");
                fw.flush();
            }
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    @Override
    public int getNumberOfOtherMessages() {
        return _nMexForFG;
    }
    @Override
    public long getDimensionOfOtherMessages() {
        return _FGMexBytes;
    }

    private void newNeighbour(EntityID function) {
        ArrayList<EntityID> possibleNewNeighbours = new ArrayList<>();

        List<EntityID> best = problemDefinition.getNBestFireAgents(problemDefinition.getNumFireAgents(), function);
        ArrayList<EntityID> alreadyConsidered = _consideredVariables.get(function);
        for (EntityID possibleNewNeighbour : best) {
            if (!alreadyConsidered.contains(possibleNewNeighbour)) {
                possibleNewNeighbours.add(possibleNewNeighbour);
            }
        }

        if (!possibleNewNeighbours.isEmpty()) {
            this.buildNeighborhood(function, possibleNewNeighbours, _dependencies - 1);
        }
    }
}

