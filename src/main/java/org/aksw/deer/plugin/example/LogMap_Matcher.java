package org.aksw.deer.plugin.example;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.krr.logmap2.LogMap2_Matcher;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class LogMap_Matcher {

	// Matching Ontologies using LogMap and sending the matched Model back to caller
	public Model UsingLogMapMatcher(String file1, String file2, int a, String endpoint1, String endpoint2)
			throws OWLOntologyCreationException {

		// Log Map variables
		OWLOntology onto1;
		OWLOntology onto2;

		OWLOntologyManager onto_manager;

		// String onto1_iri = "lov_linkeddata_es_dataset_lov.nt";
		String onto1_iri = file1;
		String onto2_iri = file2;

		
		LogMap2_Matcher logmap2;

		// try {

		onto_manager = OWLManager.createOWLOntologyManager();
		MissingImportHandlingStrategy silent = MissingImportHandlingStrategy.SILENT;
		OWLOntologyLoaderConfiguration setMissingImportHandlingStrategy = onto_manager.getOntologyLoaderConfiguration()
				.setMissingImportHandlingStrategy(silent);
		onto_manager.setOntologyLoaderConfiguration(setMissingImportHandlingStrategy);
		OWLOntologyManager onto_manager1 = OWLManager.createOWLOntologyManager();
		MissingImportHandlingStrategy silent1 = MissingImportHandlingStrategy.SILENT;
		OWLOntologyLoaderConfiguration setMissingImportHandlingStrategy1 = onto_manager1
				.getOntologyLoaderConfiguration().setMissingImportHandlingStrategy(silent1);
		onto_manager1.setOntologyLoaderConfiguration(setMissingImportHandlingStrategy1);

		onto1 = onto_manager.loadOntologyFromOntologyDocument(new File(onto1_iri));
		onto2 = onto_manager1.loadOntologyFromOntologyDocument(new File(onto2_iri));
		// Call to logMap system
		logmap2 = new LogMap2_Matcher(onto1, onto2, false);

		// Set of mappings computed my LogMap
		Set<MappingObjectStr> logmap2_mappings = logmap2.getOverEstimationOfMappings();
		// Set<MappingObjectStr> logmap2_mappings = logmap2.getLogmap2_Mappings();
		Iterator<MappingObjectStr> iterator = logmap2_mappings.iterator();

		// Adding Model
		Model model = ModelFactory.createDefaultModel();

		// Variable for tracking Type of Mapping
		int typeOfMapping = -1;

		String deer = "https://w3id.org/deer/";

		// Returns elements of the LogMap
		while (iterator.hasNext()) {
			// Generates IRIs of matched classes
			// Structuralconfidence, Lexicalconfidence and confidence values
			// if properties are matched
			MappingObjectStr next = iterator.next();
			if (next.getTypeOfMapping() == a) {
				typeOfMapping = next.getTypeOfMapping();

				// setting prefix for model
				model.setNsPrefix("deer", deer);
				// Output format
				System.out.println("---------------output format-------------------");

				final Resource matchResource = model.createResource(deer + "Match");
				final Property matchProperty = model.createProperty(deer, "found");

				Resource resource = model.createResource(next.getIRIStrEnt1());
				// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
				Property related = model.createProperty(deer, "matchesWith");
				Resource resource2 = model.createResource(next.getIRIStrEnt2());
				// confidence
				// Property confProp = model.createProperty("confidence");
				Property confProp = model.createProperty(deer, "confidenceValue");
				double confidence2 = next.getConfidence();
				Literal confidence = model.createLiteral(String.valueOf(confidence2));

				Property spEP1 = model.createProperty(deer, "SubjectEndPoint");
				Resource sparqlEndPoint1 = model.createResource(endpoint1);

				Property spEP2 = model.createProperty(deer, "ObjectEndPoint");
				Resource sparqlEndPoint2 = model.createResource(endpoint2);
				Statement stmt2 = model.createStatement(resource, related, resource2);

				ReifiedStatement createReifiedStatement = model.createReifiedStatement(stmt2);
				createReifiedStatement.addProperty(confProp, confidence);
				createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
				createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
				// createReifiedStatement.addProperty(dataProp, dataPropMap);
				// createReifiedStatement.addProperty(objectProp, objectPropMap);

				model.add(matchResource, matchProperty, createReifiedStatement);
			}
		}
		return model;
	}

}
