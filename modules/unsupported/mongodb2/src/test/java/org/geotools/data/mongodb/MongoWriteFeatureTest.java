package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import org.junit.Test;

public class MongoWriteFeatureTest {

    @Test
    public void set() {
        BasicDBObject obj = new BasicDBObject();
        MongoDBObjectFeature f = new MongoDBObjectFeature(obj, null, new AddHocMapper());
        f.setDBOValue("root.child1", 1d);
        f.getDBOValue("root.child1");
        
    }
}
