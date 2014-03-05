/*
 * 
 */
package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoUtil {
    
    public static Object getDBOValue(DBObject dbo, String path) {
        return getDBOValue(dbo, Arrays.asList(path.split("\\.")).iterator());
    }
    
    public static Object getDBOValue(DBObject dbo, Iterator<String> path) {
        return getDBOValueInternal(path, dbo);
    }
    
    private static Object getDBOValueInternal(Iterator<String> path, Object current) {
        if (path.hasNext()) {
            if (current instanceof DBObject) {
                String key = path.next();
                Object value = ((DBObject)current).get(key);
                return getDBOValueInternal(path, value);
            }
            return null;
        } else {
            return current;
        }
    }
    
    public static void setDBOValue(DBObject dbo, String path, Object value) {
        setDBOValue(dbo, Arrays.asList(path.split("\\.")).iterator(), value);
    }
    
    public static void setDBOValue(DBObject dbo, Iterator<String> path, Object value) {
        setDBOValueInternal(dbo, path, value);
    }
    
    private static void setDBOValueInternal(DBObject currentDBO, Iterator<String> path, Object value) {
        String key = path.next();
        if (path.hasNext()) {
            Object next = currentDBO.get(key);
            DBObject nextDBO;
            if (next instanceof DBObject) {
                nextDBO = (DBObject)next;
            } else {
                currentDBO.put(key, nextDBO = new BasicDBObject());
            }
            setDBOValueInternal(nextDBO, path, value);
        } else {
            currentDBO.put(key, value);
        }
    }
    
    public static Set<String> findIndexedGeometries(DBCollection dbc) {
        return findIndexedProperties(dbc, "2dsphere");
    } 
    
    public static Set<String> findIndexedProperties(DBCollection dbc, String type) {
        Set<String> properties = new LinkedHashSet<String>();
        List<DBObject> indices = dbc.getIndexInfo();
        for (DBObject index : indices) {
            Object key = index.get("key");
            if (key instanceof DBObject) {
                for (Map.Entry entry : ((Map<?,?>)((DBObject)key).toMap()).entrySet()) {
                    if (type == null || type.equals(entry.getValue())) {
                        properties.add(entry.getKey().toString());
                    }
                }
            }
        }        
        return properties;
    }
    
    public static Class<?> mapBSONObjectToJavaType(Object o) {
        if (o instanceof String || 
            o instanceof Double ||
            o instanceof Long ||
            o instanceof Integer ||
            o instanceof Boolean || 
            o instanceof Date) {
            return o.getClass();
        }
        return null;
    } 
}
