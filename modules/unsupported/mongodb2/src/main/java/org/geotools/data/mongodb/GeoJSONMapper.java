package org.geotools.data.mongodb;

import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

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
    public void readSchema(SimpleFeatureTypeBuilder typeBuilder, DBCollection collection) {
        typeBuilder.add("properties", Map.class);
    }

    @Override
    public void readGeometry(DBObject object, SimpleFeatureBuilder featureBuilder) {
        featureBuilder.set("geometry", geomBuilder.build((DBObject)object.get("geometry")));
    }

    @Override
    public void readAttributes(DBObject object, SimpleFeatureBuilder featureBuilder) {
        featureBuilder.set("properties", object.get("properties"));
    }
}
