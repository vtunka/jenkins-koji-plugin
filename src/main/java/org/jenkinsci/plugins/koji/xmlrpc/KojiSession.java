package org.jenkinsci.plugins.koji.xmlrpc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class KojiSession {
    private String sessionId;
    private String sessionKey;

    private String encodedSessionId;
    private String encodedSessionKey;

    private String kojiInstanceURL;

    public KojiSession(String kojiInstanceURL, Map<String, ?> xmlRpcResult) {
        this(kojiInstanceURL, parseSessionId(xmlRpcResult), (String) xmlRpcResult.get("session-key"));
    }

    public KojiSession(String kojiInstanceURL, String sessionId, String sessionKey) {
        this.kojiInstanceURL = kojiInstanceURL;
        this.sessionId = sessionId;
        this.sessionKey = sessionKey;

        encodedSessionId = encodeUTF(sessionId);
        encodedSessionKey = encodeUTF(sessionKey);
    }

    private String encodeUTF(String string) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return encoded;
    }

    private static String parseSessionId(Map<String, ?> xmlRpcResult) {
        Integer sessionIdInt = (Integer) xmlRpcResult.get("session-id");
        return sessionIdInt.toString();
    }

    public String getAuthenticatedHubURL() {
        StringBuilder sb = new StringBuilder();

        sb.append(kojiInstanceURL);
        if (! kojiInstanceURL.endsWith("/")) {
            sb.append("/");
        }
        sb.append("?session-id=");
        sb.append(encodedSessionId);
        sb.append("&session-key=");
        sb.append(encodedSessionKey);

        return sb.toString();
    }
}
