/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;

public class ResourceModelReader {

  private static ResourceModelReader reader;

  /**
   * The top level resource folder/directory.
   * External locations are supported by using the base.path.resource property. If the property is not set, a
   * default location is assigned
   */
  private Path resourceFolder;

  /**
   * Creates the ResourceModelReader instance
   */
  private ResourceModelReader() {
    try {
      Configurations configs = new Configurations();
      // Read data from this file
      PropertiesConfiguration config = configs.properties(new File("config.properties"));
      String resourceLoc = config.getString("base.path.resource");
      resourceFolder= (StringUtils.isNotBlank(resourceLoc)) ? Paths.get(resourceLoc): Paths.get(Constants.DEFAULT_HL7_RESOURCES);
    } catch (ConfigurationException e) {
      throw new IllegalStateException("Cannot read configuration for resource location", e);
    }
  }


  /**
   * Loads a resource model from a path within a resource folder.
   * @param resourcePath The resource
   * @return
   */
  public ResourceModel generateResourceModel(String resourcePath) {

    Path templateFilePath = Paths.get(resourceFolder.toString(), resourcePath + ".yml");
    InputStream templateFileStream = null;
    HL7DataBasedResourceModel resourceModel = null;

    try {
      if (Files.exists(templateFilePath)) {
        templateFileStream = new FileInputStream(templateFilePath.toFile());
      } else {
        templateFileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(templateFilePath.toString());
      }
      resourceModel = ObjectMapperUtil
              .getYAMLInstance()
              .readValue(templateFileStream, HL7DataBasedResourceModel.class);

    } catch (IOException e) {
        throw new RuntimeException("Error accessing template file " + templateFilePath.toString(), e);
    }

    if (StringUtils.isBlank(resourceModel.getName()) ||
            StringUtils.equalsIgnoreCase(resourceModel.getName(), "unknown")) {
      resourceModel.setName(FilenameUtils.removeExtension(templateFilePath.toFile().getName()));
    }
    return resourceModel;
  }

  public static ResourceModelReader getInstance() {
    if (reader == null) {
      reader = new ResourceModelReader();
    }
    return reader;
  }



}
