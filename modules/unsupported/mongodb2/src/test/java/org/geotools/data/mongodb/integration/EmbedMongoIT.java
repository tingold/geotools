package org.geotools.data.mongodb.integration;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;


/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class EmbedMongoIT {

  static final int PORT;
  
  static {
    String portAsString = System.getProperty("embedmongo.port");
    int port = 27017;
    if (!(portAsString == null || portAsString.isEmpty())) {
      try {
        port = Integer.parseInt(portAsString);
      } catch (NumberFormatException e) {
        System.out.println("Exception extracting EmbedMongo port from property");
      }
    }
    PORT = port;
    System.out.println("EmbedMongo Port is " + PORT);
  }
  
  @Test
  public void testConnect() throws UnknownHostException {
    MongoClient mc = new MongoClient("localhost", PORT);
    try {
      assertThat(mc, is(notNullValue()));
      DB db = mc.getDB("db");
      DBCollection coll = db.getCollection("dbc");
      BasicDBObject bdo = new BasicDBObject("name", "MongoDB").
                              append("type", "database").
                              append("count", 1).
                              append("info", new BasicDBObject("x", 203).append("y", 102));

      coll.insert(bdo);
      System.out.println(coll.findOne());
    } finally {
      mc.close();
    }
  }

}
