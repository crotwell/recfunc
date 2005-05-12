/**
 * HKStack.java
 * 
 * @author Created by Omnicore CodeGuide
 */
package edu.sc.seis.receiverFunction;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.LinkedList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.w3c.dom.Element;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.model.UnitRangeImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.display.BorderedDisplay;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.display.borders.Border;
import edu.sc.seis.fissuresUtil.display.borders.UnitRangeBorder;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.WilsonRistra;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.receiverFunction.web.GMTColorPalette;
import edu.sc.seis.sod.SodUtil;
import edu.sc.seis.sod.status.FissuresFormatter;

public class HKStack implements Serializable {

    protected HKStack(QuantityImpl alpha,
                      float p,
                      float percentMatch,
                      QuantityImpl minH,
                      QuantityImpl stepH,
                      int numH,
                      float minK,
                      float stepK,
                      int numK,
                      float weightPs,
                      float weightPpPs,
                      float weightPsPs) {
        this.alpha = alpha;
        this.p = p;
        this.percentMatch = percentMatch;
        this.minH = minH.convertTo(UnitImpl.KILOMETER);
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
        this.weightPs = weightPs;
        this.weightPpPs = weightPpPs;
        this.weightPsPs = weightPsPs;
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   DataSetSeismogram recFunc) throws FissuresException {
        this(alpha,
             p,
             percentMatch,
             minH,
             stepH,
             numH,
             minK,
             stepK,
             numK,
             weightPs,
             weightPpPs,
             weightPsPs);
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet()
                .getChannel(recFunc.getRequestFilter().channel_id);
        calculate();
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   LocalSeismogramImpl recFuncSeis,
                   Channel chan,
                   TimeInterval shift) throws FissuresException {
        this(alpha,
             p,
             percentMatch,
             minH,
             stepH,
             numH,
             minK,
             stepK,
             numK,
             weightPs,
             weightPpPs,
             weightPsPs);
        this.recFunc = new MemoryDataSetSeismogram(recFuncSeis);
        this.chan = chan;
        calculate(recFuncSeis, shift);
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   float[][] stack) {
        this(alpha,
             p,
             percentMatch,
             minH,
             stepH,
             numH,
             minK,
             stepK,
             numK,
             weightPs,
             weightPpPs,
             weightPsPs);
        this.recFunc = null;
        this.stack = stack;
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   float[][] stack,
                   DataSetSeismogram recFunc) {
        this(alpha,
             p,
             percentMatch,
             minH,
             stepH,
             numH,
             minK,
             stepK,
             numK,
             weightPs,
             weightPpPs,
             weightPsPs,
             stack);
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet()
                .getChannel(recFunc.getRequestFilter().channel_id);
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   float[][] stack,
                   Channel chan) {
        this(alpha,
             p,
             percentMatch,
             minH,
             stepH,
             numH,
             minK,
             stepK,
             numK,
             weightPs,
             weightPpPs,
             weightPsPs,
             stack);
        this.chan = chan;
    }

