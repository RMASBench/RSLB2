/*
 *  Copyright (C) 2011 Michele Roncalli <roncallim at gmail dot com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package RSLBench.maxSumAdapters;

import exception.OutOfAgentNumberException;
import exception.PostServiceNotSetException;
import exception.VariableNotSetException;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import messages.MessageQ;
import messages.MessageR;
import messages.PostService;
import misc.Utils;
import olimpo.Athena;
import operation.Operator;
import RSLBench.Comm.AbstractMessage;
import java.util.Collection;
import java.util.ArrayList;

/**
 * AgentMS_Sync that controls variables in a COP problem instance.
 * @author Michele Roncalli < roncallim at gmail dot com >
 */
public class AgentMS_Sync {

    /**
     * Static map to have unique id.
     */
    private static HashMap<Integer, AgentMS_Sync> table = new HashMap<Integer, AgentMS_Sync>();

    static final int debug = test.DebugVerbosity.debugAgent;

    /**
     * Operator that handle max sum
     */
    private MSumOperator_Sync op;
    /**
     * PostService to send and retrieve messages. Used by the Nodes.
     */
    private PostService postservice = null;

    private int id;
    static int lastId = -1;

    /**
     * NodeVariables controlled by the AgentMS_Sync
     */
    private HashSet<NodeVariable> variables;
    /**
     * NodeFunctions controlled by the AgentMS_Sync
     */
    private HashSet<NodeFunction> functions;
    
    private AgentMS_Sync(int id){
        this.id = id;
        lastId = id;
        this.variables = new HashSet<NodeVariable>();
        this.functions = new HashSet<NodeFunction>();

    }

    public static AgentMS_Sync getAgent(int id){
        if (!(AgentMS_Sync.table.containsKey(id))){
            AgentMS_Sync.table.put(id, new AgentMS_Sync(id));
        }
        return AgentMS_Sync.table.get(id);
    }

    public static AgentMS_Sync getNewNextAgent(){
        int id = lastId + 1;
        while (AgentMS_Sync.table.containsKey(id)) {
            id++;
        }
        return AgentMS_Sync.getAgent(id);
    }

    public void setOp(MSumOperator_Sync op) {
        this.op = op;
    }

    public PostService getPostservice() {
        return postservice;
    }

    public void setPostservice(PostService postservice) {
        this.postservice = postservice;
    }

    

    public Set<NodeVariable> getVariables(){
        return this.variables;
    }

    public Set<NodeFunction> getFunctions(){
        return this.functions;
    }

    public HashSet<NodeFunction> getFunctionsOfVariable(NodeVariable x){
        return x.getNeighbour();
    }

    public HashSet<NodeVariable> getVariablesOfFunction(NodeFunction f){
        return f.getNeighbour();
    }


    /**
     * Send Q-messages phase.
     * @throws PostServiceNotSetException if ps not set.
     * @return true if at least one message has been updated
     */
    /*
    public boolean sendQMessages() throws PostServiceNotSetException{

        if (this.postservice == null){
            throw new PostServiceNotSetException();
        }

        boolean atLeastOneUpdated = false;

        switch (Athena.shuffleMessage){

            case 1:
                Object[] arrayx = this.getVariables().toArray();
                arrayx = Utils.shuffleArrayFY(arrayx);

                for (Object nodeVariable : arrayx) {
                    Object[] arrayf = this.getFunctionsOfVariable(((NodeVariable)nodeVariable)).toArray();
                    arrayf = Utils.shuffleArrayFY(arrayf);

                    // TODO: shuffling is DA WAY
                    
                    for (Object nodeFunction : arrayf) {
                        //atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice);
                        atLeastOneUpdated |= this.op.updateQ((NodeVariable)nodeVariable, (NodeFunction)nodeFunction, this.postservice);
                    }


                }
                break;

            case 0:
            default:
            //do not shuffle, use them as-is
                Iterator<NodeVariable> iteratorv = this.getVariables().iterator();
                NodeVariable variable; // = null;
                NodeFunction function = null;

                while (iteratorv.hasNext()){
                    variable = iteratorv.next();

                    // gotcha a variable, looking for its functions

                    //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
                    Iterator<NodeFunction> iteratorf = this.getFunctionsOfVariable(variable).iterator();
                    while (iteratorf.hasNext()){

                        function = iteratorf.next();
                        // got variable, function

                        if (debug>=1) {
                                String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
                                String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
                                System.out.println("---------------------------------------");
                                System.out.println("[class: "+dclass+" method: " + dmethod+ "] " + "AgentMS_Sync "+this+" preparing Q from " + variable + " to " + function);
                                System.out.println("---------------------------------------");
                        }
                        atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice);
                        if (debug>=1) {
                                String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                                String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                                System.out.println("---------------------------------------");
                                System.out.println("[class: "+dclass+" method: " + dmethod+ "] " + "messageQ updated:");
                                MessageQ mq = this.postservice.readQMessage(variable, function);
                                System.out.println("Sender "+ mq.getSender() + " Receiver " + mq.getReceiver() + " message "+ mq );
                                System.out.println("---------------------------------------");
                        }

                    }

                }
                break;
                //end of case 0



        }
        return atLeastOneUpdated;
    }*/

