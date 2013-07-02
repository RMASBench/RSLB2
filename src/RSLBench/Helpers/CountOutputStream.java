/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Helpers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that just counts the written bytes.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class CountOutputStream extends OutputStream {
    
    private int byteCount;

    public CountOutputStream() {
        this.byteCount = 0;
    }
    
    public void reset() {
        byteCount = 0;
    }

    @Override
    public void write(int b) throws IOException {
        byteCount++;
    }
    
    public int getByteCount() {
        return byteCount;
    }
    
}
