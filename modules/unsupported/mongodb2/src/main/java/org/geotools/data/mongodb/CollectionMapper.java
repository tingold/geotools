package org.geotools.data.mongodb;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 * A strategy for mapping a mongo collection to a feature.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class CollectionMapper {

    public abstract Geometry getGeometry(DBObject obj);

    public abstract void setGeometry(DBObject obj, Geometry g);

    public abstract DBObject toObject(Geometry g);
    
    public abstract String getGeometryPath();

    public abstract String getPropertyPath(String property);

    public abstract SimpleFeatureType buildFeatureType(Name name, DBCollection collection);

    public abstract SimpleFeature buildFeature(DBObject obj, SimpleFeatureType featureType);
}
