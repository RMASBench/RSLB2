/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.maxSumAdapters;

import operation.*;
import java.util.Hashtable;
import function.FunctionEvaluator;
import factorgraph.NodeArgument;
import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import messages.MessageContent;
import messages.MessageQ;
import messages.MessageR;
import messages.MessageRArrayDouble;
import messages.PostService;
import messages.RMessageList;
import misc.Utils;


/**
 *
 * @author riccardo
 */
public class MSumOperator_Sync extends MSumOperator {

    final static int debug = 0;
   // Hashtable<NodeVariable, Hashtable<NodeFunction, LinkedList<MessageR>>> _Rmessage = new Hashtable<NodeVariable, Hashtable<NodeFunction, LinkedList<MessageR>>>();
    Hashtable<String, LinkedList<MessageQ>> _Qmessage;
    Hashtable<String, LinkedList<MessageR>> _Rmessage;
   
    // Hashtable<Object, Object> _Qmessage;

    public MSumOperator_Sync(OTimes otimes, OPlus oplus) {
        super(otimes, oplus);
        _Rmessage = new Hashtable<String, LinkedList<MessageR>>();
        _Qmessage = new Hashtable<String, LinkedList<MessageQ>>();
    }

    public boolean readRmessage(NodeVariable x, NodeFunction f, PostService postservice) {
        /*
        if (_Rmessage.get(x) == null) //se non ho mai messo x lo metto
        {
            _Rmessage.put(x, new Hashtable<NodeFunction, LinkedList<MessageR>>());
        }
        if (_Rmessage.get(x).get(f) == null) //se per x non ho mai messo f lo metto
        {
            _Rmessage.get(x).put(f, new LinkedList<MessageR>());
        }*/

        LinkedList<MessageR> rmex = new LinkedList<MessageR>();
        NodeFunction function = null;

        // R from other functions f' to v
        //Iterator<NodeFunction> iterator = this.variableToFunctions.get(v).iterator();
        Iterator<NodeFunction> iterator = x.getNeighbour().iterator();
        // every new iterator value is a new index in M(i)
        while (iterator.hasNext()) {
            function = iterator.next();
            if (!(function.equals(f))) {
                //rmessages.add(postservice.readRMessage(f, x));

                if (postservice.readRMessage(function, x) != null) {
                    // if there's a message in (f,x) add it to the list
                    /*try {
                        fw = new FileWriter("assignment.txt", true);
                        fw.write("Messaggio dalla funzione "+function.getId()+" alla variabile "+x.getId()+":");
                        } catch (IOException i) {}
                    MessageR mex = postservice.readRMessage(function, x);
                    for (int i=0; i < mex.size(); i++) {
                        try {
                        fw.write(" "+mex.getValue(i));
                        } catch (IOException ii) {}
                    }
                    try {
                        fw.close();
                        } catch (IOException iii) {}*/
                   // _Rmessage.get(x).get(f).add(postservice.readRMessage(function, x));
                    rmex.add(postservice.readRMessage(function, x));
                   /*LinkedList<MessageR> prova = _Rmessage.get(x).get(f);
                   System.out.println("Messaggio dalla funzione "+function.getId()+" alla variabile "+x.getId()+":");
                   for (MessageR p: prova) {
                       for (int i = 0; i<p.size(); i++) {
                       System.out.println(" "+p.getValue(i));
                }

            }*/
        }
            }
        }
        _Rmessage.put(x.toString()+f.toString(),rmex);
        return true;
    }
    
