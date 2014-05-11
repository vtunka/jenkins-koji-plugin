package org.jenkinsci.plugins.koji.xmlrpc;

import org.apache.log4j.*;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class XMLRPCTest {

    private KojiClient koji;

    private final String build = "classworlds-classworlds-1.1_alpha_2-1";
    private final String pkg = "classworlds-classworlds";
    private final String tag = "mead-import-maven-all";

    public static void main(String[] args) {
//        String kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";
        String kojiInstanceURL = "http://koji.localdomain/kojihub";

        XMLRPCTest kojiTest = new XMLRPCTest(kojiInstanceURL);
        kojiTest.executeTests();
    }

    public void executeTests() {
        testKojiHello();

        testLogin();
        KojiClient.BuildParams buildParams = new KojiClient.BuildParamsBuilder().setTag(tag).setPackage(pkg).setLatest(true).build();
        koji.listTaggedBuilds(buildParams);

//        System.out.println(koji.getSession());
//
//        testGetLatestBuilds();
//
//        testGeBuildInfo();

    private void testLogin() {
        Map<String, ?> session = null;
        try {
            session = koji.login();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

//        printMap(session);
//        Integer sessionId = (Integer) session.get("session-id");

        System.out.println(koji.getSession());

    }

    public static void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> m : map.entrySet()) {
            String key = m.getKey();
            Object value = m.getValue();
            System.out.println(key + ": " + value);
        }
    }

    private void testKojiHello() {
        String hello = koji.sayHello();
        System.out.println(hello);
    }

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

    public XMLRPCTest(String kojiInstanceURL) {

        this.koji = KojiClient.getKojiClient(kojiInstanceURL);
        koji.setDebug(true);

        initLogger();
    }

    private void initLogger() {
        Logger logger = LogManager.getLogger(getClass());
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

}
