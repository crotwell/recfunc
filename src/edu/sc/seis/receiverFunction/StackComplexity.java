package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.synth.SimpleSynthReceiverFunction;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class StackComplexity {

    private float gaussianWidth;

    public StackComplexity(HKStack hkplot, int num_points, float gaussianWidth) {
        this.hkplot = hkplot;
        this.samp = hkplot.getChannel().sampling_info;
        this.num_points = num_points;
        this.gaussianWidth = gaussianWidth;
    }

    public HKStack getSynthetic(StationResult staResult)
            throws FissuresException {
        float flatRP = sphRayParamRad / 6371;
        return getSyntheticForRayParam(staResult, flatRP);
    }

    public HKStack getSyntheticForDist(StationResult staResult, float distDeg)
            throws FissuresException, TauModelException {
        Arrival[] arrivals = TauPUtil.getTauPUtil()
                .calcTravelTimes(distDeg, 0, new String[] {"P"});
        return getSyntheticForRayParam(staResult,
                                       (float)arrivals[0].getRayParam() / 6371);
    }

    public HKStack getSyntheticForRayParam(StationResult staResult, float flatRP)
            throws FissuresException {
        SimpleSynthReceiverFunction synth = new SimpleSynthReceiverFunction(staResult,
                                                                            samp,
                                                                            num_points);
        LocalSeismogramImpl synthRF = synth.calculate(flatRP,
                                                      ClockUtil.now()
                                                              .getFissuresTime(),
                                                      RecFunc.getDefaultShift(),
                                                      hkplot.getChannel()
                                                              .get_id(), gaussianWidth);
        HKStack synthStack = new HKStack(hkplot.getAlpha(),
                                         flatRP,
                                         gaussianWidth,
                                         100,
                                         hkplot.minH,
                                         hkplot.stepH,
                                         hkplot.numH,
                                         hkplot.minK,
                                         hkplot.stepK,
                                         hkplot.numK,
                                         1 / 3f,
                                         1 / 3f,
                                         1 / 3f,
                                         synthRF,
                                         hkplot.getChannel(),
                                         RecFunc.getDefaultShift());
        synthStack.compact();
        return synthStack;
    }

    public HKStack getResidual(StationResult staResult, float distDeg)
            throws FissuresException, TauModelException {
        HKStack synthStack = getSyntheticForDist(staResult, distDeg);
        return getResidual(hkplot, synthStack);
    }
    
    public static HKStack getResidual(HKStack real, HKStack synth) {
        float[][] data = real.getStack();
        float[][] synthData = synth.getStack();
        // scale synth data by max of data so best HK -> 0
        // hopefully this subtracts the bulk of the "mountain" around the max
        int[] maxIndex = synth.getMaxValueIndices();
        System.out.println("data max="+data[maxIndex[0]][maxIndex[1]]+"  synth max="+synthData[maxIndex[0]][maxIndex[1]]);
        float scale = data[maxIndex[0]][maxIndex[1]]
                / synthData[maxIndex[0]][maxIndex[1]];
        float[][] diff = new float[real.numH][real.numK];
        for(int i = 0; i < diff.length; i++) {
            for(int jj = 0; jj < diff[0].length; jj++) {
                if (synthData[i][jj] > 0) {
                    diff[i][jj] = data[i][jj] - synthData[i][jj] * scale;
                } else {
                    diff[i][jj] = data[i][jj];
                }
            }
        }
        return new HKStack(real.getAlpha(),
                           sphRayParamRad,
                           real.gwidth,
                           -1,
                           real.minH,
                           real.stepH,
                           real.numH,
                           real.minK,
                           real.stepK,
                           real.numK,
                           1 / 3f,
                           1 / 3f,
                           1 / 3f,
                           diff,
                           real.getChannel());
    }

    /**
     * s/deg for P for 60 deg distance
     */
    static final float sphRayParamRad = 6.877f;

    SumHKStack stack;
    HKStack hkplot;

    Sampling samp;

    int num_points;
}