/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.kml.bindings;

import com.vividsolutions.jts.geom.Envelope;
import org.geotools.kml.KML;
import org.geotools.kml.KMLTestSupport;
import org.geotools.xml.Binding;

/**
 * 
 * 
 * @source $URL$
 */
public class RegionTypeBindingTest extends KMLTestSupport {
    public void testType() {
        assertEquals(Envelope.class, binding(KML.RegionType).getType());
    }

    public void testExecutionMode() {
        assertEquals(Binding.OVERRIDE, binding(KML.RegionType).getExecutionMode());
    }

    public void testParse() throws Exception {
        String xml = "<Region>" + "<LatLonAltBox>" + "<north>1</north>" + "<south>-1</south>"
                + "<east>1</east>" + "<west>-1</west>" + "</LatLonAltBox>" + "</Region>";

        buildDocument(xml);

        Envelope box = (Envelope) parse();
        assertEquals(-1d, box.getMinX(), 0.1);
        assertEquals(1d, box.getMaxX(), 0.1);
        assertEquals(-1d, box.getMinY(), 0.1);
        assertEquals(1d, box.getMaxY(), 0.1);
    }
}
