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

public class FCA_Matcher {
	static int numberOfMatches=0;

	public List<Model> fcaInvoker(String file1, String file2, String subjectEndpoint, String ObjectEndpoint, int fileCounter,
			String typeOfMap) throws IOException {
		ModelStorage source = new ModelStorage(file1);
		ModelStorage target = new ModelStorage(file2);

		/************************** Lexical-level Matching ***************************/

		LexicalMatcher lexicalMatcher = MatcherFactory.createLexicalMatcher();
		Mapping lexicalOntClassMappings = new Mapping();

		lexicalMatcher.setSourceTarget(source, target);
		lexicalMatcher.setExtractType(true, true);
		lexicalMatcher.mapOntClasses(lexicalOntClassMappings);

		/************************* Structural-level Matching *************************/
		StructuralMatcher structuralMatcher = MatcherFactory.createStructuralMatcher();
		structuralMatcher.setSourceTarget(source, target);
		structuralMatcher.setExtractType(true, true);
		structuralMatcher.addCommonPredicate(RDFS.subClassOf);
		structuralMatcher.addCommonPredicate(OWL.disjointWith);
		structuralMatcher.addAllSubjectAnchors(lexicalOntClassMappings);
		structuralMatcher.addAllObjectAnchors(lexicalOntClassMappings);

		// FCA_Matcher fca = new FCA_Matcher();

		List<Model> matchedModel = new ArrayList<Model>();
		
		System.out.println("*********Inside FCA Matcher class ***********");
		switch (typeOfMap) {
		case "Classes":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher);
			
			break;

		case "dataProperty":
			matchedModel = dataProperty(structuralMatcher, lexicalMatcher);
			
			break;

		case "objectProperty":
			matchedModel = objectPropertyMatching(structuralMatcher, lexicalMatcher);
			
			break;

		case "Classes and dataProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher);
			matchedModel.addAll(dataProperty(structuralMatcher, lexicalMatcher));
			break;
			
		case "Classes and objectProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher);
			matchedModel.addAll(objectPropertyMatching(structuralMatcher, lexicalMatcher));
			break;
			
		case "Classes and dataProperty and objectProperty":
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher);
			matchedModel.addAll(dataProperty(structuralMatcher, lexicalMatcher));
			matchedModel.addAll(objectPropertyMatching(structuralMatcher, lexicalMatcher));
			break;

