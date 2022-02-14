package org.aksw.deer.plugin.example;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import cn.ac.amss.semanticweb.alignment.Mapping;
import cn.ac.amss.semanticweb.alignment.MappingCell;
import cn.ac.amss.semanticweb.matching.LexicalMatcher;
import cn.ac.amss.semanticweb.matching.MatcherFactory;
import cn.ac.amss.semanticweb.matching.StructuralMatcher;
import cn.ac.amss.semanticweb.model.ModelStorage;

/**
 * 
 * @author Krishna Madhav and Sowmya Kamath Ramesh
 * This class contains implementation of FCA Map
 */
public class FCA_Matcher {
	final String deer = "https://w3id.org/deer/";

	public List<Model> fcaInvoker(String file1, String file2, String subjectEndpoint, String objectEndpoint, int fileCounter,
			String typeOfMap) throws IOException {
		
		ModelStorage source = new ModelStorage(file1);
		ModelStorage target = new ModelStorage(file2);

		//************************** Lexical-level Matching ***************************/

		LexicalMatcher lexicalMatcher = MatcherFactory.createLexicalMatcher();
		Mapping lexicalOntClassMappings = new Mapping();

		lexicalMatcher.setSourceTarget(source, target);
		lexicalMatcher.setExtractType(true, true);
		lexicalMatcher.mapOntClasses(lexicalOntClassMappings);

		//************************* Structural-level Matching *************************/
		StructuralMatcher structuralMatcher = MatcherFactory.createStructuralMatcher();
		structuralMatcher.setSourceTarget(source, target);
		structuralMatcher.setExtractType(true, true);
		structuralMatcher.addCommonPredicate(RDFS.subClassOf);
		structuralMatcher.addCommonPredicate(OWL.disjointWith);
		structuralMatcher.addAllSubjectAnchors(lexicalOntClassMappings);
		structuralMatcher.addAllObjectAnchors(lexicalOntClassMappings);

		// Storing ArrayList of Matched Models
		List<Model> matchedModel = new ArrayList<Model>();
		
		
		// Choosing Type of Map
		switch (typeOfMap) {
		
		case "Classes":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher,subjectEndpoint,objectEndpoint);
			
			break;

		case "dataProperty":
			matchedModel = dataProperty(structuralMatcher, lexicalMatcher,subjectEndpoint,objectEndpoint);
			
			break;

		case "objectProperty":
			matchedModel = objectPropertyMatching(structuralMatcher, lexicalMatcher,subjectEndpoint,objectEndpoint);
			
			break;

		case "Classes and dataProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher,subjectEndpoint,objectEndpoint);
			matchedModel.addAll(dataProperty(structuralMatcher, lexicalMatcher,subjectEndpoint,objectEndpoint));
			
			break;
			
		case "Classes and objectProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher,subjectEndpoint,objectEndpoint);
			matchedModel.addAll(objectPropertyMatching(structuralMatcher, lexicalMatcher, subjectEndpoint, objectEndpoint));
			
			break;
			
		case "Classes and dataProperty and objectProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher,subjectEndpoint,objectEndpoint);
			matchedModel.addAll(dataProperty(structuralMatcher, lexicalMatcher,subjectEndpoint,objectEndpoint));
			matchedModel.addAll(objectPropertyMatching(structuralMatcher, lexicalMatcher, subjectEndpoint, objectEndpoint));
			
			break;

		default:
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher,subjectEndpoint,objectEndpoint);
			
