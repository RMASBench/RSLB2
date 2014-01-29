/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.NewMS;

import RSLBench.Assignment.DCOP.AbstractDCOPAgent;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Comm.Message;
import RSLBench.Helpers.Utility.ProblemDefinition;
import es.csic.iiia.ms.Communicator;
import es.csic.iiia.ms.Variable;
import es.csic.iiia.ms.functions.CostFunction;
import es.csic.iiia.ms.functions.CostFunctionFactory;
import es.csic.iiia.ms.functions.MasterIterator;
import es.csic.iiia.ms.node.FunctionNode;
import es.csic.iiia.ms.node.Node;
import es.csic.iiia.ms.node.VariableNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class MSAgent extends AbstractDCOPAgent {

    private CostFunctionFactory cfFactory = new CostFunctionFactory();

    private Communicator communicator;
    private VariableNode variableNode;
    private List<FunctionNode> functionNodes;
    private Map<Identity, EntityID> nodeLocations;

    private Variable getVariable(EntityID fireAgent) {
        final ProblemDefinition problem = getProblem();
        final int nCandidateFires = problem.getFireAgentNeighbors(fireAgent).size();
        return new Variable(new Identity(fireAgent), nCandidateFires);
    }

    private CostFunction buildFireAgentPotential(EntityID fireAgent) {
        final ProblemDefinition problem = getProblem();
        final List<EntityID> candidates = problem.getFireAgentNeighbors(fireAgent);

        // The potential is a tabular function of just one variable (a vector), with
        // the unary costs of each candidate.
        Variable v = getVariable(fireAgent);
        CostFunction potential = cfFactory.buildCostFunction(new Variable[]{v}, 0);
        for (int i=0; i<candidates.size(); i++) {
            final double value = problem.getFireUtility(fireAgent, candidates.get(i));
            potential.setValue(i, value);
        }

        return potential;
    }

    private CostFunction buildFirePotential(EntityID fire) {
        final ProblemDefinition problem = getProblem();
        final List<EntityID> fireAgents = problem.getFireAgentNeighbors(fire);
        final int nFireAgents = fireAgents.size();

        // List of variables involved in this factor
        final Variable[] variables = new Variable[nFireAgents];
        // List of values where the variable is assigned to this factor's fire
        final int[] assignments = new int[nFireAgents];
        for (int i=0; i<nFireAgents; i++) {
            final EntityID fireAgent = fireAgents.get(i);
            variables[i] = getVariable(fireAgent);
            assignments[i] = problem.getFireAgentNeighbors(fireAgent).indexOf(fire);
        }

        // Build the potential CostFunction and set the values
        CostFunction f = cfFactory.buildCostFunction(variables, 0);
        for (MasterIterator it = f.masterIterator(); it.hasNext();) {
            final long idx = it.next();
            final int[] indices = it.getIndices();

            int nActiveCandidates = 0;
            for (int i=0; i<nFireAgents; i++) {
                if (indices[i] == assignments[i]) {
                    nActiveCandidates++;
                }
            }

            f.setValue(idx, problem.getUtilityPenalty(fire, nActiveCandidates));
        }

        return f;
    }

    private void buildFactorNodes() {
        final ProblemDefinition problem = getProblem();
        final List<EntityID> fires = problem.getFires();

        final int nAgent  = problem.getFireAgents().indexOf(getID());
        final int nFireAgents = problem.getNumFireAgents();
        final int nFires = fires.size();

        for (int i=nAgent; i<nFires; i+=nFireAgents) {
            final EntityID fire = fires.get(i);
            FunctionNode function = new FunctionNode(new Identity(fire), communicator, buildFirePotential(fire));
            functionNodes.add(function);
        }
    }

    private void computeNodeLocations() {
        final ProblemDefinition problem = getProblem();
        final List<EntityID> fireAgents = problem.getFireAgents();
        final List<EntityID> fires = problem.getFires();
        final int nFires = fires.size();
        final int nFireAgents = fireAgents.size();

        // Compute the location of the fire agents
        for (EntityID fireAgent : fireAgents) {
            nodeLocations.put(new Identity(fireAgent), fireAgent);
        }

        // And now the location of the fires
        for (int i=0; i<nFires; i++) {
            final int nAgent = i % nFireAgents;
            nodeLocations.put(new Identity(fires.get(i)), fireAgents.get(nAgent));
        }
    }

    @Override
    public void initialize(Config config, EntityID id, ProblemDefinition problem) {
        super.initialize(config, id, problem);

        // Create the variable node for this agent
        communicator = new MSCommunicator();
        variableNode = new VariableNode(new Identity(id), communicator, buildFireAgentPotential(id));

        // Create the factor nodes handled by this agent
        buildFactorNodes();

        // And compute the locations of neighboring nodes, so we can message them
        computeNodeLocations();
    }



    @Override
    public boolean improveAssignment() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<? extends Message> sendMessages(CommunicationLayer com) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void receiveMessages(Collection<Message> messages) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getConstraintChecks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
