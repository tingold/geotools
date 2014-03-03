/*
 * 
 */

package org.geotools.data.mongodb;

import java.io.File;
import static org.geotools.data.mongodb.MongoSchemaFileStore.typeName;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoSchemaFileStoreTest {
    
    public MongoSchemaFileStoreTest() {
    }

    @Test
    public void testTypeName() {
        assertThat(typeName(new File("testMe.json")),
                is(equalTo("testMe")));
        assertThat(typeName(new File("c:/testMe.json")),
                is(equalTo("testMe")));
        assertThat(typeName(new File("/opt/tomcat/webapps/data/mongodb-schemas/teststore/testMe.json")),
                is(equalTo("testMe")));
    }
    
}
