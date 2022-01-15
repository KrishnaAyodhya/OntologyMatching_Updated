package org.aksw.deer.plugin.example;

import java.io.*;
//import java.net.URI;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.aksw.deer.enrichments.AbstractParameterizedEnrichmentOperator;
import org.aksw.deer.vocabulary.DEER;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
import org.aksw.limes.core.controller.Controller;
import org.aksw.limes.core.controller.LimesResult;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.config.KBInfo;
import org.aksw.limes.core.io.config.writer.RDFConfigurationWriter;
import org.aksw.limes.core.io.query.ModelRegistry;
import org.aksw.limes.core.io.serializer.ISerializer;
import org.aksw.limes.core.io.serializer.SerializerFactory;
import org.aksw.limes.core.ml.algorithm.LearningParameter;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;
//import org.aksw.simba.tapioca.preprocessing.labelretrieving.WorkerBasedLabelRetrievingDocumentSupplierDecorator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.VCARD;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Extension
public class KGMatchingOperator extends AbstractParameterizedEnrichmentOperator {

  private static final Logger logger = LoggerFactory.getLogger(KGMatchingOperator.class);
  public static  Property Approach=DEER.property("matchingApproach");
  public static List<Model> modelToNextGroup=new ArrayList<>();
  public static Model modelDefault=ModelFactory.createDefaultModel();
  public static Property pop=ResourceFactory.createProperty("http://example.com/test#matches");

  public KGMatchingOperator() {

    super();
  }

  @Override
  public ValidatableParameterMap createParameterMap() { // 2
    return ValidatableParameterMap.builder().declareProperty(Approach)
      .declareValidationShape(getValidationModelFor(KGMatchingOperator.class)).build();
  }

  @Override
  protected List<Model> safeApply(List<Model> models)  { // 3
    //List<Model> mod = new ArrayList<>();
    String choice= getParameterMap().getOptional(Approach).map(RDFNode::asLiteral).map(Literal::getString)
      .orElse("did not able to find type of choice");
    System.out.println("your choice is"+choice);
    if(choice.equals("LIMES")) {
      KGMatchingOperator kg = new KGMatchingOperator();
      Configuration con = kg.createLimeConfigurationFile(models);
      kg.callLimes(con);
      modelToNextGroup=kg.createSparQLEndpoint();
    }
	/*
	 * else if (choice.equals("JaccardSimilarity")) { App app = new App();
	 * modelToNextGroup= app.jacardSimilarityFunction(models); } else if
	 * (choice.equals("WeightedJaccardSimilarity")) {
	 * System.out.println("please provide the correct choice"); }
	 */
    return modelToNextGroup;
  }

  public Configuration createLimeConfigurationFile(List<Model> models) throws IllegalArgumentException {
    // Creating Limes configuration Object
    Configuration conf = new Configuration();
    // adding prefix
    conf.addPrefix("ns1", "https://example.com/test#");
    conf.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
    conf.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

    KBInfo src = new KBInfo();
    src.setId("sourceId");
    src.setEndpoint("jsontordfoutput.ttl");
    //src.setEndpoint(String.valueOf(models.get(0)));
    src.setVar("?o");
    src.setPageSize(1000);
    src.setType("TURTLE");
    src.setRestrictions(new ArrayList<String>(Arrays.asList(new String[]{"?s ns1:dataset ?o"})));
    src.setProperties(Arrays.asList(new String[]{"ns1:keywords", "ns1:domain"}));
    //src.setProperties(Arrays.asList(new String[] {  }));

    Map<String, String> prefixes = new HashMap<String, String>();
    prefixes.put("ns1", "https://example.com/test#");
    prefixes.put("owl", "http://www.w3.org/2002/07/owl#");

    prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

    src.setPrefixes(prefixes);

    // src.setFunctions(functions);

    HashMap<String, String> tempHashMap = new HashMap<String, String>();
    tempHashMap.put("ns1:keywords", "");
    LinkedHashMap<String, Map<String, String>> functions = new LinkedHashMap<String, Map<String, String>>();
    functions.put("ns1:keywords", tempHashMap);
    src.setFunctions(functions);


    conf.setSourceInfo(src);

    KBInfo target = new KBInfo();
    target.setId("targetId");
      target.setEndpoint("jsontordfoutput.ttl");
    //target.setEndpoint(String.valueOf(models.get(0)));

    target.setType("TURTLE");
    target.setVar("?v");
    target.setPageSize(1000);
    target.setRestrictions(new ArrayList<String>(Arrays.asList(new String[]{"?t ns1:dataset ?v"})));
    target.setProperties(Arrays.asList(new String[]{"ns1:keywords", "ns1:domain"}));
    //target.setProperties(Arrays.asList(new String[] {  }));
    target.setPrefixes(prefixes);
    target.setFunctions(functions);
    conf.setTargetInfo(target);

    conf.setMetricExpression("AND(cosine(o.ns1:keywords,v.ns1:keywords)|0.9,exactmatch(o.ns1:domain,v.ns1:domain)|0.8)");

    conf.setAcceptanceThreshold(0.8);
    conf.setAcceptanceFile("accepted.nt");
    conf.setAcceptanceRelation("owl:sameAs");

    // Review
    conf.setVerificationThreshold(0.8);
    conf.setVerificationFile("reviewme.nt");
    conf.setVerificationRelation("owl:sameAs");

    // EXECUTION
    conf.setExecutionRewriter("default");
    conf.setExecutionPlanner("default");
    conf.setExecutionEngine("default");


    // Output format CSV etc
    conf.setOutputFormat("TTL");
    //callLimes(conf);
    System.out.println("successfully executed created configuration function");


    return conf;
  }

