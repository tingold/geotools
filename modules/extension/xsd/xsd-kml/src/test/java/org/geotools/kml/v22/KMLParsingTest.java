package org.geotools.kml.v22;

import java.util.List;
import java.util.Map;

import org.geotools.xml.Parser;
import org.geotools.xml.PullParser;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

public class KMLParsingTest extends KMLTestSupport {

    public void testParseDocument() throws Exception {
        SimpleFeature doc = parseSamples();

        assertNotNull(doc);
        assertEquals("document", doc.getType().getTypeName());
        assertEquals("KML Samples", doc.getAttribute("name"));
        assertEquals(6, ((List)doc.getAttribute("Feature")).size());
    }

    public void testParseFolder() throws Exception {
        SimpleFeature doc = parseSamples();
        SimpleFeature folder = (SimpleFeature) ((List)doc.getAttribute("Feature")).get(0); 

        assertEquals("Placemarks", folder.getAttribute("name"));
        assertTrue(folder.getAttribute("description").toString().startsWith("These are just some"));
        assertEquals(3, ((List)folder.getAttribute("Feature")).size());
    }

    public void testParsePlacemark() throws Exception {
        SimpleFeature doc = parseSamples();
        SimpleFeature folder = (SimpleFeature) ((List)doc.getAttribute("Feature")).get(0); 
        SimpleFeature placemark = (SimpleFeature) ((List)folder.getAttribute("Feature")).get(0);
        
        assertEquals("Simple placemark", placemark.getAttribute("name"));
        assertTrue(placemark.getAttribute("description").toString().startsWith("Attached to the ground"));
        Point p = (Point) placemark.getDefaultGeometry();
        assertEquals(-122.08220, p.getX(), 0.0001);
        assertEquals(37.42229, p.getY(), 0.0001);
    }

    public void testParseWithSchema() throws Exception {
        
    }

    public void testStreamParse() throws Exception {
        StreamingParser p = new StreamingParser(createConfiguration(), 
            getClass().getResourceAsStream("KML_Samples.kml"), KML.Placemark);
        int count = 0;
        while(p.parse() != null) {
            count++;
        }
        assertEquals(20, count);
    }

    public void testPullParse() throws Exception {
        PullParser p = new PullParser(createConfiguration(),
            getClass().getResourceAsStream("KML_Samples.kml"), KML.Placemark);
     
        int count = 0;
        while(p.parse() != null) {
            count++;
        }
        assertEquals(20, count);
    }

    
    public void testParsemarkExtendedData() throws Exception {
        String xml = 
            " <Placemark> " + 
            "    <name>Club house</name> " + 
            "    <ExtendedData> " + 
            "      <Data name='holeNumber'> " + 
            "        <value>1</value> " + 
            "      </Data> " + 
            "      <Data name='holeYardage'> " + 
            "        <value>234</value> " + 
            "      </Data> " + 
            "      <Data name='holePar'> " + 
            "        <value>4</value> " + 
            "      </Data> " + 
            "    </ExtendedData> " + 
            "    <Point> " + 
            "      <coordinates>-111.956,33.5043</coordinates> " + 
            "    </Point> " + 
            "  </Placemark> ";
        buildDocument(xml);

        SimpleFeature f = (SimpleFeature) parse();
        Map<Object, Object> userData = f.getUserData();
        assertNotNull(userData);

        @SuppressWarnings("unchecked")
        Map<String, String> extData = (Map<String, String>) userData.get("ExtendedData");
        assertEquals("1", extData.get("holeNumber"));
        assertEquals("234", extData.get("holeYardage"));
        assertEquals("4", extData.get("holePar"));
    }

    SimpleFeature parseSamples() throws Exception {
        Parser p = new Parser(createConfiguration());
        SimpleFeature doc =
            (SimpleFeature) p.parse(getClass().getResourceAsStream("KML_Samples.kml"));
        return doc;
    }
}