			break;
		}
		
		source.clear();
		target.clear();
		return matchedModel;
	}

	
	/**
	 * Model created are for lexical and structural Class Matching
	 * @param lexicalOntClassMappings
	 * @param structuralMatcher
	 * @param subjectEndpoint
	 * @param objectEndpoint
	 * @return
	 */
	
	public List<Model> classMatching(Mapping lexicalOntClassMappings, StructuralMatcher structuralMatcher, String subjectEndpoint, String objectEndpoint) {
		List<Model> classMatching = new ArrayList<Model>();

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator = lexicalOntClassMappings.iterator();
		
		// Reification
		while (iterrator.hasNext()) {
			MappingCell next = iterrator.next();

			// setting prefix for model
			model1.setNsPrefix("deer", deer);			
			
			final Resource matchResource = model1.createResource(deer + OntologyConstants.MATCH );
			final Property matchProperty = model1.createProperty(deer, OntologyConstants.FOUND);
			double confidence2 = next.getMeasure();

			Resource resource = model1.createResource(next.getEntity1());
			
			Property related = model1.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model1.createResource(next.getEntity2());
			
			// confidence
			Property confProp = model1.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			
			Property spEP1 = model1.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model1.createResource(subjectEndpoint);

			Property spEP2 = model1.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model1.createResource(objectEndpoint);

			Statement stmt2 = model1.createStatement(resource, related, resource2);
			
			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
			createReifiedStatement.addProperty(confProp, confidence);

			model1.add(matchResource, matchProperty, createReifiedStatement);
			classMatching.add(model1);
		
		}
		

		Mapping structuralOntClassMappings = new Mapping();
		structuralMatcher.mapOntClasses(structuralOntClassMappings);

		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator3 = structuralOntClassMappings.iterator();
		
		// Reification 
		while (iterrator3.hasNext()) {
			MappingCell next = iterrator3.next();

			// setting prefix for model
			model2.setNsPrefix("deer", deer);
			
			final Resource matchResource = model2.createResource(deer + OntologyConstants.MATCH );
			final Property matchProperty = model2.createProperty(deer, OntologyConstants.FOUND);

			Resource resource = model2.createResource(next.getEntity1());
			
			Property related = model2.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model2.createResource(next.getEntity2());
			
			Property confProp = model2.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));
			
			Property spEP1 = model2.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model2.createResource(subjectEndpoint);

			Property spEP2 = model2.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model2.createResource(objectEndpoint);
			
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
			
			model2.add(matchResource, matchProperty, createReifiedStatement);
			classMatching.add(model2);
			
		}
		
		return classMatching;

	}


	/**
	 * Model created are for lexical and structural Object Property Matching
	 * @param structuralMatcher
	 * @param lexicalMatcher
	 * @param subjectEndpoint
	 * @param objectEndpoint
	 * @return
	 */
	public List<Model> objectPropertyMatching(StructuralMatcher structuralMatcher, LexicalMatcher lexicalMatcher, String subjectEndpoint, String objectEndpoint) {

		List<Model> objectPropertyModel = new ArrayList<Model>();
		Mapping lexicalObjectPropertyMappings = new Mapping();
		lexicalMatcher.mapObjectProperties(lexicalObjectPropertyMappings);

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator1 = lexicalObjectPropertyMappings.iterator();
		
		// Reification
		while (iterrator1.hasNext()) {
			
			MappingCell next = iterrator1.next();
			
			// setting prefix for model
			model1.setNsPrefix("deer", deer);
			
			final Resource matchResource = model1.createResource(deer + OntologyConstants.MATCH);
			final Property matchProperty = model1.createProperty(deer, OntologyConstants.FOUND);

			Resource resource = model1.createResource(next.getEntity1());
			
			Property related = model1.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model1.createResource(next.getEntity2());
			Property confProp = model1.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			double confidence2 = next.getMeasure();
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			
			Property spEP1 = model1.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model1.createResource(subjectEndpoint);

			Property spEP2 = model1.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model1.createResource(objectEndpoint);
			
			Statement stmt2 = model1.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
			model1.add(matchResource, matchProperty, createReifiedStatement);
			
			objectPropertyModel.add(model1);
		}

		// Structural Mappings
		Mapping structuralDataTypeMappings = new Mapping();
		structuralMatcher.mapDataTypeProperties(structuralDataTypeMappings);

		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator4 = structuralDataTypeMappings.iterator();
		
		// Reification
		while (iterrator4.hasNext()) {
			MappingCell next = iterrator4.next();
			
			// setting prefix for model
			model2.setNsPrefix("deer", deer);
			
			final Resource matchResource = model2.createResource(deer + OntologyConstants.MATCH );
			final Property matchProperty = model2.createProperty(deer, OntologyConstants.FOUND);

			Resource resource = model2.createResource(next.getEntity1());

			Property related = model2.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model2.createResource(next.getEntity2());
			// confidence

			Property confProp = model2.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));
			
			Property spEP1 = model2.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model2.createResource(subjectEndpoint);

			Property spEP2 = model2.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model2.createResource(objectEndpoint);
			
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);

			model2.add(matchResource, matchProperty, createReifiedStatement);
			objectPropertyModel.add(model2);
			
		}
		
		return objectPropertyModel;

	}

	/**
	 * Model created are for lexical and structural Data Propert Matching
	 * @param structuralMatcher
	 * @param lexicalMatcher
	 * @param subjectEndpoint
	 * @param objectEndpoint
	 * @return
	 */
	public List<Model> dataProperty(StructuralMatcher structuralMatcher, LexicalMatcher lexicalMatcher, String subjectEndpoint, String objectEndpoint) {

		List<Model> dataPropertyModel = new ArrayList<Model>();
		Mapping lexicalDataPropertyMappings = new Mapping();
		lexicalMatcher.mapDataTypeProperties(lexicalDataPropertyMappings);

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator2 = lexicalDataPropertyMappings.iterator();
		
		while (iterrator2.hasNext()) {
			MappingCell next = iterrator2.next();
			
			// setting prefix for model
			model1.setNsPrefix("deer", deer);
			
			final Resource matchResource = model1.createResource(deer + OntologyConstants.MATCH);
			final Property matchProperty = model1.createProperty(deer, OntologyConstants.FOUND);
		
			Resource resource = model1.createResource(next.getEntity1());
			
			Property related = model1.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model1.createResource(next.getEntity2());
		
			// confidence
			Property confProp = model1.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			double confidence2 = next.getMeasure();
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			
			Property spEP1 = model1.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model1.createResource(subjectEndpoint);

			Property spEP2 = model1.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model1.createResource(objectEndpoint);
			
			Statement stmt2 = model1.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);

			model1.add(matchResource, matchProperty, createReifiedStatement);
			dataPropertyModel.add(model1);
			
		}
		
		Mapping structuralObjectTypeMappings = new Mapping();
		structuralMatcher.mapObjectProperties(structuralObjectTypeMappings);
		
		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator5 = structuralObjectTypeMappings.iterator();
		
		// Reification
		while (iterrator5.hasNext()) {
			
			MappingCell next = iterrator5.next();
			
			// setting prefix for model
			model2.setNsPrefix("deer", deer);
			
			final Resource matchResource = model2.createResource(deer + OntologyConstants.MATCH );
			final Property matchProperty = model2.createProperty(deer, OntologyConstants.FOUND);
			
			Resource resource = model2.createResource(next.getEntity1());
			
			Property related = model2.createProperty(deer, OntologyConstants.MATCHES_WITH);
			Resource resource2 = model2.createResource(next.getEntity2());
			Property confProp = model2.createProperty(deer, OntologyConstants.CONFIDENCE_VALUE);
			
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));

			Property spEP1 = model2.createProperty(deer, OntologyConstants.SUBJECT_ENDPOINT);
			Resource sparqlEndPoint1 = model2.createResource(subjectEndpoint);

			Property spEP2 = model2.createProperty(deer, OntologyConstants.OBJECT_ENDPOINT);
			Resource sparqlEndPoint2 = model2.createResource(objectEndpoint);
			
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
			createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);

			model2.add(matchResource, matchProperty, createReifiedStatement);
			dataPropertyModel.add(model2);
			
		}
		
		return dataPropertyModel;
	}

}