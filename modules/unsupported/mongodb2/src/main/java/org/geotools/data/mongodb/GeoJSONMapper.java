package org.geotools.data.mongodb;


import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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
        tb.userData("mapping", "geometry");
        tb.userData("encoding", "GeoJSON");
        tb.add("geometry", Geometry.class, DefaultGeographicCRS.WGS84);
        
        DBObject rootDBO = collection.findOne();
        if (rootDBO != null && rootDBO.containsField("properties")) {
          DBObject propertiesDBO = (DBObject)rootDBO.get("properties");
          for (String key : propertiesDBO.keySet()) {
              Object v = propertiesDBO.get(key);
              Class<?> binding = MongoUtil.mapBSONObjectToJavaType(v);
              if (binding != null) {
                  tb.userData("mapping", "properties." + key);
                  tb.add(key, binding);
              } else {
                System.err.println("unmapped key, " + key + " with type of " + v.getClass().getCanonicalName());
              }
          }
        }
        return tb.buildFeatureType();
    }
    
    @Override
    public SimpleFeature buildFeature(DBObject rootDBO, SimpleFeatureType featureType) {
      
        String gdLocalName = featureType.getGeometryDescriptor().getLocalName();
        List<AttributeDescriptor> adList = featureType.getAttributeDescriptors();
        
        List values = new ArrayList(adList.size());      
        for (AttributeDescriptor descriptor : adList) {
          String adLocalName = descriptor.getLocalName();
          if (gdLocalName.equals(adLocalName)) {
            values.add(getGeometry(rootDBO));
          } else {
            values.add(MongoUtil.getDBOValue(rootDBO, (String)descriptor.getUserData().get(MongoDataStore.KEY_mapping)));
          }
        }
        
        return new MongoFeature(values.toArray(), featureType, rootDBO.get("_id").toString());
    }
}
