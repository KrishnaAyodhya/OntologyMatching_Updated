package org.aksw.deer.plugin.example;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.aksw.deer.io.AbstractModelReader;
import org.aksw.deer.vocabulary.DEER;
import org.aksw.faraday_cage.engine.ValidatableParameterMap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class DirectoryReader extends AbstractModelReader {
  private static final Logger logger = LoggerFactory.getLogger(DirectoryReader.class);

  public static final Property FROM_PATH = DEER.property("fromPath");


  @Override
  public ValidatableParameterMap createParameterMap() {
    return ValidatableParameterMap.builder()
      .declareProperty(FROM_PATH)
      .declareValidationShape(getValidationModelFor(DirectoryReader.class))
      .build();
  }

  @Override
  protected List<Model> safeApply(List<Model> data) {
    final Optional<String> path = getParameterMap().getOptional(FROM_PATH)
      .map(RDFNode::asLiteral).map(Literal::getString);
    String locator = "";
    if (path.isPresent()) {
      try {
        locator = injectWorkingDirectory(path.get());
      } catch (InvalidPathException e) {

        locator = path.get();
      }
    }
    System.out.println("locator:"+locator);
    System.out.println("path:"+path.get());
    String dirPath = locator.replace("\\","");
    final long startTime = System.currentTimeMillis();
    List<Model> li=new ArrayList<>();
    File f = new File(dirPath);
    String absolutePath = f.getAbsolutePath();
    System.out.println("absolutePath:" + absolutePath);
    final File folder = new File(absolutePath+"\\");

    for (final File fileentry: folder.listFiles())
    {
      if (fileentry.getName().endsWith(".nt"))
      {
        Model result = ModelFactory.createDefaultModel();
        RDFDataMgr.read(result, absolutePath+"\\"+fileentry.getName(), Lang.N3);
        li.add(result);
      }
    }

    logger.info("Loading {} is done in {}ms.", locator,
      (System.currentTimeMillis() - startTime));
    return li;
  }
}

