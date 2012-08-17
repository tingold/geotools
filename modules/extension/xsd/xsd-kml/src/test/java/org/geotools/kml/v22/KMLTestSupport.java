package org.geotools.kml.v22;

import org.geotools.xml.Configuration;
import org.geotools.xml.test.XMLTestSupport;

public class KMLTestSupport extends XMLTestSupport {

    @Override
    protected Configuration createConfiguration() {
        return new KMLConfiguration();
    }

}
