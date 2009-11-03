package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.EventDB;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.hibernate.SodDB;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class StationList extends Revlet {

    public StationList() throws SQLException, ConfigurationException, Exception {
        DATA_LOC = Start.getDataLoc();
    }

    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        RevletContext context = new RevletContext(getVelocityTemplate(req),
                                                  Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        ArrayList<VelocityStation> stationList = getStations(req, context);
        cleanStations(stationList);
        Collections.sort(stationList, new Comparator<VelocityStation>() {
            public int compare(VelocityStation n1, VelocityStation n2) {
                if (n1.get_code().equals(n2.get_code())) {
                    try {
                    return n1.getId().begin_time.date_time.compareTo(n2.getId().begin_time.date_time);
                    } catch(NullPointerException e) {
                        String bad = "";
                        if (n1 == null) {bad+= "n1 null";}
                        if (n2 == null) {bad+= "n2 null";}
                        if (n1 != null && n1.getId() == null) {bad+= "n1.getId() null";}
                        if (n2 != null && n2.getId() == null) {bad+= "n2.getId() null";}
                        GlobalExceptionHandler.handle(bad+" "+n1.get_code()+" "+n2.get_code(), e);
                        throw e;
                    }
                }
                return n1.getCode().compareTo(n2.getCode());
            }});
        logger.debug("getStations done: " + stationList.size());
        HashMap<VelocityStation, SumHKStack> summary = getSummaries(stationList, context, req);
        logger.debug("getSummaries done: " + summary.keySet().size());
        logger.debug("count successful events done");
        context.put("stationList", stationList);
        context.put("summary", summary);
        postProcess(req, context, stationList, summary);
        return context;
    }
    
    public void postProcess(HttpServletRequest req, RevletContext context, ArrayList<VelocityStation> stationList, HashMap summary) {
    	    return;
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if(fileType.equals(RevUtil.MIME_CSV)
                || fileType.equals(RevUtil.MIME_TEXT)) {
            return "stationListTxt.vm";
        } else {
            return "stationList.vm";
        }
    }

    protected void setContentType(HttpServletRequest req,
                                  HttpServletResponse response) {
        String path = req.getServletPath();
        if(path.endsWith(".txt")) {
            response.setContentType("text/plain");
        } else if(path.endsWith(".xml")) {
            response.setContentType("text/xml");
        } else if(path.endsWith(".html")) {
            response.setContentType("text/html");
        } else {
            throw new RuntimeException("Unknown URL: " + req.getRequestURI());
        }
    }

    public ArrayList<VelocityStation> getStations(HttpServletRequest req, RevletContext context)
            throws SQLException, NotFound {
        VelocityNetwork net = Start.getNetwork(req);
        context.put("net", net);
        ArrayList<VelocityStation> stationList = new ArrayList<VelocityStation>();
        List<StationImpl> stations = NetworkDB.getSingleton().getStationForNet(net.getWrapped());
        for(StationImpl stationImpl : stations) {
            stationList.add(new VelocityStation(stationImpl));
        }
        return stationList;
    }

    /**
     * Populates a hashmap with keys (objects of type Station) from the list and
     * values of SumHKStack. Also populates the dbid for the stations and
     * network.
     * 
     * @throws SQLException
     * @throws IOException
     */
    public HashMap<VelocityStation, SumHKStack> getSummaries(ArrayList stationList,
                                RevletContext context,
                                HttpServletRequest req) throws SQLException,
            IOException, NotFound {
        float gaussianWidth = getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian(), context);
        float minPercentMatch = getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch(), context);
        float maxComplexity = getFloat("maxComplexity",
                                               req,
                                               1.0f, context);
        float minVpvs = getFloat("minVpvs",
                                               req,
                                               0.0f, context);
        float maxVpvs = getFloat("maxVpvs",
                                               req,
                                               3.0f, context);
        float minH = getFloat("minH",
                                         req,
                                         -90.0f, context);
        float maxH = getFloat("maxH",
                                         req,
                                         99999.0f, context);
        int minEQ = getInt("minEQ",
                                      req,
                                      0, context);
        int maxEQ = getInt("maxEQ",
                                      req,
                                      99999, context);
        Iterator it = stationList.iterator();
        HashMap<VelocityStation, SumHKStack> summary = new HashMap<VelocityStation, SumHKStack>();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            SumHKStack sumStack = RecFuncDB.getSingleton().getSumStack((NetworkAttrImpl)sta.getWrapped().getNetworkAttr(),
                                                                       sta.get_code(), gaussianWidth);
            if (sumStack == null) {continue;}
            float bestVpvs = sumStack.getComplexityResult().getBestK();
            float bestH = (float)sumStack.getComplexityResult().getBestH();
            if (sumStack.getComplexityResidual() <= maxComplexity &&
                    bestVpvs >= minVpvs && bestVpvs <= maxVpvs &&
                    bestH >= minH && bestH <= maxH &&
                    sumStack.getNumEQ() >= minEQ && sumStack.getNumEQ() <= maxEQ) {
                summary.put(sta, sumStack);
            }
        }
        logger.debug("found " + summary.size() + " summaries");
        return summary;
    }
    
    public float getFloat(String name, 
                          HttpServletRequest req, 
                          float defaultValue, 
                          RevletContext context) {
        float val = RevUtil.getFloat(name,
                                     req,
                                     defaultValue);
        context.put(name, ""+val);
        return val;
    }
    
    public int getInt(String name, HttpServletRequest req, int defaultValue, RevletContext context) {
        int val = RevUtil.getInt(name, req, defaultValue);
        context.put(name, "" + val);
        return val;
    }

    /** weed out stations with same net and station code to avoid duplicates
     in list. */
    public static void cleanStations(ArrayList stationList) {
        HashMap codeMap = new HashMap();
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            Station sta = (Station)it.next();
            String key = StationIdUtil.toStringNoDates(sta.get_id());
            if(codeMap.containsKey(key)) {
                Station previousSta = (Station)codeMap.get(key);
                MicroSecondDate staBegin = new MicroSecondDate(sta.getEffectiveTime().start_time);
                MicroSecondDate previousStaBegin = new MicroSecondDate(previousSta.getEffectiveTime().start_time);
                if(staBegin.after(previousStaBegin)) {
                    codeMap.put(key, sta);
                }
            } else {
                codeMap.put(key, sta);
            }
        }
        stationList.clear();
        stationList.addAll(codeMap.values());
    }

    public HashMap cleanSummaries(ArrayList stationList, HashMap summary) {
        logger.debug("before cleanSummaries stationList.size()="
                + stationList.size() + "  summary.size()=" + summary.size());
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            Object next = it.next();
            if(summary.get(next) == null) {
                summary.remove(next);
                it.remove();
            }
        }
        logger.debug("after cleanSummaries stationList.size()="
                + stationList.size() + "  summary.size()=" + summary.size());
        return summary;
    }

    String DATA_LOC;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationList.class);
}