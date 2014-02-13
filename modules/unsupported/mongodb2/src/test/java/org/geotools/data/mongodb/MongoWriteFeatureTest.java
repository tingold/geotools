package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import org.junit.Test;

public class MongoWriteFeatureTest {

    @Test
    public void set() {
        BasicDBObject obj = new BasicDBObject();
        MongoWriteFeature f = new MongoWriteFeature(obj, null, new AddHocMapper());
        f.set("root.child1", 1d);
        f.get("root.child1");
        
    }
}
