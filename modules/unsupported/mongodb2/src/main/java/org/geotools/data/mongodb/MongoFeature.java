package org.geotools.data.mongodb;

import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeatureType;

public class MongoFeature extends SimpleFeatureImpl {

    public MongoFeature(List<Object> values, SimpleFeatureType featureType, String id, 
        Map<String,Integer> attLookup) {
        super(values.toArray(new Object[values.size()]), featureType, new FeatureIdImpl(id), false);
        this.index = attLookup;
    }

}
