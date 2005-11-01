package edu.sc.seis.receiverFunction.synth;

import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.ReflTransCoefficient;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.compare.StationResult;

public class SimpleSynthReceiverFunction {

    public SimpleSynthReceiverFunction(StationResult model, Sampling samp, int num_points) {
        this(model, samp, num_points, 2.7, 8.0, 4.5, 3.2);
    }

    public SimpleSynthReceiverFunction(StationResult model,
                                       Sampling samp,
                                       int num_points,
                                       double crustRho,
                                       double mantleVp,
                                       double mantleVs,
                                       double mantleRho) {
        this.model = model;
        this.samp = samp;
        this.num_points = num_points;
        logger.info("calc for model: " + model);
        downgoingRFCoeff = new ReflTransCoefficient(model.getVp().getValue(kmps),
                                                    model.getVs().getValue(kmps),
                                                    crustRho,
                                                    mantleVp,
                                                    mantleVs,
                                                    mantleRho);
        upgoingRFCoeff = downgoingRFCoeff.flip();
    }

    public LocalSeismogramImpl calculate(float flatRP,
                                         Time begin_time,
                                         TimeInterval lagZeroOffset,
                                         ChannelId chan,
                                         float gaussianWidth) {
        float[] data = new float[num_points];
        LocalSeismogramImpl seis = new LocalSeismogramImpl("SimpleSynthReceiverFunction",
                                                           begin_time,
                                                           num_points,
                                                           samp,
                                                           UnitImpl.COUNT,
                                                           chan,
                                                           data);
        // P
        TimeInterval timeP = new TimeInterval(0, UnitImpl.SECOND);
        double refTransP = getAmpP(flatRP);
        float index = HKStack.getDataIndex(seis, timeP.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransP;
        // Ps
        TimeInterval timePs = HKStack.getTimePs(flatRP, model.getVp(), model.getVpVs(), model.getH());
        double refTransPs = getAmpPs(flatRP);
        index = HKStack.getDataIndex(seis, timePs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPs;
        System.out.println("Ps: " + timePs + "  " + index + "  " + refTransPs);
        // PpPs
        TimeInterval timePpPs = HKStack.getTimePpPs(flatRP, model.getVp(), model.getVpVs(), model.getH());
        double refTransPpPs = getAmpPpPs(flatRP);
        index = HKStack.getDataIndex(seis, timePpPs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPpPs;
        System.out.println("PpPs: " + timePpPs + "  " + index + " " + refTransPpPs);
        // PsPs+PpSs
        TimeInterval timePsPs = HKStack.getTimePsPs(flatRP, model.getVp(), model.getVpVs(), model.getH());
        double refTransPsPs = getAmpPsPs(flatRP);
        index = HKStack.getDataIndex(seis, timePsPs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPsPs;
        System.out.println("PsPs: " + timePsPs + "  " + index + "  " + refTransPsPs);
        System.out.println((float)((SamplingImpl)seis.sampling_info).getPeriod().getValue(UnitImpl.SECOND));
        float[] tmp = IterDecon.gaussianFilter(data, 2.5f, (float)((SamplingImpl)seis.sampling_info).getPeriod()
                .getValue(UnitImpl.SECOND));
        System.arraycopy(tmp, 0, data, 0, data.length);
        return seis;
    }

    public double getAmpP(double flatRP) {
        calcBasicTerms(flatRP);
        return rpz0;
    }

    public double getRawAmpPs(double flatRP) {
        calcBasicTerms(flatRP);
        return upgoingRFCoeff.getPtoSVTrans(flatRP) / upgoingRFCoeff.getPtoPTrans(flatRP);
    }

    public double getAmpPs(double flatRP) {
        calcBasicTerms(flatRP);
        return getRawAmpPs(flatRP) * rpz0 * (rsr0 - zsz0);
    }

    public double getAmpPpPs(double flatRP) {
        calcBasicTerms(flatRP);
        double refTransPpPs = upgoingRFCoeff.getFreePtoPRefl(flatRP)
                * (downgoingRFCoeff.getPtoSVRefl(flatRP) - 
                        getRawAmpPs(flatRP) * downgoingRFCoeff.getPtoPRefl(flatRP));
        refTransPpPs *= rpz0 * (rsr0 - zsz0);
        return refTransPpPs;
    }

    public double getAmpPsPs(double flatRP) {
        calcBasicTerms(flatRP);
        double refTransPsPs = upgoingRFCoeff.getFreePtoSVRefl(flatRP)
                * downgoingRFCoeff.getSVtoSVRefl(flatRP)
                - getRawAmpPs(flatRP)
                * (upgoingRFCoeff.getFreePtoSVRefl(flatRP) * downgoingRFCoeff.getPtoSVRefl(flatRP)
                        + upgoingRFCoeff.getFreePtoSVRefl(flatRP) * downgoingRFCoeff.getSVtoPRefl(flatRP) + upgoingRFCoeff.getPtoSVTrans(flatRP)
                        * upgoingRFCoeff.getFreeSVtoPRefl(flatRP)
                        * downgoingRFCoeff.getPtoPRefl(flatRP)
                        / upgoingRFCoeff.getPtoPTrans(flatRP)) + upgoingRFCoeff.getFreePtoPRefl(flatRP)
                * downgoingRFCoeff.getPtoSVRefl(flatRP) * upgoingRFCoeff.getPtoSVTrans(flatRP)
                / upgoingRFCoeff.getPtoPTrans(flatRP);
        refTransPsPs *= rpz0 * (rsr0 - zsz0);
        return refTransPsPs;
    }

    private void calcBasicTerms(double flatRP) {
        double Vs = model.getVs().getValue(UnitImpl.KILOMETER_PER_SECOND);
        double Vp = model.getVp().getValue(UnitImpl.KILOMETER_PER_SECOND);
        double etas0 = Math.sqrt(1 / (Vs * Vs) - flatRP * flatRP);
        double etap0 = Math.sqrt(1 / (Vp * Vp) - flatRP * flatRP);
        double c1 = (1 / (Vs * Vs) - 2 * flatRP * flatRP);
        double c2 = 4 * flatRP * flatRP * etap0 * etas0;
        rsr0 = Vs * c1 / 2. / Vp / flatRP / etap0;
        zsz0 = -2. * flatRP * etas0 * Vs / c1 / Vp;
        rpz0 = 2. * flatRP * etas0 / (1. / Vs / Vs - 2. * flatRP * flatRP);
    }

    double rsr0;

    double zsz0;

    double rpz0;

    ReflTransCoefficient downgoingRFCoeff, upgoingRFCoeff;

    StationResult model;

    Sampling samp;

    int num_points;

    static UnitImpl kmps = UnitImpl.KILOMETER_PER_SECOND;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SimpleSynthReceiverFunction.class);
}