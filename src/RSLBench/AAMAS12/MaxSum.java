/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.AAMAS12;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DecentralAssignment;
import RSLBench.Comm.AbstractMessage;
import RSLBench.Comm.ComSimulator;

import RSLBench.Helpers.UtilityMatrix;
import RSLBench.Params;
import java.util.Collection;
import java.util.HashSet;
import rescuecore2.worldmodel.EntityID;
import java.util.LinkedList;
import maxsum.Agent;

import messages.MessageQ;
import messages.MessageR;

import exception.PostServiceNotSetException;
import exception.VariableNotSetException;
import messages.MailMan;
import messages.MessageFactory;
import messages.MessageFactoryArrayDouble;
import factorgraph.NodeVariable;
import factorgraph.NodeFunction;
import factorgraph.NodeArgument;
import RSLBench.maxSumAdapters.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import operation.*;

/**
 * This class implements the MaxSum algorithm according to RMASBench specification.
 */
public class MaxSum implements DecentralAssignment {

    private static UtilityMatrix _utilityM = null;
    private EntityID _agentID;
    private EntityID _targetID = Assignment.UNKNOWN_TARGET_ID;
    private static MailMan _com = new MailMan();
    private AgentMS_Sync _maxSumAgent;
    //private Agent _maxSumAgent;
    private static MessageFactory _mfactory = new MessageFactoryArrayDouble();
    private static OTimes _otimes = new OTimes_MaxSum(_mfactory);
    private static OPlus _oplus = new OPlus_MaxSum(_mfactory);
    private static double _latestValue_start = Double.NEGATIVE_INFINITY;
    private static MSumOperator_Sync _op = new MSumOperator_Sync(_otimes, _oplus);
//private static MSumOperator _op = new MSumOperator(_otimes, _oplus);
    private static HashSet<NodeVariable> _variables = new HashSet<NodeVariable>();
    private static HashSet<NodeFunction> _functions = new HashSet<NodeFunction>();
    private static final int _targetPerAgent = 4;//numero di funzioni per agente
    private static int _dependencies = Params.MaxSum_NUMBER_OF_NEIGHBOURS; //numero di variabili per funzione massimo e viceversa
    public static boolean toReset = false;
    private static int _initializedAgents = 0;
    private static int _localNumberOfTargets = 70;
    private static HashMap<EntityID, ArrayList<EntityID>> _consideredVariables = new HashMap<EntityID, ArrayList<EntityID>>();
    private int _sizeMex, _nMex, _nMexForFG;
    private long _FGMexBytes;
    private static double BIGNUMBER = 10000;
    
    /*PROVVISORIO*
     * aggiungo una lista in memoria condivisa che contiene tutti gli agenti
     */
    private static ArrayList<MaxSum> _allAgents = new ArrayList<MaxSum>();
    
