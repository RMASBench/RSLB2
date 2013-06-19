package RSLBench.maxSumAdapters;

import RSLBench.Comm.AbstractMessage;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import messages.MessageQ;
import messages.MessageR;



public class MS_Message extends AbstractMessage{
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
    
    
}

