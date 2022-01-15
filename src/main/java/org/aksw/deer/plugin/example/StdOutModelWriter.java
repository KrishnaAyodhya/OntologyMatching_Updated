package org.aksw.deer.plugin.example;

import org.aksw.deer.io.AbstractModelWriter;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
import org.apache.jena.rdf.model.Model;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

@Extension
public class StdOutModelWriter extends AbstractModelWriter {

  private static final Logger logger = LoggerFactory.getLogger(StdOutModelWriter.class);

  @Override
  public ValidatableParameterMap createParameterMap() {
    return ValidatableParameterMap.builder()
      .declareValidationShape(getValidationModelFor(StdOutModelWriter.class))
      .build();
  }

  @Override
  protected List<Model> safeApply(List<Model> models) {
	  
	  System.out.println(" alph models: " + models );
	  System.out.println(" alph models end: " );
	  
	  System.out.println(" alph models models.get(0): " +  models.get(0)); 
 
    Writer writer = new StringWriter();
    models.get(0).write(writer, "TTL");
    System.out.println("before -kd  : " + writer);
    System.out.println(writer);
    System.out.println("after -kd : " + writer);
     return models;
  }
}