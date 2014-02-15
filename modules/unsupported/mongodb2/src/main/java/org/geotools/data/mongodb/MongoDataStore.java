package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterCapabilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.spatial.BBOX;

public class MongoDataStore extends ContentDataStore {

    DB db;
    FilterCapabilities filterCapabilities;
    CollectionMapper defaultMapper;
    
    SimpleFeatureType featureType;

    public MongoDataStore(DB db) {
        this.db = db;
        filterCapabilities = createFilterCapabilties();
        defaultMapper = new GeoJSONMapper();
    }

    FilterCapabilities createFilterCapabilties() {
        FilterCapabilities capabilities = new FilterCapabilities();

        capabilities.addAll(FilterCapabilities.LOGICAL_OPENGIS);
        capabilities.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
        capabilities.addType(PropertyIsNull.class);
        capabilities.addType(PropertyIsBetween.class);
        capabilities.addType(BBOX.class);

        /*capabilities.addType(Id.class);
        capabilities.addType(IncludeFilter.class);
        capabilities.addType(ExcludeFilter.class);
        
        //temporal filters
        capabilities.addType(After.class);
        capabilities.addType(Before.class);
        capabilities.addType(Begins.class);
        capabilities.addType(BegunBy.class);
        capabilities.addType(During.class);
        capabilities.addType(Ends.class);
        capabilities.addType(EndedBy.class);*/

        return capabilities;
    }

    public DB getDb() {
        return db;
    }

    public FilterCapabilities getFilterCapabilities() {
        return filterCapabilities;
    }

    public CollectionMapper getDefaultMapper() {
        return defaultMapper;
    }

    public void setDefaultMapper(CollectionMapper defaultMapper) {
        this.defaultMapper = defaultMapper;
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {

        // Need to generate FeatureType instance with proper namespace URI
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);
        builder.setName(name(featureType.getTypeName()));
        featureType = builder.buildFeatureType();
        
        // Collection needs to exist so that it's returned with createTypeNames()
        db.createCollection(featureType.getTypeName(), new BasicDBObject());
       
        // Store FeatureType instance since it can't be inferred (no documents)
        ContentEntry entry = entry (featureType.getName());
        ContentState state = entry.getState(null);
        state.setFeatureType(featureType);
        
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        List<Name> typeNames = new ArrayList();
        for (String name : db.getCollectionNames()) {
            if (name.startsWith("system.")) {
                continue;
            }
            typeNames.add(name(name));
        }
        return typeNames;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new MongoFeatureStore(entry, null);
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction tx) throws IOException {
        if (tx != Transaction.AUTO_COMMIT) {
            throw new IllegalArgumentException("Transactions not currently supported");
        }
        return super.getFeatureWriter(typeName, filter, tx);
    }
}