    @Override
    public void initialize(EntityID agentID, UtilityMatrix utilityM) {
        this.resetStructures();
        
        
        _nMexForFG = 0;
        _FGMexBytes = 0;
        _initializedAgents++;
        _agentID = agentID;
        _utilityM = utilityM;

        _maxSumAgent = AgentMS_Sync.getAgent(_agentID.getValue());//Agent.getAgent(_agentID.getValue());
        _maxSumAgent.setPostservice(_com);
        _maxSumAgent.setOp(_op);
        //variable
        //each agent controls only one variable, so we can associate it with the agentid
        NodeVariable nodevariable = NodeVariable.getNodeVariable(_agentID.getValue());

        _variables.add(nodevariable);
        _maxSumAgent.addNodeVariable(nodevariable);

        // Assegnamento delle funzioni agli agenti
        ArrayList<EntityID> me = new ArrayList<EntityID>();
        me.add(_agentID);
        ArrayList<EntityID> targets = (ArrayList<EntityID>) _utilityM.getNBestTargets(_localNumberOfTargets, me);
        /*FileWriter fw = null;
        try {
        fw = new FileWriter("history.txt", true);
        } catch (IOException i) {
        }*/
        Iterator targetIterator = targets.iterator();
        for (int i = 0; i < _targetPerAgent; i++) {
            if (targetIterator.hasNext()) {
                //_nMexForFG +=2;
                EntityID nextTargetID = (EntityID) targetIterator.next();
                boolean alreadyAssigned = false;
                for (NodeFunction function : _functions) {
                    if (function.getId() == nextTargetID.getValue()) {
                        alreadyAssigned = true;
                        break;
                    }
                }
                if (!alreadyAssigned) {
                    /*try {
                    fw.write("Sono l'agente "+_agentID+" e controllo temporaneamente la funzione "+nextTargetID+".\n");               
                    fw.flush();
                    } catch (IOException ii) {}*/
                    NodeFunction target = NodeFunction.putNodeFunction(nextTargetID.getValue(), new RMASTabularFunction());
                    _functions.add(target);
                    _maxSumAgent.addNodeFunction(target);
                    _consideredVariables.put(nextTargetID, new ArrayList<EntityID>());
                } else {
                    i--;
                }
            } else {
                break;
            }
        }
        /*try {
        fw.close();
        } catch (IOException ii) {}*/
        /*for (NodeFunction fun: _functions) {
        System.out.println(fun.getId());
        }*/
        //costruisco il factorGraph: assegno le funzioni alle variabili e viceversa
        Iterator factorGraphIterator = _maxSumAgent.getFunctions().iterator();
        while (factorGraphIterator.hasNext()) {
            NodeFunction nodeTarget = (NodeFunction) factorGraphIterator.next();
            int count = 0;
            ArrayList<EntityID> myTarget = new ArrayList<EntityID>();
            int tarID = nodeTarget.getId();
            EntityID target = _utilityM.getTargetID(tarID);
            myTarget.add(target);
            ArrayList<EntityID> bestAgents = (ArrayList<EntityID>) _utilityM.getNBestAgents(_utilityM.getNumAgents(), myTarget);
            //System.out.println("");
            this.buildNeighborhood(target, bestAgents, count);

        }
        /*System.out.println("Stampa di tutti i vicini di tutte le funzioni");
        for (NodeFunction function: _functions) {
        System.out.print("Sono la funzione " + function.getId() + " e i miei vicini sono ");
        
        Iterator<NodeVariable> iterator = function.getNeighbour().iterator();
        while (iterator.hasNext()) {
        System.out.print(" "+iterator.next().getId());
        }
        System.out.println("");
        }
        System.out.println("Stampa di tutti i vicini delle mie funzioni");
        for (NodeFunction function : _maxSumAgent.getFunctions()) {
        System.out.println("Sono la funzione "+function.getId()+ " e i miei vicini sono: "); 
        Iterator<NodeVariable> iterator = function.getNeighbour().iterator();
        while (iterator.hasNext()) {
        System.out.print(" "+iterator.next().getId());
        }
        System.out.println("");
        
        }*/

        if (_initializedAgents == _utilityM.getNumAgents()) {
            //System.out.println("Sono nel tuplebuilder");
            tupleBuilder();
            reassignFunctions();
            /*for(NodeFunction function: _functions) {
            function.getFunction().toString();
            }*/
        }

        /*boolean noFileWriter = false;
        FileWriter fw = null;
        try {
        fw = new FileWriter("tables.stats", true);
        } catch (IOException i) {
        noFileWriter = true;
        }
        for(NodeFunction function: _functions) {
        if (!noFileWriter) {
        try {
        fw.write("Table dimension for function "+function.getId()+": "+Math.pow(2,function.getNeighbour().size())+"\n");
        
        fw.flush();
        fw.close();
        } catch (IOException i) {}
        }
        
        /*System.out.println("L'agente "+_agentID.getValue()+" ha "+_maxSumAgent.getFunctions().size()+" vicini.");
        System.out.println("La variabile "+nodevariable.getId()+" ha "+nodevariable.getNeighbour().size()+" vicini.");*/
        //}
        _allAgents.add(this); // PROVVISORIO!
    }

