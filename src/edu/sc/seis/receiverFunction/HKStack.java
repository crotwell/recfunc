/**
 * HKStack.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import java.util.LinkedList;
import org.w3c.dom.Element;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;



public class HKStack  {
    
    protected HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK) {
        this.alpha = alpha;
        this.p = p;
        this.minH = minH;
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
    }
    
    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   DataSetSeismogram recFunc) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = recFunc;
        calculate();
    }
    
    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = null;
        this.stack = stack;
    }
    
    protected void calculate() {
        stack = new float[numH][numK];
        float etaP = (float) Math.sqrt(1/(alpha*alpha)-p*p);
        if (Float.isNaN(etaP)) {
            System.out.println("Warning: Eta P is NaN alpha="+alpha+"  p="+p);
        }
        Element shiftElement =
            (Element)recFunc.getAuxillaryData("recFunc.alignShift");
        QuantityImpl shift = (QuantityImpl)XMLQuantity.getQuantity(shiftElement);
        shift = shift.convertTo(UnitImpl.SECOND);
        DataGetter dataGetter = new DataGetter();
        recFunc.retrieveData(dataGetter);
        LinkedList data = dataGetter.getData();
        LocalSeismogramImpl seis;
        if (data.size() != 1) {
            throw new IllegalArgumentException("Receiver function DSS must have exactly one seismogram");
        } else {
            seis = (LocalSeismogramImpl)data.get(0);
        }
        for (int i = 0; i < numK; i++) {
            float beta = alpha/(minK + i*stepK);
            float etaS = (float) Math.sqrt(1/(beta*beta)-p*p);
            if (Float.isNaN(etaS)) {
                System.out.println("Warning: Eta S is NaN "+i+"  beta="+beta+"  p="+p);
            }
            for (int j = 0; j < numH; j++) {
                float h = minH + j*stepH;
                double timePs = h * (etaS - etaP) + shift.value;
                double timePpPs = h * (etaS + etaP) + shift.value;
                double timePsPs = h * (2 * etaS) + shift.value;
                stack[j][i] += getAmp(seis, timePs)
                    + getAmp(seis, timePpPs)
                    - getAmp(seis, timePsPs);
            }
        }
    }
    
    
    /** gets the amp at the given time offset from the start of the seismogram. */
    float getAmp(LocalSeismogramImpl seis, double time) {
        double sampOffset = time/seis.getSampling().getPeriod().convertTo(UnitImpl.SECOND).value;
        if (sampOffset < 0 || sampOffset > seis.getNumPoints()-2) {
            throw new IllegalArgumentException("time "+time+" is outside of seismogram");
        }
        int offset = (int)Math.floor(sampOffset);
        
        float valA = seis.get_as_floats()[offset];
        float valB = seis.get_as_floats()[offset+1];
        // linear interp
        float retVal = (float)SimplePlotUtil.linearInterp(offset, valA, offset+1, valB, sampOffset);
        if (Float.isNaN(retVal)) {
            System.out.println("Got a NaN for getAmp at "+time);
        }
        return retVal;
    }
    
    public DataSetSeismogram getRecFunc() {
        return recFunc;
    }
    
    public float[][] getStack() {
        return stack;
    }
    
    public float getP() {
        return p;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public float getMinH() {
        return minH;
    }
    
    public float getStepH() {
        return stepH;
    }
    
    public float getNumH() {
        return numH;
    }
    
    public float getMinK() {
        return minK;
    }
    
    public float getStepK() {
        return stepK;
    }
    
    public float getnumK() {
        return numK;
    }
    
    /** Writes the HKStack to the DataOutputStream. The DataSetSeismogram
     *  is NOT written as it is assumed that this will be saved separatedly.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeFloat(p);
        out.writeFloat(alpha);
        out.writeFloat(minH);
        out.writeFloat(stepH);
        out.writeInt(numH);
        out.writeFloat(minK);
        out.writeFloat(stepK);
        out.writeInt(numK);
        out.writeInt(stack.length);
        out.writeInt(stack[0].length);
        for (int i = 0; i < stack.length; i++) {
            for (int j = 0; j < stack[0].length; j++) {
                out.writeFloat(stack[i][j]);
            }
        }
    }
        
    /** Reades the HKStack from the DataInputStream. The DataSetSeismogram
     *  is NOT read as it is assumed that this will be saved separatedly.
     */
    public static HKStack read(DataInputStream in) throws IOException {
        float p = in.readFloat();
        float alpha = in.readFloat();
        float minH = in.readFloat();
        float stepH = in.readFloat();
        int numH = in.readInt();
        float minK = in.readFloat();
        float stepK = in.readFloat();
        int numK = in.readInt();
        int iDim = in.readInt();
        int jDim = in.readInt();
        float[][] stack = new float[iDim][jDim];
        HKStack out = new HKStack(alpha, p, minH, stepH, numH, minK, stepK, numK, stack);
        for (int i = 0; i < stack.length; i++) {
            for (int j = 0; j < stack[0].length; j++) {
                stack[i][j] = in.readFloat();
            }
        }
        return out;
    }
    
    float[][] stack;
    float p;
    float alpha;
    float minH;
    float stepH;
    int numH;
    float minK;
    float stepK;
    int numK;
    DataSetSeismogram recFunc;
    
    class DataGetter implements SeisDataChangeListener {
        
        LinkedList data = new LinkedList();
        
        LinkedList errors = new LinkedList();
        
        boolean finished = false;
        
        public boolean isFinished() {
            return finished;
        }
        
        public synchronized LinkedList getData() {
            while (finished == false) {
                try {
                    wait();
                } catch (InterruptedException e) { }
            }
            return data;
        }
        
        public synchronized void finished(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for (int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
            finished = true;
            notifyAll();
        }
        
        public synchronized void error(SeisDataErrorEvent sdce) {
            errors.add(sdce);
        }
        
        public synchronized void pushData(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for (int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
        }
    }
}

