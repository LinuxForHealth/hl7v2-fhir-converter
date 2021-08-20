/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.apache.commons.configuration2.io.FileLocator;
import org.apache.commons.configuration2.io.FileSystem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigDirectoryLocationStrategy implements FileLocationStrategy {

  private static final String ENV_CONF_PROP_HOME = "hl7converter.config.home";
  private static final String CONF_PROP_HOME = "config.home";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConfigDirectoryLocationStrategy.class);

  /** The home directory to be searched for the requested file. */
  private final String confDirectory;


  public ConfigDirectoryLocationStrategy() {
    confDirectory = fetchHomeDirectory();
  }



  public String getConfDirectory() {
    return confDirectory;
  }


  /**
   * {@inheritDoc} This implementation searches in the home directory for a file described by the
   * passed in {@code FileLocator}. If the locator defines a base path and the
   * {@code evaluateBasePath} property is <b>true</b>, a sub directory of the home directory is
   * searched.
   */
  @Override
  public URL locate(final FileSystem fileSystem, final FileLocator locator) {
    if (StringUtils.isNotEmpty(locator.getFileName())) {
      if (confDirectory != null) {
        LOGGER.info("Looking for config file in location {} based on config.home system property.",
            confDirectory);
      final File file = new File(confDirectory, locator.getFileName());

      if (file.isFile()) {
        try {
          return file.toURI().toURL();
        } catch (MalformedURLException e) {
          return null;
        }
      }
    }
    }
    return null;
  }



  private static String fetchHomeDirectory() {
	String homeDirectory = System.getenv(ENV_CONF_PROP_HOME);
	if (homeDirectory==null || homeDirectory.isBlank())
		return System.getProperty(CONF_PROP_HOME);
	return homeDirectory;
  }
}
