/*
 * 
 */
package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import org.bson.types.BSONTimestamp;

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
    
    /**
     *  For attribute type binding... 
     * @param o
     * @return binding to be use for attribute type
     */
    public static Class<?> mapBSONObjectToJavaType(Object o) {
        if (o instanceof String || 
            o instanceof Double ||
            o instanceof Long ||
            o instanceof Integer ||
            o instanceof Boolean || 
            o instanceof Date) {
            return o.getClass();
        } else if (o instanceof BSONTimestamp) {
            return Date.class;
        }
        return null;
    } 
}
