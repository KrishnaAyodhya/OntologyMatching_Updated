package org.aksw.deer.plugin.example;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KGMatchingPlugin extends Plugin {

  private static final Logger logger = LoggerFactory.getLogger(KGMatchingPlugin.class);

  public KGMatchingPlugin(PluginWrapper wrapper) {
    super(wrapper);
  }

  @Override
  public void start() {
    logger.info("WelcomePlugin.start()");
  }

  @Override
  public void stop() {
    logger.info("WelcomePlugin.stop()");
  }

}
