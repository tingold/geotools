/*
 * 
 */

package org.geotools.data.mongodb;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class FeatureTypeDBObjectTest {
    
    public FeatureTypeDBObjectTest() {
    }

    @Test
    public void testRoundTripConversion() throws FileNotFoundException, IOException, FactoryException {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("sampleType");
        
        builder.userData("mapping", "geometry");
        builder.userData("encoding", "GeoJSON");
        builder.add("geometry", MultiPolygon.class, CRS.decode("EPSG:4269", true));
        
        builder.userData("mapping", "child.prop1");
        builder.add("prop1", String.class);
        
        builder.userData("mapping", "child.prop2");
        builder.add("prop2", Double.class);
        
        builder.userData("mapping", "child.prop3");
        builder.add("prop3", Integer.class);
        
        SimpleFeatureType original = builder.buildFeatureType();
        
        DBObject dbo = FeatureTypeDBObject.convert(original);
        
        // make sure we're dealing with proper BSON/JSON by round-tripping it
        // through serialization...
        StringBuilder jsonBuffer = new StringBuilder();
        JSON.serialize(dbo, jsonBuffer);
        String json = jsonBuffer.toString();
        System.out.println(MongoTestUtil.prettyPrint(json));
        Object o = JSON.parse(json);
        assertThat(o, is(instanceOf(DBObject.class)));
        dbo = (DBObject)o;
        
        SimpleFeatureType result = FeatureTypeDBObject.convert(dbo).buildFeatureType();
        
        // verify we persist and restore name
        assertThat(result.getTypeName(), is(equalTo(original.getTypeName())));
        // verify we persist and restore same number of attributes
        assertThat(result.getAttributeCount(), is(equalTo(original.getAttributeCount())));
        
        // verify we persist and restore geometry name
        String rgdName = result.getGeometryDescriptor().getLocalName();
        assertThat(rgdName, is(equalTo(original.getGeometryDescriptor().getLocalName())));
        // verify we persist and restore CRS (this should always be WGS84 in the wild)
        assertTrue("CRS are equal", CRS.equalsIgnoreMetadata(result.getCoordinateReferenceSystem(), original.getCoordinateReferenceSystem()));
        
        // NOTE!  Geometry type is generalized when persisted...  This 2 asserts are here for documentation only
        assertThat((Class)result.getGeometryDescriptor().getType().getBinding(),
                is(not(equalTo(original.getGeometryDescriptor().getType().getBinding()))));
        assertThat((Class)result.getGeometryDescriptor().getType().getBinding(), is(equalTo(Geometry.class)));
        
        for (AttributeDescriptor rad: result.getAttributeDescriptors()) {
            String radName = rad.getLocalName();
            AttributeDescriptor oad = original.getDescriptor(radName);
            assertThat(rad.getMinOccurs(), is(equalTo(oad.getMinOccurs())));
            assertThat(rad.getMaxOccurs(), is(equalTo(oad.getMaxOccurs())));
            assertThat(rad.getDefaultValue(), is(equalTo(oad.getDefaultValue())));
            if (!radName.equals(rgdName)) {
                assertThat((Class)rad.getType().getBinding(), is(equalTo(oad.getType().getBinding())));
            }
            Map<?,?> radUserData = rad.getUserData();
            Map<?,?> oadUserData = oad.getUserData();
            assertThat(radUserData.size(), is(equalTo(oadUserData.size())));
            for (Map.Entry entry : radUserData.entrySet()) {
                assertThat(entry.getValue(), is(equalTo(oadUserData.get(entry.getKey()))));
            }
        }
    }
    
    @Test
    public void crsFromGeoJSON() {
        DBObject crsDBO = FeatureTypeDBObject.encodeCRSToGeoJSON(DefaultGeographicCRS.WGS84);

        CoordinateReferenceSystem result = FeatureTypeDBObject.decodeCRSFromGeoJSON(crsDBO);

        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, result));
    }
}