  public void callLimes(Configuration config) throws IllegalArgumentException {

    //String limesOutputLocation = "F://Newfolder//LIMES//t"; // for output

    String limesOutputLocation = new File("").getAbsolutePath();
    LimesResult mappings = Controller.getMapping(config);
    String outputFormat = config.getOutputFormat();
    ISerializer output = SerializerFactory.createSerializer(outputFormat);

    output.setPrefixes(config.getPrefixes());

    String workingDir = limesOutputLocation;// "F:\\Newfolder\\LIMES\\t";
    File verificationFile = new File(workingDir, config.getVerificationFile());
    File acceptanceFile = new File(workingDir, config.getAcceptanceFile());

    System.out.println(acceptanceFile.getAbsolutePath());
    System.out.println(verificationFile.getAbsolutePath());
    System.out.println(acceptanceFile.getPath());
    System.out.println(verificationFile.getPath());

    output.writeToFile(mappings.getVerificationMapping(), config.getVerificationRelation(),
      verificationFile.getPath().trim());
    output.writeToFile(mappings.getAcceptanceMapping(), config.getAcceptanceRelation(),
      acceptanceFile.getPath().trim());
    //	System.out.println(" acceptance mappings" + mappings.getAcceptanceMapping());
    //	System.out.println(" statisstics mappings " + mappings.getStatistics());
  }

  public List<Model> createSparQLEndpoint() {
    FileReader file1 = null;
    String nameOfFile="limesmodeloutput.rdf";

    try {
      file1 = new FileReader("accepted.nt");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block

    }


    try (BufferedReader br = new BufferedReader(file1)) {
      String line;
      while ((line = br.readLine()) != null) {
        // process the line.
        String[] split = line.split(" ");
        //System.out.println(Arrays.toString(split));
        //break;
        if (!split[0].equals("@prefix")) {
          int len1 = split[0].length();
          String KG1 = split[0].substring(26, len1 - 1);
          int len2 = split[2].length();
          String KG2 = split[2].substring(26, len2 - 1);
          insertSparQLEndpoint(KG1, KG2);
        }
      }
      modelToNextGroup.add(modelDefault);
      System.out.println(" -Completed- ");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      try {
        FileWriter out=new FileWriter(nameOfFile);
        modelDefault.write(out, "TTL");
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    return modelToNextGroup;
  }

  private static void insertSparQLEndpoint(String kG1, String kG2) {
    // TODO Auto-generated method stub
    FileWriter writer;
    String kG1Sparql = getSparQLEndpoint(kG1);
    String kG2Sparql1 = getSparQLEndpoint(kG2);
    String kg2 = getSparQLEndpoint(kG2);

   // node.asResource(kG2Sparql1);
   // String

    if (kG1Sparql != null && kg2 != null && !(kG1Sparql.equals(kg2))) {
      try {
       // kG1Sparql = kG1Sparql.replace("<","").replace(">","");
       // kg2 = kG2Sparql1.replaceAll("^\"|\"$", "");
     // Resource r= modelDefault.createResource(kG1Sparql).addLiteral(pop,kg2);
      Resource res = modelDefault.createResource(kG1Sparql);
      modelDefault.add(res,pop,modelDefault.createResource(kg2));
      modelDefault.write(new FileOutputStream("Check_the_output.ttl"), "TTL");
      System.out.println(modelDefault);
       /* writer = new FileWriter("Sparql.ttl", true);
        writer.write("<" + kG1Sparql + "> ");
        writer.write("<http://example.com/test#matches>");
        writer.write(" <" + kG2Sparql + ">");
        writer.write("\n");
        writer.close();*/
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();

      }
    }

  }


  private static String getSparQLEndpoint(String kG1) {
    // TODO Auto-generated method stub
    FileReader file = null;
    try {
      file = new FileReader("SparQLList.txt");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try (BufferedReader br = new BufferedReader(file)) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] split = line.split(" : ");
        if (split[0].equals(kG1))
          return split[1];
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }


}

