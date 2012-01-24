import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class KMLEncode {

    public static void main(String[] args) throws Exception {
        SimpleFeatureTypeBuilder stb = new SimpleFeatureTypeBuilder();
        stb.setName("test");
        stb.add("the_geom", Point.class);
        stb.add("a", String.class);
        stb.add("b", String.class);
        SimpleFeatureType type = stb.buildFeatureType();
        SimpleFeatureBuilder sb = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new
DefaultFeatureCollection("test", type);
        GeometryFactory geomFac=new GeometryFactory();
        
        fc.add(sb.buildFeature("1", new Object[]
{geomFac.createPoint(new Coordinate(1,1)), "a1", "b1" }));
        fc.add(sb.buildFeature("2", new Object[]
{geomFac.createPoint(new Coordinate(2,2)), "a2", "b2" }));

        KMLConfiguration con = new KMLConfiguration();

        Encoder encoder = new Encoder(con);
        encoder.setIndenting(true);
        encoder.encode(fc, KML.Document, System.out);
    }
}