    public void reassignFunctions() {
        /*FileWriter fw = null;
        try {
        fw = new FileWriter("history.txt", true);
        } catch (IOException i) {
        }*/
        ArrayList<EntityID> agents = (ArrayList<EntityID>) _utilityM.getAgents();
        for (EntityID agent : agents) {
            AgentMS_Sync maxSumAgent = AgentMS_Sync.getAgent(agent.getValue());
//Agent maxSumAgent = Agent.getAgent(agent.getValue());
            maxSumAgent.resetNodeFunction();
            /*HashSet<NodeVariable> var = (HashSet<NodeVariable>)maxSumAgent.getVariables();
            for (NodeVariable v: var) {
            HashSet<NodeFunction> fun = (HashSet<NodeFunction>)v.getNeighbour();
            for (NodeFunction f: fun) {
            try {
            fw.write("Sono la variabile "+_agentID+" e un mio vicino è la funzione "+f.getId()+".\n");               
            fw.flush();
            } catch (IOException ii) {}  
            }
            }
            HashSet<NodeFunction> funcs = (HashSet<NodeFunction>)maxSumAgent.getFunctions();
            for (NodeFunction func: funcs) {
            HashSet<NodeVariable> vars = (HashSet<NodeVariable>)func.getNeighbour();
            for (NodeVariable variab: vars) {
            try {
            fw.write("Sono la funzione "+func.getId()+" e un mio vicino è la variabile "+variab.getId()+".\n");               
            fw.flush();
            fw.close();
            } catch (IOException ii) {}  
            }
            }*/
        }
        for (NodeFunction function : _functions) {
            HashSet<NodeVariable> neighbour = function.getNeighbour();
            Iterator it = neighbour.iterator();
            if (it.hasNext()) {
                NodeVariable controller = (NodeVariable) it.next();
                AgentMS_Sync agent = AgentMS_Sync.getAgent(controller.getId());
//Agent agent = Agent.getAgent(controller.getId());
                agent.addNodeFunction(function);

            } else {
                _maxSumAgent.addNodeFunction(function);
            }
        }
    }

    public void buildNeighborhood(EntityID target, ArrayList<EntityID> bestAgents, int count) {
        NodeFunction nodeTarget = null;
        try {
            nodeTarget = NodeFunction.getNodeFunction(target.getValue());
        } catch (exception.FunctionNotPresentException e) {
            e.printStackTrace();
            System.exit(0);
        }
        int tarID = target.getValue();
        Iterator agentIterator = bestAgents.iterator();
        while (agentIterator.hasNext() && count < _dependencies) {
            EntityID agent = (EntityID) agentIterator.next();
            ArrayList<EntityID> consideredVariables = _consideredVariables.get(target);
            consideredVariables.add(agent);
            _consideredVariables.put(target, consideredVariables);
            NodeVariable tempVar = NodeVariable.getNodeVariable(agent.getValue());
            if (tempVar.getNeighbour().size() < _dependencies) {
                _nMexForFG += 2;
                _FGMexBytes += 2*4;
                count++;
                //System.out.println("Sto costruendo il factorgraph");
                nodeTarget.addNeighbour(NodeVariable.getNodeVariable(agent.getValue()));
                NodeVariable.getNodeVariable(agent.getValue()).addNeighbour(nodeTarget);
                NodeVariable.getNodeVariable(agent.getValue()).addValue(NodeArgument.getNodeArgument(nodeTarget.getId()));

            } else {
                _FGMexBytes += 2*4;
                _nMexForFG += 2;
                HashSet<NodeFunction> assignedToMe = tempVar.getNeighbour();
                EntityID worstTarget = _utilityM.getTargetID(tarID);
                double targetUtility = _utilityM.getUtility(agent, worstTarget);
                double worstUtility = targetUtility;
                for (NodeFunction assigned : assignedToMe) {
                    double oldUtility = _utilityM.getUtility(agent, _utilityM.getTargetID(assigned.getId()));
                    if (oldUtility < worstUtility) {
                        worstUtility = oldUtility;
                        worstTarget = _utilityM.getTargetID(assigned.getId());
                    }
                }

                if (worstUtility != targetUtility) {
                    _FGMexBytes += 4;
                    _nMexForFG++;
                    count++;
                    nodeTarget.addNeighbour(tempVar);
                    try {
                        RMASNodeFunctionUtility.removeNeighbourBeforeTuples(NodeFunction.getNodeFunction(worstTarget.getValue()), tempVar);//NodeFunction.getNodeFunction(worstTarget.getValue()).removeNeighbourBeforeTuples(tempVar);
                        tempVar.changeNeighbour(NodeFunction.getNodeFunction(worstTarget.getValue()), nodeTarget);
                        this.newNeighbour(worstTarget, agent);
                    } catch (exception.FunctionNotPresentException e) {
                        e.printStackTrace();
                        System.exit(0);
                    }
                    tempVar.changeValue(NodeArgument.getNodeArgument(worstTarget.getValue()), NodeArgument.getNodeArgument(nodeTarget.getId()));

                }
            }

        }
        //System.out.println("--------------------");
    }

