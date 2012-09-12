package org.geotools.kml.bindings;

import javax.xml.namespace.QName;

import org.geotools.kml.Folder;
import org.geotools.kml.FolderStack;
import org.geotools.kml.v22.KML;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.Binding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.picocontainer.MutablePicoContainer;

public class FolderBinding extends AbstractComplexBinding {

    private final FolderStack folderStack;

    private final static String name = KML.name.getLocalPart();

    public FolderBinding(FolderStack folderStack) {
        this.folderStack = folderStack;
    }

    @Override
    public QName getTarget() {
        return KML.Folder;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getType() {
        return SimpleFeature.class;
    }

    @Override
    public int getExecutionMode() {
        return Binding.AFTER;
    }

    @Override
    public void initializeChildContext(ElementInstance childInstance, Node node,
            MutablePicoContainer context) {
        super.initializeChildContext(childInstance, node, context);
        String childName = childInstance.getName();
        if (childName.equals(name)) {
            folderStack.push(new Folder());
        }
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        folderStack.pop();
        return value;
    }

}
