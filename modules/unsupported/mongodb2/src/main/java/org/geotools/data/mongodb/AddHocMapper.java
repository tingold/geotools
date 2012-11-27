package org.geotools.data.mongodb;

import java.util.Iterator;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class AddHocMapper extends CollectionMapper {

    String[] geometryPath;
    GeometryFactory geometryFactory;

    public AddHocMapper() {
        setGeometryPath("loc");
        setGeometryFactory(new GeometryFactory());
    }

    public void setGeometryPath(String geometryPath) {
        this.geometryPath = geometryPath.split("\\.");
    }

    @Override
    public String getPropertyPath(String property) {
        return property;
    }

    public String getGeometryPath() {
        StringBuffer sb = new StringBuffer();
        for (String p : geometryPath) {
            sb.append(p).append(".");
        }
        sb.setLength(sb.length()-1);
        return sb.toString();
    }

    public void setGeometryFactory(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    @Override
    public void readSchema(SimpleFeatureTypeBuilder typeBuilder, DBCollection collection) {
        typeBuilder.add("geometry", Point.class);
        typeBuilder.add("properties", Map.class);
    }

    @Override
    public void readGeometry(DBObject object, SimpleFeatureBuilder featureBuilder) {
        DBObject dbObj = object;
        for (String p : geometryPath) {
            dbObj = (DBObject) object.get(p);
            if (dbObj == null) {
                break;
            }
        }

        if (dbObj == null) {
            throw new IllegalArgumentException("Could not resolve geometry path: " + 
                getGeometryPath() + " for object with id: " + object.get("_id"));
        }

        double x, y;

        if (dbObj instanceof BasicDBList) {
            BasicDBList list = (BasicDBList) dbObj;
            x = ((Number)list.get(0)).doubleValue();
            y = ((Number)list.get(1)).doubleValue();
        }
        else {
            if (dbObj.keySet().size() != 2) {
                throw new IllegalArgumentException("Geometry object contain two keys for object with" +
                    " id: " + object.get("_id"));
            }

            Iterator<String> it = dbObj.keySet().iterator();
            x = ((Number)dbObj.get(it.next())).doubleValue();
            y = ((Number)dbObj.get(it.next())).doubleValue();
        }

        featureBuilder.set("geometry", geometryFactory.createPoint(new Coordinate(x,y)));
    }

    @Override
    public void readAttributes(DBObject object, SimpleFeatureBuilder featureBuilder) {
        featureBuilder.set("properties", object);
    }

}
