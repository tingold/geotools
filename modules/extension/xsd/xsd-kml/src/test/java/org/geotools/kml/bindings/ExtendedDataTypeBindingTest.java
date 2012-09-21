package org.geotools.kml.bindings;

import java.util.Map;

import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.KMLTestSupport;
import org.geotools.xml.Binding;

public class ExtendedDataTypeBindingTest extends KMLTestSupport {

    public void testExecutionMode() throws Exception {
        assertEquals(Binding.OVERRIDE, binding(KML.ExtendedDataType).getExecutionMode());
    }

    public void testGetType() {
        assertEquals(Map.class, binding(KML.ExtendedDataType).getType());
    }

    // to avoid warnings
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseExtendedData() throws Exception {
        return (Map<String, Map<String, Object>>) parse();
    }

    public void testParseEmpty() throws Exception {
        String xml = "<ExtendedData></ExtendedData>";
        buildDocument(xml);
        Map<String, Map<String, Object>> document = parseExtendedData();
        assertEquals(2, document.size());
    }

    public void testParseUntyped() throws Exception {
        String xml = "<ExtendedData>" + "<Data name=\"foo\"><value>bar</value></Data>"
                + "</ExtendedData>";
        buildDocument(xml);
        Map<String, Map<String, Object>> document = parseExtendedData();
        Map<String, Object> untyped = document.get("untyped");
        assertEquals("bar", untyped.get("foo"));
    }

    public void testParseTyped() throws Exception {
        String xml = "<ExtendedData>" + "<SchemaData schemaUrl=\"#foo\">"
                + "<SimpleData name=\"quux\">morx</SimpleData>" + "</SchemaData>"
                + "</ExtendedData>";
        buildDocument(xml);
        Map<String, Map<String, Object>> document = parseExtendedData();
        Map<String, Object> typed = document.get("typed");
        assertEquals("morx", typed.get("quux"));
    }

}
