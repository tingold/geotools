package org.geotools.data.mongodb;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;

public class MongoDataStoreFactory extends AbstractDataStoreFactory {

    public static final Param DATASTORE_URI = new Param("data_store", String.class, "MongoDB URI", true, "mongodb://localhost");
    public static final Param SCHEMASTORE_URI = new Param("schema_store", String.class, "Schema Store URI");
    
    @Override
    public String getDisplayName() {
        return "MongoDB";
    }
    
    @Override
    public String getDescription() {
        return "MongoDB database";
    }
    
    @Override
    public Param[] getParametersInfo() {
        return new Param[]{DATASTORE_URI, SCHEMASTORE_URI};
    }

    public DB connect(Map<String, Serializable> params) throws IOException {
        String dataStoreURI = (String) DATASTORE_URI.lookUp(params);
        String schemaStoreURI = (String) SCHEMASTORE_URI.lookUp(params);

        MongoClientURI mcURI = new MongoClientURI(dataStoreURI);
        MongoClient mc = new MongoClient(mcURI);
        // TODO:  this needs to change, no way to close connection gracefully
        return mc.getDB(mcURI.getDatabase());
    }

    @Override
    public MongoDataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return new MongoDataStore(connect(params));
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException();
    }

}