    //////////////////////////////
    /**
     * Send R-messages phase.
     * @throws PostServiceNotSetException if ps not set.
     * @return true if at least one message has been updated
     */
    /*
    public boolean sendRMessages() throws PostServiceNotSetException{

        if (this.postservice == null){
            throw new PostServiceNotSetException();
        }

        boolean atLeastOneUpdated = false;

        switch (Athena.shuffleMessage){

            case 1:
                Object[] arrayf = this.getFunctions().toArray();
                arrayf = Utils.shuffleArrayFY(arrayf);

                for (Object nodeFunction : arrayf) {
                    Object[] arrayx = this.getVariablesOfFunction((NodeFunction)nodeFunction).toArray();
                    arrayx = Utils.shuffleArrayFY(arrayx);

                    // TODO: shuffling is DA WAY

                    for (Object nodeVariable : arrayx) {
                        //atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice);
                        atLeastOneUpdated |= this.op.updateR((NodeFunction)nodeFunction, (NodeVariable)nodeVariable, this.postservice);
                    }


                }
                break;

            case 0:
            default:

                Iterator<NodeFunction> iteratorf = this.getFunctions().iterator();
                NodeVariable variable = null;
                NodeFunction function = null;

                while (iteratorf.hasNext()){
                    function = iteratorf.next();

                    // gotcha a variable, looking for its functions

                    //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
                    Iterator<NodeVariable> iteratorv = this.getVariablesOfFunction(function).iterator();
                    while (iteratorv.hasNext()){

                        variable = iteratorv.next();
                        // got variable, function
                        if (debug>=1) {
                                String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
                                String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
                                System.out.println("---------------------------------------");
                                System.out.println("[class: "+dclass+" method: " + dmethod+ "] " + "AgentMS_Sync "+this+"  preparing R from " + function + " to " + variable);
                                System.out.println("---------------------------------------");
                        }
                        atLeastOneUpdated |= this.op.updateR(function, variable, this.postservice);
                        if (debug>=1) {
                                String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                                String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                                System.out.println("---------------------------------------");
                                System.out.println("[class: "+dclass+" method: " + dmethod+ "] " + "messageR updated:");
                                MessageR mq = this.postservice.readRMessage(function, variable);
                                System.out.println("Sender "+ mq.getSender() + " Receiver " + mq.getReceiver() + " message "+ mq );
                                System.out.println("---------------------------------------");
                        }
                    }

                }
        }
        return atLeastOneUpdated;
    }
*/
    public void setFunctions(HashSet<NodeFunction> functions) {
        this.functions = functions;
    }

    public void resetNodeFunction() {
        this.functions.clear();
    }

    public void setVariables(HashSet<NodeVariable> variables) {
        this.variables = variables;
    }


