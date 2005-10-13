package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class Overview extends StationList {

    public Overview() throws SQLException, ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public ArrayList getStations(HttpServletRequest req, RevletContext context) throws SQLException, NotFound {
        checkDataLoaded(req);
        return new ArrayList(data.keySet());
    }

    public HashMap getSummaries(HttpServletRequest req, ArrayList stationList, RevletContext context)
            throws SQLException, IOException {
        checkDataLoaded(req);
        return data;
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        if(req.getServletPath().endsWith(".txt")) {
            return "overview_txt.vm";
        } else if(req.getServletPath().endsWith(".html")) {
            return "overview_html.vm";
        } else {
            throw new RuntimeException("Unknown URL: " + req.getRequestURI());
        }
    }

    void checkDataLoaded(HttpServletRequest req) throws SQLException {
        if(data == null) {
            data = new HashMap();
            try {
                ArrayList summaryList = jdbcSumHKStack.getAllWithoutData();
                int minEQ = RevUtil.getInt("minEQ", req, 2);
                for(Iterator iter = summaryList.iterator(); iter.hasNext();) {
                    SumHKStack stack = (SumHKStack)iter.next();
                    if(stack.getNumEQ() >= minEQ) {
                        data.put(new VelocityStation(stack.getChannel().my_site.my_station), stack);
                    }
                }
            } catch(NotFound e) {
                // I don't think this should ever happen
                throw new RuntimeException(e);
            } catch(IOException e) {
                // bad database problem if this happens
                throw new RuntimeException(e);
            }
        }
    }

    HashMap data = null;
}