		default:
			matchedModel = classMatching(lexicalOntClassMappings, structuralMatcher);
			break;
		}
		
		System.out.println(matchedModel);
		source.clear();
		target.clear();
		return matchedModel;
	}

	// Model and model3 are class matching
	public List<Model> classMatching(Mapping lexicalOntClassMappings, StructuralMatcher structuralMatcher) {
		List<Model> classMatching = new ArrayList<Model>();

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator = lexicalOntClassMappings.iterator();
		while (iterrator.hasNext()) {
			MappingCell next = iterrator.next();

			String deer = "https://w3id.org/deer/";
			
			final Resource matchResource = model1.createResource(deer + "Match_" + numberOfMatches);
			final Property matchProperty = model1.createProperty(deer, "found");
			

			Resource resource = model1.createResource(next.getEntity1());
			// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
			Property related = model1.createProperty(deer, "matchesWith");
			Resource resource2 = model1.createResource(next.getEntity2());
			// confidence
			Property confProp = model1.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model1.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);

			model1.add(matchResource, matchProperty, createReifiedStatement);
			classMatching.add(model1);
			if (!model1.isEmpty()) FCA_Matcher.numberOfMatches++;
		}
		try (OutputStream output = new FileOutputStream("MappingOutputLexicalMatcherClass" + ".ttl")) {
			model1.write(output, "TURTLE");
			model1.write(System.out, "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Mapping structuralOntClassMappings = new Mapping();
		structuralMatcher.mapOntClasses(structuralOntClassMappings);

		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator3 = structuralOntClassMappings.iterator();
		while (iterrator3.hasNext()) {
			MappingCell next = iterrator3.next();
			String deer = "https://w3id.org/deer/";
			// int numberOfMatches = 1;
			final Resource matchResource = model2.createResource(deer + "Match_" + numberOfMatches);
			final Property matchProperty = model2.createProperty(deer, "found");
			// numberOfMatches++;

			Resource resource = model2.createResource(next.getEntity1());
			// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
			Property related = model2.createProperty(deer, "matchesWith");
			Resource resource2 = model2.createResource(next.getEntity2());
			// confidence
			// Property confProp = model.createProperty("confidence");
			Property confProp = model2.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);
			
			

			model2.add(matchResource, matchProperty, createReifiedStatement);
			classMatching.add(model2);
			if (!model2.isEmpty()) numberOfMatches++;

		}
		try (OutputStream output = new FileOutputStream("MappingOutputStructuralClassMappings" + ".ttl")) {
			model2.write(output, "TURTLE");
			model2.write(System.out, "TURTLE");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classMatching;

	}

	/* Model 1 and Model 4 are Object property */
	public List<Model> objectPropertyMatching(StructuralMatcher structuralMatcher, LexicalMatcher lexicalMatcher) {

		List<Model> objectPropertyModel = new ArrayList<Model>();
		Mapping lexicalObjectPropertyMappings = new Mapping();
		lexicalMatcher.mapObjectProperties(lexicalObjectPropertyMappings);

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator1 = lexicalObjectPropertyMappings.iterator();
		while (iterrator1.hasNext()) {
			// 1 2 3 4 5
			MappingCell next = iterrator1.next();
			/*
			 * System.out.println(next.getEntity1()); System.out.println(next.getEntity2());
			 * //System.out.println(iterrator.next().getMeasure()); }
			 */
			String deer = "https://w3id.org/deer/";
			// int numberOfMatches = 1;
			final Resource matchResource = model1.createResource(deer + "Match_" +numberOfMatches);
			final Property matchProperty = model1.createProperty(deer, "found");
			// numberOfMatches++;

			Resource resource = model1.createResource(next.getEntity1());
			// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
			Property related = model1.createProperty(deer, "matchesWith");
			Resource resource2 = model1.createResource(next.getEntity2());
			// confidence
			// Property confProp = model.createProperty("confidence");
			Property confProp = model1.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model1.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);

			model1.add(matchResource, matchProperty, createReifiedStatement);
			objectPropertyModel.add(model1);
			if (!model1.isEmpty()) numberOfMatches++;
		}
		try (OutputStream output = new FileOutputStream("MappingOutputLexicalObjectPropertyMappings" + ".ttl")) {
			model1.write(output, "TURTLE");
			model1.write(System.out, "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Mapping structuralDataTypeMappings = new Mapping();
		structuralMatcher.mapDataTypeProperties(structuralDataTypeMappings);

		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator4 = structuralDataTypeMappings.iterator();
		while (iterrator4.hasNext()) {
			MappingCell next = iterrator4.next();
			String deer = "https://w3id.org/deer/";
			// int numberOfMatches = 1;
			final Resource matchResource = model2.createResource(deer + "Match_" + numberOfMatches);
			final Property matchProperty = model2.createProperty(deer, "found");
			// numberOfMatches++;

			Resource resource = model2.createResource(next.getEntity1());

			Property related = model2.createProperty(deer, "matchesWith");
			Resource resource2 = model2.createResource(next.getEntity2());
			// confidence

			Property confProp = model2.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);

			model2.add(matchResource, matchProperty, createReifiedStatement);
			objectPropertyModel.add(model2);
			if (!model2.isEmpty()) numberOfMatches++;
		}
		try (OutputStream output = new FileOutputStream("MappingOutputStructuralDataTypeMappings" + ".ttl")) {
			model2.write(output, "TURTLE");
			model2.write(System.out, "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return objectPropertyModel;

	}

	/* Model2 and Model 5 are data property */
	public List<Model> dataProperty(StructuralMatcher structuralMatcher, LexicalMatcher lexicalMatcher) {

		List<Model> dataPropertyModel = new ArrayList<Model>();
		Mapping lexicalDataPropertyMappings = new Mapping();
		lexicalMatcher.mapDataTypeProperties(lexicalDataPropertyMappings);

		Model model1 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator2 = lexicalDataPropertyMappings.iterator();
		while (iterrator2.hasNext()) {
			MappingCell next = iterrator2.next();
			String deer = "https://w3id.org/deer/";
			// int numberOfMatches = 1;
			final Resource matchResource = model1.createResource(deer + "Match_"+numberOfMatches);
			final Property matchProperty = model1.createProperty(deer, "found");
			// numberOfMatches++;

			Resource resource = model1.createResource(next.getEntity1());
			// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
			Property related = model1.createProperty(deer, "matchesWith");
			Resource resource2 = model1.createResource(next.getEntity2());
			// confidence
			// Property confProp = model.createProperty("confidence");
			Property confProp = model1.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model1.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model1.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model1.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);

			model1.add(matchResource, matchProperty, createReifiedStatement);
			dataPropertyModel.add(model1);
			if (!model1.isEmpty()) numberOfMatches++;
		}
		try (OutputStream output = new FileOutputStream("MappingOutputLexicalDataPropertyMappings" + ".ttl")) {
			model1.write(output, "TURTLE");
			model1.write(System.out, "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Mapping structuralObjectTypeMappings = new Mapping();
		structuralMatcher.mapObjectProperties(structuralObjectTypeMappings);
		Model model2 = ModelFactory.createDefaultModel();
		Iterator<MappingCell> iterrator5 = structuralObjectTypeMappings.iterator();
		while (iterrator5.hasNext()) {
			MappingCell next = iterrator5.next();
			String deer = "https://w3id.org/deer/";
			// int numberOfMatches = 1;
			final Resource matchResource = model2.createResource(deer + "Match_" + numberOfMatches);
			final Property matchProperty = model2.createProperty(deer, "found");
			// numberOfMatches++;

			Resource resource = model2.createResource(next.getEntity1());
			// Property related = model.createProperty("https://w3id.org/deer/matchesWith");
			Property related = model2.createProperty(deer, "matchesWith");
			Resource resource2 = model2.createResource(next.getEntity2());
			// confidence
			// Property confProp = model.createProperty("confidence");
			Property confProp = model2.createProperty(deer, "confidenceValue");
			double confidence2 = next.getMeasure();
			Literal confidence = model2.createLiteral(String.valueOf(confidence2));
			Statement stmt2 = model2.createStatement(resource, related, resource2);

			ReifiedStatement createReifiedStatement = model2.createReifiedStatement(stmt2);
			createReifiedStatement.addProperty(confProp, confidence);

			model2.add(matchResource, matchProperty, createReifiedStatement);
			dataPropertyModel.add(model2);
			if (!model2.isEmpty()) numberOfMatches++;
		}
		try (OutputStream output = new FileOutputStream("MappingOutputStructuralObjectTypeMappings" + ".ttl")) {
			model2.write(output, "TURTLE");
			model2.write(System.out, "TURTLE");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return dataPropertyModel;
	}

}