    /**
     * returns the x and y indices for the max value in the stack. The min x
     * value is in index 0 and the y in index 1. The max x value is in 2 and the
     * y in 3.
     */
    public int[] getMinValueIndices() {
        float[][] stackOut = getStack();
        float min = stackOut[0][0];
        int minIndexX = 0;
        int minIndexY = 0;
        for(int j = 0; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(stackOut[j][k] < min) {
                    min = stackOut[j][k];
                    minIndexX = j;
                    minIndexY = k;
                }
            }
        }
        int[] xy = new int[2];
        xy[0] = minIndexX;
        xy[1] = minIndexY;
        return xy;
    }

    /**
     * returns the x and y indices for the max value in the stack. The min x
     * value is in index 0 and the y in index 1. The max x value is in 2 and the
     * y in 3.
     */
    public int[] getMaxValueIndices() {
        return getMaxValueIndices(0);
    }

    /**
     * returns the x and y indices for the max value in the stack for a depth
     * grater than minH.
     */
    public int[] getMaxValueIndices(int startHIndex) {
        return getLocalMaxima(startHIndex, 1)[0];
    }

    public int[][] getLocalMaxima(int startHIndex, int num) {
        return getLocalMaxima(startHIndex, num, 2, .04f);
    }

    /**
     * Finds the top num local maxuma that are not within minDeltaH and
     * minDeltaK of another local maxima.
     */
    public int[][] getLocalMaxima(int startHIndex,
                                  int num,
                                  float minDeltaH,
                                  float minDeltaK) {
        float[][] stackOut = getStack();
        int[][] out = new int[num][2];
        float[] maxima = new float[num];
        maxima[0] = Math.min(0, stackOut[0][0]);
        for(int i = 1; i < maxima.length; i++) {
            maxima[i] = maxima[0];
        }
        float max = stackOut[startHIndex][0];
        int maxIndexX = 0;
        int maxIndexY = 0;
        for(int j = startHIndex; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(isLocalMaxima(j, k, stackOut)) {
                    // check list and see if it is in top num
                    for(int i = 0; i < maxima.length; i++) {
                        if(Math.abs(j - out[i][0])
                                * getStepH().getValue(UnitImpl.KILOMETER) < minDeltaH
                                && Math.abs(k - out[i][1]) * getStepK() < minDeltaK) {
                            // too close to an existing maxima
                            if(maxima[i] < stackOut[j][k]) {
                                // replace previous max with this as it is
                                // larger
                                maxima[i] = stackOut[j][k];
                                out[i][0] = j;
                                out[i][1] = k;
                            }
                            break;
                        } else {
                            if(maxima[i] < stackOut[j][k]) {
                                // in the list,
                                // shift others down
                                if(i != maxima.length - 1) {
                                    System.arraycopy(maxima,
                                                     i,
                                                     maxima,
                                                     i + 1,
                                                     maxima.length - i - 1);
                                    System.arraycopy(out,
                                                     i,
                                                     out,
                                                     i + 1,
                                                     out.length - i - 1);
                                }
                                maxima[i] = stackOut[j][k];
                                // must create a new int[] as out[i] and
                                // out[i+1] now point to the same int[] object
                                out[i] = new int[2];
                                out[i][0] = j;
                                out[i][1] = k;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private boolean isLocalMaxima(int j, int k, float[][] stackOut) {
        if(j != 0 && stackOut[j - 1][k] > stackOut[j][k]) {
            return false;
        }
        if(j != stackOut.length - 1 && stackOut[j + 1][k] > stackOut[j][k]) {
            return false;
        }
        if(k != 0 && stackOut[j][k - 1] > stackOut[j][k]) {
            return false;
        }
        if(k != stackOut[0].length - 1 && stackOut[j][k + 1] > stackOut[j][k]) {
            return false;
        }
        // check corners
        if(j != 0) {
            if(k != 0 && stackOut[j - 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j - 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        if(j != stackOut.length - 1) {
            if(k != 0 && stackOut[j + 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j + 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        // must be a maxima
        return true;
    }

    public int getHIndex(QuantityImpl h) {
        return Math.round(getHIndexFloat(h));
    }

    public int getKIndex(float k) {
        return Math.round(getKIndexFloat(k));
    }

    public float getHIndexFloat(QuantityImpl h) {
        return (float)(h.subtract(getMinH()).divideBy(getStepH())).getValue();
    }

    public float getKIndexFloat(double k) {
        return (float)((k - getMinK()) / getStepK());
    }

    public QuantityImpl getMaxValueH() {
        try {
            int[] indicies = getMaxValueIndices();
            QuantityImpl peakH = getMinH().add(getStepH().multiplyBy(indicies[0]));
            return peakH;
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            return new QuantityImpl(0, UnitImpl.METER);
        }
    }

    public QuantityImpl getHFromIndex(int index) {
        return getMinH().add(getStepH().multiplyBy(index));
    }

    public String formatMaxValueH() {
        return FissuresFormatter.formatQuantity(getMaxValueH());
    }

    public float getMaxValueK() {
        int[] indicies = getMaxValueIndices();
        float peakK = getMinK() + getStepK() * indicies[1];
        return peakK;
    }

    public float getKFromIndex(int index) {
        return getMinK() + getStepK() * index;
    }

    public String formatMaxValueK() {
        return vpvsFormat.format(getMaxValueK());
    }

    public float getMaxValue() {
        int[] indicies = getMaxValueIndices();
        float peakVal = getStack()[indicies[0]][indicies[1]];
        return peakVal;
    }

    public String formatMaxValue() {
        return maxValueFormat.format(getMaxValue());
    }

    public QuantityImpl getVs() {
        return getAlpha().divideBy(getMaxValueK());
    }

    public String formatVs() {
        return FissuresFormatter.formatQuantity(getVs());
    }

    public float getPoissonsRatio() {
        return (float)PoissonsRatio.calcPoissonsRatio(getMaxValueK());
    }

    public String formatPoissonsRatio() {
        return vpvsFormat.format(getPoissonsRatio());
    }

    public BorderedDisplay getStackComponent() {
        return getStackComponent(minH);
    }

    public BorderedDisplay getStackComponent(QuantityImpl smallestH) {
        int startHIndex = getHIndex(smallestH);
        HKStackImage stackImage = new HKStackImage(this, startHIndex);
        if(crust2 != null) {
            StationResult result = crust2.getStationResult(chan.my_site.my_station);
            stackImage.addMarker(result, Color.blue);
        }
        if(wilson != null) {
            StationResult result = wilson.getResult(chan.my_site.my_station.get_id());
            if(result != null) {
                stackImage.addMarker(result, Color.GREEN);
            }
        }
        BorderedDisplay bd = new BorderedDisplay(stackImage);
        UnitRangeImpl depthRange = new UnitRangeImpl(getMinH().getValue()
                + startHIndex * getStepH().getValue(), getMinH().getValue()
                + getNumH() * getStepH().getValue(), UnitImpl.KILOMETER);
        UnitRangeBorder depthLeftBorder = new UnitRangeBorder(Border.LEFT,
                                                              Border.DESCENDING,
                                                              "Depth",
                                                              depthRange);
        bd.add(depthLeftBorder, bd.CENTER_LEFT);
        UnitRangeBorder depthRightBorder = new UnitRangeBorder(Border.RIGHT,
                                                               Border.DESCENDING,
                                                               "Depth",
                                                               depthRange);
        bd.add(depthRightBorder, bd.CENTER_RIGHT);
        UnitRangeBorder kTopBorder = new UnitRangeBorder(Border.TOP,
                                                         Border.ASCENDING,
                                                         "Vp/Vs",
                                                         new UnitRangeImpl(getMinK(),
                                                                           getMinK()
                                                                                   + getNumK()
                                                                                   * getStepK(),
                                                                           UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,
                                                                                           UnitImpl.KILOMETER_PER_SECOND,
                                                                                           "km/s/km/s")));
        bd.add(kTopBorder, bd.TOP_CENTER);
        UnitRangeBorder kBottomBorder = new UnitRangeBorder(Border.BOTTOM,
                                                            Border.ASCENDING,
                                                            "Vp/Vs",
                                                            new UnitRangeImpl(getMinK(),
                                                                              getMinK()
                                                                                      + getNumK()
                                                                                      * getStepK(),
                                                                              UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,
                                                                                              UnitImpl.KILOMETER_PER_SECOND,
                                                                                              "km/s/km/s")));
        bd.add(kBottomBorder, bd.BOTTOM_CENTER);
        Dimension dim = stackImage.getPreferredSize();
        dim = new Dimension(dim.width
                + depthLeftBorder.getPreferredSize().width
                + depthRightBorder.getPreferredSize().width, dim.height
                + kTopBorder.getPreferredSize().height
                + kBottomBorder.getPreferredSize().height);
        bd.setPreferredSize(dim);
        bd.setSize(dim);
        logger.info("end getStackComponent");
        return bd;
    }

    public BufferedImage createStackImage() {
        BorderedDisplay comp = getStackComponent();
        JFrame frame = null;
        Graphics2D g = null;
        BufferedImage bufImage = null;
        try {
            if(comp.getRootPane() == null) {
                comp.addNotify();
                comp.validate();
            }
            Dimension size = comp.getPreferredSize();
            int fullWidth = size.width + 40;
            int fullHeight = size.height + 140;
            bufImage = new BufferedImage(fullWidth,
                                         fullHeight,
                                         BufferedImage.TYPE_INT_RGB);
            g = bufImage.createGraphics();
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Color.darkGray);
            g.fillRect(0, 0, bufImage.getWidth(), bufImage.getHeight());
            g.translate(0, 5);
            String title = ChannelIdUtil.toStringNoDates(getChannelId());
            g.setColor(Color.white);
            g.drawString(title,
                         (fullWidth - fm.stringWidth(title)) / 2,
                         fm.getHeight());
            g.translate(5, fm.getHeight() + fm.getDescent());
            comp.print(g);
            g.translate(0, size.height);
            int[] xyMin = getMinValueIndices();
            int[] xyMax = getMaxValueIndices();
            float min = stack[xyMin[0]][xyMin[1]];
            float max = stack[xyMax[0]][xyMax[1]];
            g.setColor(Color.white);
            g.drawString("% match=" + percentMatch, 0, fm.getHeight());
            g.drawString("    ", 0, 2 * fm.getHeight());
            g.translate(0, 2 * fm.getHeight());
            g.drawString("Max H="
                                 + (getMinH().getValue() + xyMax[0]
                                         * getStepH().getValue()),
                         0,
                         fm.getHeight());
            g.drawString("    K=" + (getMinK() + xyMax[1] * getStepK()),
                         0,
                         2 * fm.getHeight());
            g.translate(0, 2 * fm.getHeight());
            GMTColorPalette colorPallete = ((HKStackImage)comp.get(BorderedDisplay.CENTER)).getColorPallete();
            for(int i = 0; i < size.width; i++) {
                g.setColor(colorPallete.getColor(SimplePlotUtil.linearInterp(0,
                                                                             0,
                                                                             size.width,
                                                                             max,
                                                                             i)));
                g.fillRect(i, 0, 1, 15);
            }
            g.setColor(Color.white);
            g.drawString("Min=0", 0, 15 + fm.getHeight());
            g.setColor(Color.white);
            String maxString = "Max=" + max;
            int stringWidth = fm.stringWidth(maxString);
            g.drawString(maxString,
                         size.width - 5 - stringWidth,
                         15 + fm.getHeight());
        } finally {
            if(g != null) {
                g.dispose();
            }
            if(frame != null) {
                frame.dispose();
            }
        }
        return bufImage;
    }

    public static float getPercentMatch(DataSetSeismogram recFunc) {
        String percentMatch = "-9999";
        if(recFunc != null) {
            Element e = (Element)recFunc.getAuxillaryData("recFunc.percentMatch");
            percentMatch = SodUtil.getNestedText(e);
        }
        return Float.parseFloat(percentMatch);
    }

    protected void calculate() throws FissuresException {
        Element shiftElement = (Element)recFunc.getAuxillaryData("recFunc.alignShift");
        QuantityImpl shift = XMLQuantity.getQuantity(shiftElement);
        shift = shift.convertTo(UnitImpl.SECOND);
        DataGetter dataGetter = new DataGetter();
        recFunc.retrieveData(dataGetter);
        LinkedList data = dataGetter.getData();
        LocalSeismogramImpl seis;
        if(data.size() != 1) {
            throw new IllegalArgumentException("Receiver function DSS must have exactly one seismogram");
        } else {
            seis = (LocalSeismogramImpl)data.get(0);
        }
        calculate(seis, shift);
    }

    public static float[][] createArray(int numH, int numK) {
        return new float[numH][numK];
    }

    void calculate(LocalSeismogramImpl seis, QuantityImpl shift)
            throws FissuresException {
        stack = createArray(numH, numK);
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        if(Float.isNaN(etaP)) {
            System.out.println("Warning: Eta P is NaN alpha=" + alpha + "  p="
                    + p);
        }
        for(int kIndex = 0; kIndex < numK; kIndex++) {
            float beta = a / (minK + kIndex * stepK);
            float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
            if(Float.isNaN(etaS)) {
                System.out.println("Warning: Eta S is NaN " + kIndex
                        + "  beta=" + beta + "  p=" + p);
            }
            for(int hIndex = 0; hIndex < numH; hIndex++) {
                float h = (float)(minH.getValue() + hIndex * stepH.getValue());
                double timePs = h * (etaS - etaP) + shift.value;
                double timePpPs = h * (etaS + etaP) + shift.value;
                double timePsPs = h * (2 * etaS) + shift.value;
                stack[hIndex][kIndex] += calcForStack(seis,
                                                      timePs,
                                                      timePpPs,
                                                      timePsPs);
            }
        }
    }

    public float calcForStack(LocalSeismogramImpl seis,
                              double timePs,
                              double timePpPs,
                              double timePsPs) throws FissuresException {
        return weightPs * getAmp(seis, timePs) + weightPpPs
                * getAmp(seis, timePpPs) - weightPsPs * getAmp(seis, timePsPs);
    }

    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs) throws TauModelException,
            FissuresException {
        return create(cachedResult,
                      weightPs,
                      weightPpPs,
                      weightPsPs,
                      crust2.getStationResult(cachedResult.channels[0].my_site.my_station));
    }

    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs,
                                 StationResult staResult)
            throws TauModelException, FissuresException {
        String[] pPhases = {"P"};
        TauPUtil tauPTime = TauPUtil.getTauPUtil(modelName);
        Arrival[] arrivals = tauPTime.calcTravelTimes(cachedResult.channels[0].my_site.my_station,
                                                      cachedResult.prefOrigin,
                                                      pPhases);
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                .getRadiusOfEarth());
        HKStack stack = new HKStack(staResult.getVp(),
                                    kmRayParam,
                                    cachedResult.radialMatch,
                                    getDefaultMinH(),
                                    new QuantityImpl(.25f, UnitImpl.KILOMETER),
                                    240,
                                    1.6f,
                                    .0025f,
                                    200,
                                    weightPs,
                                    weightPpPs,
                                    weightPsPs,
                                    (LocalSeismogramImpl)cachedResult.radial,
                                    cachedResult.channels[0],
                                    RecFunc.getDefaultShift());
        return stack;
    }

    public TimeInterval getTimePs() {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / getMaxValueK();
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        double h = getMaxValueH().getValue();
        return new TimeInterval(h * (etaS - etaP), UnitImpl.SECOND);
    }

    public TimeInterval getTimePpPs() {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / getMaxValueK();
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        double h = getMaxValueH().getValue();
        return new TimeInterval(h * (etaS + etaP), UnitImpl.SECOND);
    }

    public TimeInterval getTimePsPs() {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / getMaxValueK();
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        double h = getMaxValueH().getValue();
        return new TimeInterval(h * (2 * etaS), UnitImpl.SECOND);
    }

    /** gets the amp at the given time offset from the start of the seismogram. */
    float getAmp(LocalSeismogramImpl seis, double time)
            throws FissuresException {
        double sampOffset = time
                / seis.getSampling().getPeriod().convertTo(UnitImpl.SECOND).value;
        if(sampOffset < 0 || sampOffset > seis.getNumPoints() - 2) {
            //throw new IllegalArgumentException("time "+time+" is outside of
            // seismogram: "+seis.getBeginTime()+" - "+seis.getEndTime());
            return 0;
        }
        int offset = (int)Math.floor(sampOffset);
        float valA = seis.get_as_floats()[offset];
        float valB = seis.get_as_floats()[offset + 1];
        // linear interp
        float retVal = (float)SimplePlotUtil.linearInterp(offset,
                                                          valA,
                                                          offset + 1,
                                                          valB,
                                                          sampOffset);
        if(Float.isNaN(retVal)) {
            logger.error("Got a NaN for HKStack.getAmp() at " + time);
        }
        return retVal;
    }

    public ChannelId getChannelId() {
        if(recFunc != null) {
            return getRecFunc().getRequestFilter().channel_id;
        } else {
            return chan.get_id();
        }
    }

    /**
     * Returns the channel, which may be null.
     */
    public Channel getChannel() {
        return chan;
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

    public String formatP() {
        return vpvsFormat.format(getP());
    }

    public QuantityImpl getAlpha() {
        return alpha;
    }

    public String formatAlpha() {
        return FissuresFormatter.formatQuantity(getAlpha());
    }

    public float getPercentMatch() {
        return percentMatch;
    }

    public String formatPercentMatch() {
        return vpvsFormat.format(getPercentMatch());
    }

    public QuantityImpl getMinH() {
        return minH;
    }

    public QuantityImpl getStepH() {
        return stepH;
    }

    public int getNumH() {
        return numH;
    }

    public float getMinK() {
        return minK;
    }

    public float getStepK() {
        return stepK;
    }

    public int getNumK() {
        return numK;
    }

    public float getWeightPpPs() {
        return weightPpPs;
    }

    public String formatWeightPpPs() {
        return vpvsFormat.format(getWeightPpPs());
    }

    public float getWeightPs() {
        return weightPs;
    }

    public String formatWeightPs() {
        return vpvsFormat.format(getWeightPs());
    }

    public float getWeightPsPs() {
        return weightPsPs;
    }

    public String formatWeightPsPs() {
        return vpvsFormat.format(getWeightPsPs());
    }

    /**
     * Writes the HKStack report to a string.
     */
    public void writeReport(BufferedWriter out) throws IOException {
        out.write("p=" + p);
        out.newLine();
        out.write("alpha=" + alpha);
        out.newLine();
        int[] xyMin = getMinValueIndices();
        int[] xyMax = getMaxValueIndices();
        float max = stack[xyMax[0]][xyMax[1]];
        out.write("Max H="
                + (getMinH().getValue() + xyMax[0] * getStepH().getValue()));
        out.write("    K=" + (getMinK() + xyMax[1] * getStepK()));
        out.write("  max=" + max);
        out.write("alpha=" + alpha);
        out.newLine();
        out.write("percentMatch=" + percentMatch);
        out.newLine();
        out.write("minH=" + minH);
        out.newLine();
        out.write("stepH=" + stepH);
        out.newLine();
        out.write("numH=" + numH);
        out.newLine();
        out.write("minK=" + minK);
        out.newLine();
        out.write("stepK=" + stepK);
        out.newLine();
        out.write("numK=" + numK);
        out.newLine();
        out.write("stack.length=" + stack.length);
        out.newLine();
        out.write("stack[0].length=" + stack[0].length);
        out.newLine();
    }

    /**
     * Writes the HKStack to the DataOutputStream. The DataSetSeismogram is NOT
     * written as it is assumed that this will be saved separately.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeFloat(p);
        out.writeFloat((float)alpha.getValue(UnitImpl.KILOMETER_PER_SECOND));
        out.writeFloat(percentMatch);
        out.writeFloat((float)minH.getValue(UnitImpl.KILOMETER));
        out.writeFloat((float)stepH.getValue());
        out.writeInt(numH);
        out.writeFloat(minK);
        out.writeFloat(stepK);
        out.writeInt(numK);
        out.writeInt(stack.length);
        out.writeInt(stack[0].length);
        for(int i = 0; i < stack.length; i++) {
            for(int j = 0; j < stack[0].length; j++) {
                out.writeFloat(stack[i][j]);
            }
        }
    }

    /**
     * Reads the HKStack from the DataInputStream. The DataSetSeismogram is NOT
     * read as it is assumed that this will be saved separatedly.
     */
    public static HKStack read(DataInputStream in, DataSetSeismogram recFunc)
            throws IOException {
        HKStack hks = read(in);
        hks.recFunc = recFunc;
        return hks;
    }

    /**
     * Reads the HKStack from the DataInputStream. The DataSetSeismogram is NOT
     * read as it is assumed that this will be saved separatedly.
     */
    public static HKStack read(DataInputStream in) throws IOException {
        float p = in.readFloat();
        QuantityImpl alpha = new QuantityImpl(in.readFloat(),
                                              UnitImpl.KILOMETER_PER_SECOND);
        float percentMatch = in.readFloat();
        QuantityImpl minH = new QuantityImpl(in.readFloat(), UnitImpl.KILOMETER);
        QuantityImpl stepH = new QuantityImpl(in.readFloat(),
                                              UnitImpl.KILOMETER);
        int numH = in.readInt();
        float minK = in.readFloat();
        float stepK = in.readFloat();
        int numK = in.readInt();
        int iDim = in.readInt();
        int jDim = in.readInt();
        float[][] stack = new float[iDim][jDim];
        System.out.println("WARNING: weights are assumed to be  1");
        HKStack out = new HKStack(alpha,
                                  p,
                                  percentMatch,
                                  minH,
                                  stepH,
                                  numH,
                                  minK,
                                  stepK,
                                  numK,
                                  1,
                                  1,
                                  1,
                                  stack);
        for(int i = 0; i < stack.length; i++) {
            for(int j = 0; j < stack[0].length; j++) {
                stack[i][j] = in.readFloat();
            }
        }
        return out;
    }

    float[][] stack;

    float p;

    QuantityImpl alpha;

    float percentMatch;

    QuantityImpl minH;

    QuantityImpl stepH;

    int numH;

    float minK;

    float stepK;

    int numK;

    float weightPs = 1;

    float weightPpPs = 1;

    float weightPsPs = 1;

    private static final QuantityImpl DEFAULT_MIN_H = new QuantityImpl(10,
                                                                       UnitImpl.KILOMETER);

    static String modelName = "iasp91";

    transient static Crust2 crust2 = null;

    transient static WilsonRistra wilson = null;
    static {
        try {
            crust2 = new Crust2();
        } catch(IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Crust2.0", e);
        }
        try {
            wilson = new WilsonRistra();
        } catch(IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Wilson RISTRA", e);
        }
    }

    public static Crust2 getCrust2() {
        return crust2;
    }

    public static WilsonRistra getWilsonRistra() {
        return wilson;
    }

    public static QuantityImpl getDefaultMinH() {
        return DEFAULT_MIN_H;
    }

    public static QuantityImpl getBestSmallestH(Station station,
                                                QuantityImpl smallestH) {
        Crust2Profile crust2 = HKStack.getCrust2()
                .getClosest(station.my_location.longitude,
                            station.my_location.latitude);
        QuantityImpl crust2H = crust2.getCrustThickness();
        QuantityImpl modSmallestH = smallestH;
        if(crust2H.subtract(smallestH).getValue() < 5) {
            modSmallestH = crust2H.subtract(new QuantityImpl(5,
                                                             UnitImpl.KILOMETER));
            if(modSmallestH.lessThan(HKStack.getDefaultMinH())) {
                modSmallestH = HKStack.getDefaultMinH();
            }
        }
        return modSmallestH;
    }

    // don't serialize the DSS
    transient DataSetSeismogram recFunc;

    Channel chan;

    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat maxValueFormat = new DecimalFormat("0.0000");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");

    class DataGetter implements SeisDataChangeListener {

        LinkedList data = new LinkedList();

        LinkedList errors = new LinkedList();

        boolean finished = false;

        public boolean isFinished() {
            return finished;
        }

        public synchronized LinkedList getData() {
            while(finished == false) {
                try {
                    wait();
                } catch(InterruptedException e) {}
            }
            return data;
        }

        public synchronized void finished(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for(int i = 0; i < seis.length; i++) {
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
            for(int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
        }
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStack.class);
}