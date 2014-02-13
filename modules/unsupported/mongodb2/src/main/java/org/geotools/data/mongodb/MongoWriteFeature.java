package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

public class MongoWriteFeature implements SimpleFeature {

    private final SimpleFeatureType featureType;
    private final DBObject featureDBO;
    private final CollectionMapper mapper;
    
    private final Map<Object, Object> userData; 

    public MongoWriteFeature(DBObject dbo, SimpleFeatureType featureType, CollectionMapper mapper) {
        this.featureDBO = dbo;
        this.featureType = featureType;
        this.mapper = mapper;
        
        userData = new HashMap<Object, Object>();
    }

    public DBObject getObject() {
        return featureDBO;
    }

    @Override
    public SimpleFeatureType getType() {
        return featureType;
    }
    
    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public FeatureId getIdentifier() {
        String id = getID();
        return id != null ? new FeatureIdImpl(id) : null;
    }

    @Override
    public String getID() {
        Object id = featureDBO.get("_id");
        return id != null ? id.toString() : null; 
    }

    @Override
    public BoundingBox getBounds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultGeometry() {
        Object o = get(mapper.getGeometryPath());
        return o instanceof DBObject ? mapper.getGeometry((DBObject)o) : null;
    }

    @Override
    public void setDefaultGeometry(Object geometry) {
        geometry = convertToMongo(geometry);
        set(mapper.getGeometryPath(), geometry);
    }

    @Override
    public Object getAttribute(Name name) {
        return getAttribute(name.getLocalPart());
    }

    @Override
    public Object getAttribute(String name) {
        AttributeDescriptor d = featureType.getDescriptor(name);
        if (d instanceof GeometryDescriptor) {
            Object o = get(mapper.getGeometryPath());
            return o instanceof DBObject ?
                    mapper.getGeometry((DBObject)o) : null;
        }
        return get(mapper.getPropertyPath(name));
    }

    @Override
    public void setAttribute(Name name, Object value) {
        setAttribute(name.getLocalPart(), value);
    }

    @Override
    public void setAttribute(String name, Object value) {
        value = convertToMongo(value);
        AttributeDescriptor d = featureType.getDescriptor(name);
        if (d instanceof GeometryDescriptor) {
            set(mapper.getGeometryPath(), value);
        } else {
            set(mapper.getPropertyPath(d.getLocalName()), value);
        }
    }

    @Override
    public Object getAttribute(int index) throws IndexOutOfBoundsException {
        AttributeDescriptor d = featureType.getDescriptor(index);
        if (d instanceof GeometryDescriptor) {
            Object o = get(mapper.getGeometryPath());
            return o instanceof DBObject ?
                    mapper.getGeometry((DBObject)o) : null;
        }
        return get(mapper.getPropertyPath(d.getLocalName()));
    }

    @Override
    public void setAttribute(int index, Object value) throws IndexOutOfBoundsException {
        value = convertToMongo(value);
        AttributeDescriptor d = featureType.getDescriptor(index);
        if (d instanceof GeometryDescriptor) {
            set(mapper.getGeometryPath(), value);
        } else {
            set(mapper.getPropertyPath(d.getLocalName()), value);
        }
    }

    String key(int i) {
        return new ArrayList<String>(featureDBO.keySet()).get(i);
    }

    Object get(String path) {
        return get(Arrays.asList(path.split("\\.")).iterator(), featureDBO);
    }
    
    Object get(Iterator<String> path, Object currentDBO) {
        if (path.hasNext()) {
            if (currentDBO instanceof DBObject) {
                String key = path.next();
                Object value = ((DBObject)currentDBO).get(key);
                return get(path, value);
            }
            return null;
        } else {
            return currentDBO;
        }
    }
    
    void set(String path, Object obj) {
        set(Arrays.asList(path.split("\\.")).iterator(), obj, featureDBO);
    }
    
    void set(Iterator<String> path, Object value, DBObject currentDBO) {
        String key = path.next();
        if (path.hasNext()) {
            Object next = currentDBO.get(key);
            DBObject nextDBO;
            if (next instanceof DBObject) {
                nextDBO = (DBObject)next;
            } else {
                currentDBO.put(key, nextDBO = new BasicDBObject());
            }
            set(path, value, nextDBO);
        } else {
            currentDBO.put(key, value);
        }
    }

    Object convertToMongo(Object o) {
        if (o instanceof Geometry) {
            o =  mapper.toObject((Geometry)o);
        }
        return o;
    }

    @Override
    public int getAttributeCount() {
        return featureType.getAttributeCount();
    }

    @Override
    public List<Object> getAttributes() {
        List<Object> values = new ArrayList(getAttributeCount());
        for (AttributeDescriptor d : featureType.getAttributeDescriptors()) {
            values.add(getAttribute(d.getLocalName()));
        }
        return values;
    }

    @Override
    public void setAttributes(List<Object> values) {
        int index = 0;
        for(Object value : values) {
            setAttribute(index++, value);
        }
    }

    @Override
    public void setAttributes(Object[] values) {
        int index = 0;
        for(Object value : values) {
            setAttribute(index++, value);
        }
    }

    @Override
    public Map<Object, Object> getUserData() {
        return userData;
    }

    @Override
    public GeometryAttribute getDefaultGeometryProperty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultGeometryProperty(GeometryAttribute defaultGeometry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties(Name name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(Name name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<?extends Property> getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Collection<Property> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeDescriptor getDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Name getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNillable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void validate() {
    }

}
