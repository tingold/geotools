package org.geotools.data.mongodb;

import java.util.Map;
import java.util.Properties;

import org.geotools.test.OnlineTestCase;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;

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
}
