package org.geotools.data.mongodb.integration;

import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import org.apache.commons.lang3.StringUtils;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.experimental.categories.Category;


/**
 *
 * @author tkunicki@boundlessgeo.com
 */
@Category(IntegrationTest.class)
public class EmbedMongoTest {

  static final int PORT;
  
  static {
    String portAsString = System.getProperty("embedmongo.port");
    int port = 27017;
    if (!StringUtils.isBlank(portAsString)) {
      try {
        port = Integer.parseInt(portAsString);
      } catch (NumberFormatException e) {
        System.out.println("Exception extracting EmbedMongo port from property");
      }
    }
    PORT = port;
  }
  
  @Test
  public void testConnect() throws UnknownHostException {
    MongoClient mc = new MongoClient("localhost", PORT);
    try {
      assertThat(mc, is(notNullValue()));
    } finally {
      mc.close();
    }
  }

}
