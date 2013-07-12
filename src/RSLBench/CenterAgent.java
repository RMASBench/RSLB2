package RSLBench;


import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.CompositeSolver;
import RSLBench.Assignment.Solver;
import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.UtilityFactory;
import RSLBench.Helpers.Utility.UtilityMatrix;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.config.NoSuchConfigOptionException;

/**
 * It is a "fake" agent that does not appears in the graphic simulation, but that serves as a "station"
 * for all the other agent. It is the agent that starts and updates the simulation and that
 * communicates the new target to each PlatoonFireAgent.
 */
public class CenterAgent extends StandardAgent<Building>
{
    private static final Logger Logger = LogManager.getLogger(CenterAgent.class);

    /** Config key to the (fully qualified) class of the solver to run */
    public static final String CONF_KEY_SOLVER_CLASS = "solver.class";

    /** Config key to set the solving classes to test **/
    public static final String CONF_KEY_TEST_CLASSES = "test.classes";
    
    private Solver solver = null;
    private ArrayList<EntityID> agents = new ArrayList<>();
    private Assignment lastAssignment = new Assignment();
    private List<PlatoonFireAgent> fireAgents;

    public CenterAgent(List<PlatoonFireAgent> fireAgents) {
    	Logger.info(Markers.BLUE, "Center Agent CREATED");
        this.fireAgents = fireAgents;
        for (PlatoonFireAgent fagent : fireAgents) {
            agents.add(fagent.getID());
        }
    }
    
    @Override
    public String toString()
    {
        return "Center Agent";
    }

    /**
     * Sets up the center agent.
     *
     * At this point, the center agent already has a world model, and has
     * laoded the kernel's configuration. Hence, it is ready to setup the
     * assignment solver(s).
     */
    @Override
    public void postConnect() {
        super.postConnect();
        initializeParameters();
        solver = buildSolver();
        solver.initialize(model, config);
    }

    private void initializeParameters() {
        // Set a UUID for this run
        config.setValue(Constants.KEY_RUN_ID, UUID.randomUUID().toString());

        // Set the utility function to use
        String utilityClass = config.getValue(Constants.KEY_UTILITY_CLASS);
        UtilityFactory.setClass(utilityClass);

        // Extract the map and scenario names
        String map = config.getValue("gis.map.dir");
        map = map.substring(map.lastIndexOf("/")+1);
        config.setValue(Constants.KEY_MAP_NAME, map);
        String scenario = config.getValue("gis.map.scenario");
        scenario = scenario.substring(scenario.lastIndexOf("/")+1);
        config.setValue(Constants.KEY_MAP_SCENARIO, scenario);

        // The experiment can not start before the agent ignore time
        int ignore = config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
        int start  = config.getIntValue(Constants.KEY_START_EXPERIMENT_TIME);
        if (ignore > start) {
            Logger.error("The experiment can't start at time {} because agent commands are ignored until time {}", start, ignore);
            System.exit(0);
        }
    }

    private Solver buildSolver() {
        // Load main solver class
        solver = buildSolver(config.getValue(CONF_KEY_SOLVER_CLASS));
        Logger.info("Using main solver: {}", solver.getIdentifier());
        config.setValue(Constants.KEY_MAIN_SOLVER, solver.getIdentifier());

        // And any additional test solvers
        try {
            String[] testClasses = config.getValue(CONF_KEY_TEST_CLASSES).split("[,\\s]+");
            if (testClasses.length > 0) {
                CompositeSolver comp = new CompositeSolver(solver);
                for (String solverClass : testClasses) {
                    Solver s = buildSolver(solverClass);
                    Logger.info("Also testing solver: {}", s.getIdentifier());
                    comp.addSolver(s);
                }
                solver = comp;
            }
        } catch (NoSuchConfigOptionException ex) {}
        return solver;
    }

    private Solver buildSolver(String clazz) {
        try {
            Class<?> c = Class.forName(clazz);
            Object s = c.newInstance();
            if (s instanceof Solver) {
                return (Solver)s;
            }

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.catching(Level.ERROR, ex);
        }

        Logger.error("Unable to initialize solver {}", clazz);
        System.exit(1);
        return null;
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard)
    {
        Collection<EntityID> burning = getBurningBuildings();
        Logger.info(Markers.WHITE, "TIME IS {} | {} known burning buildings.",
                new Object[]{time, burning.size()});

        if (time < config.getIntValue(Constants.KEY_START_EXPERIMENT_TIME)) {
            Logger.debug("Waiting until experiment starts.");
            return;
        }

        // Stop the simulation if all fires have been extinguished
        if (burning.isEmpty()) {
            Logger.info("All fires extinguished. Good job!");
            System.exit(0);
        }

        // Compute assignment
        ArrayList<EntityID> targets = new ArrayList<>(burning);
        UtilityMatrix utility = new UtilityMatrix(config, agents, targets, lastAssignment, model);
        lastAssignment = solver.solve(time, utility);

        // Send assignment to agents
        for (PlatoonFireAgent fagent : fireAgents) {
            fagent.enqueueAssignment(lastAssignment.getAssignment(fagent.getID()));
        }
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum()
    {
        return EnumSet.of(StandardEntityURN.FIRE_STATION, StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.POLICE_OFFICE);
    }
    
    /**
     * It returns the burning buildings
     * @return a collection of burning buildings.
     */
    private Collection<EntityID> getBurningBuildings()
    {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<EntityID> result = new ArrayList<>();
        for (StandardEntity next : e)
        {
            if (next instanceof Building)
            {
                Building b = (Building) next;
                if (b.getFieryness() > 0 && b.getFieryness() < 4)
                {
                    EntityID id = b.getID();
                    if (id == null) {
                        Logger.warn("Found a building with no id: {}. Dropped.", b);
                    }
                    result.add(id);
                }
            }
        }
        // Sort by distance
        return result;
    }
}
