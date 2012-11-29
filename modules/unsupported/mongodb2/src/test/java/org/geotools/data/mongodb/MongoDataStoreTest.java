package org.geotools.data.mongodb;

import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public abstract class MongoDataStoreTest extends MongoTestSupport {

    protected MongoDataStoreTest(MongoTestSetup testSetup) {
        super(testSetup);
    }

    public void testGetTypeNames() throws Exception {
        String[] typeNames = dataStore.getTypeNames();
        assertEquals(1, typeNames.length);
        assertEquals("ft1", typeNames[0]);
    }

    public void testGetSchema() throws Exception {
        SimpleFeatureType schema = dataStore.getSchema("ft1");
        assertNotNull(schema);

        assertNotNull(schema.getDescriptor("geometry"));
        assertTrue(Geometry.class.isAssignableFrom(schema.getDescriptor("geometry").getType().getBinding()));
    }

    public void testGetFeatureReader() throws Exception {
        SimpleFeatureReader reader = (SimpleFeatureReader) 
            dataStore.getFeatureReader(new Query("ft1"), Transaction.AUTO_COMMIT);
        try {
            for (int i = 0; i < 3; i++) {
                assertTrue(reader.hasNext());
                SimpleFeature f = reader.next();

                assertFeature(f);
            } 
            assertFalse(reader.hasNext());
        }
        finally {
            reader.close();
        }
    }

    public void testGetFeatureSource() throws Exception {
        SimpleFeatureSource source = dataStore.getFeatureSource("ft1");
        assertEquals(3, source.getCount(Query.ALL));

        ReferencedEnvelope env = source.getBounds();
        assertEquals(0d, env.getMinX(), 0.1);
        assertEquals(0d, env.getMinY(), 0.1);
        assertEquals(2d, env.getMaxX(), 0.1);
        assertEquals(2d, env.getMaxY(), 0.1);
    }
}
