package RSLBench;

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
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Comm.SimpleProtocolToServer;
import RSLBench.Helpers.DistanceSorter;
import RSLBench.Helpers.Logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A sample fire brigade agent.
 */
public class PlatoonFireAgent extends PlatoonAbstractAgent<FireBrigade>
{
    private static final Logger Logger = LogManager.getLogger(PlatoonFireAgent.class);
    
    private static final String MAX_WATER_KEY = "fire.tank.maximum";
    private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
    private static final String IGNORE_ACT_UNTIL = "kernel.agents.ignoreuntil";

    private int maxWater;
    private int maxDistance;
    private int maxPower;
    private int firstTimeToAct;
    private int assignedTarget = -1;

    public PlatoonFireAgent() {
    	Logger.debug(Markers.BLUE, "Platoon Fire Agent CREATED");
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
        firstTimeToAct = Math.max(config.getIntValue(IGNORE_ACT_UNTIL) + 1,
                config.getIntValue("experiment_start_time", 25));
        maxPower = config.getIntValue(MAX_POWER_KEY);
        Logger.info("Sample fire brigade connected: max extinguish distance = "
                + maxDistance + ", max power = " + maxPower + ", max tank = "
                + maxWater);
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == Params.IGNORE_AGENT_COMMANDS_KEY_UNTIL) {
            // Subscribe to station channel
            sendSubscribe(time, Params.STATION_CHANNEL);
        }
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                AKSpeak speak = (AKSpeak) next;
                int senderIdValue = speak.getAgentID().getValue();
                byte content[] = speak.getContent();
                EntityID eid = speak.getAgentID();
                StandardEntity entity = model.getEntity(eid);
                if (entity.getStandardURN() == StandardEntityURN.FIRE_STATION) {
                    Logger.debug(Markers.GREEN, "Heard FROM FIRE_STATION: " + next);
                    processStationMessage(content, senderIdValue);
                } 
                else if (entity.getStandardURN() == StandardEntityURN.FIRE_BRIGADE) {
                    // Logger.debugColor("Heard FROM OTHER FIRE_BRIGADE: " +
                    // next,Logger.FG_LIGHTBLUE);
                }
            }
        }

        if (time < firstTimeToAct)
            return;

        if (time == Params.END_EXPERIMENT_TIME)
            System.exit(0);

        // Start to act
        // //////////////////////////////////////////////////////////////////////////////////////
        FireBrigade me = me();

        // / Send position to station
        sendSpeak(time, Params.PLATOON_CHANNEL, SimpleProtocolToServer
                .getPosMessage(me()));

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
        if (assignedTarget != -1 && !burning.contains(new EntityID(assignedTarget))) {
            assignedTarget = -1;
        }

        if (assignedTarget != -1) {
            EntityID target = new EntityID(assignedTarget);

            // Extinguish if the assigned target is in range
            if (model.getDistance(me().getPosition(), target) <= maxDistance) {
                Logger.debug(Markers.MAGENTA, "Agent {} extinguishing ASSIGNED target {}", getID(), target);
                sendExtinguish(time, target, maxPower);
                // sendSpeak(time, 1, ("Extinguishing " + next).getBytes());
                return;
            }

            // Try to approach the target (if we are here, it is not yet in range)
            List<EntityID> path = planPathToFire(target);
            if (path != null) {
                Logger.debug(Markers.MAGENTA, "Agent {} approaching ASSIGNED target {}", getID(), target);
                sendMove(time, path);
            } else {
                Logger.warn(Markers.RED, "Agent {} can't find a path to ASSIGNED target {}. Moving randomly.", getID(), target);
                sendMove(time, randomWalk());
            }
            return;
        } 
        
        // If agents can independently choose targets, do it
        if (!Params.ONLY_ACT_ON_ASSIGNED_TARGETS) {
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
    
    /**
     * Reads and processes the station message, i.e. if it is an assignment messages
     * a target is assigned to the PlatoonFireAgent.
     * @param msg
     * @param senderId 
     */
    private void processStationMessage(byte msg[], int senderId) {
        // Logger.debugColor("PROCESS MSG FROM STATION", //Logger.BG_LIGHTBLUE);
        if (msg.length == 0)
            return;
        byte MESSAGE_TYPE = msg[0];

        switch (MESSAGE_TYPE) {
        case SimpleProtocolToServer.POS_MESSAGE:
            break;
        case SimpleProtocolToServer.STATION_ASSIGNMENT_MESSAGE:
            // Logger.debugColor("msg received " + me(), //Logger.BG_MAGENTA);
            byte[] raw = SimpleProtocolToServer.removeHeader(msg);
            int[] intArray = SimpleProtocolToServer.byteArrayToIntArray(raw, false);
            int id = me().getID().getValue();
            for (int i = 0; i < intArray.length; i++) {
                if (intArray[i] == id) {
                    int targetID = intArray[i + 1];
                    Building newT = (Building) model.getEntity(new EntityID(
                            targetID));
                    if (!newT.isOnFire()) {
                        // Logger.debugColor("ERROR: Got target from station that is NOT ON FIRE: "
                        // + newT.getID() + " fire is " + newT.getFieryness(),
                        // //Logger.FG_RED);
                        continue;
                    }

                    // Take station assignment
                    if (assignedTarget == -1) {
                        assignedTarget = targetID;
                        // Logger.debugColor("Setting station assignment " +
                        // assignedTarget, //Logger.FG_GREEN);
                        break;
                    }

                    // Change current target if better one assigned
                    Building curT = (Building) model.getEntity(new EntityID(
                            assignedTarget));

                    if (curT.getFieryness() > newT.getFieryness()) {
                        assignedTarget = targetID;
                        // Logger.debugColor("Setting station assignment " +
                        // assignedTarget, //Logger.FG_GREEN);
                        break;
                    }
                }
            }
            break;
        default:
            // Logger.debugColor("Agent: cannot parse message of type " +
            // MESSAGE_TYPE + " Message size is " + msg.length,
            // //Logger.FG_RED);
            break;
        }
    }
}