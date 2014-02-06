/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import RSLBench.Assignment.AbstractSolver;
import RSLBench.Assignment.Assignment;
import RSLBench.Assignment.DCOP.DCOPSolver;
import RSLBench.Constants;
import RSLBench.Helpers.Utility.ProblemDefinition;
import RSLBench.Helpers.Utility.UtilityFactory;
import RSLBench.Helpers.Utility.UtilityFunction;
import RSLBench.Search.DistanceInterface;
import RSLBench.Search.Graph;
import RSLBench.Search.SearchAlgorithm;
import RSLBench.Search.SearchFactory;
import RSLBench.Search.SearchResults;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
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
        config.setValue(Constants.KEY_RESULTS_PATH, "build/test");
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
        Random random = new Random(RANDOM_SEED);

        Config config = mock(Config.class);
        when(config.getValue(Constants.KEY_RESULTS_PATH)).thenReturn("build/test");
        when(config.getValue(Constants.KEY_RUN_ID)).thenReturn("1");
        when(config.getRandom()).thenReturn(random);

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

    private static long RANDOM_SEED = 200L;

    @Test
    public void testBuildMaxSumAgentsAndFires() throws Exception {
        Random random = new Random(RANDOM_SEED);

        Config config = mock(Config.class);
        when(config.getValue(Constants.KEY_RESULTS_PATH)).thenReturn("build/test");
        when(config.getValue(Constants.KEY_RUN_ID)).thenReturn("1");
        when(config.getRandom()).thenReturn(random);

        double[][] utilities = new double[][] {
            new double[]{1.11, 2.12, 3.13, 4.14, 5.15, 6.16, 0.17, 2.18},
            new double[]{2.21, 3.22, 1.23, 3.24, 2.25, 3.26, 0.27, 1.28},
            new double[]{1.31, 1.32, 2.33, 3.34, 4.35, 5.36, 0.37, 6.38},
            new double[]{3.41, 1.42, 3.43, 0.44, 1.45, 1.47, 0.48, 4.49},
            //new double[]{3,1,3,3,1,1,0,4},
            //new double[]{3,1,3,3,1,1,0,4},
            //new double[]{3,1,3,3,1,1,0,4},
        };

        HashMap<EntityID, FireBrigade> fireAgentsMap = new HashMap<>();
        ArrayList<EntityID> fireAgents = new ArrayList<>();
        for (int i=1; i<=utilities.length; i++) {
            EntityID e = new EntityID(i);
            fireAgentsMap.put(e, mock(FireBrigade.class));
            fireAgents.add(e);
        }

        HashMap<EntityID, Building> firesMap = new HashMap<>();
        ArrayList<EntityID> fires = new ArrayList<>();
        for (int j=100;j<100+utilities[0].length;j++) {
            EntityID e = new EntityID(j);
            firesMap.put(e, mock(Building.class));
            fires.add(e);
        }

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

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MaxSumAgent.printFG(os);
            System.err.println(os);
        }
    }

    @Ignore
    private void testStoredTest(double[][] utilities, int nNeighbors) throws Exception {
        Random random = new Random(RANDOM_SEED);

        Config config = mock(Config.class);
        when(config.getValue(Constants.KEY_RESULTS_PATH)).thenReturn("build/test");
        when(config.getValue(Constants.KEY_RUN_ID)).thenReturn("1");
        when(config.getRandom()).thenReturn(random);

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
    }

    private final String RESOURCE_FOLDER = "src/test/resources/RSLBench/Algorithms/MS/";

    @Test
    public void testStoredTests() throws Exception {
        final String TEST_NAME = "4agents";

        TestReader reader = new TestReader(TEST_NAME + ".test.txt");
        for (int i=1; reader.hasNextTest(); i++) {
            double[][] utilities = reader.nextTest();

            for (int nNeighbors = 2; nNeighbors < 5; nNeighbors ++) {
                testStoredTest(utilities, nNeighbors);

                // Load expected test result
                String resultName =  TEST_NAME + ".result." + nNeighbors + "n." + i + ".dot";
                ByteArrayOutputStream expected = new ByteArrayOutputStream();
                copy(this.getClass().getResourceAsStream(resultName), expected);

                ByteArrayOutputStream actual = new ByteArrayOutputStream();
                MaxSumAgent.printFG(actual);

                assertEquals("Test case " + i + " with " + nNeighbors + " neighbors failed.",
                        expected.toString(), actual.toString());
                actual.close();
                expected.close();
            }
        }
    }

    private static final int BUFFER_SIZE = 1024;
    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int n = 0;
        while (-1 != (n = is.read(buffer))) {
            os.write(buffer, 0, n);
        }
    }

    /**
     * Reads a graph prunning test file.
     *
     * The file's format is one test case per line, containing an array of arrays of doubles
     * that is converted to the cost matrix to test (firefigthers are rows, fires are columns)
     */
    private class TestReader {
        private BufferedReader reader;
        private final Pattern pattern = Pattern.compile("\\[([^\\]\\[]+)\\]");
        private String line;

        public TestReader(String testName) {
            reader = new BufferedReader(new InputStreamReader(
                    this.getClass().getResourceAsStream(testName)
            ));
            next();
        }

        private void next() {
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean hasNextTest() {
            return line != null;
        }

        public double[][] nextTest() {
            int nAgent = 0;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                nAgent++;
            }

            double[][] result = new double[nAgent][];
            matcher = pattern.matcher(line);
            nAgent = 0;
            while (matcher.find()) {
                String[] values = matcher.group(1).split(", ");
                result[nAgent] = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    result[nAgent][i] = Double.valueOf(values[i]);
                }
                nAgent++;
            }

            assertEquals("Parsed values do not match the test.", line, Arrays.deepToString(result));
            next();
            return result;
        }
    }

}