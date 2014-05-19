package org.jenkinsci.plugins.koji.xmlrpc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * This class holds information about active session. Once login operations in KojiClient passes, Koji returns
 * two values session_id and session_key, which are stored in this class and included to every XML-RPC request as HTTP
 * parameters.
 */
public class KojiSession {
    /**
     * Numberic ID unique for every client.
     */
    private String sessionId;
    /**
     * Hash used for authentication.
     */
    private String sessionKey;

    /**
     * URL encoded values.
     */
    private String encodedSessionId;
    private String encodedSessionKey;

    /**
     * Koji XML-RPC Hub instance URL.
     */
    private String kojiInstanceURL;

    /**
     * Create a Koji Session based on de-marshalling of XMl-RPC result and current Koji Hub XML-RPC URL.
     * @param kojiInstanceURL XML-RPC hub URL.
     * @param xmlRpcResult XML RPC result to be de-marshalled.
     */
    public KojiSession(String kojiInstanceURL, Map<String, ?> xmlRpcResult) {
        this(kojiInstanceURL, parseSessionId(xmlRpcResult), (String) xmlRpcResult.get("session-key"));
    }

    /**
     * Users can also use direct constructor, if the data are already de-marshalled.
     * @param kojiInstanceURL XML-RPC hub URL.
     * @param sessionId See field documentation.
     * @param sessionKey See field documentation.
     */
    public KojiSession(String kojiInstanceURL, String sessionId, String sessionKey) {
        this.kojiInstanceURL = kojiInstanceURL;
        this.sessionId = sessionId;
        this.sessionKey = sessionKey;

        encodedSessionId = encodeUTF(sessionId);
        encodedSessionKey = encodeUTF(sessionKey);
    }

    /**
     * Convenience method for encoding UTF-8 URLS.
     * @param string String to be encoded.
     * @return Encoded result.
     */
    private String encodeUTF(String string) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return encoded;
    }

    /**
     * ASF WS XML-RPC library always returns <String, String> typed map, however when the library is adding values
     * to the collection, they have their own type, for example Integer in this case. This is an issue at extraction
     * time as the collection appears to be <String, String>, but in reality it's <String, ?>
     * @param xmlRpcResult XML RPC result collection to be de-marshalled.
     * @return Session Id Integer.
     */
    private static String parseSessionId(Map<String, ?> xmlRpcResult) {
        Integer sessionIdInt = (Integer) xmlRpcResult.get("session-id");
        return sessionIdInt.toString();
    }

    /**
     * Constructs URL that has embedded authentication.
     * @return Koji XML-RPC URL hub with authentication parameters put into HTTP format.
     */
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
