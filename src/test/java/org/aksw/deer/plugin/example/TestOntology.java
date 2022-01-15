package org.aksw.deer.plugin.example;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import org.aksw.deer.Deer;
import org.aksw.faraday_cage.engine.CompiledExecutionGraph;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Test;
import org.pf4j.DefaultPluginManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class TestOntology {

	@Test
	public void testParameterless() throws OWLOntologyCreationException {

		OntologyMatchingOperator onto = new OntologyMatchingOperator();

		onto.initPluginId(ResourceFactory.createResource("urn:test-operator"));

		ValidatableParameterMap params = onto.createParameterMap().init();
		onto.initParameters(params);

		List<Model> res = onto.safeApply(List.of(ModelFactory.createDefaultModel()));

	}

	@Test
	public void testConfiguration() {
		String url = Objects
				.requireNonNull(TestOntology.class.getClassLoader().getResource("configuration.ttl"))
				.toExternalForm();
		Model configurationModel = ModelFactory.createDefaultModel().read(url);
		CompiledExecutionGraph executionGraph = Deer.getExecutionContext(new DefaultPluginManager())
				.compile(configurationModel);
		executionGraph.run();
		executionGraph.join();
	}

}
