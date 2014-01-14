package RSLBench.Algorithms.MS;

import RSLBench.Comm.Message;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;

public abstract class MS_Message implements Message {

    private static final int BYTES_TYPE = 1;

    private NodeFunction function;
    private NodeVariable variable;
    private String messageType;

    public MS_Message(NodeFunction f, NodeVariable v, String type){
        function = f;
        variable = v;
        messageType = type;
    }

    public NodeFunction getFunction(){
        return function;
    }

    public NodeVariable getVariable(){
        return variable;
    }

    public String getMessageType(){
        return messageType;
    }

    @Override
    public int getBytes() {
        return Message.BYTES_ENTITY_ID*2 + BYTES_TYPE;
    }

}