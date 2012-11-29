package org.geotools.data.mongodb.adhoc;

import org.geotools.data.mongodb.AddHocMapper;
import org.geotools.data.mongodb.MongoDataStore;
import org.geotools.data.mongodb.MongoTestSetup;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;

public class AdHocMongoTestSetup extends MongoTestSetup {

    @Override
    protected void setUpDataStore(MongoDataStore dataStore) {
        dataStore.setMapper(new AddHocMapper("geometry"));
    }

    @Override
    public void setUp(DB db) {
        DBCollection ft1 = db.getCollection("ft1");
        ft1.drop();

        BasicDBObjectBuilder builder = new BasicDBObjectBuilder();
        ft1.save(builder.start()
            .add("id", "ft1.0")
            .add("geometry", list(0,0))
            .add("intProperty", 0)
            .add("doubleProperty", 0.0)
            .add("stringProperty", "zero")
        .get());
        ft1.save(builder.start()
            .add("id", "ft1.1")
            .add("geometry", list(1,1))
            .add("intProperty", 1)
            .add("doubleProperty", 1.1)
            .add("stringProperty", "one")
        .get());
        ft1.save(builder.start()
            .add("id", "ft1.2")
            .add("geometry", list(2,2))
            .add("intProperty", 2)
            .add("doubleProperty", 2.2)
            .add("stringProperty", "two")
        .get());
        
        ft1.ensureIndex(new BasicDBObject("geometry", "2d"));
    }
}
