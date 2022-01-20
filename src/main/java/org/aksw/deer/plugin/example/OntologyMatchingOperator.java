package org.aksw.deer.plugin.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.deer.enrichments.AbstractParameterizedEnrichmentOperator;
import org.aksw.deer.vocabulary.DEER;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.pf4j.Extension;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ox.krr.logmap2.LogMap2_Matcher;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

/**
 */
/**
 * @author soumya
 *
 */
@Extension
public class OntologyMatchingOperator extends AbstractParameterizedEnrichmentOperator {

	private static final Logger logger = LoggerFactory.getLogger(OntologyMatchingOperator.class);

	public static final Property SUBJECT = DEER.property("subject");
	public static final Property PREDICATE = DEER.property("predicate");
	public static final Property OBJECT = DEER.property("object");
	public static final Property SELECTOR = DEER.property("selector");
	public static final Property SPARQL_CONSTRUCT_QUERY = DEER.property("sparqlConstructQuery");
	private static int fileNameCounter = 1;

	public static final Property TYPEOFMAP = DEER.property("typeOfMap");

	public OntologyMatchingOperator() throws OWLOntologyCreationException {

		super();
	}

	@Override
	public ValidatableParameterMap createParameterMap() { // 2
		return ValidatableParameterMap.builder().declareProperty(SELECTOR).declareProperty(SPARQL_CONSTRUCT_QUERY)
				.declareProperty(TYPEOFMAP)
				.declareValidationShape(getValidationModelFor(OntologyMatchingOperator.class)).build();
	}

