package org.apache.xmlrpc.client;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.NullSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class MyTypeFactory extends TypeFactoryImpl {

    public MyTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    /**
     * Correctly handle XML-RPC "None" elements in incoming responses from server.
     */
    @Override
    public TypeParser getParser(XmlRpcStreamConfig pConfig,
                                NamespaceContextImpl pContext, String pURI, String pLocalName) {

        if ("".equals(pURI) && NullSerializer.NIL_TAG.equals(pLocalName)) {
            return new NullParser();
        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    /**
     * Correctly handle outgoing requests containing XML-RPC "None" values.
     */
    @Override
    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        if (pObject == null) {
            return new TypeSerializerImpl() {
                public void write(ContentHandler pHandler, Object o) throws SAXException {
                    pHandler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
                    pHandler.startElement("", NullSerializer.NIL_TAG, NullSerializer.NIL_TAG, ZERO_ATTRIBUTES);
                    pHandler.endElement("", NullSerializer.NIL_TAG, NullSerializer.NIL_TAG);
                    pHandler.endElement("", VALUE_TAG, VALUE_TAG);
                }
            };
        } else {
            return super.getSerializer(pConfig, pObject);
        }
    }
}