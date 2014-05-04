package org.jenkinsci.plugins.koji;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.MyTypeFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

public class XMLRPCTest {
    private final String kojiInstanceURL;
    private XmlRpcClient koji;

    public static void main(String[] args) {
        XMLRPCTest koji = new XMLRPCTest();

        koji.sayHello();



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

    public XMLRPCTest() {
        this.kojiInstanceURL = "http://koji.fedoraproject.org/kojihub";

        XmlRpcClient koji = connect();
        this.koji = koji;
    }

    public Object[] getLatestBuilds(String tag, String pkg) {

        List<Object> params = new ArrayList<Object>();
        params.add(tag);
        params.add(null);
        params.add(pkg);
        try {
            Object[] latestBuilds = (Object[]) koji.execute("getLatestBuilds", params);

            return latestBuilds;
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Map<String, String> getBuildInfo(String buildNVR) {
        List<Object> params = new ArrayList<Object>();
        params.add(buildNVR);
        try {
            Map<String, String> buildInfo = (Map<String, String>) koji.execute("getBuild", params);

            return buildInfo;
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void sayHello() {
        try {
            Object result = koji.execute("hello", new Object[]{"Hello"});
            System.out.println("Jenkins-Koji Plugin: Hello Koji server ");
            System.out.println("Koji: " + result);
        } catch (XmlRpcException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public XmlRpcClient connect() {
        XmlRpcClient koji = new XmlRpcClient();
        koji.setTransportFactory(new XmlRpcCommonsTransportFactory(koji));
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        koji.setTransportFactory(new XmlRpcCommonsTransportFactory(koji));
        koji.setTypeFactory(new MyTypeFactory(koji));
        config.setEnabledForExtensions(true);
        config.setEnabledForExceptions(true);

        try {
            config.setServerURL(new URL(kojiInstanceURL));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        koji.setConfig(config);

        return koji;
    }
}
