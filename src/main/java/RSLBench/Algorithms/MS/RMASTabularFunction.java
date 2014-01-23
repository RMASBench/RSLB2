/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.MS;

import function.TabularFunction;
import factorgraph.NodeArgument;
import misc.NodeArgumentArray;

/**
 * Implements a special table that evaluates on binary arguments.
 *
 * @author fabio
 * @author Marc Pujol-Gonzalez
 */
public class RMASTabularFunction extends TabularFunction {

    private int _ccc = 0;

    @Override
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

    public int getNCCC() {
        return this._ccc;
    }
}
