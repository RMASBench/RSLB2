package RSLBench.Comm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

/**
 * This class represents a communication layer that the agents use to
 * communicate between themselves during the execution of each timestep,
 * ignoring the messages limitations of the usual communication layer between
 * agents and kernel. In this way agents can exchange as many messages as they
 * want as expected in a DCOP algorithm. When a message is sent to an agent, it
 * is added in the messageInbox of the recipient (represented by an EntityID),
 * so that the recipient can read all the messages sent to him when the time
 * comes.
 */
public class ComSimulator {

    private static final Logger Logger = LogManager.getLogger(ComSimulator.class);
    Map<EntityID, List<AbstractMessage>> messageInboxes;
    private boolean initialized;

    /**
     * It builds a ComSimulator.
     *
     * @param maxCommunicationRange: the range of communication of the agents.
     */
    public ComSimulator() {
        initialized = false;
    }

    /**
     * This method initializes the communicator for each agent.
     *
     * @param agents: all the agents in the simulation
     */
    public void initialize(List<EntityID> agents) {
        Logger.debug("initialize Com for " + agents.size() + " agents");
        messageInboxes = new HashMap<>();
        for (EntityID id : agents) {
            messageInboxes.put(id, new ArrayList<AbstractMessage>());
        }
        initialized = true;
    }

    /**
     * This method resets all the messageInboxes.
     */
    public void update() {
        for (List<AbstractMessage> inbox : messageInboxes.values()) {
            inbox.clear();
        }
    }

    /**
     * This method memorizes a message in the messageInbox of the recipient.
     *
     * @param agentID: the id of the recipient
     * @param message: the message
     */
    public void send(EntityID agentID, AbstractMessage message) {
        messageInboxes.get(agentID).add(message);
    }

    /**
     * This method memorizes a series of messages in the messageInbox of the
     * recipient
     *
     * @param agentID: the id of the recipient
     */
    public void send(EntityID agentID, Collection<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            messageInboxes.get(agentID).add(message);
        }
    }

    /**
     * This method retrieves the messages from the inbox of an agent
     *
     * @param agentID: the id of the recipient
     * @return a list of alla the messages received
     */
    public List<AbstractMessage> retrieveMessages(EntityID agentID) {
        List<AbstractMessage> mesageInbox = messageInboxes.get(agentID);
        messageInboxes.put(agentID, new ArrayList<AbstractMessage>());
        return mesageInbox;
    }

    /**
     * Returns if the ComSimulator is initialized.
     *
     * @return true if the ComSimulator is initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

}
