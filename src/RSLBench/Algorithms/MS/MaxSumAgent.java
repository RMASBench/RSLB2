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

    /* Class-wide variables (seriously...) */
    private static MSumOperator_Sync jMSOperator = new MSumOperator_Sync(JMS_OTIMES, JMS_OPLUS);
    private static MailMan jMSMailMan = new MailMan();
    private static HashSet<NodeVariable> allJMSVariables = new HashSet<>();
    private static HashSet<NodeFunction> allJMSFunctions = new HashSet<>();
    private static ArrayList<MaxSumAgent> allMaxSumAgents = new ArrayList<>();

    private static int maxLinksPerNode;
    private static int nPrioritizedFiresToFetch = 70;

    private static ProblemDefinition problemDefinition = null;

    private EntityID agentID;
    private EntityID targetID = Assignment.UNKNOWN_TARGET_ID;
    private JMSAgent jMSAgent;
    private int nFGMessages = 0;
    private long nFGSentBytes = 0;

    private static HashMap<EntityID, ArrayList<EntityID>> _consideredVariables = new HashMap<>();

    /**
     * Get the jMaxSum agent for this MaxSum agent.
     *
     * @return the jMaxSum agent
     */
    public JMSAgent getJMSAgent() {
        return jMSAgent;
    }

    @Override
    public void initialize(Config config, EntityID agentID, ProblemDefinition definition) {
        allMaxSumAgents.add(this);
        problemDefinition = definition;
        maxLinksPerNode = config.getIntValue(Constants.KEY_MAXSUM_NEIGHBORS);

        this.agentID = agentID;
        jMSAgent = JMSAgent.getAgent(agentID.getValue());
        jMSAgent.setPostservice(jMSMailMan);
        jMSAgent.setOp(jMSOperator);

        // Each agent controls only one variable, so we can associate it with the agentid
        NodeVariable nodevariable = NodeVariable.getNodeVariable(agentID.getValue());
        allJMSVariables.add(nodevariable);
        jMSAgent.addVariable(nodevariable);

        // Assign functions to agents (in order of preference)
        List<EntityID> targets = problemDefinition.getNBestFires(nPrioritizedFiresToFetch, agentID);
        Iterator<EntityID> targetIterator = targets.iterator();
        int nAssignedFunctions = 0;
        while (nAssignedFunctions < maxLinksPerNode && targetIterator.hasNext()) {
            EntityID nextTargetID = targetIterator.next();

            boolean alreadyAssigned = false;
            for (NodeFunction function : allJMSFunctions) {
                if (function.getId() == nextTargetID.getValue()) {
                    alreadyAssigned = true;
                    break;
                }
            }

            if (!alreadyAssigned) {
                nAssignedFunctions++;
                NodeFunction target = NodeFunction.putNodeFunction(nextTargetID.getValue(), new RMASTabularFunction());
                allJMSFunctions.add(target);
                jMSAgent.addFunction(target);
                _consideredVariables.put(nextTargetID, new ArrayList<EntityID>());
            }
        }

        for (NodeFunction nodeTarget : jMSAgent.getFunctions()) {
            int count = 0;
            EntityID target = new EntityID(nodeTarget.getId());
            List<EntityID> bestAgents = problemDefinition.getNBestFireAgents(problemDefinition.getNumFireAgents(), target);
            this.buildNeighborhood(target, bestAgents, count);
        }
    }

    private static void reassignFunctions() {
        ArrayList<EntityID> agents = problemDefinition.getFireAgents();
        for (EntityID agent : agents) {
            JMSAgent maxSumAgent = JMSAgent.getAgent(agent.getValue());
            maxSumAgent.clearFunctions();
        }

        for (NodeFunction function : allJMSFunctions) {
            if (function.getNeighbour().isEmpty()) {
                // Assign this function to some random node
                Logger.warn("Fire " + function.getId() + " has no candidates due to pruning!");
                MaxSumAgent a = allMaxSumAgents.get(allMaxSumAgents.size()-1);
                a.getJMSAgent().addFunction(function);
            } else {
                NodeVariable controller = function.getNeighbour().iterator().next();
                JMSAgent agent = JMSAgent.getAgent(controller.getId());
                agent.addFunction(function);
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
        while (agentIterator.hasNext() && count < maxLinksPerNode) {
            EntityID agent = agentIterator.next();
            ArrayList<EntityID> consideredVariables = _consideredVariables.get(target);
            consideredVariables.add(agent);
            _consideredVariables.put(target, consideredVariables);
            NodeVariable tempVar = NodeVariable.getNodeVariable(agent.getValue());
            if (tempVar.getNeighbour().size() < maxLinksPerNode) {
                nFGMessages += 2;
                nFGSentBytes += 2*4;
                count++;

                nodeTarget.addNeighbour(NodeVariable.getNodeVariable(agent.getValue()));
                NodeVariable.getNodeVariable(agent.getValue()).addNeighbour(nodeTarget);
                NodeVariable.getNodeVariable(agent.getValue()).addValue(NodeArgument.getNodeArgument(nodeTarget.getId()));

            } else {
                nFGSentBytes += 2*4;
                nFGMessages += 2;
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
                    nFGSentBytes += 4;
                    nFGMessages++;
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

    private static void tupleBuilder() {
        for (NodeFunction function : allJMSFunctions) {
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
        Set<NodeVariable> vars = jMSAgent.getVariables();
        Iterator<NodeVariable> it = vars.iterator();
        NodeVariable var = it.next();
        HashSet<NodeFunction> func = var.getNeighbour();
        if (!func.isEmpty()) {
            jMSAgent.updateVariablesValues();
        }

        for (NodeVariable variable : jMSAgent.getVariables()) {
            try {
                String target = variable.getStateArgument().getValue().toString();
                targetID = new EntityID(Integer.parseInt(target));
            } catch (exception.VariableNotSetException e) {
                Logger.warn("Agent " + getAgentID() + " unassigned!");
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
        Collection<MS_MessageQ> qMessages = jMSAgent.sendQMessages();
        for (MS_MessageQ messageQ : qMessages) {
            // Locate the agent that controls this function, and send the message to that ID
            for (MaxSumAgent agent : allMaxSumAgents) {
                if (agent.isLocalFunction(messageQ.getFunction())) {
                    com.send(agent.getAgentID(), messageQ);
                    break;
                }
            }
        }


        Collection<MS_MessageR> rMessages = jMSAgent.sendRMessages();
        for (MS_MessageR messageR : rMessages) {
            // The recipient variable id matches the EntityID of the recipient agent
            com.send(new EntityID(messageR.getVariable().getId()), messageR);
        }

        jMSAgent.sendZMessages();

        // Combine the lists of both message types and return all of them
        Collection<Message> allmex = new ArrayList<>();
        allmex.addAll(qMessages);
        allmex.addAll(rMessages);
        return allmex;
    }

    private boolean isLocalFunction(NodeFunction f) {
        return jMSAgent.getFunctions().contains(f);
    }

    @Override
    public void receiveMessages(Collection<Message> messages) {
        Collection<MS_MessageQ> mexQ = new ArrayList<>();
        Collection<MS_MessageR> mexR = new ArrayList<>();

        for (Message msg : messages) {
            MS_Message mex = (MS_Message)msg;
            if (mex instanceof MS_MessageQ) {
                mexQ.add((MS_MessageQ)mex);
            } else if (mex instanceof MS_MessageR) {
                mexR.add((MS_MessageR)mex);
            }
        }

        jMSAgent.readQMessages(mexQ);
        jMSAgent.readRMessages(mexR);
    }

    private static int[][] createCombinations(int[] possibleValues) {
        int totalCombinations = (int) Math.pow(2, maxLinksPerNode);

        int[][] combinationsMatrix = new int[totalCombinations][maxLinksPerNode];
        int changeIndex = 1;

        for (int i = 0; i < maxLinksPerNode; i++) {
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

    /**
     * Resets the static members of this class so that it gets ready for a new step of the
     * simulator.
     */
    public static void reset() {
        Logger.debug("Resetting!");

        JMSAgent.resetIds();
        NodeVariable.resetIds();
        NodeFunction.resetIds();
        NodeArgument.resetIds();
        jMSOperator = new MSumOperator_Sync(JMS_OTIMES, JMS_OPLUS);
        jMSMailMan = new MailMan();

        allJMSVariables.clear();
        allJMSFunctions.clear();
        _consideredVariables.clear();
        allMaxSumAgents.clear();
    }

    /**
     * Finishes the initialization phase (after constructing all agents)
     */
    public static void finishInitialization() {
        tupleBuilder();
        reassignFunctions();
    }

    @Override
    public long getConstraintChecks() {
        int totalnccc = 0;
        for (NodeFunction function : allJMSFunctions) {
            totalnccc += ((RMASTabularFunction) function.getFunction()).getNCCC();
        }
        return totalnccc;
    }

    private static int n_graph = 0;
    /**
     * Prints the FactorGraph in .dot format (to be visualized with graphviz or similar)
     */
    private void printFG() {
        n_graph++;
        try (FileWriter fw = new FileWriter("factor_graph" + n_graph + ".dot", false)) {
            fw.write("graph Factor {\n");
            for (NodeVariable var : allJMSVariables) {
                int agent_id = var.getId();
                JMSAgent agent = JMSAgent.getAgent(agent_id);
                Set<NodeFunction> agent_fun = agent.getFunctions();

                fw.write(agent_id + " [shape=box]\n");
                for (NodeFunction f : agent_fun) {
                    fw.write(agent_id + " -- " + f.getId() + " [color=blue]\n");
                }
                for (NodeFunction f : NodeVariable.getNodeVariable(agent_id).getNeighbour()) {
                    if (!agent_fun.contains(f)) {
                        fw.write(agent_id + " -- " + f.getId() + "\n");
                    }
                }
                fw.flush();
            }
            fw.write("}\n\n");
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    public void printDimTuples() {
        for (NodeVariable var : allJMSVariables) {
            int agent_id = var.getId();
            JMSAgent agent = JMSAgent.getAgent(agent_id);
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
            for (NodeFunction function : (HashSet<NodeFunction>) jMSAgent.getFunctions()) {
                fw.write("Number of tuples tried for function " + function.getId() + ": " + ((RMASTabularFunction) function.getFunction()).getNCCC() + "\n");
                fw.flush();
            }
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    @Override
    public int getNumberOfOtherMessages() {
        return nFGMessages;
    }
    @Override
    public long getDimensionOfOtherMessages() {
        return nFGSentBytes;
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
            this.buildNeighborhood(function, possibleNewNeighbours, maxLinksPerNode - 1);
        }
    }
}

