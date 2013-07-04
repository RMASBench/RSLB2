/*
 * Software License Agreement (BSD License)
 *
 * Copyright 2013 Marc Pujol <mpujol@iiia.csic.es>.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 *   Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 *   Neither the name of IIIA-CSIC, Artificial Intelligence Research Institute
 *   nor the names of its contributors may be used to
 *   endorse or promote products derived from this
 *   software without specific prior written permission of
 *   IIIA-CSIC, Artificial Intelligence Research Institute
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package RSLBench.Algorithms.BMS;

import java.util.Collection;
import java.util.ArrayList;

import rescuecore2.worldmodel.EntityID;

import RSLBench.Assignment.DecentralAssignment;
import RSLBench.Comm.Message;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Helpers.Utility.UtilityMatrix;
import es.csic.iiia.maxsum.CardinalityFactor;

import es.csic.iiia.maxsum.CommunicationAdapter;
import es.csic.iiia.maxsum.Factor;
import es.csic.iiia.maxsum.MaxAgFunction;
import es.csic.iiia.maxsum.MaxOperator;
import es.csic.iiia.maxsum.Maximize;
import es.csic.iiia.maxsum.SelectorFactor;
import es.csic.iiia.maxsum.CardinalityFunction;
import es.csic.iiia.maxsum.CompositeIndependentFactor;
import es.csic.iiia.maxsum.IndependentFactor;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is a binary max-sum agent.
 */
public class BinaryMaxSum implements DecentralAssignment {
    private static final Logger Logger = LogManager.getLogger(BinaryMaxSum.class);
    
    private static final double HARD_CONSTRAINT_UTILITY = -10000;
    private static final MaxOperator maxOperator = new Maximize();
    
    private EntityID id;
    private UtilityMatrix utilities;
    private SelectorFactor<EntityID> variableNode;
    private HashMap<EntityID, Factor<EntityID>> factors;
    private HashMap<EntityID, EntityID> factorLocations;
    private RSLBenchCommunicationAdapter communicationAdapter;
    private EntityID targetId;
    private long constraintChecks;
    
    /**
     * Initialize this max-sum agent (firefighting team)
     * 
     * @param agentID The platform ID of the firefighting team
     * @param utilityM A "utility maxtrix" that contains <em>all</em> u_at values
     */
    @Override
    public void initialize(EntityID agentID, UtilityMatrix utilityM) {
        Logger.trace("Initializing agent {}", agentID);
        
        this.id = agentID;
        this.targetId = null;
        this.utilities = utilityM;
        
        // Reset internal structures
        factors = new HashMap<>();
        factorLocations = new HashMap<>();
        communicationAdapter = new RSLBenchCommunicationAdapter();

        // Build the variable node
        addSelectorNode();

        // And the fire utility nodes that correspond to this agent
        addUtilityNodes();
        
        // Finally, compute the location of each factor in the simulation
        computeFactorLocations();
        
        Logger.trace("Agent {} initialized.", agentID);
    }
    
    /**
     * Adds a new factor to this agent.
     */
    private void addFactor(EntityID id, Factor<EntityID> factor) {
        factors.put(id, factor);
        factor.setMaxOperator(maxOperator);
        factor.setIdentity(id);
        factor.setCommunicationAdapter(communicationAdapter);
    }
    
    /**
     * Creates a selector node for the agent's "variable".
     */
    private void addSelectorNode() {
        this.variableNode = new SelectorFactor<>();
        
        // The agent's factor is the selector plus the independent utilities
        // of this agent for each fire.
        CompositeIndependentFactor<EntityID> agentFactor = new CompositeIndependentFactor<>();
        agentFactor.setInnerFactor(variableNode);
        
        IndependentFactor<EntityID> utils = new IndependentFactor<>();
        agentFactor.setIndependentFactor(utils);
        for (EntityID fire : utilities.getTargets()) {
            // Link the agent to each fire
            agentFactor.addNeighbor(fire);
            // ... and populate the utilities
            final double value = utilities.getUtility(id, fire);
            utils.setPotential(fire, value);
            Logger.trace("Utility for {}: {}", new Object[]{fire, value});
        }
        
        addFactor(id, agentFactor);
    }
    
    /** 
     * Create the utility nodes of the fires "controlled" by this agent.
     * 
     * Utility functions get assigned to the agents according to their 
     * indices within the utilities list of agents and targets.
     * 
     * Agent i gets all fires f s.t. f mod len(agents) == i
     * If there are 2 agents and 5 utility functions, the assignment goes
     * like that:
     * Agent 0 (agents.get(0)) gets Fires 0, 2, 4
     * Agent 1 (agents.get(1)) gets Fires 1, 3
     * 
     **/
    private void addUtilityNodes() {
        ArrayList<EntityID> agents = utilities.getAgents();
        ArrayList<EntityID> fires  = utilities.getTargets();
        final int nAgents = agents.size();
        final int nFires  = fires.size();
        final int nAgent  = agents.indexOf(id);
        
        // Iterate over the fires whose utility functions must run within this
        // agent.
        for (int i = nAgent; i < nFires; i += nAgents) {
            final EntityID fire = fires.get(i);
            
            // Build the utility node
            CardinalityFactor<EntityID> f = new CardinalityFactor<>();
            
            // Set the maximum number of agents that should be attending this
            // fire
            CardinalityFunction wf = new MaxAgFunction(
                utilities.getRequiredAgentCount(fire), HARD_CONSTRAINT_UTILITY
                //1, HARD_CONSTRAINT_UTILITY
            );
            f.setFunction(wf);
            
            // Link the fire with all agents
            for (EntityID agent : agents) {
                f.addNeighbor(agent);
            }
            
            // Finally add the factor to this agent
            addFactor(fire, f);
        }
    }
    
