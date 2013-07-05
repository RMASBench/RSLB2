package RSLBench;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import RSLBench.Assignment.AssignmentSolver;
import RSLBench.Comm.SimpleProtocolToServer;
import RSLBench.Helpers.DistanceSorter;
import RSLBench.Helpers.Logging.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * It is a "fake" agent that does not appears in the graphic simulation, but that serves as a "station"
 * for all the other agent. It is the agent that starts and updates the simulation and that
 * communicates the new target to each PlatoonFireAgent.
 */
public class CenterAgent extends StandardAgent<Building>
{
    private static final Logger Logger = LogManager.getLogger(CenterAgent.class);
    
    private AssignmentSolver assignmentSolver = null;
    private ArrayList<EntityID> agents = new ArrayList<EntityID>();
    private HashMap<EntityID, EntityID> agentLocations = new HashMap<EntityID, EntityID>(); 

    protected CenterAgent() {
    	Logger.info(Markers.BLUE, "Center Agent CREATED");
    }

    
    @Override
    public String toString()
    {
        return "Center Agent";
    }
    

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard)
    {
    	// Initialize solver
        if (assignmentSolver == null && model != null) {
            assignmentSolver = new AssignmentSolver(model, config);
        }
        else if (model == null) {
            Logger.error("Cannot run solver without world model! ");
            return;
        }

        // Print out time
        Logger.info(Markers.WHITE, "CenterAgent: TIME IS " + time);
        
        // Find all buildings that are on fire
        Collection<EntityID> burning = getBurningBuildings();
        Logger.info(Markers.LIGHT_BLUE, "Number of known BURNING buildings: " + burning.size());
        		
        // Subscribe to station channels
        if (time == Params.IGNORE_AGENT_COMMANDS_KEY_UNTIL) {
            sendSubscribe(time, Params.PLATOON_CHANNEL);
        }

        // Process all incoming messages
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                AKSpeak speak = (AKSpeak) next;
                EntityID senderId = speak.getAgentID();

                // add fire brigade agents
                if (model.getEntity(senderId).getStandardURN() == StandardEntityURN.FIRE_BRIGADE) {
                    if (!agents.contains(senderId))
                        agents.add(senderId);
                    byte content[] = speak.getContent();
                    processMessageContent(content, senderId.getValue());
                }
            }
            sendRest(time);
        }        

        // Compute assignment
        ArrayList<EntityID> targets = new ArrayList<EntityID>(burning.size());
        for (EntityID next : burning)
        {
            EntityID t = model.getEntity(next).getID();
            targets.add(t);
        }
        byte[] message = assignmentSolver.act(time, agents, targets, agentLocations, model);

        // Send out assignment
        if (message != null)
        {
            int[] intArray = SimpleProtocolToServer.byteArrayToIntArray(message, true);
            if (Logger.isInfoEnabled()) {
                StringBuilder buf = new StringBuilder("STATION SENDS AssignmentMessage: ");
                for (int i = 0; i < intArray.length; i++) {
                    buf.append(intArray[i]).append(" ");
                }
                Logger.info(buf.toString());
            }

            sendSpeak(time, Params.STATION_CHANNEL, message);
        }
    }
    
    /**
     * It processes the content of the message containing the agent assignments.
     * @param content
     * @param senderId 
     */
    private void processMessageContent(byte content[], int senderId)
    {
    	if (content.length == 0)
    		return;
    	byte MESSAGE_TYPE = content[0];

    	switch (MESSAGE_TYPE)
    	{
    	case SimpleProtocolToServer.POS_MESSAGE:
    		int posInt = SimpleProtocolToServer.getPosIdFromMessage(content);
    		StandardEntity agent = model.getEntity(new EntityID(senderId));
    		StandardEntity pos = model.getEntity(new EntityID(posInt));    		
    		//Logger.debugColor("Station: heard from agent " + agent + " Pos: " + pos, Logger.BG_LIGHTBLUE);
    		agentLocations.put(agent.getID(), pos.getID());    		
    		break;
    	default:
    		Logger.warn(Markers.RED, "Station: cannot parse message of type " + MESSAGE_TYPE + " Message size is " + content.length);
    		break;
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
