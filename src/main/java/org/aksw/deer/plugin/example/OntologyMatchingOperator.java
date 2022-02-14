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
	private final int classesMapID = 0;
	private final int dataPropertyMapID = 1;
	private final int objectPropertyMapID = 2;

	public static final Property TYPEOFMAP = DEER.property("typeOfMap");
	public static final Property MACTHING_LIBRARY = DEER.property("matching_Library");
	public static final Property TYPE_OF_KGMATCH = DEER.property("typeofKGMatch");

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

		// List of Models for storing output of Logmap/FCA
		List<Model> listModel = new ArrayList<>();

		String typeOfMap = getParameterMap().getOptional(TYPEOFMAP).map(RDFNode::asLiteral).map(Literal::getString)
				.orElse("did not find type of map");
		String library_Matching = getParameterMap().getOptional(MACTHING_LIBRARY).map(RDFNode::asLiteral)
				.map(Literal::getString).orElse("did not find which libarary to used for Matching");
		String typeofKGMatch = getParameterMap().getOptional(TYPE_OF_KGMATCH).map(RDFNode::asLiteral)
				.map(Literal::getString).orElse("did not find type of KG Match");

		HashMap<Resource, RDFNode> objectSubjectMap = new HashMap<>();
		StmtIterator listStatements = model.listStatements();

		while (listStatements.hasNext()) {
			Statement next = listStatements.next();
			objectSubjectMap.put(next.getSubject(), next.getObject());
		}

		Set<Resource> subjectsKey = objectSubjectMap.keySet();

		if (!typeofKGMatch.equalsIgnoreCase("Limes")) {

		}

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
						.sparqlService(getRedirectedUrl(objectSubjectMap.get(subjectEndpoint).toString()), new_query)
						.execConstruct();

				// file_name1
				model1.write(new FileOutputStream("endpoint_1_" + fileNameCounter + ".rdf"), "RDF/XML");

				// file_name2
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

		System.out.println("-----Library for Matching----" + library_Matching + "*********");

		// Invoking LogMap matcher based on configuration file
		if (!endpointsMap.isEmpty() && library_Matching.equalsIgnoreCase("Logmap")) {

			LogMap_Matcher logMapObject = new LogMap_Matcher();
			for (int fileCounterTemp = 1; fileCounterTemp <= fileNameCounter - 1
					&& (endpointsMap.size() > 2); fileCounterTemp++) {

				try {
					switch (typeOfMap) {
					case "Class":

						System.out.println("----------------Classes Mapping------------------------");
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", classesMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

						System.out.println("endpoint_1_" + fileCounterTemp + ".rdf" + " Endpoint :: "
								+ endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"));
						System.out.println("endpoint_2_" + fileCounterTemp + ".rdf" + " Endpoint :: "
								+ endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf"));
						System.out.println("Model output " + listModel);

						break;

					case "Data Property":

						System.out.println("--------Data Property Mapping--------------");
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", dataPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));
						break;

					case "Object Property":

						System.out.println("-------------Object Property Mapping---------------");
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", objectPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));
						break;

					case "Class and Data Property":
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", classesMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", dataPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

						break;

					case "Class and Object Property":

						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", classesMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", objectPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

						break;
					case "Class and Object Property and Data Property":
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", classesMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", objectPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", dataPropertyMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

						break;

					default:

						System.out.println("------------------Classes Mapping-------------------");
						listModel.add(logMapObject.UsingLogMapMatcher("endpoint_1_" + fileCounterTemp + ".rdf",
								"endpoint_2_" + fileCounterTemp + ".rdf", classesMapID,
								endpointsMap.get("endpoint_1_" + fileCounterTemp + ".rdf"),
								endpointsMap.get("endpoint_2_" + fileCounterTemp + ".rdf")));

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
					listModel.addAll(fcaMatcher.fcaInvoker("endpoint_1_" + i + ".rdf", "endpoint_2_" + i + ".rdf",
							endpointsMap.get("endpoint_1_" + i + ".rdf"), endpointsMap.get("endpoint_2_" + i + ".rdf"),
							i, typeOfMap));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Writing final output to File

		try (OutputStream out = new FileOutputStream("MappingOutputFinal" + ".ttl")) {
			for (Iterator<Model> iterator = listModel.iterator(); iterator.hasNext();) {
				Model model2 = (Model) iterator.next();
				model2.write(out, "TTL"); //
				model2.write(System.out, "TTL");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(listModel);
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
}