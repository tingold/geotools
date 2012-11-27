package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.filter.FilterCapabilities;
import org.opengis.feature.type.Name;
import org.opengis.filter.ExcludeFilter;
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

import com.mongodb.DB;

public class MongoDataStore extends ContentDataStore {

    DB db;
    CollectionMapper mapper;
    FilterCapabilities filterCapabilities;

    public MongoDataStore(DB db, CollectionMapper mapper) {
        this.db = db;
        this.mapper = mapper;
        filterCapabilities = createFilterCapabilties();
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

    public CollectionMapper getMapper() {
        return mapper;
    }

    public void setMapper(CollectionMapper mapper) {
        this.mapper = mapper;
    }

    public FilterCapabilities getFilterCapabilities() {
        return filterCapabilities;
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
        return new MongoFeatureSource(entry, null);
    }

}
