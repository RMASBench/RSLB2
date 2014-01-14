/*
 *  Copyright (C) 2011 Michele Roncalli <roncallim at gmail dot com>
 *  Copyright (C) 2014 Marc Pujol <mpujol@iiia.csic.es>
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
package RSLBench.Algorithms.MS;

import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import messages.MessageQ;
import messages.MessageR;
import messages.PostService;
import olimpo.Athena;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

/**
 * JMSAgent that controls variables in a COP problem instance.
 *
 * @author Michele Roncalli < roncallim at gmail dot com >
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class JMSAgent {

    /**
     * Static map to have unique id.
     */
    private static HashMap<Integer, JMSAgent> table = new HashMap<>();

    /**
     * Identifier of this agent
     */
    public final int id;

    /**
     * Operator that handle max sum
     */
    private MSumOperator_Sync op;

    /**
     * PostService to send and retrieve messages. Used by the Nodes.
     */
    private PostService postservice = null;

    /**
     * NodeVariables controlled by the AgentMS_Sync
     */
    private final HashSet<NodeVariable> variables = new HashSet<>();

    /**
     * NodeFunctions controlled by the AgentMS_Sync
     */
    private final HashSet<NodeFunction> functions = new HashSet<>();

    /**
     * Get the agent with the specified id. The agent is created if it didn't exist yet.
     *
     * @param id identifier of the agent.
     * @return jMaxSum agent with the specified identifier.
     */
    public static JMSAgent getAgent(int id){
        if (!JMSAgent.table.containsKey(id)) {
            JMSAgent.table.put(id, new JMSAgent(id));
        }
        return JMSAgent.table.get(id);
    }

    private JMSAgent(int id){
        this.id = id;
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
        return Collections.unmodifiableSet(this.variables);
    }

    public void addVariable(NodeVariable nodevariable) {
        this.variables.add(nodevariable);
    }

    public Set<NodeFunction> getFunctions(){
        return Collections.unmodifiableSet(this.functions);
    }

    public void addFunction(NodeFunction nodefunction) {
        this.functions.add(nodefunction);
    }

    public Set<NodeFunction> getFunctionsOfVariable(NodeVariable x){
        return x.getNeighbour();
    }

    public Set<NodeVariable> getVariablesOfFunction(NodeFunction f){
        return f.getNeighbour();
    }

    /**
     * Clears the list of functions assigned to this node.
     */
    public void clearFunctions() {
        this.functions.clear();
    }

    /**
     * Compute the Z-messages and set the variables to the value of argmax.
     */
    public void sendZMessages() {

        switch (Athena.shuffleMessage){
            case 1:
                ArrayList<NodeVariable> randomizedVariables = new ArrayList<>(variables);
                Collections.shuffle(randomizedVariables);
                for (NodeVariable nodeVariable : randomizedVariables) {
                    this.op.updateZ(nodeVariable, postservice);
                }
                break;

            case 0:
            default:
                for (NodeVariable nodeVariable : variables){
                    this.op.updateZ(nodeVariable, postservice);
                }
        }

    }

    @Override
    public String toString(){
        return "JMSAgent[" + this.id + "]";
    }

    /**
     * Updates the variable's value to the most preferrable one according to the Z messages.
     */
    public void updateVariablesValues(){
        for (NodeVariable x : variables) {
            x.setStateIndex(this.op.argOfInterestOfZ(x, postservice));
        }
    }

    /**
     * Clears the static map that holds all functions.
     */
    public static void resetIds(){
        table.clear();
    }

    /**
     * Processes the given list of R messages by introducing them to the jMaxSum's communication
     * channel.
     *
     * @param RMEssages list of R messages to introduce
     * @return <code>true</code>
     */
    public boolean readRMessages(Collection<MS_MessageR> RMEssages) {
        for (NodeVariable variable : variables) {
            for (NodeFunction function : variable.getNeighbour()) {
                this.op.readRmessage(variable, function, this.postservice);
            }
        }

        for (MS_MessageR m : RMEssages) {
            this.op.readRmessage_com(m.getVariable(), m.getFunction(), m.getMessage());
        }

        return true;
    }

    /**
     * Processes the given list of Q messages by introducing them to the jMaxSum's communication
     * channel.
     *
     * @param QMessages list of Q messages to introduce
     */
    public void readQMessages(Collection<MS_MessageQ> QMessages) {
        for (NodeFunction function : functions) {
            for (NodeVariable variable : function.getNeighbour()) {
                if (variables.contains(variable)) {
                       this.op.readQmessages(function, variable, this.postservice);
                }
            }
        }

        for (MS_MessageQ m : QMessages) {
            this.op.readQmessages_com(m.getFunction(), m.getVariable(), m.getMessage());
        }
    }

    /**
     * Send Q-messages phase.
     *
     * @return list of sent messages.
     */
    public Collection<MS_MessageQ> sendQMessages() {
        Collection<MS_MessageQ> messages = new ArrayList<>();

        if (Athena.shuffleMessage == 1) {
            throw new UnsupportedOperationException("Shuffling is not supported yet.");
        }

        for (NodeVariable variable : variables) {
            for (NodeFunction function : variable.getNeighbour()) {
                if (functions.contains(function)) {
                    MessageQ msg = this.op.updateQ_com(variable, function);
                    messages.add(new MS_MessageQ(variable, function, msg));
                }
            }
        }

        return messages;
    }

    /**
     * Send R-messages phase.
     *
     * @return list of sent messages.
     */
    public Collection<MS_MessageR> sendRMessages() {
        Collection<MS_MessageR> messages = new ArrayList<>();

        if (Athena.shuffleMessage == 1) {
            throw new UnsupportedOperationException("Shuffling is not supported yet.");
        }

        for (NodeFunction function : functions) {
            for (NodeVariable variable : function.getNeighbour()) {
                if (variables.contains(variable)) {
                    MessageR msg = this.op.updateR_com(function, variable);
                    messages.add(new MS_MessageR(function, variable, msg));
                }
            }
        }

        return messages;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JMSAgent other = (JMSAgent) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}