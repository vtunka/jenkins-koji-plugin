package org.jenkinsci.plugins.koji;

import java.util.Map;

public class XMLRPCTest {

    private KojiClient koji;

    public static void main(String[] args) {
        String kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";

        XMLRPCTest kojiTest = new XMLRPCTest(kojiInstanceURL);
        kojiTest.test();
    }

    public void test() {
        String hello = koji.sayHello();
        System.out.println(hello);

        Object[] result;
        result = koji.getLatestBuilds("f21", "kernel");
        for (Object object : result) {
            System.out.println(object);
        }

        String build = "kernel-3.15.0-0.rc3.git5.3.fc21";
        Map<String, String> buildInfo;
        buildInfo = koji.getBuildInfo(build);
        for (Map.Entry<String, String> entry : buildInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            System.out.println(key + ": " + value);
        }
    }

    public XMLRPCTest(String kojiInstanceURL) {
        this.koji = KojiClient.getKojiClient(kojiInstanceURL);
    }

}
