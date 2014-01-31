package org.geotools.data.mongodb;


import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.types.BSONTimestamp;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

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
        return "geometry";
    }

    @Override
    public String getPropertyPath(String property) {
        return "properties." + property;
    }

    @Override
    public Geometry getGeometry(DBObject obj) {
        return geomBuilder.toGeometry((DBObject)obj.get("geometry"));
    }

    @Override
    public DBObject toObject(Geometry g) {
        return geomBuilder.toObject(g);
    }

    @Override
    public void setGeometry(DBObject obj, Geometry g) {
        obj.put("geometry", toObject(g));
    }

    @Override
    public SimpleFeatureType buildFeatureType(Name name, DBCollection collection) {
        
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(name);
        tb.add("geometry", Geometry.class, DefaultGeographicCRS.WGS84);
        DBObject rootDBO = collection.findOne();
        if (rootDBO.containsField("properties")) {
          DBObject propertiesDBO = (DBObject)rootDBO.get("properties");
          for (String key : propertiesDBO.keySet()) {
              Object v = propertiesDBO.get(key);
              Class<?> c = v.getClass();
              if (v instanceof String) {
                tb.add(key, v.getClass());
              } else if (v instanceof Double) {
                tb.add(key, v.getClass());
              } else if (v instanceof Long) {
                tb.add(key, v.getClass());
              } else if (v instanceof Integer) {
                tb.add(key, v.getClass());
              } else if (v instanceof Boolean) {
                tb.add(key, v.getClass());
              } else if (v instanceof Date) {
                tb.add(key, Date.class);
              } else if (v instanceof BSONTimestamp) {
                tb.add(key, Date.class);
              } else {
                System.err.println("unmapped key, " + key + " with type of " + v.getClass().getCanonicalName());
              }
          }
        }
        return tb.buildFeatureType();
    }
    
    @Override
    public SimpleFeature buildFeature(DBObject rootDBO, SimpleFeatureType featureType) {
      
        DBObject propertiesDBO = (DBObject) rootDBO.get("properties");

        List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
        List values = new ArrayList(descriptors.size());
        
        for (AttributeDescriptor descriptor : descriptors) {
          String name = descriptor.getLocalName();
          if ("geometry".equals(name)) {
            values.add(getGeometry(rootDBO));
          } else {
            if (propertiesDBO != null) {
              values.add(propertiesDBO == null ? null : propertiesDBO.get(name));
            }
          }
        }
        
        String id = (String) rootDBO.get("_id").toString();
        
        return new MongoFeature(values.toArray(), featureType, id);

    }
   
}
