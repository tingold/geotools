package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterCapabilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MongoDataStore extends ContentDataStore {
    
    final static String KEY_mapping = "mapping";
    final static String KEY_encoding = "encoding";
    final static String KEY_collection = "collection";

    final MongoSchemaStore schemaStore;
    
    final MongoClient dataStoreClient;
    final DB dataStoreDB;
    
    FilterCapabilities filterCapabilities;
    CollectionMapper defaultMapper;
    
    public MongoDataStore(String dataStoreURI) {
        this(dataStoreURI, null);
    }
    
    public MongoDataStore(String dataStoreURI, String schemaStoreURI) {
        MongoClientURI dataStoreClientURI = createMongoClientURI(dataStoreURI);
        dataStoreClient = createMongoClient(dataStoreClientURI);
        dataStoreDB = createDB(dataStoreClient, dataStoreClientURI.getDatabase(), true);
        if (dataStoreDB == null) {
            dataStoreClient.close(); // This smells bad...
            throw new IllegalArgumentException("Unknown mongodb database, \"" + dataStoreClientURI.getDatabase() + "\"");
        }
        
        schemaStore = createSchemaStore(schemaStoreURI);
        
        filterCapabilities = createFilterCapabilties();
        defaultMapper = new GeoJSONMapper();
    }
    
    final MongoClientURI createMongoClientURI(String dataStoreURI) {
        if (dataStoreURI == null) {
            throw new IllegalArgumentException("dataStoreURI may not be null");
        }
        if (!dataStoreURI.startsWith("mongodb://")) {
            throw new IllegalArgumentException("incorrect scheme for URI, expected to begin with \"mongodb://\", found URI of \"" + dataStoreURI + "\"");
        }
        return new MongoClientURI(dataStoreURI.toString());
    }
        
    final MongoClient createMongoClient(MongoClientURI mongoClientURI) {
        try {
            return new MongoClient(mongoClientURI);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown mongodb host(s)", e);
        }
    }
    
    final DB createDB(MongoClient mongoClient, String databaseName, boolean databaseMustExist) {
        if (databaseMustExist & !mongoClient.getDatabaseNames().contains(databaseName)) {
            return null;
        }
        return mongoClient.getDB(databaseName);
    }
    
    private MongoSchemaStore createSchemaStore(String schemaStoreURI) {
        if (schemaStoreURI.startsWith("file:")) {
            try {
                return new MongoSchemaFileStore(schemaStoreURI);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Unable to create file-based schema store with URI \"" + schemaStoreURI + "\"", e);
            }
        } else if (schemaStoreURI.startsWith("mongodb://")) {
            throw new UnsupportedOperationException("mongodb-based schema store not implemented yet.");
        } else {
            throw new IllegalArgumentException("Unsupported URI protocol for MongoDB schema store");
        }
    }

    final FilterCapabilities createFilterCapabilties() {
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
        return dataStoreDB;
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
    public void createSchema(SimpleFeatureType incoming) throws IOException {

        CoordinateReferenceSystem incomingCRS = incoming.getCoordinateReferenceSystem();
        if (incomingCRS == null) {
            incoming.getGeometryDescriptor().getCoordinateReferenceSystem();
        }
        if (!CRS.equalsIgnoreMetadata(incomingCRS, DefaultGeographicCRS.WGS84)) {
            throw new IllegalArgumentException("Unsupported coordinate reference system, only WGS84 supported");
        }
        // Need to generate FeatureType instance with proper namespace URI
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(incoming);
        builder.setName(name(incoming.getTypeName()));
        incoming = builder.buildFeatureType();
        
        String gdName = incoming.getGeometryDescriptor().getLocalName();
        for (AttributeDescriptor ad : incoming.getAttributeDescriptors()) {
            String adName = ad.getLocalName();
            if (gdName.equals(adName)) {
                ad.getUserData().put(KEY_mapping, "geometry");
                ad.getUserData().put(KEY_encoding, "GeoJSON");
            } else {
                ad.getUserData().put(KEY_mapping, "properties." + adName );
            }
        }
        
        // Collection needs to exist so that it's returned with createTypeNames()
        getDb().createCollection(incoming.getTypeName(), new BasicDBObject());
       
        // Store FeatureType instance since it can't be inferred (no documents)
        ContentEntry entry = entry (incoming.getName());
        ContentState state = entry.getState(null);
        state.setFeatureType(incoming);
        
        schemaStore.storeSchema(incoming);
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        List<Name> typeNames = new ArrayList();
        for (String name : getDb().getCollectionNames()) {
            if (name.startsWith("system.")) {
                continue;
            }
            typeNames.add(name(name));
        }
        return typeNames;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        ContentState state = entry.getState(null);
        SimpleFeatureType stateFeatureType = state.getFeatureType();
        if (stateFeatureType == null) {
            stateFeatureType = schemaStore.retrieveSchema(entry.getName());
            if (stateFeatureType != null) {
                state.setFeatureType(stateFeatureType);
            }
        }
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

    @Override
    protected ContentState createContentState(ContentEntry entry) {
        ContentState state = super.createContentState(entry);
        try {
            SimpleFeatureType type = schemaStore.retrieveSchema(entry.getName());
            if (type != null) {
                state.setFeatureType(type);
            }
        } catch (IOException ex) {
            Logger.getLogger(MongoDataStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return state;
    }
    
    @Override
    public void dispose() {
        dataStoreClient.close();
        super.dispose();
    }
}