    private void tupleBuilder() {
        //System.out.println("Costruisco le tuple.");
        for (NodeFunction function : _functions) {
            double cost = 0;
            int countAgent = 0;
            int targetID = function.getId();
            int[] possibleValues = {0, targetID};
            int[][] combinations = createCombinations(possibleValues);
            for (int[] arguments : combinations) {
                NodeArgument[] arg = new NodeArgument[function.size()];
                //System.out.print("I vicini sono: ");
                for (int i = 0; i < function.size(); i++) {
                    arg[i] = NodeArgument.getNodeArgument(arguments[i]);
                    Iterator prova = function.getNeighbour().iterator();
                    if (((Integer) arg[i].getValue()).intValue() == targetID) {
                        countAgent++;
                        NodeVariable var = (NodeVariable) prova.next();
                        //System.out.print(var.getId());
                        cost = cost + _utilityM.getUtility((EntityID) _utilityM.getAgentIDFromNumericID(var.getId()), _utilityM.getTargetID(targetID));
                    }
                }
                cost = cost * 1000000000;
                if (countAgent > _utilityM.getRequiredAgentCount(_utilityM.getTargetID(targetID))) {
                    cost = -BIGNUMBER;
                }
                //System.out.println("");


                function.getFunction().addParametersCost(arg, cost);

            }
        }

        /*printFG();
        printDimTuples();
        FileWriter fw = null;
        try {

            fw = new FileWriter("factor_graph.txt", true);
            fw.write("------------------------------------------------------------------------------------------\n");
            fw.write("------------------------------------------------------------------------------------------\n");
            fw.flush();
            fw.close();
            
            fw = new FileWriter("tuples_dim.txt", true);
            fw.write("------------------------------------------------------------------------------------------\n");
            fw.write("------------------------------------------------------------------------------------------\n");
            fw.flush();
            fw.close();
        } catch (IOException i) {
        }*/

    }
    @Override
    public boolean improveAssignment() {
        //this.printNMex();
        HashSet<NodeVariable> vars = new HashSet<NodeVariable>();
        vars = (HashSet<NodeVariable>) _maxSumAgent.getVariables();
        Iterator it = vars.iterator();
        NodeVariable var = (NodeVariable) it.next();
        HashSet<NodeFunction> func = new HashSet<NodeFunction>();
        func = var.getNeighbour();
        if (!func.isEmpty()) {
            _maxSumAgent.updateVariableValue();

        }

        toReset = true;
        /*boolean toWrite = true;
        FileWriter fw = null;
        try {
            fw = new FileWriter("assignment.txt", true);
        } catch (IOException i) {
        }*/
        for (NodeVariable variable : _maxSumAgent.getVariables()) {
            try {
                String target = variable.getStateArgument().getValue().toString();

                _targetID = new EntityID(Integer.parseInt(target));
                //System.out.println("Agente "+variable.getId()+" valore "+_targetID.getValue());
            } catch (exception.VariableNotSetException e) {
                _targetID = _utilityM.getHighestTargetForAgent(_agentID);
                //System.out.println("Agent " + _agentID + " doesn't have an assigned variable. This can mean that there are so few targets that some agents aren't assigned one.");
                //toWrite = false;
            }
        }
        /*if (toWrite) {
            try {
                fw.write(_targetID + " ");

                fw.flush();

            } catch (IOException ii) {
            }
        }

        try {
            fw.close();
        } catch (IOException iii) {
        }*/
        return true;
    }

