package RSLBench;

import RSLBench.Helpers.Logging.Markers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import RSLBench.Search.BreadthFirstSearch;
import RSLBench.Search.DistanceInterface;
import RSLBench.Search.Graph;
import RSLBench.Search.SearchAlgorithm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.Constants;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

/**
   Abstract base class for agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class PlatoonAbstractAgent<E extends StandardEntity> extends StandardAgent<E> {
    private static final Logger Logger = LogManager.getLogger(PlatoonAbstractAgent.class);
    
    private static final int RANDOM_WALK_LENGTH = 50;
        
    //private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();
    
    public static final String KEY_SEARCH_CLASS = "agent.search.class";

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
     * Cache of refuge IDs.
     */
    protected List<EntityID> refugeIDs;

    /**
     * the connectivity graph of all places in the world
     */
    protected Graph connectivityGraph;
    
    /**
     * a matrix containing the pre-computed distances between each two areas in the world
     */
    protected DistanceInterface distanceMatrix;
    
    /**
     * The search algorithm.
     */
    protected SearchAlgorithm search;
    
    protected EntityID randomExplorationGoal = null;
    
    /**
     * Construct an AbstractSampleAgent.
     */

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
        }
        
        // load correct search algorithm
        search = selectSearchAlgorithm();
        connectivityGraph = new Graph(model);
        distanceMatrix = new DistanceInterface(model);
        
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = ((Human)me()).getPosition();
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(connectivityGraph.getNeighbors(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            for (EntityID next : possible) {
                if (seen.contains(next)) {
                    continue;
                }
                current = next;
                found = true;
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        return result;
    }
    
    protected List<EntityID> randomExplore()
    {
        // check if goal reached
        EntityID position = ((Human)me()).getPosition();
        if (randomExplorationGoal != null)
        {
            int distance = model.getDistance(position, randomExplorationGoal);
            Logger.debug(Markers.BLUE, "RANDOM_EXPLORATION: distance to goal: " + distance);
            if (distance <= 20000)
            {
                randomExplorationGoal = null;
                Logger.debug(Markers.BLUE, "RANDOM_EXPLORATION: goal reached");
            }
        }
        
        // select new exploration goal
        if (randomExplorationGoal == null)
        {
            //Logger.debugColor("RANDOM_EXPLORATION: selecting new goal", Logger.BG_BLUE);
            Collection<StandardEntity> roads = model.getEntitiesOfType(StandardEntityURN.ROAD);
            Entity[] roadArray = roads.toArray(new Entity[0]);
            int index = random.nextInt(100000);
            int step = getID().getValue();
            while (randomExplorationGoal == null)
            {
                index += step;
                index %= roadArray.length;
                Entity entity = roadArray[index];
                if (model.getDistance(position, entity.getID()) > 20000)
                {
                    randomExplorationGoal = entity.getID();
                    Logger.debug(Markers.BLUE, "RANDOM_EXPLORATION: new goal selected");
                }
            }
        }
        
        // plan path to goal
        return search.search(position, randomExplorationGoal, connectivityGraph, distanceMatrix);
    }
    
    private SearchAlgorithm selectSearchAlgorithm()
    {
        // construct default search algorithm
        SearchAlgorithm instance = new BreadthFirstSearch();
        
        // retrieve data from config
        String searchClassName = config.getValue(KEY_SEARCH_CLASS);
        try
        {   
            // instantiate search algorithm
            Class<?> concreteSearchClass = Class.forName(searchClassName);
            Object object = concreteSearchClass.newInstance();
            if (object instanceof SearchAlgorithm)
            {
                instance = (SearchAlgorithm) object;
                Logger.debug(Markers.LIGHT_BLUE, "Using search class: " + searchClassName);
            }
            else
            {
                Logger.error(Markers.RED, searchClassName + " is not a SearchAlgorithm.");
            }
        } catch (NoSuchConfigOptionException e)
        {
            Logger.warn(Markers.RED, "SearchAlgorithm config not found. Using BreadthFirstSearch.");
        } catch (ClassNotFoundException e)
        {
            Logger.error("SearchAlgorithm could not be found: " + searchClassName);
        } catch (InstantiationException e)
        {
            Logger.error("SearchAlgorithm " + searchClassName + " could not be instantiated. (abstract?!)");
        } catch (IllegalAccessException e)
        {
            Logger.error("SearchAlgorithm " + searchClassName + " must have an empty constructor.");
        }
        
        return instance;
    }
}
