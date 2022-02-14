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


/**
 * 
 * @author Krishna Madhav and Sowmya Kamath Ramesh
 *
 */
@Extension
public class StdOutModelWriter extends AbstractModelWriter {

	private static final Logger logger = LoggerFactory.getLogger(StdOutModelWriter.class);

	@Override
	public ValidatableParameterMap createParameterMap() {
		return ValidatableParameterMap.builder().declareValidationShape(getValidationModelFor(StdOutModelWriter.class))
				.build();
	}

	@Override
	protected List<Model> safeApply(List<Model> models) {

		System.out.println("Out*****1");
		Writer writer = new StringWriter();
		System.out.println("Out*****2");
		if (!models.isEmpty()) {
			models.get(0).write(writer, "TTL");
		}
		System.out.println("Out*****3");
		System.out.println(writer);
		System.out.println("Out*****4");
		return models;
	}
}