package org.geotools.data.mongodb;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.Parameter;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class MongoDataStoreFactory extends AbstractDataStoreFactory {

    public static final Param HOST = new Param("host", String.class, "Host", true, "localhost");
    public static final Param PORT = new Param("port", Integer.class, "Port", true, 27017);
    public static final Param USER = new Param("user", String.class, "User", false);
    public static final Param PASSWD = new Param("passwd", String.class, "Password", false, null, 
        Collections.singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));
    public static final Param DATABASE = new Param("database", String.class, "Database", true);
    
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
        return new Param[]{HOST, PORT, DATABASE, USER, PASSWD};
    }

    public DB connect(Map<String, Serializable> params) throws IOException {
        String user = (String) USER.lookUp(params);
        String passwd = (String) PASSWD.lookUp(params);
        String host = (String) HOST.lookUp(params);
        Integer port = (Integer) PORT.lookUp(params);
        String dbname = (String) DATABASE.lookUp(params);

        String uri = String.format("mongodb://%s:%d", host, port);
        Mongo m = new Mongo(new MongoURI(uri));

        DB db = m.getDB(dbname);
        if (user != null && passwd != null && !db.authenticate(user, passwd.toCharArray())) {
            throw new IOException(
                String.format("Authentication of user %s failed against database %s", user, dbname));
        }
        return db;
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
