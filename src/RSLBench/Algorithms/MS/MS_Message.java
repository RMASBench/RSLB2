package RSLBench.Algorithms.MS;

import RSLBench.Comm.Message;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;



public abstract class MS_Message implements Message{
    
    private static final int BYTES_TYPE = 1;
    
    private NodeFunction _function;
    private NodeVariable _varaible;
    private String _typeMessage;
    
    public MS_Message(NodeFunction f, NodeVariable v, String type){
        _function = f;
        _varaible = v;
        _typeMessage = type;
    }
    
    public NodeFunction getFunction(){
        return _function;
    }
    
    public NodeVariable getVariable(){
        return _varaible;
    }
       
    public String getMessageType(){
        return _typeMessage;
    }

    @Override
    public int getBytes() {
        return Message.BYTES_ENTITY_ID*2 + BYTES_TYPE;
    }
    
    
}

