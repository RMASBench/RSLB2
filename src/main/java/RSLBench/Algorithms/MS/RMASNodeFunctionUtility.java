/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;


import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
/**
 *
 * @author fabio
 */
public class RMASNodeFunctionUtility {

   public static void removeNeighbourBeforeTuples(NodeFunction nodefunction, NodeVariable x) {
       //System.err.println("Removing " + x.getId() + " from " + nodefunction.getId());
       ((RMASTabularFunction)nodefunction.getFunction()).removeNeighbourBeforeTuples(x);
   }

}
