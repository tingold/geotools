package org.geotools.data.mongodb;

import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class MongoAppendFeatureWriter implements SimpleFeatureWriter {

    DBCollection collection;
    SimpleFeatureType featureType;

    CollectionMapper mapper;
    MongoWriteFeature current;

    public MongoAppendFeatureWriter(DBCollection collection, SimpleFeatureType featureType, 
        MongoFeatureStore featureStore) {
        this.collection = collection;
        this.featureType = featureType;
        mapper = featureStore.getMapper();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }
    
    @Override
    public boolean hasNext() throws IOException {
        return false;
    }

    @Override
    public SimpleFeature next() throws IOException {
        return current = new MongoWriteFeature(new BasicDBObject(), featureType, mapper);
    }
    
    @Override
    public void write() throws IOException {
        if (current == null) {
            throw new IllegalStateException("No current feature, must call next() before write()");
        }

        collection.save(current.getObject());
    }

    @Override
    public void remove() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    
    }

}
