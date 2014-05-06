package org.jenkinsci.plugins.koji.xmlrpc;

import java.util.Map;

public class XMLRPCTest {

    private KojiClient koji;

    public static void main(String[] args) {
        String kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";

        XMLRPCTest kojiTest = new XMLRPCTest(kojiInstanceURL);
        kojiTest.executeTests();
    }

    public void executeTests() {
        testKojiHello();

        KojiClient.BuildParams buildParams = new KojiClient.BuildParamsBuilder().setTag("f21").setPackage("kernel").setLatest(true).build();
        koji.listTaggedBuilds(buildParams);

//        koji.getSession();
//
//        testGetLatestBuilds();
//
//        testGeBuildInfo();
    }

    private void testKojiHello() {
        String hello = koji.sayHello();
        System.out.println(hello);
    }

    private void testGeBuildInfo() {
        String build = "kernel-3.15.0-0.rc3.git5.3.fc21";
        Map<String, String> buildInfo;
        buildInfo = koji.getBuildInfo(build);
        for (Map.Entry<String, String> entry : buildInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            System.out.println(key + ": " + value);
        }
    }

    private void testGetLatestBuilds() {
        Object[] result;
        result = koji.getLatestBuilds("f21", "kernel");
        for (Object object : result) {
            System.out.println(object);
        }
    }

    public XMLRPCTest(String kojiInstanceURL) {
        this.koji = KojiClient.getKojiClient(kojiInstanceURL);
    }

}
