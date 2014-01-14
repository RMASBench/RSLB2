package RSLBench.Algorithms.MS;

import RSLBench.Comm.Message;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import messages.MessageR;

public class MS_MessageR extends MS_Message {

    private MessageR jMSMessage;

    public MS_MessageR(NodeFunction f, NodeVariable v, MessageR m){
        super(f,v,"R");
        jMSMessage = m;
    }

    public MessageR getMessage(){
        return jMSMessage;
    }

    @Override
    public int getBytes() {
        return getMessage().size() * Message.BYTES_UTILITY_VALUE
                + super.getBytes();
    }

}