    public EntityID getAgentID() {
        return _agentID;
    }

    public EntityID getTargetID() {
        return _targetID;
    }

    public Collection<AbstractMessage> sendMessages(ComSimulator com) {
        _sizeMex = 0; //dimensione mex inviati
        _nMex = 0;
        Collection<AbstractMessage> mexQ = new ArrayList<AbstractMessage>();
        Collection<AbstractMessage> mexR = new ArrayList<AbstractMessage>();
        Collection<AbstractMessage> allmex = new ArrayList<AbstractMessage>();
        try {
            //System.out.println("Stampa messaggi 1");
            mexQ = _maxSumAgent.sendQMessages();
            /* Send dei Q */
            MS_MessageQ messageQ = null;
            Iterator<AbstractMessage> iteratorm = mexQ.iterator();
            while(iteratorm.hasNext()){
                //usare com per i Q per ogni mex devo recuperare destinatario
                messageQ = (MS_MessageQ)iteratorm.next();
                
                /* PRovvisorio..ricavo funzioni ciclando INEFFICIENTE*/
                Iterator<MaxSum> agentiter = _allAgents.iterator();
                MaxSum agent = null;
                while(agentiter.hasNext()){
                    agent = agentiter.next();
                    if(agent.ismyFunction(messageQ.getFunction())){ // se la funzione è di proprietà dell'agente X il destinatario è LUI
                        com.send(agent.getAgentID(), messageQ); // Se trovo il destinatario invio
                        break;
                    }
                    
                }
                
            }

            //System.out.println("Stampa messaggi 2");
            mexR = _maxSumAgent.sendRMessages();
            iteratorm = mexR.iterator();
            MS_MessageR messageR = null;
            while(iteratorm.hasNext()){
                //usare com per i R per ogni mex devo recuperare destinatario
                messageR = (MS_MessageR)iteratorm.next();
                com.send(new EntityID(messageR.getVariable().getId()), messageR); //Ricavo destinatario dall'id della variabile
            }
                        
          

            _maxSumAgent.sendZMessages(); // controllare sendZ
        } catch (PostServiceNotSetException p) {
            p.printStackTrace();
            System.exit(0);
        }
        allmex.addAll(mexQ);
        allmex.addAll(mexR);
        return allmex;
    }

    public boolean ismyFunction(NodeFunction f) {
        Set<NodeFunction> functions = _maxSumAgent.getFunctions();
        NodeFunction function = null;
        Iterator<NodeFunction> iteratorf = functions.iterator();

        while (iteratorf.hasNext()) {
            function = iteratorf.next();
            if (f.equals(function)) { //se possiedo f ritorno true altrimenti false
                return true;
            }
        }
        return false;
    }

    public boolean ismyVariable(NodeVariable v) {
        Set<NodeVariable> variables = _maxSumAgent.getVariables(); //mie variabili
        NodeVariable variable = null;
        Iterator<NodeVariable> iteratorv = variables.iterator();

        while (iteratorv.hasNext()) {
            variable = iteratorv.next();
            if (v.getId() == variable.getId()) { //se la variabile è la mia
                return true;
            }
        }
        return false;
    }

