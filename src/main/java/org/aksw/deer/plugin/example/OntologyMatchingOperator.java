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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.aksw.deer.enrichments.AbstractParameterizedEnrichmentOperator;
import org.aksw.deer.vocabulary.DEER;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
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
//import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
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
	private static int numberOfMatches = 0;
	private final int classesMapID = 0;
	private final int dataPropertyMapID = 1;
	private final int objectPropertyMapID = 2;

	public static final Property TYPEOFMAP = DEER.property("typeOfMap");
	public static final Property MACTHING_LIBRARY = DEER.property("matching_Library");

	public OntologyMatchingOperator() throws OWLOntologyCreationException {

		super();
	}

	@Override
	public ValidatableParameterMap createParameterMap() { // 2
		return ValidatableParameterMap.builder().declareProperty(SELECTOR).declareProperty(SPARQL_CONSTRUCT_QUERY)
				.declareProperty(TYPEOFMAP).declareProperty(MACTHING_LIBRARY)
				.declareValidationShape(getValidationModelFor(OntologyMatchingOperator.class)).build();
	}

	/**
	 *
	 */
	@Override
	protected List<Model> safeApply(List<Model> models) { // 3
		// Model a = filterModel(models.get(0));

		System.out.println("-----------------------------------Safe apply-----------------------------------");

		Model model = models.get(0);
		HashMap<Resource, RDFNode> objectSubjectMap = new HashMap<>();
		StmtIterator listStatements = model.listStatements();

		while (listStatements.hasNext()) {
			Statement next = listStatements.next();
			objectSubjectMap.put(next.getSubject(), next.getObject());
		}

		String new_query = "construct{?s ?p ?o}  where {?s ?p ?o} LIMIT 1000";
		System.out.println("----------------------query---------------------");
		Set<Resource> subjectsKey = objectSubjectMap.keySet();

		// Map for storing End-point and file names
		LinkedHashMap<String, String> endpointsMap = new LinkedHashMap<>();

		for (Resource subjectEndpoint : subjectsKey) {
			if (fileNameCounter == 5)
				break;
			try {
				System.out.println("------------------------------------");
				// First model
				Model model1 = QueryExecutionFactory
						.sparqlService(getRedirectedUrl(subjectEndpoint.toString()), new_query).execConstruct();

				// Model model1 = QueryExecutionHTTP.service(subjectEndpoint.toString(),
				// new_query).execConstruct();

				// Second model
				Model model2 = QueryExecutionFactory
						.sparqlService(getRedirectedUrl(objectSubjectMap.get(subjectEndpoint).toString()), new_query)
						.execConstruct();

				// Model model2 =
				// QueryExecutionHTTP.service(objectSubjectMap.get(subjectEndpoint).toString(),
				// new_query).execConstruct();
				model1.write(new FileOutputStream("endpoint_1_" + fileNameCounter + ".rdf"), "RDF/XML"); // file_name1 -
				model2.write(new FileOutputStream("endpoint_2_" + fileNameCounter + ".rdf"), "RDF/XML");

				endpointsMap.put("endpoint_1_" + fileNameCounter + ".rdf", subjectEndpoint.toString());
				endpointsMap.put("endpoint_2_" + fileNameCounter + ".rdf",
						objectSubjectMap.get(subjectEndpoint).toString());

				System.out.println(endpointsMap + " %%%%%%%%% ");

				fileNameCounter++;
			} catch (Exception e) {
				System.out.println("Exception caught for the Subject : " + subjectEndpoint + " ,Exception name : " + e);
			}
		}

		// List of Models for storing output of Logmap
		List<Model> listModel = new ArrayList<>();

		String typeOfMap = getParameterMap().getOptional(TYPEOFMAP).map(RDFNode::asLiteral).map(Literal::getString)
				.orElse("did not find type of map");
		String library_Matching = getParameterMap().getOptional(MACTHING_LIBRARY).map(RDFNode::asLiteral)
				.map(Literal::getString).orElse("did not find which libarary to used for Matching");

		System.out.println("-----TypeOfMap---- " + typeOfMap + "******");
		System.out.println("-----Library for Matching----" + library_Matching + "*********");

		// Invoking LogMap matcher based on configuration file
		if (!endpointsMap.isEmpty() && library_Matching.equalsIgnoreCase("Logmap")) {

			for (int i = 1; i <= fileNameCounter - 1 && (endpointsMap.size() > 2); i++) {

				try {
					switch (typeOfMap) {
					case "Class":

						System.out.println("----------------Classes Mapping------------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
								classesMapID, endpointsMap.get("endpoint_1_" + i + ".rdf"),
								endpointsMap.get("endpoint_2_" + i + ".rdf"), i));
						System.out.println("endpoint_1_" + i + ".rdf" + " Endpoint :: "
								+ endpointsMap.get("endpoint_1_" + i + ".rdf"));
						System.out.println("endpoint_2_" + i + ".rdf" + " Endpoint :: "
								+ endpointsMap.get("endpoint_2_" + i + ".rdf"));
						System.out.println("Model output " + listModel);
						break;

					case "Data Property":

						System.out.println("--------Data Property Mapping--------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
								dataPropertyMapID, endpointsMap.get("endpoint_1_" + i + ".rdf"),
								endpointsMap.get("endpoint_2_" + i + ".rdf"), i));
						break;

					case "Object Property":

						System.out.println("-------------Object Property Mapping---------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
								objectPropertyMapID, endpointsMap.get("endpoint_1_" + i + ".rdf"),
								endpointsMap.get("endpoint_2_" + i + ".rdf"), i));
						break;

					case "Class and Data Property":
						
					case "Class and Object Property":
						
					case "Class and Data Property and and Data Property":
						
						
					default:

						System.out.println("------------------Classes Mapping-------------------");
						listModel.add(UsingLogMapMatcher("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
								classesMapID, endpointsMap.get("endpoint_1_" + i + ".rdf"),
								endpointsMap.get("endpoint_2_" + i + ".rdf"), i));

					}
					numberOfMatches++;
				} catch (OWLOntologyCreationException e) {

					e.printStackTrace();
				}

			}
		}


		// Invoking FCA matcher based on configuration file
		else if (!endpointsMap.isEmpty() && library_Matching.equalsIgnoreCase("FCA")) {
			
			System.out.println("******************Detected that its FCA *************");
			// Calling FCA for Matching Ontologies
			FCA_Matcher fcaMatcher = new FCA_Matcher();
		
			for (int i = 1; i < fileNameCounter - 1 && (endpointsMap.size() > 2); i++) {
				
				try {
					listModel.addAll(fcaMatcher.fcaInvoker("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
							endpointsMap.get("endpoint_1_" + i + ".rdf"), endpointsMap.get("endpoint_2_" + i + ".rdf"), i,
							typeOfMap));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				numberOfMatches++;

			}
		}

		// Writing final output to File
		try (OutputStream out = new FileOutputStream("MappingOutputFinal" + ".ttl")) {
			for (Iterator<Model> iterator = listModel.iterator(); iterator.hasNext();) {
				Model model2 = (Model) iterator.next();
				model2.write(out, "TTL");
				//model2.write(System.out, "TTL");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

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

	// Matching Ontologies using LogMap and sending the mactched Model back to
	// caller
	public Model UsingLogMapMatcher(String file1, String file2, int a, String endpoint1, String endpoint2,
			int fileCounter) throws OWLOntologyCreationException {

		// Log Map variables
		OWLOntology onto1;
		OWLOntology onto2;

		OWLOntologyManager onto_manager;

		// String onto1_iri = "lov_linkeddata_es_dataset_lov.nt";
		String onto1_iri = file1;
		String onto2_iri = file2;

		// String onto1_iri = "onto_fel_cvut_cz_rdf4j-server_repositories.nt";
		// //example-output.rdf

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
				
				final Resource matchResource = model.createResource(deer + "Match_" + numberOfMatches);
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
		try (OutputStream out = new FileOutputStream("MappingOutput" + fileCounter + ".ttl")) {
			model.write(out, "TTL");
			//model.write(System.out, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (typeOfMapping == a) {
			System.out.println("Number of mappings computed by LogMap: " + logmap2_mappings.size());
			System.out.println("-----------------------------------");
		}

		return model;

	}

}