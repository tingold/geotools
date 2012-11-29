package org.geotools.data.mongodb.adhoc;

import org.geotools.data.mongodb.MongoDataStoreTest;

public class AdHocMongoDataStoreTest extends MongoDataStoreTest {

    public AdHocMongoDataStoreTest() {
        super(new AdHocMongoTestSetup());
    }

}
