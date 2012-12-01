package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;

import junit.framework.TestCase;

public class MongoWriteFeatureTest extends TestCase {

    public void test() throws Exception {
        BasicDBObject obj = new BasicDBObject();
        MongoWriteFeature f = new MongoWriteFeature(obj, null, new AddHocMapper());

        f.setAttribute("foo", "bar");
        f.setAttribute("bom.bam", "hi");

        System.out.print(obj);
    }
}