    /**
     * Compute the Z-messages and set the variables to the value of argmax.
     * @throws PostServiceNotSetException if no post service is set
     */
    public void sendZMessages() throws PostServiceNotSetException{
        
        if (this.postservice == null){
            throw new PostServiceNotSetException();
        }

        switch (Athena.shuffleMessage){

            case 1:
                Object[] arrayx = this.getVariables().toArray();
                arrayx = Utils.shuffleArrayFY(arrayx);

                for (Object nodeVariable : arrayx) {
                    this.op.updateZ((NodeVariable)nodeVariable, postservice);
                }
                break;

            case 0:
            default:
                for (NodeVariable nodeVariable:this.getVariables()){
                    if (debug>=3) {
                            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                            System.out.println("---------------------------------------");
                            System.out.println("[class: "+dclass+" method: " + dmethod+ "] AgentMS_Sync "+this+"" + "preparing to update ZMessage for "+nodeVariable);
                            System.out.println("---------------------------------------");
                    }
                    this.op.updateZ(nodeVariable, postservice);
                }
        }

    }

    public void addNodeVariable(NodeVariable nodevariable) {
        this.variables.add(nodevariable);
    }

    public void addNodeFunction(NodeFunction nodefunction) {
        this.functions.add(nodefunction);
    }

    public String toString(){
        return "AgentMS_Sync_"+this.id;
    }

    public int id() {
        return this.id;
    }

    /**
     * Set the NodeVariable x value as the argMax of Z-message
     * @param x the NodeVariable to set.
     */
    public void setVariableArgumentFromZ(NodeVariable x){
        if (debug>=3) {
                String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                System.out.println("---------------------------------------");
                System.out.println("[class: "+dclass+" method: " + dmethod+ "] " + "for the nodevariable "+x+" set the value "+this.op.argOfInterestOfZ(x, postservice));
                System.out.println("---------------------------------------");
        }
        x.setStateIndex(
                this.op.argOfInterestOfZ(x, postservice)
                );
    }

    public void updateVariableValue(){
        for (NodeVariable x : this.getVariables() ){
            this.setVariableArgumentFromZ(x);
        }
    }

    public String variableValueToString(){
        StringBuilder string = new StringBuilder();
        for (NodeVariable x : this.getVariables() ){
            try {
                //string.append("[" + x + "] value: " + x.getStateArgument() + " at position: " + (x.getStateIndex() + 1) + "/" + x.getValues().size() + "\n");
                string.append("[").append(x).append("] value: ").append(x.getStateArgument()).append(" at position: ").append(x.getStateIndex() + 1).append("/").append(x.getValues().size()).append("\n");
            } catch (VariableNotSetException ex) {
                string.append("[").append(x).append("] IS NOT SET\n");
            }
        }
        return string.toString();
    }