    public void receiveMessages(Collection<AbstractMessage> messages) {
        Collection<AbstractMessage> mexQ = new ArrayList<AbstractMessage>();
        Collection<AbstractMessage> mexR = new ArrayList<AbstractMessage>();
        Iterator<AbstractMessage> itermex = messages.iterator();
        MS_Message mex = null;
        while(itermex.hasNext()){ //leggo la lista dei mex
            mex = (MS_Message)itermex.next();
            if(mex.getMessageType().compareTo("Q") == 0) //se il mex è di tipo Q lo metto nella lista dei Q
                mexQ.add(mex);
            else if(mex.getMessageType().compareTo("R")==0) //altrimenti negli R
                mexR.add(mex);
        }

        try {
            _maxSumAgent.readQMessages(mexQ);
            
            /*for (AbstractMessage am : mexQ) {
                MS_MessageQ msm = (MS_MessageQ)am;
                MessageQ msq = msm.getMessage();
                System.out.println(msq.toString());
            }*/
            _maxSumAgent.readRMessages(mexR);
            
            /*for (AbstractMessage am : mexR) {
                MS_MessageR msm = (MS_MessageR)am;
                MessageR msr = msm.getMessage();
                System.out.println(msr.toString());
            }*/
            
        } catch (Exception e) {
            e.printStackTrace();
        }

/* Questa parte sarà da rimuovere */
        Set<NodeFunction> functions = _maxSumAgent.getFunctions();
        Set<NodeVariable> variables = _maxSumAgent.getVariables();
        // Read Q messages 
        Iterator<NodeVariable> iteratorv = variables.iterator();
        // Ne ha una in teoria 
        NodeVariable variable = null;
        NodeFunction function = null;
        while (iteratorv.hasNext()) {
            variable = iteratorv.next();
            Iterator<NodeFunction> iteratorf = variable.getNeighbour().iterator();
            // Funzioni legate alla variabile 
            while (iteratorf.hasNext()) {
                function = iteratorf.next();
                if (!ismyFunction(function)) { //se function non è mia allora calcolo i mex tra var e function
                    MessageQ mq = _com.readQMessage(variable, function);
                    int dim = mq.size() * 8;
                    _sizeMex += dim;
                    _nMex++;
                }
            }
        }
        // Read R messages
        variable = null;
        function = null;
        Iterator<NodeFunction> iteratorf = functions.iterator();
        while (iteratorf.hasNext()) {    //Per ogni funzione che possiedo
            function = iteratorf.next();
            Iterator<NodeVariable> iteratorv_ = function.getNeighbour().iterator();
            while (iteratorv_.hasNext()) { //per ogni variabile controllo prima che non sia la mia
                variable = iteratorv_.next();
                if (!ismyVariable(variable)) {
                    MessageR mr = _com.readRMessage(function, variable);
                    int dim = mr.size() * 8;
                    _sizeMex += dim;
                    _nMex++;
                }
            }
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
//Agent.resetIds();
            NodeVariable.resetIds();
            NodeFunction.resetIds();
            NodeArgument.resetIds();
            _mfactory = new MessageFactoryArrayDouble();
            _otimes = new OTimes_MaxSum(_mfactory);
            _oplus = new OPlus_MaxSum(_mfactory);
            _op = new MSumOperator_Sync(_otimes, _oplus);
            //_op = new MSumOperator(_otimes, _oplus);
            _com = new MailMan();
            _variables = new HashSet<NodeVariable>();
            _functions = new HashSet<NodeFunction>();
            _initializedAgents = 0;
            _consideredVariables = new HashMap<EntityID, ArrayList<EntityID>>();
        }

    }

    public int getNccc() {
        int totalnccc = 0;
        for (NodeFunction function : _functions) {
            totalnccc += ((RMASTabularFunction) function.getFunction()).getNCCC();
        }
        return totalnccc;
    }

