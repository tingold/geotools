package org.geotools.data.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;

public class MongoWriteFeature implements SimpleFeature {

    DBObject dbo;
    SimpleFeatureType featureType;
    CollectionMapper mapper;

    public MongoWriteFeature(DBObject dbo, SimpleFeatureType featureType, CollectionMapper mapper) {
        this.dbo = dbo;
        this.featureType = featureType;
        this.mapper = mapper;
    }

    public DBObject getObject() {
        return dbo;
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
        Object id = dbo.get("_id");
        return id != null ? id.toString() : null; 
    }

    @Override
    public BoundingBox getBounds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultGeometry() {
        return mapper.getGeometry(dbo);
    }

    @Override
    public void setDefaultGeometry(Object geometry) {
        mapper.setGeometry(dbo, (Geometry)geometry);
    }

    @Override
    public Object getAttribute(Name name) {
        return getAttribute(name.getLocalPart());
    }

    @Override
    public Object getAttribute(String name) {
        
        return get(name);
    }

    @Override
    public void setAttribute(Name name, Object value) {
        setAttribute(name.getLocalPart(), value);
    }

    @Override
    public void setAttribute(String name, Object value) {
        //TODO: check for geometry
        value = convertToMongo(value);
        set(name, value);
    }

    @Override
    public Object getAttribute(int index) throws IndexOutOfBoundsException {
        return getAttribute(key(index)); 
    }

    @Override
    public void setAttribute(int index, Object value) throws IndexOutOfBoundsException {
        setAttribute(key(index), value);
    }

    String key(int i) {
        return new ArrayList<String>(dbo.keySet()).get(i);
    }

    Object get(String path) {
        Object o = this.dbo;
        String[] p = path.split("\\.");
        for (int i = 0; i < p.length; i++) {
            if (o == null || !(o instanceof DBObject)) {
                break;
            }
            o = ((DBObject)o).get(p[i]);
        }

        return o;
    }

    void set(String path, Object obj) {
        DBObject dbo = this.dbo;
        String[] p = path.split("\\.");
        for(int i = 0; i < p.length-1; i++) {
            if (!dbo.containsField(p[i])) {
                dbo.put(p[i], new BasicDBObject());
            }
            dbo = (DBObject) dbo.get(p[i]);
        }

        dbo.put(p[p.length-1], obj);
    }

    Object convertToMongo(Object o) {
        if (o instanceof Geometry) {
            o =  mapper.toObject((Geometry)o);
        }
        return o;
    }

    @Override
    public int getAttributeCount() {
        return dbo.keySet().size();
    }

    @Override
    public List<Object> getAttributes() {
        List<Object> values = new ArrayList();
        for (String key : dbo.keySet()) {
            values.add(dbo.get(key));
        }
        return values;
    }

    @Override
    public void setAttributes(List<Object> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttributes(Object[] values) {
        throw new UnsupportedOperationException();
    }

    public Map<Object, Object> getUserData() {
        return Collections.emptyMap();
    }

    public GeometryAttribute getDefaultGeometryProperty() {
        throw new UnsupportedOperationException();
    }

    public void setDefaultGeometryProperty(GeometryAttribute defaultGeometry) {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties() {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties(Name name) {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties(String name) {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(Name name) {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    public Collection<?extends Property> getValue() {
        throw new UnsupportedOperationException();
    }

    public void setValue(Collection<Property> value) {
        throw new UnsupportedOperationException();
    }

    public AttributeDescriptor getDescriptor() {
        throw new UnsupportedOperationException();
    }

    public Name getName() {
        throw new UnsupportedOperationException();
    }

    public boolean isNillable() {
        throw new UnsupportedOperationException();
    }

    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }
    public void validate() {
    }

}
