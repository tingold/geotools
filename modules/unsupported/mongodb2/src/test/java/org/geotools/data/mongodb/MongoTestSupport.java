package org.geotools.data.mongodb;

import java.util.Map;
import java.util.Properties;

import org.geotools.test.OnlineTestCase;
import org.opengis.feature.simple.SimpleFeature;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.vividsolutions.jts.geom.Point;

public class MongoTestSupport extends OnlineTestCase {

    protected MongoTestSetup testSetup;
    protected MongoDataStore dataStore;

    protected MongoTestSupport(MongoTestSetup testSetup) {
        this.testSetup = testSetup;
    }

    @Override
    protected String getFixtureId() {
        return "mongodb";
    }

    @Override
    protected boolean isOnline() throws Exception {
        return doConnect() != null;
    }

    @Override
    protected void connect() throws Exception {
         setUp(doConnect());
    }

    DB doConnect() throws Exception {
        MongoDataStoreFactory factory = new MongoDataStoreFactory();
        return factory.connect((Map)fixture);
    }

    protected void setUp(DB db) throws Exception {
        testSetup.setUp(db);
        dataStore = testSetup.createDataStore(fixture);
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        dataStore.dispose();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("host", "localhost");
        fixture.put("port", "27017");
        fixture.put("database", "geotools");
        fixture.put("user", "geotools");
        fixture.put("passwd", "geotools");
        return fixture;
    }

    protected void assertFeature(SimpleFeature f) {
        int i = (Integer) f.getAttribute("intProperty");
        assertFeature(f, i);
    }
    
    protected void assertFeature(SimpleFeature f, int i) {
        assertNotNull(f.getDefaultGeometry());
        Point p = (Point) f.getDefaultGeometry();

        assertNotNull(f.getAttribute("intProperty"));

        assertEquals((double)i, p.getX(), 0.1);
        assertEquals((double)i, p.getY(), 0.1);

        assertNotNull(f.getAttribute("doubleProperty"));
        assertEquals(i + i*0.1, (Double)f.getAttribute("doubleProperty"), 0.1);

        assertNotNull(f.getAttribute("stringProperty"));
        assertEquals(toString(i), (String)f.getAttribute("stringProperty"));
    }

    protected String toString(int i) {
        return i == 0 ? "zero" : i == 1 ? "one" : i == 2 ? "two" : null;
    }
}
