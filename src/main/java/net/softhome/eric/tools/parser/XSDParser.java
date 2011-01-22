/*
 * File: XSDParser.java
 * Package: net.softhome.eric.tools.parser
 * Project: Xsd2SaxParser
 * Created on: 02 Dec 2009
 * By: Eric Blanchard
 *
 */
package net.softhome.eric.tools.parser;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Implements a SAX parser
 */
public class XSDParser extends DefaultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(XSDParser.class);
    private static boolean configured = false;
    private static SAXParserFactory factory = null;
    private SAXParser saxParser = null;
    private String elementToSkip = null;
    private int skipedElementDepth = 0;
    private CharArrayWriter text = null;
    private int currentDepth = 0;
    
    
    private static final String ELEMENT_TAG = "element";
    private static final String ATTRIBUTE_TAG = "attribute";
    
    private Map<String,String> elements = null;
    private Map<String,String> attributes = null;
    
    
    /**
     * Private constructor
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    private XSDParser() throws ParserConfigurationException, SAXException {
            text = new CharArrayWriter ();
            saxParser = factory.newSAXParser();
            elements = new HashMap<String,String>();
            attributes = new HashMap<String,String>();
    }

    /**
     * Gets a new instance of XSDParser.
     * Ensuring that the SAX parser factory has been configured only once.
     * @return a <b>XSDParser</b> instance.
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public static XSDParser getInstance() throws ParserConfigurationException, SAXException {
        if (! configured) {
            configure();
        }
        return new XSDParser();
    }

    /**
     * Configures the SAX parser factory.
     * The default JRE provided SAX parser is used (controlled by
     * <code>"javax.xml.parsers.SAXParserFactory"</code> system property.
     */
    public static void configure() {
        // Use the configured SAXParserFactory
        LOGGER.debug("configure:");

        factory = SAXParserFactory.newInstance();
        /* Customize SAX parser */
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        configured = true;
    }
    
    public void parseFrontString(final String str) throws SAXException, IOException {
        LOGGER.debug("parseFrontString: str={}", str);
        if (str == null || str.length() == 0) {
            LOGGER.warn("parseFrontString: null string");
            return;
        }
        InputSource src = new InputSource(new StringReader(str));
        parse(src);
    }
    
    public void parse(InputStream is) throws SAXException, IOException {
        LOGGER.debug("parse: InputStream={}", is);
        parse(new InputSource(is));
    }
    
    public void parse(InputSource is) throws SAXException, IOException {
        LOGGER.debug("parse: InputSource={}", is);
        saxParser.parse(is, this);
    }
    
    public void reset() {
        elements.clear();
        attributes.clear();
        text.reset();
        elementToSkip = null;
        currentDepth = 0;
        skipedElementDepth = 0;
    }
    
    public Map<String,String> getElements() {
        return elements;
    }
    
    public Map<String,String> getAttributes() {
        return attributes;
    }

    /* (non-Javadoc)
     * Resolves relative path URI entities to current directory
     * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
        LOGGER.debug("resolveEntity: systemId={}", systemId);

        /* Provide an empty entity resolver */
        LOGGER.debug("resolveEntity: ByPass the DTD by providing an empty input stream");
        return new InputSource(new StringReader(""));
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws SAXException {
        LOGGER.debug("processingInstruction: target={}, data={}", target, data);
    }

    
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name) throws SAXException {
        LOGGER.debug("skippedEntity: name={}", name);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        LOGGER.debug("startDocument:");
    }

 
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        LOGGER.debug("endDocument: ");
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement (String namespaceURI, String sName, String qName,
        Attributes attrs) throws SAXException {
        String eName = sName; /* element name (assuming namespaceAware) */

        currentDepth++;
        LOGGER.debug("startElement: {}, currentDepth={}", eName, currentDepth);
        
        if (elementToSkip != null) {
            LOGGER.debug("startElement: skipping element={}", eName);
            return;
        }

        text.reset();
        
        if (ELEMENT_TAG.equals(eName)) {
            String name = attrs.getValue("name");
            String ref = attrs.getValue("ref");
            if (name == null) {
                name = ref;
            }
            if (name == null || name.length() == 0) {
                LOGGER.warn("startElement: No name for element={}", eName);
            } else {
                String key = toKey(name) + "_TAG";
                elements.put(key, name);
            }
        } else if (ATTRIBUTE_TAG.equals(eName)) {
            String name = attrs.getValue("name");
            if (name == null || name.length() == 0) {
                LOGGER.warn("startElement: No name for element={}", eName);
            } else {
                String key = toKey(name) + "_ATTR";
                attributes.put(key, name);
            }
        } else {
           //LOGGER.debug("startElement: Unknown element={}", eName);
            /* Unknown elements fall here, so just ask to skip this unknown
             * element (and all nested elements) */
            //elementToSkip = eName;
            //skipedElementDepth = currentDepth;
        }
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String sName, String qName)
        throws SAXException {
        String eName = sName; /* element name (assuming namespaceAware) */

        LOGGER.debug("endElement: {}", eName);
        if (elementToSkip != null) {
            if (elementToSkip.equals(eName) && currentDepth == skipedElementDepth) {
                elementToSkip = null;
                skipedElementDepth = -1;
            }
            currentDepth--;
            return;
        }

        if (ELEMENT_TAG.equals(eName)) {
           
        } else if (ATTRIBUTE_TAG.equals(eName)) {
       
        } else {
            LOGGER.debug("endElement: Unknown ending tag=" + eName);
        }
        currentDepth--;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len)
        throws SAXException {
        if (elementToSkip != null) {
            return;
        }
        if (len > 0) {
            text.write (buf, offset, len);
        }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
        LOGGER.warn("warning: XML parsing warning: " + e.toString(), e);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        LOGGER.warn("error: XML parsing error: " + e.toString(), e);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        LOGGER.error("fatalError: parsing fatal error: " + e.toString(), e);
        throw e;
    }
    
    public String getText()
    {
        return text.toString().trim();
    }
    
    private String toKey(final String str) {
        String key = str.replace('-', '_');
        key = key.replace(' ', '_');
        key = key.replaceAll("(\\p{Lower})(\\p{Upper})", "$1_$2");
        
        return key.toUpperCase();
    }

}


