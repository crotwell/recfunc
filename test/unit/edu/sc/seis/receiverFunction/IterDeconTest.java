package edu.sc.seis.receiverFunction;

import junit.framework.TestCase;
// JUnitDoclet begin import
import edu.sc.seis.receiverFunction.IterDecon;
// JUnitDoclet end import

/**
 * Generated by JUnitDoclet, a tool provided by
 * ObjectFab GmbH under LGPL.
 * Please see www.junitdoclet.org, www.gnu.org
 * and www.objectfab.de for informations about
 * the tool, the licence and the authors.
 */


public class IterDeconTest
    // JUnitDoclet begin extends_implements
    extends TestCase
    // JUnitDoclet end extends_implements
{
    // JUnitDoclet begin class
    edu.sc.seis.receiverFunction.IterDecon iterdecon = null;
    // JUnitDoclet end class

    public IterDeconTest(String name) {
        // JUnitDoclet begin method IterDeconTest
        super(name);
        // JUnitDoclet end method IterDeconTest
    }

    public edu.sc.seis.receiverFunction.IterDecon createInstance() throws Exception {
        // JUnitDoclet begin method testcase.createInstance
        return new edu.sc.seis.receiverFunction.IterDecon(100, false, .0001f, 3);
        // JUnitDoclet end method testcase.createInstance
    }

    protected void setUp() throws Exception {
        // JUnitDoclet begin method testcase.setUp
        super.setUp();
        iterdecon = createInstance();
        // JUnitDoclet end method testcase.setUp
    }

    protected void tearDown() throws Exception {
        // JUnitDoclet begin method testcase.tearDown
        iterdecon = null;
        super.tearDown();
        // JUnitDoclet end method testcase.tearDown
    }

    public void testProcess() throws Exception {
        // JUnitDoclet begin method process
        // JUnitDoclet end method process
    }

    public void testPhaseShift() throws Exception {
        // JUnitDoclet begin method phaseShift
        float[] data = new float[1024];
        data[10] = 1;
        float[] out = iterdecon.phaseShift(data, 0.05f, 0.05f);
        //float[] oldout = iterdecon.oldphaseShift(data, 1.0f, 1.0f);
        //for ( int i=0; i<data.length; i++) {
        //    System.out.println("data="+data[i]+"  out="+out[i]);
        //} // end of for ()

        //                              expected  actual
        assertEquals("9 shifts to 10",   data[9], out[10], .001);
        assertEquals("10 shifts to 11", data[10], out[11], .001);
        assertEquals("11 shifts to 12", data[11], out[12], .001);
        // JUnitDoclet end method phaseShift
    }

    public void testNextPowerTwo() throws Exception {
        // JUnitDoclet begin method phaseShift
        assertEquals(iterdecon.nextPowerTwo(3), 4);
        assertEquals(iterdecon.nextPowerTwo(4), 4);
        assertEquals(iterdecon.nextPowerTwo(1025), 2048);
        // JUnitDoclet end method phaseShift
    }

    /** Gaussian filter of constant should do nothing.
     */
    public void testGaussianFilter() throws Exception {
        // JUnitDoclet begin method phaseShift
        float[] data = new float[128];
        for ( int i=0; i<data.length; i++) {
            data[i] = 1;
        }

        float[] out = iterdecon.gaussianFilter(data, 3.0f, 0.05f);
        for ( int i=0; i<out.length; i++) {
            assertEquals("gfilter "+i+"  data="+data[i]+"  out="+out[i],
                         data[i], out[i], 0.001);
        } // end of for ()

        // JUnitDoclet end method phaseShift
    }

    public void testGetMinIndex() {
        float[] data = { 3, 4, -5, 0, 4, 4, 0, -5, 4, 3};
        int index = IterDecon.getMinIndex(data);
        assertEquals("min index", 2, index);
        index = IterDecon.getMaxIndex(data);
        assertEquals("max index", 1, index);
        index = IterDecon.getAbsMaxIndex(data);
        assertEquals("abs max index", 2, index);
    }

    public void testIterDeconIdentity() throws Exception {
        // JUnitDoclet begin method phaseShift
        float[] data = new float[128];

        data[49] = .5f;

        IterDeconResult out = iterdecon.process(data, data, 1.0f);
        iterdecon.gaussianFilter(out.predicted, 3.0f, 1.0f);
        //for ( int i=0; i<out.predicted.length; i++) {
        //  System.out.println("predicted "+i+"  data="+data[i]+"  out="+out.predicted[i]);
        //            assertEquals("predicted "+i+"  data="+data[i]+"  out="+out.predicted[i],
        //                       data[i], out.predicted[i], 0.001);
        //} // end of for ()

        /* these values come from running New_Decon_Process on a impulse
         generated with sac's fg impulse command (100 datapoints, 1 at 49)
         The receiver function of data from itself should be unity at lag 0
         and zero elsewhere, of course the gaussian tends to smear it out.

         piglet 51>../New_Decon_Process/iterdecon_tjo

         Program iterdeconfd - Version 1.0X, 1997-98
         Chuck Ammon, Saint Louis University

         impulse100.sac
         impulse100.sac
         output
         100
         10
         .001
         3.0
         1
         0
         output

         The maximum spike delay is   64.00000

         File         Spike amplitude   Spike delay   Misfit   Improvement
         r001         0.100000012E+01       0.000      0.00%    100.0000%
         r002        -0.126299312E-06       0.000      0.00%      0.0000%

         Last Error Change =    0.0000%

         Hit the min improvement tolerance - halting.
         Number of bumps in final result:   1
         The final deconvolution reproduces  100.0% of the signal.


         */
        assertEquals( 0.9156569,   out.predicted[0], .000001);
        assertEquals( 0.04999885,  out.predicted[1], .000001);
        assertEquals(-0.01094833,  out.predicted[2], .000001);
        assertEquals( 0.004774094, out.predicted[3], .000001);
        assertEquals(-0.002670953, out.predicted[4], .000001);
        // JUnitDoclet end method phaseShift
    }

    /**
     * JUnitDoclet moves marker to this method, if there is not match
     * for them in the regenerated code and if the marker is not empty.
     * This way, no test gets lost when regenerating after renaming.
     * Method testVault is supposed to be empty.
     */
    public void testVault() throws Exception {
        // JUnitDoclet begin method testcase.testVault
        // JUnitDoclet end method testcase.testVault
    }

    public static void main(String[] args) {
        // JUnitDoclet begin method testcase.main
        junit.textui.TestRunner.run(IterDeconTest.class);
        // JUnitDoclet end method testcase.main
    }
}