    /**
     * Creates a map of factor id to the agent id where this factor is running,
     * for all factors within the simulation.
     * 
     * @see #addUtilityNodes() for information on how the logical factors are
     * assigned to agents.
     */
    private void computeFactorLocations() {
        ArrayList<EntityID> agents = utilities.getAgents();
        ArrayList<EntityID> fires  = utilities.getTargets();
        final int nAgents = agents.size();
        final int nFires  = fires.size();
        
        // Easy part: each agent selector runs on the corresponding agent
        for (EntityID agent : agents) {
            factorLocations.put(agent, agent);
        }
        
        // "Harder" part: each fire f runs on agent f mod len(agents)
        for (int i = 0; i < nFires; i++) {
            EntityID agent = agents.get(i % nAgents);
            EntityID fire  = fires.get(i);
            factorLocations.put(fire, agent);
        }
    }

    /**
     * Tries to improve the current assignment given the received messages.
     * <p/>
     * In binary max-sum this amounts to run each factor within this agent,
     * and then extracting the best current assignment from the selector of
     * the agent.
     */
    @Override
    public boolean improveAssignment() {
        Logger.trace("improveAssignment start...");
        constraintChecks = 0;
        
        // Let all factors run
        for (EntityID eid : factors.keySet()) {
            constraintChecks += factors.get(eid).run();
        }
        
        // Now extract our choice
        EntityID oldTarget = targetId;
        targetId = variableNode.select();
        Logger.trace("improveAssignment end.");
        //return targetId != oldTarget;
        return true;
    }

    @Override
    public EntityID getTargetID() {
        return targetId;
    }
    
    @Override
    public EntityID getAgentID() {
        return this.id;
    }
    
    @Override
    public Collection<Message> sendMessages(CommunicationLayer com) {
        // Fetch the messages that must be sent
        Collection<BinaryMaxSumMessage> messages = communicationAdapter.flushMessages();
        
        // Send them
        for (BinaryMaxSumMessage message : messages) {
            EntityID recipientAgent = factorLocations.get(message.getRecipientFactor());
            com.send(recipientAgent, message);
        }
        
        // Hack to force java to understand that a collection of binary max sum
        // messages is *also* a collection of abstract messages
        Collection<Message> result = new ArrayList<>();
        for (BinaryMaxSumMessage message : messages) {
            result.add(message);
        }
        
        return result;
    }

    /**
     * Receives a set of messages from other agents, by dispatching them to their
     * intended recipient factors.
     * 
     * @param messages messages to receive
     */
    @Override
    public void receiveMessages(Collection<Message> messages) {
        if (messages == null) {
            return;
        }
        for (Message amessage : messages) {
            if (amessage == null) {
                continue;
            }
            receiveMessage(amessage);
        }
    }
    
    /**
     * Receives a single message from another agent, dispatching it to the
     * intended recipient factor.
     * 
     * @param amessage message to receive
     */
    private void receiveMessage(Message amessage) {
        if (!(amessage instanceof BinaryMaxSumMessage)) {
            throw new IllegalArgumentException("Binary max-sum agents are only supposed to receive binary max-sum messages");
        }

        BinaryMaxSumMessage message = (BinaryMaxSumMessage)amessage;
        Factor<EntityID> recipient = factors.get(message.getRecipientFactor());
        recipient.receive(message.message, message.getSenderFactor());
    }

    @Override
    public long getConstraintChecks() {
        return constraintChecks;
    }

    /**
     * Returns 0 because the factor network is built without any communication
     * between agents (its based only on IDs).
     * 
     * @return 0
     */
    @Override
    public int getNumberOfOtherMessages() {
        return 0;
    }
    
    /**
     * Returns 0 because the factor network is built without any communication
     * between agents (its based only on IDs).
     * 
     * @return 0
     */
    @Override
    public long getDimensionOfOtherMessages() {
        return 0;
    }
    
    /**
     * Communication adapter that knows how to send messages to other agents
     * using the RSLBench communications layer.
     */
    public class RSLBenchCommunicationAdapter implements CommunicationAdapter<EntityID> {
        
        private ArrayList<BinaryMaxSumMessage> outgoingMessages;
        
        public RSLBenchCommunicationAdapter() {
            this.outgoingMessages = new ArrayList<>();
        }
        
        public Collection<BinaryMaxSumMessage> flushMessages() {
            Collection<BinaryMaxSumMessage> result = outgoingMessages;
            outgoingMessages = new ArrayList<>();
            return result;
        }

        @Override
        public void send(double message, EntityID sender, EntityID recipient) {
            Logger.trace("Message from {} to {} : {}", new Object[]{sender, recipient, message});
            outgoingMessages.add(new BinaryMaxSumMessage(message, sender, recipient));
        }
        
    }
    
}