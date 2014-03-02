package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
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

    DB db;
    FilterCapabilities filterCapabilities;
    CollectionMapper defaultMapper;
    
    final URI schemaStoreURI;

    public MongoDataStore(DB db) {
        this(db, null);
    }
    
    public MongoDataStore(DB db, URI schemaStoreRUI) {
        this.db = db;
        this.schemaStoreURI = schemaStoreRUI != null ? schemaStoreRUI :
                new File(System.getProperty("user.dir"), ".geotools/mongodb-schemas/").toURI();
        filterCapabilities = createFilterCapabilties();
        defaultMapper = new GeoJSONMapper();
        
        initializeSchemaStore();
    }
    
    private void initializeSchemaStore() {
        final String scheme = schemaStoreURI.getScheme();
        if ("file".equals(scheme)) {
            File file = new File(schemaStoreURI.getPath());
            file.mkdirs();
        } /* else if ("mongodb".equals(scheme)) {
            // TODO 
        } */ else {
            throw new IllegalArgumentException("Unsupported URI protocal for MongoDB DataStore schema storage");
        }
    }
    
    private SimpleFeatureType retreiveFeatureTypeFromSchemaStore(String name) throws IOException {
        File featureTypeFile = new File(schemaStoreURI.getPath(), name + ".json");
        if (!featureTypeFile.canRead()) {
            return null;
        }
        SimpleFeatureType featureType = null;
        BufferedReader reader = new BufferedReader(new FileReader(featureTypeFile));
        try {
            String lineSeparator = System.getProperty("line.separator");
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
                jsonBuilder.append(lineSeparator);
            }
            Object o = JSON.parse(jsonBuilder.toString());
            if (o instanceof DBObject) {
                SimpleFeatureTypeBuilder builder = FeatureTypeDBObject.convert((DBObject)o);
                if (builder != null) {
                    builder.setName(name(name));
                    featureType = builder.buildFeatureType();
                }
            }
        } finally {
            reader.close();
        }
        return featureType;
    }
    
    private void storeFeatureTypeToSchemaStore(SimpleFeatureType type) throws IOException {
        File featureTypeFile = new File(schemaStoreURI.getPath(), type.getTypeName() + ".json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(featureTypeFile));
        try {
            writer.write(JSON.serialize(FeatureTypeDBObject.convert(type)));
        } finally {
            writer.close();
        }
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
                ad.getUserData().put("mapping", "geometry");
                ad.getUserData().put("encoding", "GeoJSON");
            } else {
                ad.getUserData().put("mapping", "properties." + adName );
            }
        }
        
        // Collection needs to exist so that it's returned with createTypeNames()
        db.createCollection(incoming.getTypeName(), new BasicDBObject());
       
        // Store FeatureType instance since it can't be inferred (no documents)
        ContentEntry entry = entry (incoming.getName());
        ContentState state = entry.getState(null);
        state.setFeatureType(incoming);
        
        storeFeatureTypeToSchemaStore(incoming);
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
        ContentState state = entry.getState(null);
        SimpleFeatureType stateFeatureType = state.getFeatureType();
        if (stateFeatureType == null) {
            stateFeatureType = retreiveFeatureTypeFromSchemaStore(entry.getTypeName());
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
            SimpleFeatureType type = retreiveFeatureTypeFromSchemaStore(entry.getTypeName());
            if (type != null) {
                state.setFeatureType(type);
            }
        } catch (IOException ex) {
            Logger.getLogger(MongoDataStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return state;
    }
}
