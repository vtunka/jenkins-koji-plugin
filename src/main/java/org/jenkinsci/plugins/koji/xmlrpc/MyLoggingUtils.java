package org.jenkinsci.plugins.koji.xmlrpc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;

/**
 * This class was created to allow debugging of XML-RPC requests and responses, mainly outside of Jenkins environment.
 *
 * It requires additional dependencies to work properly, like commons httpclient, io and codec.
 */
public class MyLoggingUtils {

    /**
     * Logs a request ni raw XML form.
     * @param logger Logger to be used
     * @param requestEntity
     * @throws XmlRpcException
     */
    public static void logRequest(Logger logger,
                                  RequestEntity requestEntity) throws XmlRpcException {
        ByteArrayOutputStream bos = null;
        try {
            logger.debug("---- Request ----");
            bos = new ByteArrayOutputStream();
            requestEntity.writeRequest(bos);
            logger.debug(toPrettyXml(logger, bos.toString()));
        } catch (IOException e) {
            throw new XmlRpcException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(bos);
        }
    }

    /**
     * Convenience method to nicely log XML contents for request.
     * @param logger
     * @param content XML content to be logged.
     */
    public static void logRequest(Logger logger, String content) {
        logger.debug("---- Request ----");
        logger.debug(toPrettyXml(logger, content));
    }

    /**
     * This method logs XML from streams and nicely formats the output.
     * @param logger
     * @param stream Incoming stream.
     * @return Resulting response.
     * @throws XmlRpcException
     */
    public static String logResponse(Logger logger, InputStream stream)
            throws XmlRpcException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringBuilder respBuf = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                respBuf.append(line);
            }
            String response = respBuf.toString();
            logger.debug("---- Response ----");
            logger.debug(toPrettyXml(logger, respBuf.toString()));
            return response;
        } catch (IOException e) {
            throw new XmlRpcException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Convenience method to nicely log responses.
     * @param logger
     * @param content String XML content to be logged.
     */
    public static void logResponse(Logger logger, String content) {
        logger.debug("---- Response ----");
        logger.debug(toPrettyXml(logger, content));
    }

    /**
     * Reformat XML to be nicely logged.
     * @param logger
     * @param xml XML input
     * @return Formatted XML.
     */
    private static String toPrettyXml(Logger logger, String xml) {
        try {
            Transformer transformer =
                    TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());
            StreamSource source = new StreamSource(new StringReader(xml));
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Exception e) {
            logger.warn("Can't parse XML");
            return xml;
        }
    }
}