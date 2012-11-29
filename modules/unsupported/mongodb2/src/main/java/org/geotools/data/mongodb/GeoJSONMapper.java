package org.geotools.data.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Maps a collection containing valid GeoJSON. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoJSONMapper extends CollectionMapper {

    GeoJSONGeometryBuilder geomBuilder = new GeoJSONGeometryBuilder();

    @Override
    public String getGeometryPath() {
        return "geometry.coordinates";
    }

    @Override
    public String getPropertyPath(String property) {
        return "properties." + property;
    }

    @Override
    public SimpleFeatureType buildFeatureType(DBCollection collection) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(collection.getName());
        //tb.setSRS("EPSG:4326");
        tb.add("geometry", Geometry.class);
        return tb.buildFeatureType();
    }

    @Override
    public SimpleFeature buildFeature(DBObject obj, SimpleFeatureType featureType) {
        DBObject propObj = (DBObject) obj.get("properties");

        List<Object> values = new ArrayList<Object>(propObj.keySet().size()+1);
        Map<String,Integer> attLookup = new HashMap<String, Integer>();

        //parse geometry
        values.add(geomBuilder.build((DBObject) obj.get("geometry")));
        attLookup.put("geometry", 0);

        //grab all the properties
        int i = 1;
        for (String key : propObj.keySet()) {
            values.add(propObj.get(key));
            attLookup.put(key, i++);
        }

        //id
        String id = (String) obj.get("_id").toString();
        return new MongoFeature(values, featureType, id, attLookup);

    }
}
