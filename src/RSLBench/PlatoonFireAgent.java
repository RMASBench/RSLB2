package RSLBench;

import RSLBench.Assignment.Assignment;
import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Helpers.DistanceSorter;
import RSLBench.Helpers.Logging.Markers;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A sample fire brigade agent.
 */
public class PlatoonFireAgent extends PlatoonAbstractAgent<FireBrigade>
{
    private static final Logger Logger = LogManager.getLogger(PlatoonFireAgent.class);
    
    public static final String MAX_WATER_KEY = "fire.tank.maximum";
    public static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    public static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

    private int maxWater;
    private int maxDistance;
    private int maxPower;
    private EntityID assignedTarget = Assignment.UNKNOWN_TARGET_ID;

    /** Queue to receive assignments from the central */
    private BlockingQueue<EntityID> assignmentQueue = new ArrayBlockingQueue<>(1);

    public PlatoonFireAgent() {
    	Logger.debug(Markers.BLUE, "Platoon Fire Agent CREATED");
    }

    public boolean enqueueAssignment(EntityID target) {
        return assignmentQueue.offer(target);
    }

    @Override
    public String toString() {
        return "Sample fire brigade";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);
        maxWater = config.getIntValue(MAX_WATER_KEY);
        maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
        maxPower = config.getIntValue(MAX_POWER_KEY);
        Logger.info("{} connected: max extinguish distance = {}, " +
               "max power = {}, max tank = {}",
                this, maxDistance, maxPower, maxWater);
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {

        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to station channel
            sendSubscribe(time, Constants.STATION_CHANNEL);
        }

        if (time < config.getIntValue(Constants.KEY_START_EXPERIMENT_TIME)) {
            return;
        }

        if (time == config.getIntValue(Constants.KEY_END_EXPERIMENT_TIME))
            System.exit(0);

        // Wait until the station sends us an assignment
        ////////////////////////////////////////////////////////////////////////
        Logger.debug("Agent {} waiting for command.", getID());
        try {
            assignedTarget = assignmentQueue.take();
        } catch (InterruptedException ex) {
            Logger.error("Agent {} unable to fetch its assingment from central.",
                    ex, getID());
        }
        Logger.debug("Agent {} approaching {}!", getID(), assignedTarget);

        // Start to act
        // /////////////////////////////////////////////////////////////////////
        FireBrigade me = me();

        // Are we currently filling with water?
        // //////////////////////////////////////
        if (me.isWaterDefined() && me.getWater() < maxWater
                && location() instanceof Refuge) {
            Logger.debug(Markers.MAGENTA, "Filling with water at " + location());
            sendRest(time);
            return;
        }

        // Are we out of water?
        // //////////////////////////////////////
        if (me.isWaterDefined() && me.getWater() == 0) {
            // Head for a refuge
            List<EntityID> path = search.search(me().getPosition(), refugeIDs,
                    connectivityGraph, distanceMatrix);
            if (path != null) {
                // Logger.debugColor("Moving to refuge", //Logger.FG_MAGENTA);
                sendMove(time, path);
                return;
            } else {
                // Logger.debugColor("Couldn't plan a path to a refuge.",
                // //Logger.BG_RED);
                path = randomWalk();
                // Logger.debugColor("Moving randomly", //Logger.FG_MAGENTA);
                sendMove(time, path);
                return;
            }
        }

        // Find all buildings that are on fire
        Collection<EntityID> burning = getBurningBuildings();

        // Try to plan to assigned target
        // ///////////////////////////////
        
        // Ensure that the assigned target is still burning, and unassign the
        // agent if it is not.
        if (!burning.contains(assignedTarget)) {
            assignedTarget = Assignment.UNKNOWN_TARGET_ID;
        }

        if (!assignedTarget.equals(Assignment.UNKNOWN_TARGET_ID)) {

            // Extinguish if the assigned target is in range
            if (model.getDistance(me().getPosition(), assignedTarget) <= maxDistance) {
                Logger.debug(Markers.MAGENTA, "Agent {} extinguishing ASSIGNED target {}", getID(), assignedTarget);
                sendExtinguish(time, assignedTarget, maxPower);
                // sendSpeak(time, 1, ("Extinguishing " + next).getBytes());
                return;
            }

            // Try to approach the target (if we are here, it is not yet in range)
            List<EntityID> path = planPathToFire(assignedTarget);
            if (path != null) {
                Logger.debug(Markers.MAGENTA, "Agent {} approaching ASSIGNED target {}", getID(), assignedTarget);
                sendMove(time, path);
            } else {
                Logger.warn(Markers.RED, "Agent {} can't find a path to ASSIGNED target {}. Moving randomly.", getID(), assignedTarget);
                sendMove(time, randomWalk());
            }
            return;
        } 
        
        // If agents can independently choose targets, do it
        if (!config.getBooleanValue(Constants.KEY_AGENT_ONLY_ASSIGNED)) {
            for (EntityID next : burning) {
                List<EntityID> path = planPathToFire(next);
                if (path != null) {
                    Logger.info(Markers.MAGENTA, "Unassigned agent {} choses target {} by itself", getID(), next);
                    sendMove(time, path);
                    return;
                }
            }
            if (!burning.isEmpty()) {
                Logger.info(Markers.MAGENTA, "Unassigned agent {} can't reach any of the {} burning buildings", getID(), burning.size());
            }
        }
        
        // If the agen't can do nothing else, try to explore or just randomly
        // walk around.
        List<EntityID> path = randomExplore();
        if (path != null) {
            Logger.debug(Markers.MAGENTA, "Agent {} exploring", getID());
        } else {
            path = randomWalk();
            Logger.debug(Markers.MAGENTA, "Agent {} moving randomly", getID());
        }
        
        sendMove(time, path);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }
    
    /**
     * Returns the burning buildings.
     * @return a collection of burning buildings.
     */
    private Collection<EntityID> getBurningBuildings() {
        Collection<StandardEntity> e = model
                .getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<>();
        for (StandardEntity next : e) {
            if (next instanceof Building) {
                Building b = (Building) next;
                if (b.isOnFire()) {
                    result.add(b);
                }
            }
        }
        // Sort by distance
        Collections.sort(result, new DistanceSorter(location(), model));
        return objectsToIDs(result);
    }
    
    /**
     * Given a target, calls the chosen algorothm to plan the path to the target
     * @param target: the target
     * @return a list of EntityID representing the path to the target
     */
    private List<EntityID> planPathToFire(EntityID target) {
        Collection<StandardEntity> targets = model.getObjectsInRange(target,
                maxDistance / 2);
        return search.search(me().getPosition(), objectsToIDs(targets),
                connectivityGraph, distanceMatrix);
    }
    
}