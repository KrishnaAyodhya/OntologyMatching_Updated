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
	  
    Writer writer = new StringWriter();
    models.get(0).write(writer, "TTL");
    System.out.println(writer);
    return models;
  }
}