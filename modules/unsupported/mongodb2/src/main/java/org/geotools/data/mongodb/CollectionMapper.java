package org.geotools.data.mongodb;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

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

    public abstract SimpleFeatureType buildFeatureType(DBCollection collection);

    public abstract SimpleFeature buildFeature(DBObject obj, SimpleFeatureType featureType);
}
