/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.maxSumAdapters;
import function.TabularFunction;
import factorgraph.NodeArgument;
import factorgraph.NodeVariable;
import messages.MessageQ;
import java.util.HashMap;
import misc.NodeArgumentArray;
/**
 *
 * @author fabio
 */
public class RMASTabularFunction extends TabularFunction {
    
    private int _ccc = 0;
    
    public RMASTabularFunction() {
        super();
    }
    
    public double evaluate(NodeArgument[] params) {
        NodeArgument[] copyOfParams = new NodeArgument[params.length];
        System.arraycopy(params, 0, copyOfParams, 0, params.length);
        
        for (int i = 0; i < params.length; i++) {
            if (!copyOfParams[i].getValue().equals(new Integer(this.getFunction().getId()))) {
                copyOfParams[i] = NodeArgument.getNodeArgument(0);
                }
            }
            _ccc++;
            return this.costTable.get(NodeArgumentArray.getNodeArgumentArray(copyOfParams));
    }
    
        public void removeNeighbourBeforeTuples(NodeVariable x) {
            this.parameters.remove(x);
            this.parametersset.remove(x);
    }
        public int getNCCC() {
            return this._ccc;
        }
        
}
