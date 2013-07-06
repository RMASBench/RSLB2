package RSLBench.Comm.Platform;

import java.util.ArrayList;

import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;
import RSLBench.Assignment.Assignment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class that converts messages in bytes allowing the kernel to read them.
 */
public class SimpleProtocolToServer
{
    private static final Logger Logger = LogManager.getLogger(SimpleProtocolToServer.class);
    
    public static final byte POS_MESSAGE = 0x10;
    public static final byte STATION_ASSIGNMENT_MESSAGE = 0x20;
    
    private static final boolean DEBUG_SP = false;

    static public byte[] getPosMessage(Human agent) {
        int pos = agent.getPosition().getValue();
        byte[] posB = intToByteArray(pos);
        byte[] message = new byte[1 + 1 * 4];
        message[0] = POS_MESSAGE;
        System.arraycopy(posB, 0, message, 1, posB.length);

        if (Logger.isDebugEnabled()) {
            Logger.debug("Sending POSITION: Agent " + agent.getID().getValue() + " located at " + pos);
        }
        return message;
    }

    static public int getPosIdFromMessage(byte[] message)
    {
        return byteArrayToInt(removeHeader(message));
    }

    static public byte[] removeHeader(byte[] message)
    {
        byte[] content = new byte[message.length - 1];
        for (int i = 0; i < content.length; i++)
            content[i] = message[i + 1];
        return content;
    }

    static public String toString(byte[] message)
    {
        int[] array = byteArrayToIntArray(message, false);
        return array.toString();
    }

    /*
     * Convert INT value to BYTE array
     */
    public static byte[] intToByteArray(int value)
    {
        return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
    }

    /*
     * Convert BYTE array to INT value
     */
    public static int byteArrayToInt(byte[] b)
    {
        return (b[0] << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    /*
     * Convert BYTE array to INT array - Here we use 4 byte for each int
     */
    public static int[] byteArrayToIntArray(byte[] b, boolean ignoreFirstByte)
    {
        int length = b.length / 4;
        int startOffs = 0;
        if (ignoreFirstByte)
        {
            startOffs = 1;
            length = (b.length - 1) / 4;
        }
        int[] intarray = new int[length];
        for (int i = 0; i < length; i++)
        {
            int offs = startOffs + i * 4;
            intarray[i] = (b[offs + 0] << 24) | ((b[offs + 1] & 0xFF) << 16) | ((b[offs + 2] & 0xFF) << 8) | (b[offs + 3] & 0xFF);
        }
        return intarray;
    }

    /*
     * Convert BYTE array to INT array - Here we use 4 byte for each int
     */
    public static byte[] intArrayToByteArray(int[] iarray, int startIndexOffset)
    {
        int length = iarray.length;
        byte[] b = new byte[length * 4 + startIndexOffset];
        for (int i = 0; i < length; i++)
        {
            int value = iarray[i];
            int offs = startIndexOffset + i * 4;
            b[offs + 0] = (byte) (value >> 24);
            b[offs + 1] = (byte) (value >> 16);
            b[offs + 2] = (byte) (value >> 8);
            b[offs + 3] = (byte) value;
        }
        return b;
    }

    public static byte[] buildAssignmentMessage(Assignment A, boolean addHeader)
    {
        // Build the allocation message
    	ArrayList<EntityID> agents = A.getAgents();
        int[] intArray = new int[agents.size() * 2];
        for (int i = 0; i < agents.size(); i++)
        {
        	EntityID agent = agents.get(i);
            EntityID target = A.getAssignment(agents.get(i));
            if (target == Assignment.UNKNOWN_TARGET_ID)
                continue;
            if (Logger.isDebugEnabled()) {
            	Logger.debug("A " + agent.getValue() + " ---> T " + target.getValue());
            }
            intArray[i * 2] = agent.getValue();
            intArray[i * 2 + 1] = target.getValue();
        }
        if (addHeader)
        {
            byte[] bArray = SimpleProtocolToServer.intArrayToByteArray(intArray, 1);
            bArray[0] = SimpleProtocolToServer.STATION_ASSIGNMENT_MESSAGE;
            return bArray;
        }
        else
        {
            return SimpleProtocolToServer.intArrayToByteArray(intArray, 0);
        }
    }    
}
