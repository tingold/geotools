package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.filter.FilterCapabilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

public class MongoDataStore extends ContentDataStore {

    DB db;
    FilterCapabilities filterCapabilities;
    CollectionMapper defaultMapper;

    public MongoDataStore(DB db) {
        this.db = db;
        filterCapabilities = createFilterCapabilties();
        defaultMapper = new AddHocMapper();
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
        db.createCollection(featureType.getTypeName(), new BasicDBObject());
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
    protected ContentFeatureSource createFeatureSource(ContentEntry entry)
            throws IOException {

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
