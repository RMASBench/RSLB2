package RSLBench.Algorithms.MS;

import RSLBench.Comm.Message;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import messages.MessageQ;

public class MS_MessageQ extends MS_Message {

    private MessageQ jMSMessage;

    public MS_MessageQ(NodeVariable v, NodeFunction f, MessageQ m){
        super(f,v,"Q");
        jMSMessage = m;
    }

    public MessageQ getMessage(){
        return jMSMessage;
    }

    @Override
    public int getBytes() {
        return getMessage().size() * Message.BYTES_UTILITY_VALUE
                + super.getBytes();
    }

}