    public boolean readRmessage_com(NodeVariable x, NodeFunction f, MessageR m) {
        LinkedList<MessageR> rmex = new LinkedList<MessageR>();
        rmex.add(m);
        _Rmessage.put(x.toString()+f.toString(),rmex);
        return true;
        
    }
    public boolean readQmessages_com(NodeFunction f, NodeVariable x, MessageQ m){
        LinkedList<MessageQ> qmex = new LinkedList<MessageQ>();
        qmex.add(m);
        _Qmessage.put(f.toString()+x.toString(), qmex);
        return true;
    }
    public boolean readQmessages(NodeFunction f, NodeVariable x, PostService postservice) {
        /*
        FileWriter fw = null;
        if (_Qmessage.get(f) == null) //se non ho mai messo x lo metto
        {
            //System.out.println("oooooooooooooooooooooooooooooooooooooooooooooooooooooooo");
            _Qmessage.put(f, new Hashtable<NodeVariable, LinkedList<MessageQ>>());
        }
        if (_Qmessage.get(f).get(x) == null) //se per x non ho mai messo f lo metto
        {
            //System.out.println("oooooooooooooooooooooooooooooooooooooooooooooooooooooooo");            
            _Qmessage.get(f).put(x, new LinkedList<MessageQ>());
        }*/
        LinkedList<MessageQ> qmex = new LinkedList<MessageQ>();
        NodeVariable variable = null;
        //System.out.println("Sono nell'updateR per la variabile "+x.getId());
       


        // R from other functions f' to v
        //Iterator<NodeFunction> iterator = this.variableToFunctions.get(v).iterator();
        Iterator<NodeVariable> iterator = f.getNeighbour().iterator();
        // every new iterator value is a new index in M(i)
        //System.out.print("Sono la funzione "+f.getId()+" e ho come vicini");
        while (iterator.hasNext()) {
            variable = iterator.next();
            //System.out.print(" "+variable.getId());
            //if (variable != x) {
            if (!variable.equals(x)) {
                //System.out.println("Sono "+variable.getId()+" e sono diversa da "+x.getId());
                //qmessages.add(this.mailMan.readQMessage(v, f));
                //modifierTable.put(f.getFunction().getParameterPosition(v), this.mailMan.readQMessage(v, f));
                if (postservice.readQMessage(variable, f) != null) {
                    //System.out.println("Mi è arrivato un messaggio diverso da null");
                                        /*try {
                        fw = new FileWriter("assignment.txt", true);
                        fw.write("Messaggio dalla variabile "+variable.getId()+" alla funzione "+f.getId()+":");
                        } catch (IOException i) {}
                    MessageQ mex = postservice.readQMessage(variable, f);
                    for (int i=0; i < mex.size(); i++) {
                        try {
                        fw.write(" "+mex.getValue(i));
                        } catch (IOException ii) {}
                    }
                    try {
                        fw.close();
                        } catch (IOException iii) {}*/
                    qmex.add(postservice.readQMessage(variable, f));
                   /*LinkedList<MessageQ> prova = _Qmessage.get(f).get(x);
                   System.out.println("Messaggio dalla variabile "+variable.getId()+" alla funzione "+f.getId()+":");
                   for (MessageQ p: prova) {
                       for (int i = 0; i<p.size(); i++) {
                       System.out.println(" "+p.getValue(i));

                   }
                }*/
        }
        }
        }
        _Qmessage.put(f.toString()+x.toString(),qmex);
        return true;
    }
    public MessageR updateR_com(NodeFunction f, NodeVariable x) {
               

        LinkedList<MessageQ> qmessages = _Qmessage.get(f.toString()+x.toString());
           if(qmessages == null)
            qmessages = new  LinkedList<MessageQ>();
       // if(qmessages.isEmpty()){
         //   qmessages.add(this.getOtimes().nullMessage(x, f, x.size()));
       // }

        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "contains the following " + qmessages.size() + " qmessages:");
            Iterator<MessageQ> itq = qmessages.iterator();
            while (itq.hasNext()) {
                MessageQ messageQ = itq.next();
                System.out.println("MessageQ " + messageQ + "  and sender " + messageQ.getSender());
            }
            System.out.println("---------------------------------------");
        }

        
        MessageR messager = this.getOplus().oplus(f, x, f.getFunction(), qmessages);