	/**
	 *
	 */
	@Override
	protected List<Model> safeApply(List<Model> models) { // 3
		// Model a = filterModel(models.get(0));
		/*
		 * try { LogInputFormat(); } catch (FileNotFoundException e) {
		 * 
		 * e.printStackTrace(); }
		 */

		/*
		 * try { sparqlEndPoints(); } catch (OWLOntologyCreationException |
		 * FileNotFoundException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
		System.out.println("-----------------------------------Safe apply-----------------------------------");
		
		Model model = models.get(0);
		HashMap<Resource, RDFNode> objectSubjectMap = new HashMap<>();
		// NodeIterator listObjects = model.listObjects(); // Objects
		// ResIterator listSubjects = model.listSubjects();// Subjects
		StmtIterator listStatements = model.listStatements();

		while (listStatements.hasNext()) {
			Statement next = listStatements.next();
			objectSubjectMap.put(next.getSubject(), next.getObject());
		}

		String new_query = "construct{?s ?p ?o}  where {?s ?p ?o} LIMIT 1000";
		System.out.println("----------------------query---------------------");
		Set<Resource> subjectsKey = objectSubjectMap.keySet();

		// Map for storing End-point and file names
		HashMap<String, String> endpointsMap = new HashMap<>();

		for (Resource subjectEndpoint : subjectsKey) {
			if (fileNameCounter == 5)  break;
			try {
				// Dbpedia model
				Model model1 = QueryExecutionFactory
						.sparqlService(getRedirectedUrl(subjectEndpoint.toString()), new_query).execConstruct();
				model1.write(new FileOutputStream("endpoint_1_" + fileNameCounter + ".ttl"), "TTL"); // file_name1 -
				System.out.println("------------------------------------");
				// OutputStream out = new FileOutputStream("endpoint_1_" + fileNameCounter +
				// ".ttl");
				// RDFDataMgr.write(out, model1, Lang.TURTLE); // Endpoint1
				endpointsMap.put("endpoint_1_" + fileNameCounter + ".ttl", subjectEndpoint.toString());

				// Yago model
				// endpointStr = "https://yago-knowledge.org/sparql/query";
				Model model2 = QueryExecutionFactory
						.sparqlService(getRedirectedUrl(objectSubjectMap.get(subjectEndpoint).toString()), new_query)
						.execConstruct();
				model2.write(new FileOutputStream("endpoint_2_" + fileNameCounter + ".ttl"), "TTL");
				// OutputStream out1 = new FileOutputStream("endpoint_2_" + fileNameCounter +
				// ".ttl");
				// RDFDataMgr.write(out1, model2, Lang.TURTLE);
				endpointsMap.put("endpoint_2_" + fileNameCounter + ".ttl",
						objectSubjectMap.get(subjectEndpoint).toString());

				fileNameCounter++;
			} catch (Exception e) {
				System.out.println("Subject : " + subjectEndpoint + ",Exception name : " + e);
			}
		}

		int i = 0;
		// new LogInputFormat();
		int classesMapID = 0;
		int dataPropertyMapID = 1;
		int objectPropertyMapID = 2;

		List<Model> listModel = new ArrayList<>();

		String typeOfMap = getParameterMap().getOptional(TYPEOFMAP).map(RDFNode::asLiteral).map(Literal::getString)
				.orElse("did not able to find type of map");

		System.out.println("-----TypeOfMap---- "+ typeOfMap+ "^^^^^^^^^");
		
		for (i = 1; i <= fileNameCounter-1; i++) {

			try {
				switch (Integer.parseInt(typeOfMap)) {
				case 0:
					if (Integer.parseInt(typeOfMap) == classesMapID) {
						System.out.println(
								"-----------------------------------Classes Mapping-----------------------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".ttl", "endpoint_2_" + i + ".ttl",
								classesMapID, endpointsMap.get("endpoint_1_" + i + ".ttl"),
								endpointsMap.get("endpoint_2_" + i + ".ttl")));
					}
					break;
				case 1:
					if (Integer.parseInt(typeOfMap) == dataPropertyMapID) {
						System.out.println(
								"-----------------------------------Data Property Mapping-----------------------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".ttl", "endpoint_2_" + i + ".ttl",
								dataPropertyMapID, endpointsMap.get("endpoint_1_" + i + ".ttl"),
								endpointsMap.get("endpoint_2_" + i + ".ttl")));
					}
					break;
				case 2:

					if (Integer.parseInt(typeOfMap) == objectPropertyMapID) {
						System.out.println(
								"-----------------------------------Object Property Mapping-----------------------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".ttl", "endpoint_2_" + i + ".ttl",
								objectPropertyMapID, endpointsMap.get("endpoint_1_" + i + ".ttl"),
								endpointsMap.get("endpoint_2_" + i + ".ttl")));
					}
					break;
				

				default:
					if (Integer.parseInt(typeOfMap) == classesMapID) {
						System.out.println(
								"-----------------------------------Classes Mapping-----------------------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".ttl", "endpoint_2_" + i + ".ttl",
								classesMapID, endpointsMap.get("endpoint_1_" + i + ".ttl"),
								endpointsMap.get("endpoint_2_" + i + ".ttl")));
					}
				}

			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// create an empty Model
		// Model model = ModelFactory.createDefaultModel();
		// return List.of(model);
		return listModel;
	}

	// HTTP redirection
	public static String getRedirectedUrl(String url) throws IOException {
		HttpURLConnection con = (HttpURLConnection) (new URL(url).openConnection());
		con.setConnectTimeout(1000);
		con.setReadTimeout(1000);
		con.setRequestProperty("User-Agent", "Googlebot");
		con.setInstanceFollowRedirects(false);
		con.connect();
		String headerField = con.getHeaderField("Location");
		return headerField == null ? url : headerField;

	}

//	dynamically calling sparql endpoints from KG matching
	public static void sparqlEndPoints() throws OWLOntologyCreationException, FileNotFoundException {

		HashMap<String, String> objectSubjectMap = new HashMap<>();

		OWLOntologyManager m = OWLManager.createOWLOntologyManager();

		String owlFile = "Sparql.n3";
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		model.read(owlFile);

		StmtIterator listStatements = model.listStatements();
		while (listStatements.hasNext()) {
			Statement next = listStatements.next();
			objectSubjectMap.put(next.getSubject().toString(), next.getObject().toString());

		}

		String new_query = "construct{?s ?p ?o}  where {?s ?p ?o} LIMIT 1000";

		Set<String> subjectsKey = objectSubjectMap.keySet();

		for (String subjectEndpoint : subjectsKey) {

			try {
				// Dbpedia model
				Model model1 = QueryExecutionFactory.sparqlService(getRedirectedUrl(subjectEndpoint), new_query)
						.execConstruct();
				model1.write(new FileOutputStream("endpoint_1_" + fileNameCounter + ".ttl"), "TTL");

				// Yago model
				// endpointStr = "https://yago-knowledge.org/sparql/query";
				Model model2 = QueryExecutionFactory
						.sparqlService(getRedirectedUrl(objectSubjectMap.get(subjectEndpoint)), new_query)
						.execConstruct();
				model2.write(new FileOutputStream("endpoint_2_" + fileNameCounter + ".ttl"), "TTL");

				fileNameCounter++;
			} catch (Exception e) {
				System.out.println("Subject : " + subjectEndpoint + ",Exception name : " + e);
			}
		}

	}

	/**
	 * @throws FileNotFoundException
	 */
	public void LogInputFormat() throws FileNotFoundException {

		String endpointStr = "http://dbpedia.org/sparql"; // sparql end-point
		String query = "CONSTRUCT {?s ?p ?o} WHERE {  ?s ?p ?o\r\n"
				+ "  FILTER(!isLiteral(?o) && !isBlank(?s) && !isBlank(?o))\r\n" + "} LIMIT 1000"; // Query

		String queryYagoo = "CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. ?s"
				+ " <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://schema.org/Movie>."
				+ "	  FILTER(!isBlank(?s) && !isLiteral(?o) && !isBlank(?o)) }LIMIT 1000";

		/*
		 * "CONSTRUCT {yago:Paderborn ?p ?o } WHERE{ \r\n" + "yago:Paderborn ?p  ?o.}";
		 */
		/*
		 * "CONSTRUCT {?s ?p ?o} WHERE {  ?s ?p ?o.\r\n" +
		 * "   ?s  <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://schema.org/AdministrativeArea>   .\r\n"
		 * + "				 FILTER(!isBlank(?s) && !isLiteral(?o) && !isBlank(?o))\r\n"
		 * + "				}LIMIT 1000";
		 */
		/*
		 * CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. ?s
		 * <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://schema.org/Movie> .
		 * FILTER(!isBlank(?s) && !isLiteral(?o) && !isBlank(?o)) }LIMIT 1000
		 */

		String queryDbpedia = " CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. ?s\r\n"
				+ "		  <http://www.w3.org/2000/01/rdf-schema#subClassOf>\r\n"
				+ "		  <http://dbpedia.org/ontology/WrittenWork>. FILTER(! isBlank(?s) && !isLiteral(?o))\r\n"
				+ "		  }LIMIT 1000";

		String queryDbpedia1 = " CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. ?s\r\n"
				+ "						  <http://www.w3.org/2000/01/rdf-schema#subClassOf>\r\n"
				+ "				  <http://dbpedia.org/ontology/Book>. FILTER(! isBlank(?s) && !isLiteral(?o))\r\n"
				+ "					  }LIMIT 1000";

		/*
		 * CONSTRUCT { <http://dbpedia.org/resource/Paderborn> ?p ?o} WHERE {\r\n" +
		 * "          <http://dbpedia.org/resource/Paderborn>?p ?o\r\n" + " }
		 */ /*
			 * "CONSTRUCT {?s ?p ?o} WHERE {  ?s ?p ?o.\r\n" +
			 * " ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf>  <http://dbpedia.org/ontology/Settlement>.\r\n"
			 * + "				  FILTER(! isBlank(?s) && !isLiteral(?o))\r\n" +
			 * "				}LIMIT 1000";
			 */

		/*
		 * CONSTRUCT {?s ?p ?o} WHERE { ?s ?p ?o. ?s
		 * <http://www.w3.org/2000/01/rdf-schema#subClassOf>
		 * <http://dbpedia.org/ontology/Work>. FILTER(! isBlank(?s) && !isLiteral(?o))
		 * }LIMIT 1000
		 */
		// Dbpedia model
		Model modelDbpedia = QueryExecutionFactory.sparqlService(endpointStr, queryDbpedia).execConstruct();
		modelDbpedia.write(new FileOutputStream("dbpedia.ttl"), "TTL");

		// Yago model
		// endpointStr = "https://yago-knowledge.org/sparql/query";
		Model modelYago = QueryExecutionFactory.sparqlService(endpointStr, queryDbpedia1).execConstruct();
		modelYago.write(new FileOutputStream("yagoo.ttl"), "TTL");

	}

	public Model UsingLogMapMatcher(String file1, String file2, int a, String endpoint1, String endpoint2)
			throws OWLOntologyCreationException {

		// Log Map variables
		OWLOntology onto1;
		OWLOntology onto2;

		OWLOntologyManager onto_manager;

		// String onto1_iri = "lov_linkeddata_es_dataset_lov.nt";
		String onto1_iri = file1;
		String onto2_iri = file2;

		// String onto1_iri = "onto_fel_cvut_cz_rdf4j-server_repositories.nt";
		// //example-output.ttl

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

		// Generates labels of the matched classes
		Set<String> representativeLabelsForMappings = logmap2.getRepresentativeLabelsForMappings();

		// Set of mappings computed my LogMap
		Set<MappingObjectStr> logmap2_mappings = logmap2.getOverEstimationOfMappings();
		// Set<MappingObjectStr> logmap2_mappings = logmap2.getLogmap2_Mappings();
		Iterator<MappingObjectStr> iterator = logmap2_mappings.iterator();

		// Adding Model
		Model model = ModelFactory.createDefaultModel();

		// Variable for tracking Type of Mapping
		int typeOfMapping = -1;

		// Returns elements of the LogMap
		while (iterator.hasNext()) {
			// Generates IRIs of matched classes
			// Structuralconfidence, Lexicalconfidence and confidence values
			// if properties are matched
			MappingObjectStr next = iterator.next();
			if (next.getTypeOfMapping() == a) {
				typeOfMapping = next.getTypeOfMapping();
				System.out.println("Labels : " + representativeLabelsForMappings);

				String iriStrEnt1 = next.getIRIStrEnt1();
				String iriStrEnt2 = next.getIRIStrEnt2();
				System.out.println("URL of ontology 1 : " + iriStrEnt1);
				System.out.println("URL of ontology 2 : " + iriStrEnt2);
				System.out.println("Structural Mappings of two ontologies : " + next.getStructuralConfidenceMapping());
				System.out.println("Confidence value of the mapping : " + next.getConfidence());
				System.out.println("LexicalConfidnce of the labels:" + next.getLexicalConfidenceMapping());
				System.out.println("dataProperty in the ontologies:" + next.isDataPropertyMapping());
				System.out.println("objectProperty in the ontologies:" + next.isObjectPropertyMapping());
				// System.out.println("End point 1 : " + endpoint1);
				// System.out.println("End point 2 : " + endpoint2);
				// System.out.println(next.DATAPROPERTIES);
				// System.out.println(next.OBJECTPROPERTIES);
				// In this matching we look for matched classe's properties not instance
				// properties
				// Type of mapping:
				// it tells whether is class=0,dataproperty=1
				// ,objectproperty=2,instance=3,unknown=4 based on the aassigned numbers
				System.out.println("Type of mapping : " + next.getTypeOfMapping());
				// Output format
				System.out.println("---------------output format-------------------");
				String deer = "https://w3id.org/deer/";
				int numberOfMatches = 1;
				final Resource matchResource = model.createResource(deer + "Match " + numberOfMatches);
				final Property matchProperty = model.createProperty(deer, "found");
				numberOfMatches++;

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
				// DataProperty
				// Property dataProp = model.createProperty("dataProperty");
				/*
				 * Property dataProp = model.createProperty(deer, "dataProperty"); boolean
				 * dataPropertyMapping = next.isDataPropertyMapping(); Literal dataPropMap =
				 * model.createLiteral(String.valueOf(dataPropertyMapping)); // ObjectProperty
				 * Property objectProp = model.createProperty(deer, "objectProperty"); boolean
				 * objectPropertyMapping = next.isObjectPropertyMapping(); Literal objectPropMap
				 * = model.createLiteral(String.valueOf(objectPropertyMapping));
				 */
				// resource.addProperty(related, next.getIRIStrEnt2());

				Statement stmt2 = model.createStatement(resource, related, resource2);

				ReifiedStatement createReifiedStatement = model.createReifiedStatement(stmt2);
				createReifiedStatement.addProperty(confProp, confidence);
				createReifiedStatement.addProperty(spEP1, sparqlEndPoint1);
				createReifiedStatement.addProperty(spEP2, sparqlEndPoint2);
				// createReifiedStatement.addProperty(dataProp, dataPropMap);
				// createReifiedStatement.addProperty(objectProp, objectPropMap);

				model.add(matchResource, matchProperty, createReifiedStatement);
			}
			try (OutputStream out = new FileOutputStream("MappingOutput" + a + ".ttl")) {
				model.write(out, "TURTLE");
				model.write(System.out, "TURTLE");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (typeOfMapping == a) {
			System.out.println("Number of mappings computed by LogMap: " + logmap2_mappings.size());
			System.out.println("-----------------------------------");
		}

		/*
		 * Set<MappingObjectStr> overEstimationOfMappings =
		 * logmap2.getOverEstimationOfMappings(); iterator =
		 * overEstimationOfMappings.iterator();
		 * 
		 * // Returns elements of the LogMap while (iterator.hasNext()) { // Generates
		 * IRIs of matched classes // Structuralconfidence, Lexicalconfidence and
		 * confidence values // if properties are matched MappingObjectStr next =
		 * iterator.next(); if (next.getTypeOfMapping() == a) { String iriStrEnt1 =
		 * next.getIRIStrEnt1(); String iriStrEnt2 = next.getIRIStrEnt2();
		 * System.out.println("URL of ontology 1 : " + iriStrEnt1);
		 * System.out.println("URL of ontology 2 : " + iriStrEnt2); System.out.println(
		 * "Structural Mappings of two ontologies : " +
		 * next.getStructuralConfidenceMapping());
		 * System.out.println("Confidence value of the mapping : " +
		 * next.getConfidence()); System.out.println("LexicalConfidnce of the labels:" +
		 * next.getLexicalConfidenceMapping());
		 * System.out.println("dataProperty in the ontologies:" +
		 * next.isDataPropertyMapping());
		 * System.out.println("objectProperty in the ontologies:" +
		 * next.isObjectPropertyMapping()); // System.out.println(next.DATAPROPERTIES);
		 * // System.out.println(next.OBJECTPROPERTIES);
		 * 
		 * System.out.println("Type of mapping : " + next.getTypeOfMapping()); }
		 */
		return model;

	}

	// Output file
	/*
	 * try (OutputStream out = new FileOutputStream("MappingOutput"+a+".ttl")) {
	 * model.write(out, "TURTLE"); model.write(System.out, "TURTLE"); } catch
	 * (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) {
	 * e.printStackTrace(); }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); }
	 * 
	 * 
	 * }
	 */
}
