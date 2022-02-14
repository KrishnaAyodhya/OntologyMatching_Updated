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

/**
 * 
 * @author Krishna Madhav and Sowmya Kamath Ramesh
 *
 */
public class LogMap_Matcher {

	
	final String deer = "https://w3id.org/deer/";
	
	/**
	 	Matching Ontologies using LogMap and sending the Matched Ontologies Model back to caller.
 		This method is used for the output we get from Limes for KG Matching.
	 
	 * @param file1  	(Source File)
	 * @param file2  	(Target File)
	 * @param a      	(Type of Mapping)
	 * @param endpoint1 (Source endpoint)
	 * @param endpoint2 (Target endpoint)
	 * @return
	 * @throws OWLOntologyCreationException
	 */
	public Model usingLogMapMatcher(String file1, String file2, int a, String endpoint1, String endpoint2)
			throws OWLOntologyCreationException {

		// Log Map variables
		OWLOntology onto1;
		OWLOntology onto2;

		OWLOntologyManager onto_manager;

		String onto1_iri = file1;
		String onto2_iri = file2;

		LogMap2_Matcher logmap2;

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

		Iterator<MappingObjectStr> iterator = logmap2_mappings.iterator();

		// Adding Model
		Model model = ModelFactory.createDefaultModel();

		// Variable for tracking Type of Mapping
		int typeOfMapping = -1;

		

		// Reification
		while (iterator.hasNext()) {
			
			MappingObjectStr next = iterator.next();
			if (next.getTypeOfMapping() == a) {
				typeOfMapping = next.getTypeOfMapping();

				// setting prefix for model
				model.setNsPrefix("deer", deer);

				final Resource matchResource = model.createResource(deer + OntologyConstants.MATCH);
				final Property matchProperty = model.createProperty(deer, OntologyConstants.FOUND);

				Resource resource = model.createResource(next.getIRIStrEnt1());
		
				Property related = model.createProperty(deer, OntologyConstants.MATCHES_WITH);
				Resource resource2 = model.createResource(next.getIRIStrEnt2());
			
				// confidence
				Property confProp = model.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE); 
				double confidence2 = next.getConfidence();
				Literal confidence = model.createLiteral(String.valueOf(confidence2));

				Property spEP1 = model.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
				Resource sparqlEndPoint1 = model.createResource(endpoint1);

				Property spEP2 = model.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
				Resource sparqlEndPoint2 = model.createResource(endpoint2);
				Statement stmt2 = model.createStatement(resource, related, resource2);

				ReifiedStatement createReifiedStatement = model.createReifiedStatement(stmt2);
				createReifiedStatement.addProperty(confProp, confidence);
				createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
				createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
				
				model.add(matchResource, matchProperty, createReifiedStatement);
			}
		}
		return model;
	}

	
	/*
	 * This is part of Future Work. 
	 */
	
	/**
	 * This method is invoked for any approach except "Limes" as there is no
	 * Endpoints for the outputs given by KG Matching
	 * @param file1 (Source File)
	 * @param file2 (Target File)
	 * @param a     (Type of Mapping)
	 * @return
	 * @throws OWLOntologyCreationException
	 */
	public Model usingLogMapMatcher_2(String file1, String file2, int a) throws OWLOntologyCreationException {

		// Log Map variables
		OWLOntology onto1;
		OWLOntology onto2;

		OWLOntologyManager onto_manager;

		String onto1_iri = file1;
		String onto2_iri = file2;

		LogMap2_Matcher logmap2;

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

		Iterator<MappingObjectStr> iterator = logmap2_mappings.iterator();

		// Adding Model
		Model model = ModelFactory.createDefaultModel();

		// Returns elements of the LogMap
		while (iterator.hasNext()) {
			
			MappingObjectStr next = iterator.next();
			if (next.getTypeOfMapping() == a) {
				
				// setting prefix for model
				model.setNsPrefix("deer", deer);
				

				final Resource matchResource = model.createResource(deer + OntologyConstants.MATCH);
				final Property matchProperty = model.createProperty(deer, OntologyConstants.FOUND);

				Resource resource = model.createResource(next.getIRIStrEnt1());
				// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
				Property related = model.createProperty(deer, OntologyConstants.MATCHES_WITH);
				Resource resource2 = model.createResource(next.getIRIStrEnt2());
				// confidence
				Property confProp = model.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
				double confidence2 = next.getConfidence();
				Literal confidence = model.createLiteral(String.valueOf(confidence2));

				Statement stmt2 = model.createStatement(resource, related, resource2);

				ReifiedStatement createReifiedStatement = model.createReifiedStatement(stmt2);
				createReifiedStatement.addProperty(confProp, confidence);

				model.add(matchResource, matchProperty, createReifiedStatement);
			}
		}
		return model;
	}

}
