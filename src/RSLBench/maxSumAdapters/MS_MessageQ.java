package RSLBench.maxSumAdapters;

import RSLBench.Comm.AbstractMessage;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import messages.MessageQ;
import messages.MessageR;



public class MS_MessageQ extends MS_Message{

   
    private MessageQ _messageQ;
    
    public MS_MessageQ(NodeVariable v, NodeFunction f, MessageQ m){
        super(f,v,"Q");
       // _function = f;
        //_varaible = v;
        _messageQ = m;
      
    }
    

    
    public MessageQ getMessage(){
        return _messageQ;
    }
    

}

