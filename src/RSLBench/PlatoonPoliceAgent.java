package RSLBench;

import RSLBench.Assignment.Assignment;
import java.util.Collection;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Helpers.Logging.Markers;
import java.util.EnumSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import static rescuecore2.misc.Handy.objectsToIDs;
import rescuecore2.standard.entities.Road;



/**
 * A sample fire brigade agent.
 */
public class PlatoonPoliceAgent extends PlatoonAbstractAgent<PoliceForce>
{
    private static final Logger Logger = LogManager.getLogger(PlatoonPoliceAgent.class);

    public static final String DISTANCE_KEY = "clear.repair.distance";
    private int distance;

    /** EntityID of the road where the blockade that this agent should remove is located */
    private EntityID assignedTarget = Assignment.UNKNOWN_TARGET_ID;

    public PlatoonPoliceAgent() {
    	Logger.debug(Markers.BLUE, "Platoon Police Agent CREATED");
    }

    @Override
    public String toString() {
        return "Police force";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        distance = config.getIntValue(DISTANCE_KEY);
        Logger.info("{} connected: clearing distance = {}", this, distance);
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

        if (time == config.getIntValue(Constants.KEY_END_EXPERIMENT_TIME)) {
            System.exit(0);
        }

        // Wait until the station sends us an assignment
        ////////////////////////////////////////////////////////////////////////
        Logger.debug("Agent {} waiting for command.", getID());
        assignedTarget = fetchAssignment();
        if (assignedTarget != null) {
            Logger.debug("Agent {} got target {}", getID(), assignedTarget);
        } else {
            Logger.debug("Agent {} unable to fetch its assignment.", getID());
            assignedTarget = Assignment.UNKNOWN_TARGET_ID;
        }

        // Start to act
        // /////////////////////////////////////////////////////////////////////
        PoliceForce me = me();

        // If we have a target, approach or clear it
        // ///////////////////////////////
        if (assignedTarget != null && !assignedTarget.equals(Assignment.UNKNOWN_TARGET_ID)) {
            EntityID bID = assignedTarget;
            assignedTarget = ((Blockade)model.getEntity(bID)).getPosition();

            // Clear if in range
            if (model.getDistance(me.getPosition(), assignedTarget) <= distance) {
                Logger.debug(Markers.BLUE, "Police force {} clearing ASSIGNED target {}", getID(), assignedTarget);
                clear(time, assignedTarget);
                return;
            }

            // Approach it otherwise
            List<EntityID> path = planPathToRoad(assignedTarget);
            if (path != null) {
                Logger.debug(Markers.MAGENTA, "Police force {} approaching ASSIGNED target {}", getID(), assignedTarget);
                sendMove(time, path);
            } else {
                Logger.warn(Markers.RED, "Police force {} can't find a path to ASSIGNED target {}. Moving randomly.", getID(), assignedTarget);
                sendMove(time, randomWalk());
            }
            return;
        }

        // If agents can independently choose targets, do it
        if (!config.getBooleanValue(Constants.KEY_AGENT_ONLY_ASSIGNED)) {
            EntityID myPosition = me().getPosition();

            // Pick the closest blockade
            Double minDistance = Double.MAX_VALUE;
            EntityID bestTarget = null;
            for (StandardEntity entity : model.getEntitiesOfType(StandardEntityURN.BLOCKADE)) {
                EntityID blockadePosition = ((Blockade)entity).getPosition();
                double d = model.getDistance(myPosition, blockadePosition);
                if (d < minDistance) {
                    minDistance = d;
                    bestTarget = blockadePosition;
                }
            }

            if (bestTarget == null) {
                Logger.info(Markers.BLUE, "Unassigned police force {} can't find any target.", getID());
                explore(time);
                return;
            }

            // Clear if in range
            if (model.getDistance(me.getPosition(), bestTarget) <= distance) {
                Logger.debug(Markers.BLUE, "Police force {} clearing self-assigned target {}", getID(), bestTarget);
                clear(time, bestTarget);
                return;
            }

            List<EntityID> path = planPathToRoad(bestTarget);
            if (path != null) {
                Logger.info(Markers.BLUE, "Unassigned police force {} choses target {} by itself", getID(), bestTarget);
                sendMove(time, path);
                return;
            } else {
                Logger.warn(Markers.BLUE, "Unassigned police force {} can't find path to self-assigned target {}", getID(), bestTarget);
            }
        }

        explore(time);
    }

    private void explore(int time) {
        // If the agen't can do nothing else, try to explore or just randomly walk around.
        List<EntityID> path = randomExplore();
        if (path != null) {
            Logger.debug(Markers.BLUE, "Police force {} exploring", getID());
        } else {
            path = randomWalk();
            Logger.debug(Markers.BLUE, "Police force {} moving randomly", getID());
        }

        sendMove(time, path);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
    }

    /**
     * Given a target, calls the chosen algorothm to plan the path to the target
     * @param target: the target
     * @return a list of EntityID representing the path to the target
     */
    private List<EntityID> planPathToRoad(EntityID target) {
        Collection<StandardEntity> targets = model.getObjectsInRange(target,
                distance / 2);
        return search.search(me().getPosition(), objectsToIDs(targets),
                connectivityGraph, distanceMatrix).getPathIds();
    }

    /**
     * Failed attempt at using AKClearArea messages. Reverting to AKClear instead.
     * @param time
     * @param road  F
     *
    private void clear(int time, EntityID road) {
        StandardEntity entity = model.getEntity(road);
        if (!(entity instanceof Road)) {
            Logger.warn("Police {} tried to clear non-road {}", getID(), road);
            return;
        }
        StandardEntity bEntity = model.getEntity(((Road)entity).getBlockades().get(0));
        if (!(bEntity instanceof Blockade)) {
            Logger.warn("Police {} tried to clear road {}, but it contains no blockades", getID(), road);
        }

        Blockade target = (Blockade)bEntity;
        Logger.warn("Target apexes: {}", target.getApexes());
        List<Point2D> vertices = GeometryTools2D.vertexArrayToPoints(target.getApexes());
        double best = Double.MIN_VALUE;
        Point2D bestPoint = null;
        Point2D origin = new Point2D(me().getX(), me().getY());
        for (Point2D vertex : vertices) {
            double d = GeometryTools2D.getDistance(origin, vertex);
            if (d > best) {
                best = d;
                bestPoint = vertex;
            }
        }
        sendClear(time, (int)(bestPoint.getX()), (int)(bestPoint.getY()));
    }*/

    private void clear(int time, EntityID road) {
        StandardEntity entity = model.getEntity(road);
        if (!(entity instanceof Road)) {
            Logger.warn("Police {} tried to clear non-road {}", getID(), road);
            return;
        }
        List<EntityID> blockades = ((Road)entity).getBlockades();
        if (blockades.isEmpty()) {
            Logger.warn("Police {} tried to clear road {}, but it contains no blockades", getID(), road);
            return;
        }

        sendClear(time, blockades.get(0));
    }

}