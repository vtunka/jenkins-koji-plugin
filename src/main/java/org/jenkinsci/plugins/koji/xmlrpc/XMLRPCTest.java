package org.jenkinsci.plugins.koji.xmlrpc;

import org.apache.log4j.*;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * Testing class for XMl-RPC Koji client testing outside of Jenkins environment. Please see MyLoggingUtils for additional
 * dependencies required to run this class.
 *
 * This also has advantage, that KojiClient API is used from other consumer point, validating the desing of KojiClient.
 */
public class XMLRPCTest {

    /**
     * Koji XML RPC client instance.
     */
    private KojiClient koji;

    /**
     * Test build
     */
    private final String build = "classworlds-classworlds-1.1_alpha_2-1";
    /**
     * Test package
     */
    private final String pkg = "classworlds-classworlds";
    /**
     * Test tag
     */
    private final String tag = "mead-import-maven-all";

    /**
     * Logger instance
     */
    private Logger logger;

    /**
     * As described above, this class is used for out of container testing only.
     * @param args
     */
    public static void main(String[] args) {
//        String kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";
        String kojiInstanceURL = "http://koji.localdomain/kojihub";

        XMLRPCTest kojiTest = new XMLRPCTest(kojiInstanceURL);
        kojiTest.executeTests();
    }

    /**
     * Initializes the logging infrastructure and sets the desired log level.
     */
    private void initLogger() {

        logger = LogManager.getLogger(getClass());
        BasicConfigurator.configure(); // basic log4j configuration
        Logger.getRootLogger().setLevel(Level.DEBUG);
        FileAppender fileAppender = null;
        try {
            fileAppender =
                    new RollingFileAppender(new PatternLayout("%d{dd-MM-yyyy HH:mm:ss} %C %L %-5p:%m%n"),"file.log");
            logger.addAppender(fileAppender);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("TEST LOG ENTRY");
    }

    /**
     * Sets the Koji Client for debugging.
     * @param kojiInstanceURL
     */
    public XMLRPCTest(String kojiInstanceURL) {
        initLogger();

        try {
            this.koji = KojiClient.getKojiClient(kojiInstanceURL);
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }
        koji.setDebug(true);

    }

    /**
     * Executes the desired tests.
     */
    public void executeTests() {
        testKojiHello();

        testLogin();
        KojiClient.BuildParams buildParams = new KojiClient.BuildParamsBuilder().setTag(tag).setPackage(pkg).setLatest(true).build();
//        koji.listTaggedBuilds(buildParams);

//        System.out.println(koji.getSession());
//
//        testGetLatestBuilds();
//
//        testGeBuildInfo();
    }

    /**
     * Tests login.
     */
    private void testLogin() {
        KojiSession session = null;
        try {
            session = koji.login("kojiadmin", "kojiadmin");
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

//        printMap(session);
//        Integer sessionId = (Integer) session.get("session-id");

        System.out.println(koji.getSession());

    }

    /**
     * Convenience method to print maps containing XML RPC responses.
     * @param map
     */
    public static void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> m : map.entrySet()) {
            String key = m.getKey();
            Object value = m.getValue();
            System.out.println(key + ": " + value);
        }
    }

    /**
     * Test koji connection.
     */
    private void testKojiHello() {
        String hello = koji.sayHello();
        System.out.println(hello);
    }

    /**
     * Test advanced query.
     */
    private void testListTaggedBuilds() {

        KojiClient.BuildParams buildParams = new KojiClient.BuildParamsBuilder().setTag(tag).setPackage(pkg).setLatest(true).build();
        List<Map<String, String>> results = null;
        try {
            results = koji.listTaggedBuilds(buildParams);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                System.out.println("No build with id=" + build + " found in the database.");
                return;
            }
            else
                e.printStackTrace();
        }

        for (Map<String, String> map : results) {
            printMap(map);
        }

    }

    /**
     * Test build info.
     */
    private void testGeBuildInfo() {
        Map<String, String> buildInfo = null;
        try {
            buildInfo = koji.getBuildInfo(build);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                System.out.println("No build with id=" + build + " found in the database.");
                return;
            }
            else
                e.printStackTrace();
        }
        printMap(buildInfo);
    }

    /**
     * Test simpler query.
     */
    private void testGetLatestBuilds() {
        Map<String, String> result = null;

        try {
            result = koji.getLatestBuilds(tag, pkg);
        } catch (XmlRpcException e) {
            if (e.getMessage() == "empty") {
                System.out.println("No package " + pkg + " found for tag " + tag);
                return;
            }
            else {
                System.out.println(e.getMessage());
                return;
            }
        }
        printMap(result);
    }

}
