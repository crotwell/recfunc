package edu.sc.seis.receiverFunction;

//import edu.iris.Fissures.IfSeismogramDC.*;
import edu.sc.seis.fissuresUtil.freq.*;

/**
 * IterDecon.java
 *
 *
 * Created: Sat Mar 23 18:24:29 2002
 *
 * @author <a href="mailto:">Philip Crotwell</a>
 * @version $Id: IterDecon.java 3694 2003-04-16 13:46:02Z crotwell $
 */

public class IterDecon {
    public IterDecon (int maxBumps, 
                      boolean useAbsVal, 
                      float tol, 
                      float gwidthFactor) {
        this.maxBumps = maxBumps;
        this.useAbsVal = useAbsVal;
        this.tol = tol;
        this.gwidthFactor = gwidthFactor;
    }

    public IterDeconResult process(float[] numerator, 
                                   float[] denominator,
                                   float dt) {
        float[] amps = new float[maxBumps];
        int[] shifts = new int[maxBumps];

        /* Now begin the cross-correlation procedure
           Put the filter in the signals
        */
        float[] f  = gaussianFilter(makePowerTwo(numerator), gwidthFactor, dt);
        float[] g  = gaussianFilter(makePowerTwo(denominator), gwidthFactor, dt);

        // compute the power in the "numerator" for error scaling
        float fPower = power(f);

        float[] residual = f;
        float[] predicted = new float[0];

        float[][] corrSave = new float[maxBumps][];
        for (int bump=0; bump < maxBumps; bump++) {
	
            // correlate the signals
            float[] corr = correlate(residual, g);
            corrSave[bump] = corr;

            //  find the peak in the correlation
            float peak;
            if (useAbsVal) {
                shifts[bump] = getAbsMaxIndex(corr);
            } else {
                shifts[bump] = getMaxIndex(corr);
            } // end of else
            amps[bump] = corr[shifts[bump]]; // note don't normalize by dt here
            System.out.println("Corr max is "+amps[bump]+" at index "+shifts[bump]+" for length "+g.length+" with dt="+dt);

            predicted = buildDecon(amps, shifts, g.length, gwidthFactor, dt);
            float[] predConvolve = Cmplx.convolve(predicted, denominator);

            residual = getResidual(f, predConvolve);
        } // end of for (int bump=0; bump < maxBumps; bump++)

        System.out.println("predicted[0]="+predicted[0]+"  amps[0]="+amps[0]);

        return new IterDeconResult(maxBumps,
                                   useAbsVal,
                                   tol,
                                   gwidthFactor,
                                   numerator,
                                   denominator,
                                   dt,
                                   amps,
                                   shifts,
                                   residual,
                                   predicted,
                                   corrSave);
    }

    /** computes the correlation of f and g normalized by the zero-lag
     *  autocorrelation of g. */
    float[] correlate(float[] fdata, float[] gdata) {
        float zeroLag = 0;
        for (int i=0; i<gdata.length; i++) {
            zeroLag += gdata[i]*gdata[i];
        }
        //System.out.println("g autocorrelation at  = "+zeroLag);
        float[] corr = Cmplx.correlate(fdata, gdata);

        float temp = 1 / zeroLag;
        for (int i=0; i<corr.length; i++) {
            corr[i] *= temp;
        }
        // for (int i=0; i<corr.length; i++) {
        //  System.out.println("correlation at "+i+" = "+corr[i]);
        //}
        return corr;	    
    }

    void subtractSpike(float[] data, int shift, float amp) {

    }

    float[] buildSpikes(float[] amps, int[] shifts, int n) {
        float[] p = new float[n];
        for (int i=0; i<amps.length; i++) {
            p[shifts[i]] += amps[i];
        } // end of for (int i=0; i<amps.length; i++)
        return p;
    }

    float[] buildDecon(float[] amps, int[] shifts, int n, float gwidthFactor, float dt) {
        return gaussianFilter(buildSpikes(amps, shifts, n), gwidthFactor, dt);
    }

    public static float[] getResidual(float[] x, float[] y) {
        float[] r = new float[x.length];
        for (int i=0; i<x.length; i++) {
            r[i] = x[i]-y[i];
        } // end of for (int i=0; i<x.length; i++)
        return r;
    }

