/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.maxSumAdapters;


import factorgraph.NodeFunction;
import factorgraph.NodeVariable;
/**
 *
 * @author fabio
 */
public class RMASNodeFunctionUtility { 
   
   public static void removeNeighbourBeforeTuples(NodeFunction nodefunction, NodeVariable x) {
       ((RMASTabularFunction)nodefunction.getFunction()).removeNeighbourBeforeTuples(x);
   }
    
}
