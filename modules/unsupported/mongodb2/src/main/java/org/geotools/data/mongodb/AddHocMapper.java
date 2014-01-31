package org.geotools.data.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.opengis.feature.type.Name;

public class AddHocMapper extends CollectionMapper {

    String[] geometryPath;
    GeometryFactory geometryFactory;

    public AddHocMapper() {
        this("loc");
    }

    public AddHocMapper(String geometryPath) {
        setGeometryPath(geometryPath);
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
    public Geometry getGeometry(DBObject obj) {
        double x, y;
        if (obj instanceof BasicDBList) {
            BasicDBList list = (BasicDBList) obj;
            x = ((Number)list.get(0)).doubleValue();
            y = ((Number)list.get(1)).doubleValue();
        }
        else {
            DBObject dbo = (DBObject)obj;
            if (dbo.keySet().size() != 2) {
                throw new IllegalArgumentException("Geometry object contain two keys for object with" +
                  " id: " + obj.get("_id"));
            }

            Iterator<String> it = dbo.keySet().iterator();
            x = ((Number)dbo.get(it.next())).doubleValue();
            y = ((Number)dbo.get(it.next())).doubleValue();
        }
        return geometryFactory.createPoint(new Coordinate(x,y));
    }

    @Override
    public DBObject toObject(Geometry g) {
        GeoJSONGeometryBuilder b = new GeoJSONGeometryBuilder();
        DBObject obj = b.toObject(g);
        return (DBObject) obj.get("coordinates");
    }

    @Override
    public void setGeometry(DBObject obj, Geometry g) {
        obj.put(getGeometryPath(), toObject(g));
    }

    @Override
    public SimpleFeatureType buildFeatureType(Name name, DBCollection collection) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(name);
        tb.add(getGeometryPath(), Geometry.class);
        return tb.buildFeatureType();
    }

    @Override
    public SimpleFeature buildFeature(DBObject obj, SimpleFeatureType featureType) {
        //read the geometry
        Object o = obj;
        for (String p : geometryPath) {
            o = ((DBObject) o).get(p);
            if (o == null || !(o instanceof DBObject)) {
                break;
            }
        }

        if (o == null) {
            throw new IllegalArgumentException("Could not resolve geometry path: " + 
                getGeometryPath() + " for object with id: " + obj.get("_id"));
        }

        List<Object> values = new ArrayList<Object>(obj.keySet().size());
        Map<String,Integer> lookup = new HashMap<String, Integer>();

        values.add(getGeometry((DBObject)o));
        lookup.put(getGeometryPath(), 0);

        int i = 1;
        for (String key : obj.keySet()) {
            if (key.equals("_id")) {
                continue;
            }
            if (key.equals(geometryPath[0])) {
                //geometry, ignore
                continue;
            }
            
            values.add(obj.get(key));
            lookup.put(key, i++);
        }

        String id = obj.get("_id").toString();
//        return new MongoFeature(values, featureType, id, lookup);
        return null;
    }

//
//    @Override
//    public void readSchema(SimpleFeatureTypeBuilder typeBuilder, DBCollection collection) {
//        typeBuilder.add("geometry", Point.class);
//        typeBuilder.add("properties", Map.class);
//    }
//
//    @Override
//    public void readGeometry(DBObject object, SimpleFeatureBuilder featureBuilder) {

//    }
//
//    @Override
//    public void readAttributes(DBObject object, SimpleFeatureBuilder featureBuilder) {
//        featureBuilder.set("properties", object);
//    }

}