/*
        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "MessageR created: " + messager);
            System.out.println("---------------------------------------");
        }
        //reset*/
        _Qmessage.put(f.toString()+x.toString(), new LinkedList<MessageQ>());
        return messager;
        
    }
    public boolean updateR(NodeFunction f, NodeVariable x, PostService postservice) {
        /*
        if (_Qmessage.get(f) == null) //se non ho mai messo x lo metto
        {
            _Qmessage.put(f, new Hashtable<NodeVariable, LinkedList<MessageQ>>());
        }
        if (_Qmessage.get(f).get(x) == null) //se per x non ho mai messo f lo metto
        {
            _Qmessage.get(f).put(x, new LinkedList<MessageQ>());
        }*/

        LinkedList<MessageQ> qmessages = _Qmessage.get(f.toString()+x.toString());
           if(qmessages == null)
            qmessages = new  LinkedList<MessageQ>();
       // if(qmessages.isEmpty()){
         //   qmessages.add(this.getOtimes().nullMessage(x, f, x.size()));
       // }

        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[1].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[1].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "contains the following " + qmessages.size() + " qmessages:");
            Iterator<MessageQ> itq = qmessages.iterator();
            while (itq.hasNext()) {
                MessageQ messageQ = itq.next();
                System.out.println("MessageQ " + messageQ + "  and sender " + messageQ.getSender());
            }
            System.out.println("---------------------------------------");
        }

        
        MessageR messager = this.getOplus().oplus(f, x, f.getFunction(), qmessages);

        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "MessageR created: " + messager);
            System.out.println("---------------------------------------");
        }
        //reset
        _Qmessage.put(f.toString()+x.toString(), new LinkedList<MessageQ>());
        return postservice.sendRMessage(f, x, messager);


    }
    /**
     * Versione for the comuncator
     * @return 
     */
    public MessageQ updateQ_com(NodeVariable x, NodeFunction f){
        NodeFunction function = null;

        // R from other functions f' to v
        //Iterator<NodeFunction> iterator = this.variableToFunctions.get(v).iterator();
        Iterator<NodeFunction> iterator = x.getNeighbour().iterator();
        // every new iterator value is a new index in M(i)
        while (iterator.hasNext()) {
            function = iterator.next();
            if (!(function.equals(f))) {
            }
       }

        LinkedList<MessageR> rmessages = _Rmessage.get(x.toString()+f.toString());
        if(rmessages == null)
            rmessages = new  LinkedList<MessageR>();
        /*if (rmessages.isEmpty()) {
            System.out.println("vuotooooooooooooooooooooooooooooooooooooooooooooooooooo");
        }*/
        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + " computeq from " + x + " to " + f);
            System.out.print("rmessages contains: ");
            for (MessageR mr : rmessages) {
                System.out.print(mr + "(from " + mr.getSender() + " to " + mr.getReceiver() + ") ");
            }
            System.out.println("");
            System.out.println("---------------------------------------");
        }

        MessageQ messageq = this.computeQ(x, f, this.computeAlpha(x, f, rmessages), rmessages);
        if (messageq == null) {
            if (debug >= 3) {
                String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                System.out.println("---------------------------------------");
                System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "Building up messageR and messageQ");
                MessageR mr = this.getOplus().nullMessage(f, x, x.size());
                System.out.println("Created messager " + mr + " from " + mr.getSender() + " (it should be " + f + ") to " + mr.getReceiver() + " (it should be " + x + ")");
                messageq = mr.toMessageQ();
                System.out.println("---------------------------------------");
            }
            //System.out.println("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
            //messageq = this.oplus.nullMessage(f, x, x.size()).toMessageQ();
            messageq = this.getOtimes().nullMessage(x, function, x.size());
        }
        //reset
        _Rmessage.put(x.toString()+f.toString(), new LinkedList<MessageR>());
        
        return messageq;  
    }
    
    public boolean updateQ(NodeVariable x, NodeFunction f, PostService postservice) {
        /*
        if (_Rmessage.get(x) == null) //se non ho mai messo x lo metto
        {
            //System.out.println("ssssssssssssssssssssssssssssssssssssssssssss");
            _Rmessage.put(x, new Hashtable<NodeFunction, LinkedList<MessageR>>());
        }
        if (_Rmessage.get(x).get(f) == null) //se per x non ho mai messo f lo metto
        {
            
            //System.out.println("ssssssssssssssssssssssssssssssssssssssssssss");
            _Rmessage.get(x).put(f, new LinkedList<MessageR>());
        }*/
        NodeFunction function = null;

        // R from other functions f' to v
        //Iterator<NodeFunction> iterator = this.variableToFunctions.get(v).iterator();
        Iterator<NodeFunction> iterator = x.getNeighbour().iterator();
        // every new iterator value is a new index in M(i)
        while (iterator.hasNext()) {
            function = iterator.next();
            if (!(function.equals(f))) {
            }
       }

        LinkedList<MessageR> rmessages = _Rmessage.get(x.toString()+f.toString());
        if(rmessages == null)
            rmessages = new  LinkedList<MessageR>();
        /*if (rmessages.isEmpty()) {
            System.out.println("vuotooooooooooooooooooooooooooooooooooooooooooooooooooo");
        }*/
        if (debug >= 3) {
            String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
            String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
            System.out.println("---------------------------------------");
            System.out.println("[class: " + dclass + " method: " + dmethod + "] " + " computeq from " + x + " to " + f);
            System.out.print("rmessages contains: ");
            for (MessageR mr : rmessages) {
                System.out.print(mr + "(from " + mr.getSender() + " to " + mr.getReceiver() + ") ");
            }
            System.out.println("");
            System.out.println("---------------------------------------");
        }

        MessageQ messageq = this.computeQ(x, f, this.computeAlpha(x, f, rmessages), rmessages);
        if (messageq == null) {
            if (debug >= 3) {
                String dmethod = Thread.currentThread().getStackTrace()[2].getMethodName();
                String dclass = Thread.currentThread().getStackTrace()[2].getClassName();
                System.out.println("---------------------------------------");
                System.out.println("[class: " + dclass + " method: " + dmethod + "] " + "Building up messageR and messageQ");
                MessageR mr = this.getOplus().nullMessage(f, x, x.size());
                System.out.println("Created messager " + mr + " from " + mr.getSender() + " (it should be " + f + ") to " + mr.getReceiver() + " (it should be " + x + ")");
                messageq = mr.toMessageQ();
                System.out.println("---------------------------------------");
            }
            //System.out.println("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
            //messageq = this.oplus.nullMessage(f, x, x.size()).toMessageQ();
            messageq = this.getOtimes().nullMessage(x, function, x.size());
        }
        //reset
        _Rmessage.put(x.toString()+f.toString(), new LinkedList<MessageR>());
        return postservice.sendQMessage(x, f, messageq);
    }
     
}

