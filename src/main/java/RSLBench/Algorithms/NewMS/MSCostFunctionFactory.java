/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Algorithms.NewMS;

import es.csic.iiia.ms.functions.CostFunctionFactory;
import es.csic.iiia.ms.op.Combine;
import es.csic.iiia.ms.op.Normalize;
import es.csic.iiia.ms.op.Summarize;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class MSCostFunctionFactory extends CostFunctionFactory {

    public MSCostFunctionFactory() {
        this.setMode(Summarize.MAX, Combine.SUM, Normalize.SUM0);
    }

}