    public static int getAbsMaxIndex(float[] data) {
        int minIndex = getMinIndex(data);
        int maxIndex = getMaxIndex(data);
        if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex])) {
            return minIndex;
        } // end of if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex]))
        return maxIndex;
    }

    public static int getMinIndex(float[] data) {
        int index = 0;
        for (int i=1; i<data.length/2; i++) {
            if (data[i] < data[index]) {
                index = i;
            } 
        }
        return index;
    }	

    public static int getMaxIndex(float[] data) {
        int index = 0;
        for (int i=1; i<data.length/2; i++) {
            if (data[i] > data[index]) {
                index = i;
            }
        }
        return index;
    }	

    public static void zero(float[] data) {
        for (int i=0; i<data.length; i++) {
            data[i] = 0;
        } // end of for (int i=0; i<data.length; i++)
    }

    public static float power(float[] data) {
        float power=0;
        for (int i=0; i<data.length; i++) {
            power += data[i]*data[i];
        } // end of for (int i=0; i<data.length; i++)
        return power;
    }

    /** convolve a function with a unit-area Gaussian filter.
     *  The 1D gaussian is: f(x) = 1/(2*PI*sigma) e^(-x^2/(q * sigma^2))
     *  and the impluse response is: g(x) = 1/(2*PI)e^(-sigma^2 * u^2 / 2)
     * 
     */
    public static float[] gaussianFilter(float[] x, 
                                         float gwidthFactor, 
                                         float dt) {
        float[] forward = new float[x.length];
        System.arraycopy(x, 0, forward, 0, x.length);
        NativeFFT.forward(forward);

        double df = 1/(forward.length * dt);
        double d_omega = 2*Math.PI*df;
        double gwidth = 4*gwidthFactor*gwidthFactor;
        double gauss;
        double omega;

        // Handle the nyquist frequency
        omega = Math.PI/dt; // eliminate 2 / 2
        gauss = Math.exp(-omega*omega / gwidth);
        forward[1] *= gauss;

        int j;
        for (int i=1; i<forward.length/2; i++) {
            j  = i*2;
            omega = i*d_omega;
            gauss = Math.exp(-omega*omega / gwidth);
            forward[j] *= gauss;
            forward[j+1] *= gauss;
        }
	
        NativeFFT.inverse(forward);
        
        return forward;
    }

    public static float[] phaseShift(float[] x, float inShift, float dt) {
        // native fft has imag part with opposite sign ???
        float shift = -1 * inShift; 

        int n2 = nextPowerTwo(x.length);
        int halfpts = n2 / 2;

        float[] forward = new float[x.length];
        System.arraycopy(x, 0, forward, 0, x.length);
        NativeFFT.forward(forward);

        double df = 1/(forward.length * dt);
        double d_omega = 2*Math.PI*df;

        double omega;
        //Handle the nyquist frequency
        omega = Math.PI/dt;
        forward[1] *= (float)Math.cos(omega*shift);
        //System.out.println("nyquist  omega="+omega);

        double a,b,c,d;
        for (int j=2; j<forward.length-1; j+=2) {
            omega = (j/2)*d_omega;
            //  System.out.print(j/2+"  omega="+omega+" f="+forward[j]+" "+forward[j+1]);
            a = forward[j];
            b = forward[j+1];
            c = Math.cos(omega*shift);
            d = Math.sin(omega*shift);

            forward[j] = (float)(a*c-b*d);
            forward[j+1] = (float)(a*d+b*c);
            //System.out.println(" after f="+forward[j]+" "+forward[j+1]);
        }
	
        NativeFFT.inverse(forward);

        return forward;
    }

    /** convolve a function with a unit-area Gaussian filter.
     *  The 1D gaussian is: f(x) = 1/(2*PI*sigma) e^(-x^2/(q * sigma^2))
     *  and the impluse response is: g(x) = 1/(2*PI)e^(-sigma^2 * u^2 / 2)
     * 
     */
    public static float[] oldgaussianFilter(float[] x, 
                                            float gwidthFactor, 
                                            float dt) {
        Cmplx[] forward = Cmplx.fft(x);

        double df = 1/(forward.length * dt);
        double d_omega = 2*Math.PI*df;
        double gwidth = 4*gwidthFactor*gwidthFactor;
        double gauss;
        double omega;

        //         // Handle the nyquist frequency
        //         omega = Math.PI/dt; // eliminate 2 / 2
        //         gauss = Math.exp(-omega*omega / gwidth);

        //         forward[0].i *= gauss;

        for (int i=1; i<forward.length; i++) {
            omega = i*d_omega;
            gauss = Math.exp(-omega*omega / gwidth);
            forward[i].r *= gauss;
            forward[i].i *= gauss;
        }
	
        float[] ans = Cmplx.fftInverse(forward, x.length);
        
        //         float scaleFactor = (float)(dt * 2 * df);
        //         for (int i=0; i<ans.length; i++) {
        //             ans[i] *= scaleFactor;
        //         }

        return ans;
    }

    public static float[] oldphaseShift(float[] x, float shift, float dt) {
        int n2 = nextPowerTwo(x.length);
        int halfpts = n2 / 2;

        Cmplx[] forward = Cmplx.fft(x);

        double df = 1/(n2 * dt);
        double d_omega = 2*Math.PI*df;

        double omega;
        // Handle the nyquist frequency
        // omega = Math.PI/dt;
        //forward[0].i *= (float)Math.cos(omega*shift);

        double a,b,c,d;

        for (int i=1; i<forward.length; i++) {
            omega = i*d_omega;
            System.out.print(i+"  omega="+omega+" f="+forward[i].r+" "+forward[i].i);
            a = forward[i].r;
            b = forward[i].i;
            c = Math.cos(omega*shift);
            d = Math.sin(omega*shift);

            forward[i].r = a*c-b*d;
            forward[i].i = a*d+b*c;
            System.out.println(" after f="+forward[i].r+" "+forward[i].i);
        }
	
        float[] ans = Cmplx.fftInverse(forward, x.length);

        //        float scaleFactor = (float)(dt * 2 * df);
        //        for (int i=0; i<ans.length; i++) {
        //            ans[i] *= scaleFactor;
        //        }
        return ans;
    }

    public static float[] makePowerTwo(float[] data) {
        float[] out = new float[nextPowerTwo(data.length)];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    public static int nextPowerTwo(int n) {
        int i=1;
        while (i < n) {
            i*=2;
        }
        return i;
    }
    
    int maxBumps;
    boolean useAbsVal; 
    float tol;
    float gwidthFactor;

}// IterDecon
