package org.geotools.xml.impl;

import org.geotools.xml.Binding;
import org.geotools.xml.ComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.geotools.xml.impl.BindingWalker.Visitor;
import org.picocontainer.MutablePicoContainer;

public class ElementInitializer implements Visitor {

    private final ElementInstance instance;

    private final Node node;

    private final MutablePicoContainer context;

    public ElementInitializer(ElementInstance instance, Node node, MutablePicoContainer context) {
        this.instance = instance;
        this.node = node;
        this.context = context;
    }

    @Override
    public void visit(Binding binding) {
        if (binding instanceof ComplexBinding) {
            ComplexBinding cStrategy = (ComplexBinding) binding;
            cStrategy.initialize(instance, node, context);
        }

    }

}
