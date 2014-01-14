/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import RSLBench.Assignment.AbstractSolver;
import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Comm.CommunicationLayer;
import RSLBench.Comm.Message;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import RSLBench.Helpers.Utility.UtilityFactory;
import RSLBench.Helpers.Utility.UtilityFunction;
import RSLBench.Search.DistanceInterface;
import RSLBench.Search.Graph;
import RSLBench.Search.SearchAlgorithm;
import RSLBench.Search.SearchFactory;
import RSLBench.Search.SearchResults;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
//import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.score.BuildingDamageScoreFunction;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchFactory.class, SearchAlgorithm.class, Graph.class, ProblemDefinition.class, UtilityFactory.class,
StandardWorldModel.class, StandardEntity.class, AbstractSolver.class})
public class MaxSumAgentTest {

    public MaxSumAgentTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBuildNullProblem() {
        try {
            DCOPSolver ms = new MaxSum();
            ms.compute(null);
            fail("Compute with a null problem definition must throw an exception.");
        } catch (NullPointerException p) {}
    }

    @Test
    public void testBuildNonmockedProblem() {
        Config config = new Config();
        ArrayList<EntityID> fireAgents = new ArrayList<>();
        ArrayList<EntityID> fires = new ArrayList<>();
        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = null;
        StandardWorldModel world = null;

        try {
            ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
            fail("Compute with a null problem definition must throw an exception.");
        } catch (Exception e) {}
    }

    @Test
    public void testBuildProblemWithMocks() throws Exception {
        Config config = new Config();
        ArrayList<EntityID> fireAgents = new ArrayList<>();
        ArrayList<EntityID> fires = new ArrayList<>();
        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = null;
        StandardWorldModel world = null;

        // mock search-related stuff
        mockStatic(SearchFactory.class);
        when(SearchFactory.buildSearchAlgorithm(config)).thenReturn(mock(SearchAlgorithm.class));
        whenNew(Graph.class).withAnyArguments().thenReturn(mock(Graph.class));

        // mock utility stuff
        mockStatic(UtilityFactory.class);
        when(UtilityFactory.buildFunction()).thenReturn(mock(UtilityFunction.class));

        ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
    }

    @Test
    public void testBuildNoMaxSumAgents() throws Exception {
        Config config = new Config();
        ArrayList<EntityID> fireAgents = new ArrayList<>();
        ArrayList<EntityID> fires = new ArrayList<>();
        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = null;
        StandardWorldModel world = null;

        // mock search-related stuff
        mockStatic(SearchFactory.class);
        when(SearchFactory.buildSearchAlgorithm(config)).thenReturn(mock(SearchAlgorithm.class));
        whenNew(Graph.class).withAnyArguments().thenReturn(mock(Graph.class));

        // mock utility stuff
        mockStatic(UtilityFactory.class);
        when(UtilityFactory.buildFunction()).thenReturn(mock(UtilityFunction.class));

        ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
        MaxSum ms = new MaxSum();
        try {
            ms.initializeAgents(p);
        } catch (RuntimeException e) {
            assertEquals("No fire agents have been initialized yet.", e.getMessage());
        }
    }


    @Test
    public void testBuildMaxSumAgent() throws Exception {
        Config config = new Config();
        config.setIntValue(Constants.KEY_MAXSUM_NEIGHBORS, 4);
        config.setValue(Constants.CONF_KEY_RESULTS_PATH, "build/test");
        config.setValue(Constants.KEY_RUN_ID, "1");
        ArrayList<EntityID> fireAgents = new ArrayList<>();
        EntityID a1 = new EntityID(1);
        fireAgents.add(a1);

        ArrayList<EntityID> fires = new ArrayList<>();
        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = null;

        // mock search-related stuff
        mockStatic(SearchFactory.class);
        SearchAlgorithm sa = mock(SearchAlgorithm.class);
        when(SearchFactory.buildSearchAlgorithm(config)).thenReturn(sa);
        Graph g = mock(Graph.class);
        whenNew(Graph.class).withAnyArguments().thenReturn(g);

        // mock utility stuff
        mockStatic(UtilityFactory.class);
        UtilityFunction uf = mock(UtilityFunction.class);
        when(UtilityFactory.buildFunction()).thenReturn(uf);

        // mock entity getters
        //when(world.getEntity(org.mockito.Mockito.eq(new EntityID(1)))).thenReturn(mock(StandardEntity.class));
        StandardWorldModel world = mock(StandardWorldModel.class);
        FireBrigade entity = mock(FireBrigade.class);
        when(world.getEntity(a1)).thenReturn(entity);
        when(entity.getStandardURN()).thenReturn(StandardEntityURN.FIRE_BRIGADE);

        // mock score function for solver initialization
        BuildingDamageScoreFunction sf = mock(BuildingDamageScoreFunction.class);
        whenNew(BuildingDamageScoreFunction.class).withNoArguments().thenReturn(sf);
        doNothing().when(sf).initialise(world, config);

        ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
        MaxSum ms = new MaxSum();
        ms.initialize(world, config);
        ms.initializeAgents(p);
    }

