/*
 * 
 */

package org.geotools.data.mongodb;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoSchemaFileStore implements MongoSchemaStore {
   
    static final String SUFFIX_json = ".json";
    
    final File schemaStoreFile;

    public MongoSchemaFileStore(String uri) throws URISyntaxException {
        this(new URI(uri));
    }
    
    public MongoSchemaFileStore(URI uri) {
        schemaStoreFile = new File(uri);
    }
    
    @Override
    public void storeSchema(SimpleFeatureType schema) throws IOException {
        File schemaFile = schemaFile(schema.getTypeName());
        BufferedWriter writer = new BufferedWriter(new FileWriter(schemaFile));
        try {
            writer.write(JSON.serialize(FeatureTypeDBObject.convert(schema)));
        } finally {
            writer.close();
        }
    }

    @Override
    public SimpleFeatureType retrieveSchema(Name name) throws IOException {
        File schemaFile = schemaFile(name);
        if (!schemaFile.canRead()) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new FileReader(schemaFile));
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
                return FeatureTypeDBObject.convert((DBObject)o, name);
            }
        } finally {
            reader.close();
        }
        return null;
    }

    @Override
    public void deleteSchema(Name name) throws IOException {
        schemaFile(name).delete();
    }

    @Override
    public List<String> typeNames() {
        List<String> typeNames = new ArrayList<String>();
        for (File schemaFile : schemaStoreFile.listFiles(new SchemaFilter())) {
            typeNames.add(typeName(schemaFile));
        }
        return typeNames;
    }
    
    static String typeName(File schemaFile) {
        String typeName = schemaFile.getName();
        return typeName.substring(0, typeName.length() - SUFFIX_json.length());
    }
    
    File schemaFile(Name name) {
        return schemaFile(name.getLocalPart());
    }
    
    File schemaFile(String typeName) {
        return new File(schemaStoreFile, typeName + SUFFIX_json);
    }
    
    private static class SchemaFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().endsWith(SUFFIX_json);
        }
    }
}