    public void printFG() {
        FileWriter fw = null;
        for (NodeVariable var : _variables) {
            int agent_id = var.getId();
            AgentMS_Sync agent = AgentMS_Sync.getAgent(agent_id);
            Set<NodeFunction> agent_fun = agent.getFunctions();

            try {
                fw = new FileWriter("factor_graph.txt", true);
                fw.write("Agent: " + agent_id + "\n");
                fw.write("\t functions: " + agent_fun.toString() + "\n");
                fw.write("\t var connected to: " + NodeVariable.getNodeVariable(agent_id).getNeighbour().toString() + "\n\n");

                fw.flush();
                fw.close();
            } catch (IOException i) {
            }
        }
    }

    public void printDimTuples() {
        FileWriter fw = null;
        for (NodeVariable var : _variables) {
            int agent_id = var.getId();
            AgentMS_Sync agent = AgentMS_Sync.getAgent(agent_id);
            Set<NodeFunction> agent_fun = agent.getFunctions();
            
            try {
                fw = new FileWriter("tuples_dim.txt", true);
                fw.write("Agent: " + agent_id + "\n");
                
                for(NodeFunction f: agent_fun){
                    int num_tup = 1;
                    int num_real = 1;
                    for(NodeVariable v: f.getNeighbour()){
                        num_tup *= v.getNeighbour().size(); 
                        num_real *= 2;
                    }
                    if(num_tup == 1) num_tup = 0;
                    if(num_real == 1) num_real = 0;
                    fw.write("\t Funtion: "+f.id()+" dim: "+num_tup+"\n");
                    fw.write("\t Funtion: "+f.id()+" dim_real: "+num_real+"\n");
                }
                 fw.write("----------------------------------------------------------------\n");
                  
                fw.flush();
                fw.close();
            } catch (IOException i) {
            }
        }
    }

    public void printNMex() {
        boolean noFileWriter = false;
        FileWriter fw = null;
        try {
            fw = new FileWriter("tables.stats", true);
        } catch (IOException i) {
            noFileWriter = true;
        }
        for (NodeFunction function : (HashSet<NodeFunction>) _maxSumAgent.getFunctions()) {
            if (!noFileWriter) {
                try {
                    fw.write("Number of tuples tried for function " + function.getId() + ": " + ((RMASTabularFunction) function.getFunction()).getNCCC() + "\n");

                    fw.flush();
                    fw.close();
                } catch (IOException i) {
                }
            }
        }
    }

    public int getNumberOfOtherMessages() {
        return _nMexForFG;
    }
    public long getDimensionOfOtherMessages() {
        return _FGMexBytes;
    }
    public void newNeighbour(EntityID function, EntityID removedVariable) {
        //System.out.println("newneighbour");
        ArrayList<EntityID> possibleNewNeighbours = new ArrayList<EntityID>();
        NodeFunction nodefunction = null;
        try {
            nodefunction = NodeFunction.getNodeFunction(function.getValue());
        } catch (exception.FunctionNotPresentException e) {
            e.printStackTrace();
            System.exit(0);
        }
        HashSet<NodeVariable> neighbours = nodefunction.getNeighbour();
        ArrayList<EntityID> me = new ArrayList<EntityID>();
        me.add(function);
        ArrayList<EntityID> best = (ArrayList<EntityID>) _utilityM.getNBestAgents(_utilityM.getNumAgents(), me);
        ArrayList alreadyConsidered = _consideredVariables.get(function);
        for (EntityID possibleNewNeighbour : best) {
            if (!alreadyConsidered.contains(possibleNewNeighbour)) {
                possibleNewNeighbours.add(possibleNewNeighbour);
            }
        }
        //System.out.println("Vicinato: "+nodefunction.getNeighbour().size());
        if (!possibleNewNeighbours.isEmpty()) {
            this.buildNeighborhood(function, possibleNewNeighbours, _dependencies - 1);
        }
    }
}

