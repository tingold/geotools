package org.geotools.data.mongodb;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * A strategy for mapping a mongo collection to a feature.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class CollectionMapper {

    public abstract String getGeometryPath();

    public abstract String getPropertyPath(String property);

    public abstract void readSchema(SimpleFeatureTypeBuilder typeBuilder, DBCollection collection);

    public abstract void readGeometry(DBObject object, SimpleFeatureBuilder featureBuilder);

    public abstract void readAttributes(DBObject object, SimpleFeatureBuilder featureBuilder);
}
