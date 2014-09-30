package org.neo4j.ogm.metadata;

import java.util.Arrays;

import org.graphaware.graphmodel.neo4j.EdgeModel;
import org.graphaware.graphmodel.neo4j.NodeModel;

/**
 * Implementation of {@link ObjectFactory} that exclusively uses a class' default constructor for instantiation.
 */
public class DefaultConstructorObjectFactory implements ObjectFactory {

    private final ClassDictionary classDictionary;

    /**
     * Constructs a new {@link DefaultConstructorObjectFactory} that uses the given {@link ClassDictionary} to figure out which
     * types to instantiate.
     *
     * @param classDictionary The {@link ClassDictionary} that configures this {@link ObjectFactory}
     */
    public DefaultConstructorObjectFactory(ClassDictionary classDictionary) {
        this.classDictionary = classDictionary;
    }

    @Override
    public <T> T instantiateObjectMappedTo(NodeModel nodeModel) {
        return instantiateObjectFromTaxa(nodeModel.getLabels());
    }

    @Override
    public <T> T instantiateObjectMappedTo(EdgeModel edgeModel) {
        return instantiateObjectFromTaxa(edgeModel.getType());
    }

    private <T> T instantiateObjectFromTaxa(String... taxa) {
        if (taxa.length == 0) {
            throw new MappingException("Cannot map to a class with no taxa by which to determine the class name.");
        }

        String fqn = this.classDictionary.determineBaseClass(Arrays.asList(taxa));
        try {
            @SuppressWarnings("unchecked")
            Class<T> className = (Class<T>) Class.forName(fqn);
            return className.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new MappingException("Unable to instantiate class: " + fqn, e);
        }
    }

}