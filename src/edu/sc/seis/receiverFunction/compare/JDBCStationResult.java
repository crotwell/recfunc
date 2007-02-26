package edu.sc.seis.receiverFunction.compare;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.WrappedSQLException;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;

/**
 * @author crotwell Created on Mar 25, 2005
 */
public class JDBCStationResult extends JDBCTable {

    public JDBCStationResult(JDBCNetwork jdbcNetwork) throws SQLException {
        this(jdbcNetwork, new JDBCStationResultRef(jdbcNetwork.getConnection()));
    }

    public JDBCStationResult(JDBCNetwork jdbcNetwork,
                             JDBCStationResultRef jdbcStationResultRef)
            throws SQLException {
        super("stationResult", jdbcStationResultRef.getConnection());
        TableSetup.setup(this,
                         "edu/sc/seis/receiverFunction/compare/default.props");
        this.jdbcStationResultRef = jdbcStationResultRef;
        this.jdbcNetwork = jdbcNetwork;
    }

    public void put(StationResult result) throws SQLException {
        int ref_id = jdbcStationResultRef.put(result.getRef());
        int index = 1;
        put.setInt(index++, jdbcNetwork.put(result.getNetworkId()));
        put.setString(index++, result.getStationCode());
        put.setFloat(index++, (float)result.getH()
                .convertTo(UnitImpl.KILOMETER)
                .getValue());
        put.setFloat(index++, result.getVpVs());
        put.setFloat(index++, (float)result.getVp()
                .getValue(UnitImpl.KILOMETER_PER_SECOND));
        put.setInt(index++, ref_id);
        put.executeUpdate();
    }

    public StationResult[] get(NetworkId networkId, String stationCode)
            throws SQLException, NotFound {
        try {
            int networkDbId = jdbcNetwork.getDbId(networkId);
            int index = 1;
            get.setInt(index++, networkDbId);
            get.setString(index++, stationCode);
            ResultSet rs = get.executeQuery();
            ArrayList list = new ArrayList();
            while(rs.next()) {
                StationResultRef ref = jdbcStationResultRef.extract(rs);
                list.add(new StationResult(networkId,
                                           stationCode,
                                           new QuantityImpl(rs.getFloat("h"),
                                                            UnitImpl.KILOMETER),
                                           rs.getFloat("vpvs"),
                                           new QuantityImpl(rs.getFloat("vp"),
                                                            UnitImpl.KILOMETER_PER_SECOND),
                                           ref));
            }
            return (StationResult[])list.toArray(new StationResult[0]);
        } catch(SQLException e) {
            throw new WrappedSQLException(e.getMessage() + "     sql:" + get, e);
        }
    }

    public StationResult[] getAll(String referenceName) throws SQLException,
            NotFound {
        getAllForName.setString(1, referenceName);
        ArrayList list = new ArrayList();
        ResultSet rs = getAllForName.executeQuery();
        while(rs.next()) {
            StationResultRef ref = jdbcStationResultRef.extract(rs);
            list.add(new StationResult(jdbcNetwork.getNetworkId(rs.getInt("net_id")),
                                       rs.getString("sta_code"),
                                       new QuantityImpl(rs.getFloat("h"),
                                                        UnitImpl.KILOMETER),
                                       rs.getFloat("vpvs"),
                                       new QuantityImpl(rs.getFloat("vp"),
                                                        UnitImpl.KILOMETER_PER_SECOND),
                                       ref));
        }
        return (StationResult[])list.toArray(new StationResult[0]);
    }

    public static void main(String[] args) throws FileNotFoundException,
            IOException, SQLException, NotFound {
        if(args.length == 0) {
            System.err.println("Usage: progname -n name -m method -r reference -f filename -u url");
            System.err.println(" file is network station h vpvs vp");
            System.err.println(" passcal networks should use netYear, like XM99");
        }
        String name = "", reference = "", method = "", filename = "", url = null;
        Properties props = System.getProperties();
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-n")) {
                name = args[i + 1];
            } else if(args[i].equals("-m")) {
                method = args[i + 1];
            } else if(args[i].equals("-r")) {
                reference = args[i + 1];
            } else if(args[i].equals("-f")) {
                filename = args[i + 1];
            } else if(args[i].equals("-u")) {
                url = args[i + 1];
            } else if(args[i].equals("-props")) {
                props.load(new BufferedInputStream(new FileInputStream(args[i + 1])));
            }
        }
        ConnMgr.setDB(ConnMgr.POSTGRES);
        String dbURL = props.getProperty("cormorant.servers.ears.databaseURL");
        ConnMgr.setURL(dbURL);
        Connection conn = ConnMgr.createConnection();
        JDBCStationResultRef jdbcRef = new JDBCStationResultRef(conn);
        JDBCNetwork jdbcNetwork = new JDBCNetwork(conn);
        JDBCStationResult jdbc = new JDBCStationResult(jdbcNetwork, jdbcRef);
        StationResultRef ref = new StationResultRef(name,
                                                    reference,
                                                    method,
                                                    url);
        System.out.println("StationResultRef:");
        System.out.println(name);
        System.out.println(reference);
        System.out.println(method);
        System.out.println(url);
        int refDbId = jdbcRef.put(ref);
        File inFile = new File(filename);
        if(!inFile.exists()) {
            System.err.println(inFile.getCanonicalPath());
            throw new FileNotFoundException(filename);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)));
        String line = "";
        while((line = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String net = "", sta = "", netYear = "";
            float h = 0, vpvs = 0, vp = 0;
            for(int i = 0; i < 5; i++) {
                String token = st.nextToken();
                switch(i){
                    case 0:
                        if (token.length() > 2) {
                            net = token.substring(0, 2);
                            netYear = token.substring(2);
                        } else {
                            net = token;
                            netYear = "";
                        }
                        break;
                    case 1:
                        sta = token;
                        break;
                    case 2:
                        h = Float.parseFloat(token);
                        break;
                    case 3:
                        vpvs = Float.parseFloat(token);
                        break;
                    case 4:
                        vp = Float.parseFloat(token);
                        break;
                    default:
                        break;
                }
            }
            NetworkId[] ids = jdbcNetwork.getByCode(net);
            NetworkId networkId = null;
            if(ids.length != 0) {
                if(net.startsWith("X") || net.startsWith("Y")
                        || net.startsWith("Z")) {
                    // maybe several nets with that code
                    for(int j = 0; j < ids.length; j++) {
                        if(ids[j].begin_time.date_time.substring(2, 4)
                                .equals(netYear)) {
                            // found it
                            networkId = ids[j];
                        }
                    }
                } else {
                    networkId = ids[0];
                }
            }
            if(networkId != null) {
                jdbc.put(new StationResult(networkId,
                                           sta,
                                           new QuantityImpl(h,
                                                            UnitImpl.KILOMETER),
                                           vpvs,
                                           new QuantityImpl(vp,
                                                            UnitImpl.KILOMETER_PER_SECOND),
                                           ref));
            } else {
                logger.error(net + "  " + netYear);
            }
        }
        conn.close();
    }
    
    public JDBCStationResultRef getJDBCStationResultRef() {
        return jdbcStationResultRef;
    }

    PreparedStatement put, get, getAllForName;

    JDBCStationResultRef jdbcStationResultRef;

    JDBCNetwork jdbcNetwork;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCStationResult.class);
}