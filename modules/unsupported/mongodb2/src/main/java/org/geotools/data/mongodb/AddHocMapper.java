package org.geotools.data.mongodb;

import java.util.Iterator;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.opengis.feature.type.Name;

public class AddHocMapper extends AbstractCollectionMapper {

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
        MongoGeometryBuilder b = new MongoGeometryBuilder();
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
