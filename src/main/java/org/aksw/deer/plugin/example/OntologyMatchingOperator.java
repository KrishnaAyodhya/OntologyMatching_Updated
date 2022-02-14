package org.aksw.deer.plugin.example;

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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
//import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.pf4j.Extension;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Krishna Madhav and Sowmya Kamath Ramesh
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
	 * This method takes input List of Models. It parses through the model and
	 * invokes LogMap or FCA depending upon configuration file
	 * 
	 */
	@Override
	protected List<Model> safeApply(List<Model> models) {

		System.out.println("-----------------------------------Safe apply-----------------------------------");

		// Storing the model from previous phase in Model Object
		Model model = models.get(0);

		// List of Models for storing output of Logmap/FCA
		List<Model> listModel = new ArrayList<>();

		String typeOfMap = getParameterMap().getOptional(TYPEOFMAP).map(RDFNode::asLiteral).map(Literal::getString)
				.orElse("did not find type of map");
		String library_Matching = getParameterMap().getOptional(MACTHING_LIBRARY).map(RDFNode::asLiteral)
				.map(Literal::getString).orElse("did not find which libarary to used for Matching");

		// Storing the Jena model in HashMap and iterating through it
		HashMap<Resource, RDFNode> objectSubjectMap = new HashMap<>();
		StmtIterator listStatements = model.listStatements();

		while (listStatements.hasNext()) {
			Statement next = listStatements.next();
			objectSubjectMap.put(next.getSubject(), next.getObject());
		}

		Set<Resource> subjectsKey = objectSubjectMap.keySet();

		/*
		 * This condition checks if output of KG Matching is from Limes as they contain
		 * subjectEndpoint and objectEndPoints
		 */

		String endpointCheck = "http://";
		if (subjectsKey.toString().contains(endpointCheck)) {

			String new_query = "construct{?s ?p ?o}  where {?s ?p ?o} LIMIT 1000";
			System.out.println("----------------------query---------------------");

			// Map for storing End-point and file names
			LinkedHashMap<String, String> endpointsMap = new LinkedHashMap<>();

			for (Resource subjectEndpoint : subjectsKey) {
				if (fileNameCounter == 4)

					break;
				try {
					System.out.println("------------------------------------");
					// First model
					Model model1 = QueryExecutionFactory
							.sparqlService(getRedirectedUrl(subjectEndpoint.toString()), new_query).execConstruct();

					// Second model
					Model model2 = QueryExecutionFactory
							.sparqlService(getRedirectedUrl(objectSubjectMap.get(subjectEndpoint).toString()),
									new_query)
							.execConstruct();

					// Ontology file 1
					model1.write(new FileOutputStream(OntologyConstants.ENDPOINTS_1 + fileNameCounter + OntologyConstants.FILE_FORMAT), "RDF/XML");

					// Ontology file 2
					model2.write(new FileOutputStream(OntologyConstants.ENDPOINTS_2 + fileNameCounter + OntologyConstants.FILE_FORMAT), "RDF/XML");

					// Storing the File name and Endpoints in HashMap
					endpointsMap.put(OntologyConstants.ENDPOINTS_1 + fileNameCounter + OntologyConstants.FILE_FORMAT, subjectEndpoint.toString());
					endpointsMap.put(OntologyConstants.ENDPOINTS_2 + fileNameCounter + OntologyConstants.FILE_FORMAT,
							objectSubjectMap.get(subjectEndpoint).toString());

					System.out.println(endpointsMap + " %%%%%%%%% ");

					fileNameCounter++;
				} catch (Exception e) {
					System.out.println(
							"Exception caught for the Subject : " + subjectEndpoint + " ,Exception name : " + e);
				}
			}

			System.out.println("-----Library for Matching----" + library_Matching + "*********");

			// Invoking LogMap matcher based on configuration file
			if (!endpointsMap.isEmpty() && library_Matching.equalsIgnoreCase("Logmap")) {

				LogMap_Matcher logMapObject = new LogMap_Matcher();
				for (int fileCounterTemp = 1; fileCounterTemp <= fileNameCounter - 1
						&& (endpointsMap.size() > 2); fileCounterTemp++) {

					try {
						switch (typeOfMap) {
						case "Class":

							// Class Matching
							System.out.println("----------------Classes Mapping------------------------");
							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, classesMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

							System.out.println(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT + " Endpoint :: "
									+ endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT));
							System.out.println(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT + " Endpoint :: "
									+ endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT));
							System.out.println("Model output " + listModel);

							break;

						case "Data Property":

							// Data Property Matching

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, dataPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));
							break;

						case "Object Property":

							// Object Property Matching

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, objectPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));
							break;

						case "Class and Data Property":

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, classesMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, dataPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

							break;

						case "Class and Object Property":

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, classesMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));
							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, objectPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

							break;
						case "Class and Object Property and Data Property":

							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, classesMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));
							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, objectPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));
							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, dataPropertyMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

							break;

						default:

							System.out.println("------------------Classes Mapping-------------------");
							listModel.add(logMapObject.usingLogMapMatcher(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT,
									OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT, classesMapID,
									endpointsMap.get(OntologyConstants.ENDPOINTS_1 + fileCounterTemp + OntologyConstants.FILE_FORMAT),
									endpointsMap.get(OntologyConstants.ENDPOINTS_2 + fileCounterTemp + OntologyConstants.FILE_FORMAT)));

						}

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
						listModel.addAll(fcaMatcher.fcaInvoker(OntologyConstants.ENDPOINTS_1 + i + OntologyConstants.FILE_FORMAT, OntologyConstants.ENDPOINTS_2 + i + OntologyConstants.FILE_FORMAT,
								endpointsMap.get(OntologyConstants.ENDPOINTS_1 + i + OntologyConstants.FILE_FORMAT),
								endpointsMap.get(OntologyConstants.ENDPOINTS_2 + i + OntologyConstants.FILE_FORMAT), i, typeOfMap));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

		
		/*
		 * This condition checks if output of KG Matching is other than Limes
		 * 
		 * This part of code is for future work as dataset corresponding to .NT files
		 * were not working as they had invalid IRIs.
		 * 
		 */

		if (!subjectsKey.toString().contains(endpointCheck)) {
			System.out.println("Inside KGMatch other than Limes");

			LogMap_Matcher logMapObject = new LogMap_Matcher();

			for (Resource subjectEndpoint : subjectsKey) {
				try {

					// Calling only class matching
					// Similarly can be called for other combination of mappings
					
					logMapObject.usingLogMapMatcher_2(subjectEndpoint.toString(),
							objectSubjectMap.get(subjectEndpoint).toString(), classesMapID);
				} catch (OWLOntologyCreationException e) {
					System.out.println(
							"Exception caught for the Subject : " + subjectEndpoint + " ,Exception name : " + e);
				}
			}
		}

		// Writing final output to File

		try (OutputStream out = new FileOutputStream("MatchingOutputFinal" + ".ttl")) {
			for (Iterator<Model> iterator = listModel.iterator(); iterator.hasNext();) {
				Model model2 = (Model) iterator.next();
				model2.write(out, "TTL");
				model2.write(System.out, "TTL");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return listModel;
	}

	/**
	 * HTTP redirection
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
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
}