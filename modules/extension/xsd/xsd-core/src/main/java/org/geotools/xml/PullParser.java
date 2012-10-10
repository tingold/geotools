package org.geotools.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.geotools.xml.impl.ElementHandler;
import org.geotools.xml.impl.NodeImpl;
import org.geotools.xml.impl.ParserHandler;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * XML pull parser capable of streaming.
 * <p>
 * Similar in nature to {@link StreamingParser} but based on XPP pull parsing rather than SAX. 
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PullParser {

    PullParserHandler handler;
    XmlPullParser pp;

    Attributes atts = new Attributes();

    public PullParser(Configuration config, InputStream input, QName element) {
        this(config, input, new ElementPullParserHandler(element, config));
    }

    public PullParser(Configuration config, InputStream input, Class type) {
        this(config, input, new TypePullParserHandler(type, config));
    }

    public PullParser(Configuration config, InputStream input, Object... handlerSpecs) {
        this(config, input, new OrPullParserHandler(config, handlerSpecs));
    }

    public PullParser(Configuration config, InputStream input, PullParserHandler handler) {
        this.handler = handler;
        pp = createPullParser(input);
    }

    public Object parse() throws XmlPullParserException, IOException, SAXException {
        if (handler.getLogger() == null) {
            handler.startDocument();
        }
        int level = pp.getDepth() - 1;
        int depth;
        int[] startAndLength = new int[2];

        LOOP:
        do {
            int e = pp.next();
            String prefix = pp.getPrefix();
            String name = pp.getName();
            QName qName = name != null ? qName(prefix, name, pp) : null;
            
            switch(e) {
            case XmlPullParser.START_TAG:
                depth = pp.getDepth() - 1;
                int countPrev = (level > depth) ? pp.getNamespaceCount(depth) : 0;
                int count = pp.getNamespaceCount(depth + 1);
                for (int i = countPrev; i < count; i++) {
                    String pre = pp.getNamespacePrefix(i);
                    handler.startPrefixMapping(pre != null ? pre : "",pp.getNamespaceUri(i));
                }
                
                handler.startElement(pp.getNamespace(), pp.getName(), str(qName), atts);
                break;

            case XmlPullParser.TEXT:
                char[] chars = pp.getTextCharacters(startAndLength);
                handler.characters(chars, startAndLength[0], startAndLength[1]);
                break;

            case XmlPullParser.END_TAG:
                
                handler.endElement(pp.getNamespace(), name, str(qName));

                // when entering show prefixes for all levels!!!!
                depth = pp.getDepth();
                countPrev = (level > depth) ? pp.getNamespaceCount(pp.getDepth()) : 0;
                count = pp.getNamespaceCount(pp.getDepth() - 1);

                // undeclare them in reverse order
                for (int i = count - 1; i >= countPrev; i--) {
                    handler.endPrefixMapping(pp.getNamespacePrefix(i));
                }

                //check whether to break out
                if (handler.getObject() != null) {
                    return handler.getObject();
                }

                break;
            case XmlPullParser.END_DOCUMENT:
                break LOOP;
            }
        }
        while(true);

        return null;
    }

    QName qName(String prefix, String name, XmlPullParser pp2) {
        if(prefix != null) {
            return new QName(pp.getNamespace(prefix), name, prefix);
        }
        else {
            return new QName(name); 
        }
    }

    XmlPullParser createPullParser(InputStream input) {
        try {
            XmlPullParserFactory ppf = XmlPullParserFactory.newInstance();
            ppf.setNamespaceAware(true);
            ppf.setValidating(false);

            XmlPullParser pp = ppf.newPullParser();
            pp.setInput(new InputStreamReader(input));
            return pp;
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error creating pull parser", e);
        }
    }

    String str(QName qName) {
        return qName.getPrefix() != null ? qName.getPrefix() + ":" + qName.getLocalPart() : 
            qName.getLocalPart();
    }

    class Attributes implements org.xml.sax.Attributes {

        public int getLength() { return pp.getAttributeCount(); }
        public String getURI(int index) { return pp.getAttributeNamespace(index); }
        public String getLocalName(int index) { return pp.getAttributeName(index); }
        public String getQName(int index) {
            final String prefix = pp.getAttributePrefix(index);
            if(prefix != null) {
                return prefix+':'+pp.getAttributeName(index);
            } else {
                return pp.getAttributeName(index);
            }
        }
        public String getType(int index) { return pp.getAttributeType(index); }
        public String getValue(int index) { return pp.getAttributeValue(index); }

        public int getIndex(String uri, String localName) {
            for (int i = 0; i < pp.getAttributeCount(); i++)
            {
                if(pp.getAttributeNamespace(i).equals(uri)
                   && pp.getAttributeName(i).equals(localName))
                {
                    return i;
                }

            }
            return -1;
        }

        public int getIndex(String qName) {
            for (int i = 0; i < pp.getAttributeCount(); i++)
            {
                if(pp.getAttributeName(i).equals(qName))
                {
                    return i;
                }

            }
            return -1;
        }

        public String getType(String uri, String localName) {
            for (int i = 0; i < pp.getAttributeCount(); i++)
            {
                if(pp.getAttributeNamespace(i).equals(uri)
                   && pp.getAttributeName(i).equals(localName))
                {
                    return pp.getAttributeType(i);
                }

            }
            return null;
        }
        public String getType(String qName) {
            for (int i = 0; i < pp.getAttributeCount(); i++)
            {
                if(pp.getAttributeName(i).equals(qName))
                {
                    return pp.getAttributeType(i);
                }

            }
            return null;
        }
        public String getValue(String uri, String localName) {
            return pp.getAttributeValue(uri, localName);
        }
        public String getValue(String qName) {
            return pp.getAttributeValue(null, qName);
        }
    }

    static abstract class PullParserHandler extends ParserHandler {

        PullParser parser;
        Object object;

        public PullParserHandler(Configuration config) {
            super(config);
        }

        @Override
        protected void endElementInternal(ElementHandler handler) {
            object = null;
            if (stop(handler)) {
                object = handler.getParseNode().getValue();
                
                //remove this node from parse tree
                if (handler.getParentHandler() instanceof ElementHandler) {
                    ElementHandler parent = (ElementHandler) handler.getParentHandler();
                    ((NodeImpl) parent.getParseNode()).removeChild(handler.getParseNode());
                }
            }
        }

        public Object getObject() {
            return object;
        }

        protected abstract boolean stop(ElementHandler handler);

    }

    static class TypePullParserHandler extends PullParserHandler {
        Class type;
        public TypePullParserHandler(Class type, Configuration config) {
            super(config);
            this.type = type;
        }
        @Override
        protected boolean stop(ElementHandler handler) {
            return type.isInstance(handler.getParseNode().getValue());
        }
    }

    static class ElementPullParserHandler extends PullParserHandler {
        QName element;
        public ElementPullParserHandler(QName element, Configuration config) {
            super(config);
            this.element = element;
        }
        @Override
        protected boolean stop(ElementHandler handler) {
            boolean equal = false;
            if (element.getNamespaceURI() != null) {
                equal = element.getNamespaceURI().equals(handler.getComponent().getNamespace());
            }
            else {
                equal = handler.getComponent().getNamespace() == null;
            }
            return equal && element.getLocalPart().equals(handler.getComponent().getName());
        }
    }

    static class ElementIgnoringNamespacePullParserHandler extends ElementPullParserHandler {
        public ElementIgnoringNamespacePullParserHandler(QName element, Configuration config) {
            super(element, config);
        }
        @Override
        protected boolean stop(ElementHandler handler) {
            return element.getLocalPart().equals(handler.getComponent().getName());
        }

    }

    // aggregate the other handlers, and stop if any of them want to stop
    static class OrPullParserHandler extends PullParserHandler {
        private final Collection<PullParserHandler> parserHandlers;

        public OrPullParserHandler(Configuration config, Object... handlerSpecs) {
            super(config);
            Collection<PullParserHandler> handlers = new ArrayList<PullParserHandler>(handlerSpecs.length);
            for (Object spec : handlerSpecs) {
                if (spec instanceof Class) {
                    handlers.add(new TypePullParserHandler((Class<?>) spec, config));
                } else if (spec instanceof QName) {
                    // TODO ignoring the namespace
                    handlers.add(new ElementIgnoringNamespacePullParserHandler((QName) spec, config));
                } else if (spec instanceof PullParserHandler) {
                    handlers.add((PullParserHandler) spec);
                } else {
                    throw new IllegalArgumentException("Unknown element: "
                            + spec.toString() + " of type: " + spec.getClass());
                }
            }
            parserHandlers = Collections.unmodifiableCollection(handlers);
        }

        @Override
        protected boolean stop(ElementHandler handler) {
            for (PullParserHandler pph : parserHandlers) {
                if (pph.stop(handler)) {
                    return true;
                }
            }
            return false;
        }
    }
}
