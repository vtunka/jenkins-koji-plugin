package org.jenkinsci.plugins.koji;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.MyTypeFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used for XML-RPC communication with Koji.
 */
public class KojiClient {

    private XmlRpcClient koji;
    private static KojiClient instance;
    private String kojiInstanceURL;

    public String getKojiInstanceURL() {
        return kojiInstanceURL;
    }

    private KojiClient(String kojiInstanceURL) {
        this.kojiInstanceURL = kojiInstanceURL;
        this.koji = connect(kojiInstanceURL);
    }

    public static KojiClient getKojiClient(String kojiInstanceURL) {
        if (instance == null)
            instance = new KojiClient(kojiInstanceURL);

        return instance;
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

    public String sayHello() {
        StringBuilder sb = new StringBuilder();
        try {
            Object result = koji.execute("hello", new Object[] {"Hello"});
            sb.append("Jenkins-Koji Plugin: Hello Koji server running at " + kojiInstanceURL);
            sb.append("\nKoji: " + result);

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private XmlRpcClient connect(String kojiInstanceURL) {
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
