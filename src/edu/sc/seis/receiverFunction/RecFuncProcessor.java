/**
 * RecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.xml.*;
import java.io.*;

import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.SodUtil;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.process.waveformArm.ChannelGroupLocalSeismogramProcess;
import edu.sc.seis.sod.process.waveformArm.LocalSeismogramProcess;
import edu.sc.seis.sod.process.waveformArm.LocalSeismogramTemplateGenerator;
import edu.sc.seis.sod.process.waveformArm.SaveSeismogramToFile;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.w3c.dom.Element;

public class RecFuncProcessor extends SaveSeismogramToFile implements ChannelGroupLocalSeismogramProcess {

    public RecFuncProcessor(Element config)  throws ConfigurationException {
        super(config);
        Element gElement = SodUtil.getElement(config, "gaussianWidth");
        if (gElement != null) {
            String gwidthStr = SodUtil.getNestedText(gElement);
            gwidth = Float.parseFloat(gwidthStr);
        }
        logger.info("Init RecFuncProcessor");
    }

    /**
     * Processes localSeismograms to calculate receiver functions.
     *
     * @param event an <code>EventAccessOperations</code> value
     * @param network a <code>NetworkAccess</code> value
     * @param channel a <code>Channel</code> value
     * @param original a <code>RequestFilter[]</code> value
     * @param available a <code>RequestFilter[]</code> value
     * @param seismograms a <code>LocalSeismogram[]</code> value
     * @param cookies a <code>CookieJar</code> value
     * @exception Exception if an error occurs
     */
    public LocalSeismogramImpl[][] process(EventAccessOperations event,
                                           ChannelGroup channelGroup,
                                           RequestFilter[][] original,
                                           RequestFilter[][] available,
                                           LocalSeismogramImpl[][] seismograms,
                                           CookieJar cookieJar) throws Exception {
        // save original seismograms, return value is ignored
        for (int i = 0; i < seismograms.length; i++) {
            LocalSeismogramImpl[] saveToFileSeis = super.process(event,
                                                                 channelGroup.getChannels()[i],
                                                                 original[i],
                                                                 available[i],
                                                                 seismograms[i],
                                                                 cookieJar);
            saveToFileSeis = null;
        }

        if (recFunc == null) {
            tauPTime = new TauP_Time("iasp91");
            recFunc = new RecFunc(tauPTime,
                                  new IterDecon(100, true, .001f, gwidth));
        }
        for (int i = 0; i < seismograms.length; i++) {
            if (seismograms[i].length == 0) {
                // maybe no data after cut?
                return seismograms;
            }
            if ( ! ChannelIdUtil.areEqual(available[i][0].channel_id, seismograms[i][0].channel_id)) {
                // fix the dumb -farm or -spyder on pond available_data
                available[i][0].channel_id = seismograms[i][0].channel_id;
            }
        }

        DataSet dataset = getDataSet(event);
        DataSetSeismogram[] chGrpSeismograms =
            DisplayUtils.getComponents(dataset, available[0][0]);

        if (chGrpSeismograms.length < 3) {
            logger.debug("chGrpSeismograms.length = "+chGrpSeismograms.length);
            // must not be all here yet
            return seismograms;
        }

        logger.info("RecFunc for "+ChannelIdUtil.toStringNoDates(channelGroup.getChannels()[0].get_id()));
        for (int i=0; i<chGrpSeismograms.length; i++) {
            if (chGrpSeismograms[i] == null) {
                // must not be all here yet
                System.out.println("chGrpSeismograms["+i+"] is null");
                return seismograms;
            }
        }

        processor =
            new DataSetRecFuncProcessor(chGrpSeismograms,
                                        event,
                                        recFunc);
        for (int i=0; i<chGrpSeismograms.length; i++) {
            logger.debug("Retrieving for "+chGrpSeismograms[i].getName());
            chGrpSeismograms[i].retrieveData(processor);
        }
        Channel zeroChannel = channelGroup.getChannels()[0];
        while ( ! processor.isRecFuncFinished()) {
            try {
                System.out.println("Sleeping "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()));

                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        MicroSecondDate before = new MicroSecondDate();
        System.out.println("Before save rec func data");
        if (processor.getError() == null) {
            if (processor.getPredicted() != null) {
                for (int i = 0; i < processor.getPredicted().length; i++) {
                    String ITR_ITT = (i==0?"ITR":"ITT");
                    MemoryDataSetSeismogram predicted = processor.getPredicted()[i];
                    dataset.remove(predicted); // to avoid duplicates
                    Channel recFuncChannel = new ChannelImpl(predicted.getRequestFilter().channel_id,
                                                             "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                             new Orientation(0, 0),
                                                             zeroChannel.sampling_info,
                                                             zeroChannel.effective_time,
                                                             zeroChannel.my_site);

                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, predicted.getCache(), SeismogramFileTypes.SAC);
                    Collection aux = predicted.getAuxillaryDataKeys();
                    Iterator it = aux.iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        saved.addAuxillaryData(key, predicted.getAuxillaryData(key));
                    }
                    DistAz distAz = DisplayUtils.calculateDistAz(saved);
                    tauPTime.calculate(distAz.delta);
                    Arrival arrival = tauPTime.getArrival(0);
                    // convert radian per sec ray param into km per sec
                    float kmRayParam = (float)(arrival.getRayParam()/tauPTime.getTauModel().getRadiusOfEarth());
                    HKStack stack = new HKStack(6.5f,
                                                kmRayParam,
                                                HKStack.getPercentMatch(saved),
                                                5, .25f, 240,
                                                1.6f, .0025f, 200,
                                                saved);

                    String prefix = "HKstack_";
                    String postfix = ".raw";
                    String channelIdString = ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id);
                    File stackOutFile = new File(getEventDirectory(event),prefix+channelIdString+postfix);
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(stackOutFile)));
                    stack.write(dos);
                    dos.close();

                    if (lSeisTemplateGen == null) {
                        LocalSeismogramProcess[] processes = Start.getWaveformArm().getLocalSeismogramArm().getProcesses();
                        for (int j = 0; j < processes.length; j++) {
                            if (processes[j] instanceof LocalSeismogramTemplateGenerator) {
                                lSeisTemplateGen = (LocalSeismogramTemplateGenerator)processes[j];
                                break;
                            }
                        }
                    }

                    File imageDir = lSeisTemplateGen.getOutputFile(event, zeroChannel).getParentFile();
                    imageDir.mkdirs();
                    File outImageFile  = new File(imageDir, prefix+channelIdString+".png");
                    BufferedImage bufImage = stack.createStackImage();
                    javax.imageio.ImageIO.write(bufImage, "png", outImageFile);

                    synchronized(lSeisTemplateGen) {
                        lSeisTemplateGen.getSeismogramImageProcess().process(event, recFuncChannel, original[0], available[0], predicted.getCache(), cookieJar);
                    }

                    // put the answer in the site context as the result is shared
                    // by all channels from that site, helps with velocity template
                    VelocityContext siteContext = (VelocityContext)cookieJar.getSiteContext(event, zeroChannel.my_site);
                    siteContext.put("recFunc_hkstack_image_"+ITR_ITT, outImageFile.getName());
                    siteContext.put("recFunc_pred_auxData"+ITR_ITT, aux);
                    siteContext.put("stack_"+ITR_ITT, stack);

                    // test in out for all channels
                    Collection c = (Collection)cookieJar.get("allChanIds");
                    Iterator iterator = c.iterator();
                    boolean found = false;
                    while(iterator.hasNext()) {
                        VelocityContext cTemp = (VelocityContext)cookieJar.getContext().get(iterator.next().toString());
                        System.out.println("Check for stack"+ cTemp.get("stack_"+ITR_ITT).toString());
                        found = true;
                        break;
                    }
                    if ( ! found) {
                        System.err.println("XXXXXX  DIDN't find stack_"+ITR_ITT);
                    }


                    RecFuncTemplate rfTemplate =new RecFuncTemplate();
                    File velocityOutFile = new File(getEventDirectory(event),"Vel_"+prefix+channelIdString+".html");
                    rfTemplate.process(cookieJar.getContext(), velocityOutFile);

                    File outHtmlFile  = new File(getEventDirectory(event),prefix+channelIdString+".html");
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outHtmlFile));
                    bw.write("<html>");bw.newLine();
                    bw.write("<head>");bw.newLine();
                    bw.write("<title>"+channelIdString+"</title>");bw.newLine();
                    bw.write("</head>");bw.newLine();
                    bw.write("<body>");bw.newLine();
                    bw.write("</br><pre>");bw.newLine();
                    stack.writeReport(bw);bw.newLine();
                    bw.write("</pre></br>");bw.newLine();
                    it = aux.iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        Object val = predicted.getAuxillaryData(key);
                        if (val instanceof Element) {
                            Element e = (Element)val;
                            StringWriter swriter = new StringWriter();
                            edu.sc.seis.fissuresUtil.xml.Writer out = new edu.sc.seis.fissuresUtil.xml.Writer();
                            out.setOutput(swriter);
                            out.write(e);
                            swriter.close();
                            bw.write("<h3>"+key.toString()+"</h3>");bw.newLine();
                            bw.write("<pre>");bw.newLine();
                            bw.write(swriter.toString());bw.newLine();
                            bw.write("</pre></br>");bw.newLine();
                        } else {
                            // oh well, use toString and hope for the best
                            bw.write(key.toString()+" = "+val.toString()+"</br>");bw.newLine();
                        }
                    }
                    bw.write("<img src="+quote+outImageFile.getName()+quote+"/>");
                    bw.write("</body>");bw.newLine();
                    bw.write("</html>");bw.newLine();
                    bw.close();

                    int[] xyMax = stack.getMaxValueIndices();
                    float max = stack.stack[xyMax[0]][xyMax[1]];
                    appendToSummaryPage("<tr><td>"+getEventDirectory(event).getName()+"</td><td>"+channelIdString+"</td><td>"+stack.getPercentMatch()+"</td><td>"+(stack.getMinH()+xyMax[0]*stack.getStepH())+"</td><td>"+(stack.getMinK()+xyMax[1]*stack.getStepK())+"</td></tr>");

                    // now update global per channel stack
                    SumHKStack sum = SumHKStack.load(getParentDirectory(),
                                                     predicted.getRequestFilter().channel_id,
                                                     prefix,
                                                     postfix,
                                                     90);
                    if (sum != null) {
                        // at least on event meet the min percent match
                        File sumStackOutFile = new File(getParentDirectory(),"SumHKStack_"+ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id)+postfix);
                        if (sumStackOutFile.exists()) {sumStackOutFile.delete();}
                        DataOutputStream sumdos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sumStackOutFile)));
                        sum.write(sumdos);
                        sumdos.close();

                        File outSumImageFile  = new File(getParentDirectory(),"SumHKStack_"+ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id)+".png");
                        if (outSumImageFile.exists()) {outSumImageFile.delete();}
                        BufferedImage bufSumImage = sum.createStackImage();
                        javax.imageio.ImageIO.write(bufSumImage, "png", outSumImageFile);
                    }

                }
            } else {
                logger.error("problem with recfunc: predicted is null");
            }
            String[] names = dataset.getDataSetSeismogramNames();
            for (int i = 0; i < names.length; i++) {
                DataSetSeismogram dss = dataset.getDataSetSeismogram(names[i]);
                if (dss instanceof MemoryDataSetSeismogram) {
                    dataset.remove(dss); // avoid duplicates
                    Channel recFuncChannel = new ChannelImpl(dss.getRequestFilter().channel_id,
                                                             "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(dss.getRequestFilter().channel_id),
                                                             new Orientation(0, 0),
                                                             zeroChannel.sampling_info,
                                                             zeroChannel.effective_time,
                                                             zeroChannel.my_site);
                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, ((MemoryDataSetSeismogram)dss).getCache(), SeismogramFileTypes.SAC);
                }
            }
        } else {
            // problem occurred
            SeisDataErrorEvent error = processor.getError();
            logger.error("problem with recfunc:", error.getCausalException());
        }
        MicroSecondDate after = new MicroSecondDate();
        System.out.println("Save took "+after.subtract(before).convertTo(UnitImpl.SECOND));
        System.out.println("Done with "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()));
        return seismograms;
    }

    public synchronized void appendToSummaryPage(String val) throws IOException {
        if (summaryPage == null) {
            File summaryFile = new File(getParentDirectory(), "summary.html");
            summaryPage = new BufferedWriter(new FileWriter(summaryFile));
            summaryPage.write("<html>");summaryPage.newLine();
            summaryPage.write("<head>");summaryPage.newLine();
            summaryPage.write("<title>Receiver Function Summary</title>");summaryPage.newLine();
            summaryPage.write("</head>");summaryPage.newLine();
            summaryPage.write("<body>");summaryPage.newLine();
            summaryPage.write("<table>");summaryPage.newLine();
            summaryPage.write("<tr id="+quote+"title"+quote+">");summaryPage.newLine();
            summaryPage.write("<td>Event</td><td>Channel</td><td>Match</td><td>best H</td><td>best K</td>");summaryPage.newLine();
            summaryPage.write("</tr>");summaryPage.newLine();

            // we don't close the tags so that we can append to the file easily
        }
        summaryPage.write(val);summaryPage.newLine();
        summaryPage.flush();
    }

    boolean isDataComplete(LocalSeismogram seis) {
        return processor.isRecFuncFinished();
    }

    RecFunc recFunc;
    DataSetRecFuncProcessor processor;
    TauP_Time tauPTime;
    LocalSeismogramTemplateGenerator lSeisTemplateGen = null;

    float gwidth = 3.0f;

    static BufferedWriter summaryPage = null;

    public static final char quote = '"';

    private static Logger logger = Logger.getLogger(RecFuncProcessor.class);

}



