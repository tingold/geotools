package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;

public class GeoJSONMongoTestSetup extends MongoTestSetup {

    @Override
    protected void setUpDataStore(MongoDataStore dataStore) {
        dataStore.setMapper(new GeoJSONMapper());
    }

    @Override
    public void setUp(DB db) {
        DBCollection ft1 = db.getCollection("ft1");
        ft1.drop();

        BasicDBObjectBuilder builder = new BasicDBObjectBuilder();
        ft1.save(builder.start()
            .add("id", "ft1.0")
            .push("geometry")
                .add("type", "Point")
                .add("coordinates", list(0,0))
            .pop()
            .push("properties")
                .add("intProperty", 0)
                .add("doubleProperty", 0.0)
                .add("stringProperty", "zero")
            .pop()
        .get());
        ft1.save(builder.start()
            .add("id", "ft1.1")
            .push("geometry")
                .add("type", "Point")
                .add("coordinates", list(1,1))
            .pop()
            .push("properties")
                .add("intProperty", 1)
                .add("doubleProperty", 1.1)
                .add("stringProperty", "one")
            .pop()
        .get());
        ft1.save(builder.start()
            .add("id", "ft1.2")
            .push("geometry")
                .add("type", "Point")
                .add("coordinates", list(2,2))
            .pop()
            .push("properties")
                .add("intProperty", 2)
                .add("doubleProperty", 2.2)
                .add("stringProperty", "two")
            .pop()
        .get());

        ft1.ensureIndex(new BasicDBObject("geometry.coordinates", "2d"));
    
    }

}
