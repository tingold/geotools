/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.kml.bindings;

import javax.xml.namespace.QName;
import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.FolderStack;
import org.geotools.kml.KML;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.Binding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the type http://earth.google.com/kml/2.1:PlacemarkType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;complexType final="#all" name="PlacemarkType"&gt;
 *      &lt;complexContent&gt;
 *          &lt;extension base="kml:FeatureType"&gt;
 *              &lt;sequence&gt;
 *                  &lt;element minOccurs="0" ref="kml:Geometry"/&gt;
 *              &lt;/sequence&gt;
 *          &lt;/extension&gt;
 *      &lt;/complexContent&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 *
 *
 *
 * @source $URL$
 */
public class PlacemarkTypeBinding extends AbstractComplexBinding {
    /**
     * default feature type if no schema specified
     */
    static final SimpleFeatureType DefaultFeatureType;

    static {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        //TODO: use inheiretance when our feature model works
        tb.init(FeatureTypeBinding.FeatureType);
        tb.setName("placemark");

        //&lt;element minOccurs="0" ref="kml:Geometry"/&gt;
        tb.add("Geometry", Geometry.class);
        tb.setDefaultGeometry("Geometry");

        // contains the folder hierarchy seriazlied as a string
        tb.add("Folder", String.class);

        DefaultFeatureType = tb.buildFeatureType();
    }

    private final FolderStack folderStack;

    public PlacemarkTypeBinding(FolderStack folderStack) {
        this.folderStack = folderStack;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return KML.PlacemarkType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return SimpleFeature.class;
    }

    public int getExecutionMode() {
        return Binding.AFTER;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(DefaultFeatureType);

        SimpleFeature feature = (SimpleFeature) value;
        b.init(feature);

        //&lt;element minOccurs="0" ref="kml:Geometry"/&gt;
        b.set("Geometry", node.getChildValue(Geometry.class));

        b.set("Folder", folderStack.toString());

        return b.buildFeature(feature.getID());
    }
    
    public Object getProperty(Object object, QName name) throws Exception {
        SimpleFeature feature = (SimpleFeature) object;
        if ( KML.Geometry.equals( name ) ) {
            return feature.getDefaultGeometry();
        }
        
        return null;
    }
}
