package org.geotools.kml.bindings;

import java.util.Collection;

import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLTestSupport;
import org.geotools.xml.Binding;
import org.opengis.feature.simple.SimpleFeature;

public class NameBindingTest extends KMLTestSupport {

    public void testType() throws Exception {
        assertEquals(String.class, binding(KML.name).getType());
    }

    public void testExecutionMode() throws Exception {
        assertEquals(Binding.OVERRIDE, binding(KML.name).getExecutionMode());
    }

    public void testParseName() throws Exception {
        String xml = "<name>fleem</name>";
        buildDocument(xml);

        String name = (String) parse();
        assertEquals("fleem", name);
    }

    public void testParseNameInFolder() throws Exception {
        String xml = "<kml><Folder>" + "<name>foo</name>" + "<Placemark>" + "<name>bar</name>"
                + "</Placemark>" + "</Folder></kml>";
        buildDocument(xml);

        SimpleFeature document = (SimpleFeature) parse();
        assertEquals("foo", document.getAttribute("name"));

        @SuppressWarnings("unchecked")
        Collection<SimpleFeature> features = (Collection<SimpleFeature>) document
                .getAttribute("Feature");
        assertEquals(1, features.size());
        SimpleFeature feature = features.iterator().next();
        assertEquals("foo", feature.getAttribute("Folder"));
    }
}