    @Test
    public void testBuildMaxSumAgentAndFire() throws Exception {
        Config config = new Config();
        config.setIntValue(Constants.KEY_MAXSUM_NEIGHBORS, 4);
        config.setValue(Constants.CONF_KEY_RESULTS_PATH, "build/test");
        config.setValue(Constants.KEY_RUN_ID, "1");
        config.setFloatValue(Constants.KEY_UTIL_K, 2);
        config.setFloatValue(Constants.KEY_UTIL_ALPHA, 2);

        ArrayList<EntityID> fireAgents = new ArrayList<>();
        EntityID a1 = new EntityID(1);
        fireAgents.add(a1);

        ArrayList<EntityID> fires = new ArrayList<>();
        EntityID f1 = new EntityID(100);
        fires.add(f1);

        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = new Assignment();

        // mock search-related stuff
        mockStatic(SearchFactory.class);
        SearchAlgorithm sa = mock(SearchAlgorithm.class);
        when(SearchFactory.buildSearchAlgorithm(config)).thenReturn(sa);
        Graph g = mock(Graph.class);
        whenNew(Graph.class).withAnyArguments().thenReturn(g);
        // Always return unblocked paths
        when(sa.search(any(EntityID.class), any(EntityID.class), eq(g), any(DistanceInterface.class))).thenReturn(new SearchResults());

        // mock utility stuff
        mockStatic(UtilityFactory.class);
        UtilityFunction uf = mock(UtilityFunction.class);
        when(UtilityFactory.buildFunction()).thenReturn(uf);

        // mock entity getters
        //when(world.getEntity(org.mockito.Mockito.eq(new EntityID(1)))).thenReturn(mock(StandardEntity.class));
        StandardWorldModel world = mock(StandardWorldModel.class);
        FireBrigade entity = mock(FireBrigade.class);
        when(world.getEntity(a1)).thenReturn(entity);
        when(entity.getStandardURN()).thenReturn(StandardEntityURN.FIRE_BRIGADE);

        // mock score function for solver initialization
        BuildingDamageScoreFunction sf = mock(BuildingDamageScoreFunction.class);
        whenNew(BuildingDamageScoreFunction.class).withNoArguments().thenReturn(sf);
        doNothing().when(sf).initialise(world, config);

        ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
        MaxSum ms = new MaxSum();
        ms.initialize(world, config);
        ms.initializeAgents(p);
    }

    @Test
    public void testBuildMaxSumAgentsAndFires() throws Exception {
        Config config = new Config();
        config.setIntValue(Constants.KEY_MAXSUM_NEIGHBORS, 4);
        config.setValue(Constants.CONF_KEY_RESULTS_PATH, "build/test");
        config.setValue(Constants.KEY_RUN_ID, "1");
        config.setFloatValue(Constants.KEY_UTIL_K, 2);
        config.setFloatValue(Constants.KEY_UTIL_ALPHA, 2);

        double[][] utilities = new double[][] {
            new double[]{1,2,3,4,5,6,0,2},
            new double[]{2,3,1,3,2,3,0,1},
            new double[]{1,1,2,3,4,5,0,6},
            new double[]{3,1,3,3,1,1,0,4},
            new double[]{3,1,3,3,1,1,0,4},
            new double[]{3,1,3,3,1,1,0,4},
            new double[]{3,1,3,3,1,1,0,4},
        };

        HashMap<EntityID, FireBrigade> fireAgentsMap = new HashMap<>();
        for (int i=1; i<=utilities.length; i++) {
            fireAgentsMap.put(new EntityID(i), mock(FireBrigade.class));
        }
        ArrayList<EntityID> fireAgents = new ArrayList<>(fireAgentsMap.keySet());

        HashMap<EntityID, Building> firesMap = new HashMap<>();
        for (int j=100;j<100+utilities[0].length;j++) {
            firesMap.put(new EntityID(j), mock(Building.class));
        }
        ArrayList<EntityID> fires = new ArrayList<>(firesMap.keySet());

        ArrayList<EntityID> policeAgents = new ArrayList<>();
        ArrayList<EntityID> blockades = new ArrayList<>();
        Assignment lastAssignment = new Assignment();

        // mock search-related stuff
        mockStatic(SearchFactory.class);
        SearchAlgorithm sa = mock(SearchAlgorithm.class);
        when(SearchFactory.buildSearchAlgorithm(config)).thenReturn(sa);
        Graph g = mock(Graph.class);
        whenNew(Graph.class).withAnyArguments().thenReturn(g);
        // Always return unblocked paths
        when(sa.search(any(EntityID.class), any(EntityID.class), eq(g), any(DistanceInterface.class))).thenReturn(new SearchResults());

        // mock utility stuff
        mockStatic(UtilityFactory.class);
        UtilityFunction uf = mock(UtilityFunction.class);
        when(UtilityFactory.buildFunction()).thenReturn(uf);
        for (int i=0; i<fireAgents.size(); i++) {
            for (int j=0; j<fires.size(); j++) {
                when(uf.getFireUtility(fireAgents.get(i), fires.get(j))).thenReturn(utilities[i][j]);
            }
        }

        // mock entity getters
        //when(world.getEntity(org.mockito.Mockito.eq(new EntityID(1)))).thenReturn(mock(StandardEntity.class));
        StandardWorldModel world = mock(StandardWorldModel.class);
        for (EntityID id : fireAgents) {
            FireBrigade b = fireAgentsMap.get(id);
            when(world.getEntity(id)).thenReturn(b);
            when(b.getStandardURN()).thenReturn(StandardEntityURN.FIRE_BRIGADE);
        }

        // mock score function for solver initialization
        BuildingDamageScoreFunction sf = mock(BuildingDamageScoreFunction.class);
        whenNew(BuildingDamageScoreFunction.class).withNoArguments().thenReturn(sf);
        doNothing().when(sf).initialise(world, config);

        ProblemDefinition p = new ProblemDefinition(config, fireAgents, fires, policeAgents, blockades, lastAssignment, world);
        MaxSum ms = new MaxSum();
        ms.initialize(world, config);
        ms.initializeAgents(p);

        // get the stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MaxSumAgent.printFG(os);
        System.err.println(os);
    }

}