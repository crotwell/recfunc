package edu.sc.seis.receiverFunction.web;

import java.util.HashSet;
import java.util.Set;
import org.mortbay.jetty.servlet.ServletHandler;
import edu.sc.seis.rev.ServletFromSet;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class Start {

    /**
     *
     */
    public Start() {
        super();
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) throws Exception {
        String netHTML = "/networks.html";
        String staForNet = "/stations.html";
        Set servletStrings = new HashSet();
        servletStrings.add(netHTML);
        servletStrings.add(staForNet);
        ServletHandler sh = new ServletFromSet(servletStrings);
        sh.addServlet("Networks",
                      netHTML,
                      "edu.sc.seis.viewResult.NetworkList");
        sh.addServlet("StationEqViewer",
                      staForNet,
                      "edu.sc.seis.viewResult.StationList");
        edu.sc.seis.rev.Start.runREV(args, sh);}
}