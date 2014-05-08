package org.jenkinsci.plugins.koji.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;

import java.util.Map;

public class XMLRPCTest {

    private KojiClient koji;

    public static void main(String[] args) {
//        String kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";
        String kojiInstanceURL = "http://koji.localdomain/kojihub";

        XMLRPCTest kojiTest = new XMLRPCTest(kojiInstanceURL);
        kojiTest.executeTests();
    }

    public void executeTests() {
        testKojiHello();

//        KojiClient.BuildParams buildParams = new KojiClient.BuildParamsBuilder().setTag("f21").setPackage("kernel").setLatest(true).build();
//        koji.listTaggedBuilds(buildParams);

        System.out.println(koji.getSession());

        testGetLatestBuilds();

        testGeBuildInfo();
    }

    private void testKojiHello() {
        String hello = koji.sayHello();
        System.out.println(hello);
    }

    private void testGeBuildInfo() {
        String build = "kernel-3.15.0-0.rc3.git5.3.fc21";
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
        for (Map.Entry<String, String> entry : buildInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            System.out.println(key + ": " + value);
        }
    }

    private void testGetLatestBuilds() {
        Object[] result = null;
        String pkg = "kernel";
        String tag = "f21";

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
        for (Object object : result) {
            System.out.println(object);
        }
    }

    public XMLRPCTest(String kojiInstanceURL) {
        this.koji = KojiClient.getKojiClient(kojiInstanceURL);
    }

}