    @Override
    public int hashCode(){
        return ("AgentMS_Sync_"+this.id).hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof AgentMS_Sync) {
            return ((AgentMS_Sync)o).id() == this.id();
        }
        else {
            return false;
        }
    }

    public AgentMS_Sync getClone() throws OutOfAgentNumberException{

        AgentMS_Sync cloned = AgentMS_Sync.getNewNextAgent();
        cloned.setOp(this.op);
        cloned.setPostservice(this.postservice);

        for (NodeVariable oldx : this.getVariables()) {
            cloned.addNodeVariable(oldx);
        }

        for (NodeFunction oldf : this.getFunctions()) {
            cloned.addNodeFunction(oldf);
        }

        return cloned;

    }

    /**
     * Used in Clone, to change the NodeVariables
     * @param oldv
     * @param newv
     */
    public void changeVariable(NodeVariable oldv, NodeVariable newv) {
        this.variables.remove(oldv);
        this.variables.add(newv);
    }

    /**
     * Used in Clone, to change the NodeFunction
     * @param oldv
     * @param newv
     */
    public void changeFunction(NodeFunction oldv, NodeFunction newv) {
        this.functions.remove(oldv);
        this.functions.add(newv);
    }

    public static void resetIds(){
        table = new HashMap<Integer, AgentMS_Sync>();
        lastId = -1;
    }
    
    
    public boolean readRMessages(Collection<AbstractMessage> mexR) {
        Iterator<NodeVariable> iteratorv = this.getVariables().iterator();
        NodeVariable variable; // = null;
        NodeFunction function = null;

        while (iteratorv.hasNext()) {
            variable = iteratorv.next();

            // gotcha a variable, looking for its functions

            //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
            Iterator<NodeFunction> iteratorf = this.getFunctionsOfVariable(variable).iterator();
            while (iteratorf.hasNext()) {

                function = iteratorf.next();
                // got variable, function

                // this.op.updateQ(variable, function, this.postservice);
                this.op.readRmessage(variable, function, this.postservice);
                //devo far leggere all'operator i read message e salvarli li

            }
        }
                /**
         * Parte riservata ai mex in uscita che arrivano dal comunicator
         */
        Iterator<AbstractMessage> itermex = mexR.iterator();
        MS_MessageR m = null;
        while(itermex.hasNext()){
            m = (MS_MessageR)itermex.next();
            this.op.readRmessage_com(m.getVariable(), m.getFunction(), m.getMessage());
        }
        return true;
    }

  
    public boolean readQMessages(Collection<AbstractMessage> mexQ) throws PostServiceNotSetException {

        if (this.postservice == null) {
            throw new PostServiceNotSetException();
        }

        boolean atLeastOneUpdated = false;

        Iterator<NodeFunction> iteratorf = this.getFunctions().iterator();
        NodeVariable variable = null;
        NodeFunction function = null;

        while (iteratorf.hasNext()) {
            function = iteratorf.next();

            // gotcha a variable, looking for its functions

            //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
            Iterator<NodeVariable> iteratorv = this.getVariablesOfFunction(function).iterator();
            while (iteratorv.hasNext()) {

                variable = iteratorv.next();
                // got variable, function
                if(ismyVariable(variable)) //STA VOLTA MI AFFIDO AL POSTSERVICE SOLO SE LA VARIABILE E' MIA QUINDI IL MEX E' INTERNO
                       this.op.readQmessages(function, variable, this.postservice);
                //atLeastOneUpdated |= this.op.updateR(function, variable, this.postservice);
         
            }

        }
        /**
         * Parte riservata ai mex in uscita che arrivano dal comunicator
         */
        Iterator<AbstractMessage> itermex = mexQ.iterator();
        MS_MessageQ m = null;
        while(itermex.hasNext()){
            m = (MS_MessageQ)itermex.next();
            this.op.readQmessages_com(m.getFunction(), m.getVariable(), m.getMessage());
        }


        return true;
    }
    
    
    private boolean ismyVariable(NodeVariable v) {
        Set<NodeVariable> variables = this.getVariables(); //mie variabili
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
    
    private boolean ismyFunction(NodeFunction f) {
        Set<NodeFunction> functions = this.getFunctions();
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
    /**
     * Send Q-messages phase.
     * @throws PostServiceNotSetException if ps not set.
     * @return true if at least one message has been updated
     */
    public Collection<AbstractMessage> sendQMessages() throws PostServiceNotSetException {

        if (this.postservice == null) {
            throw new PostServiceNotSetException();
        }
        Collection<AbstractMessage> coll_mexQ = new ArrayList<AbstractMessage>();

        boolean atLeastOneUpdated = false;

        switch (Athena.shuffleMessage) {

            case 1:
                /*
                Object[] arrayx = this.getVariables().toArray();
                arrayx = Utils.shuffleArrayFY(arrayx);

                for (Object nodeVariable : arrayx) {
                    Object[] arrayf = this.getFunctionsOfVariable(((NodeVariable) nodeVariable)).toArray();
                    arrayf = Utils.shuffleArrayFY(arrayf);

                    // TODO: shuffling is DA WAY

                    for (Object nodeFunction : arrayf) {
                        //atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice);
                        atLeastOneUpdated |= this.op.updateQ((NodeVariable) nodeVariable, (NodeFunction) nodeFunction, this.postservice);
                       
                    }


                }*/
                return null;
               

            case 0:
            default:
                //do not shuffle, use them as-is
                Iterator<NodeVariable> iteratorv = this.getVariables().iterator();
                NodeVariable variable; // = null;
                NodeFunction function = null;
                

                while (iteratorv.hasNext()) {
                    variable = iteratorv.next();

                    // gotcha a variable, looking for its functions

                    //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
                    Iterator<NodeFunction> iteratorf = this.getFunctionsOfVariable(variable).iterator();
                    while (iteratorf.hasNext()) {

                        function = iteratorf.next();
                        // got variable, function

                        if (debug >= 1) {
                            String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
                            String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
                            System.out.println("---------------------------------------");
                            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "AgentMS_Sync " + this + " preparing Q from " + variable + " to " + function);
                            System.out.println("---------------------------------------");
                        }
                        if(!ismyFunction(function)){
                            atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice); //Se la funzione non è mia eseguo questo e uso vecchio operator
                            
                        }
                        else{
                            MessageQ mex = this.op.updateQ_com(variable, function);
                            coll_mexQ.add(new MS_MessageQ(variable, function, mex));
                        }
                        /*
                            if (debug >= 1) {
                            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                            System.out.println("---------------------------------------");
                            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "messageQ updated:");
                            MessageQ mq = this.postservice.readQMessage(variable, function);
                            System.out.println("Sender " + mq.getSender() + " Receiver " + mq.getReceiver() + " message " + mq);
                            System.out.println("---------------------------------------");
                        }*/

                    }

                }
                
                break;
            //end of case 0

        }
        return coll_mexQ; // da cambiare co i messaggi Q
    }

    //////////////////////////////
    /**
     * Send R-messages phase.
     * @throws PostServiceNotSetException if ps not set.
     * @return true if at least one message has been updated
     */
    
    public Collection<AbstractMessage> sendRMessages() throws PostServiceNotSetException {

        if (this.postservice == null) {
            throw new PostServiceNotSetException();
        }

        boolean atLeastOneUpdated = false;
        Collection<AbstractMessage> coll_mexR = new ArrayList<AbstractMessage>();

        switch (Athena.shuffleMessage) {

            case 1:
                /*
                Object[] arrayf = this.getFunctions().toArray();
                arrayf = Utils.shuffleArrayFY(arrayf);

                for (Object nodeFunction : arrayf) {
                    Object[] arrayx = this.getVariablesOfFunction((NodeFunction) nodeFunction).toArray();
                    arrayx = Utils.shuffleArrayFY(arrayx);

                    // TODO: shuffling is DA WAY

                    for (Object nodeVariable : arrayx) {
                        //atLeastOneUpdated |= this.op.updateQ(variable, function, this.postservice);
                        atLeastOneUpdated |= this.op.updateR((NodeFunction) nodeFunction, (NodeVariable) nodeVariable, this.postservice);
                    }


                }*/
                break;

            case 0:
            default:

                Iterator<NodeFunction> iteratorf = this.getFunctions().iterator();
                NodeVariable variable = null;
                NodeFunction function = null;

                while (iteratorf.hasNext()) {
                    function = iteratorf.next();

                    // gotcha a variable, looking for its functions

                    //Iterator<NodeFunction> iteratorf = this.variableToFunctions.get(variable).iterator();
                    Iterator<NodeVariable> iteratorv = this.getVariablesOfFunction(function).iterator();
                    while (iteratorv.hasNext()) {

                        variable = iteratorv.next();
                        // got variable, function
                        if (debug >= 1) {
                            String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
                            String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
                            System.out.println("---------------------------------------");
                            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "AgentMS_Sync " + this + "  preparing R from " + function + " to " + variable);
                            System.out.println("---------------------------------------");
                        }
                        if(!ismyVariable(variable)){    
                            atLeastOneUpdated |= this.op.updateR(function, variable, this.postservice);
                        }
                        else{
                            MessageR mex = this.op.updateR_com(function, variable);
                            coll_mexR.add(new MS_MessageR(function, variable, mex));
                        }
                        if (debug >= 1) {
                            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                            System.out.println("---------------------------------------");
                            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "messageR updated:");
                            MessageR mq = this.postservice.readRMessage(function, variable);
                            System.out.println("Sender " + mq.getSender() + " Receiver " + mq.getReceiver() + " message " + mq);
                            System.out.println("---------------------------------------");
                        }
                    }

                }
        }
        return coll_mexR;
    }
}

