/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;

/**
 * Reads resources. If the configuration file has base path defined (base.path.resource) then the
 * resources are loaded from that path. If the configuration is not defined then default path would
 * be used.
 * 
 *
 * @author pbhallam
 */
public class ResourceReader {

  private final Logger LOGGER = LoggerFactory.getLogger(ResourceReader.class);

  private static ResourceReader reader;

  private final ConverterConfiguration converterConfig = ConverterConfiguration.getInstance();

  /**
   * Loads a file resource configuration, returning a String
   * 
   * @param fileResourceConfiguration The configuration resource to load
   * @return String
   * @throws IOException if an error occurs loading the file resource
   */
  private static String loadFileResource(File fileResourceConfiguration) throws IOException {
    return FileUtils.readFileToString(fileResourceConfiguration, StandardCharsets.UTF_8);
  }

  /**
   * Loads a class path configuration resource, returning a String
   * 
   * @param resourceConfigurationPath The class path configuration resource
   * @return String the resource content
   * @throws IOException if an error occurs loading the configuration resource
   */
  private static String loadClassPathResource(String resourceConfigurationPath) throws IOException {
    return IOUtils.resourceToString(resourceConfigurationPath, StandardCharsets.UTF_8,
        ResourceReader.class.getClassLoader());
  }

  /**
   * Loads a file based resource using a two pass approach. The first pass attempts to load the
   * resource from the file system. if the file is not found on the file system, the resource is
   * loaded from the classpath.
   * 
   * @param resourcePath The relative path to the resource (hl7/, fhir/, etc)
   * @return The resource as a String
   */
  public String getResource(String resourcePath) {
    Path filePath = Paths.get(converterConfig.getResourceFolder(), resourcePath);
    String resource;

    try {
      if (filePath != null && filePath.toFile().exists()) {
        resource = loadFileResource(filePath.toFile());
      } else {
        resource = loadClassPathResource(resourcePath);
      }
    } catch (IOException ioEx) {
      String msg = "Unable to load resource " + resourcePath;
      throw new IllegalArgumentException(msg, ioEx);
    }
    return resource;
  }

  public Map<String, HL7MessageModel> getMessageTemplates() throws IOException {
    Map<String, HL7MessageModel> messagetemplates = new HashMap<>();
    List<Object> supportedMessageTemplates =
        ConverterConfiguration.getInstance().getSupportedMessageTemplates();
    for (Object template : supportedMessageTemplates) {
      HL7MessageModel rm = getMessageModel(template.toString());
      messagetemplates.put(com.google.common.io.Files.getNameWithoutExtension(template.toString()),
          rm);
    }
    return messagetemplates;
  }



  private HL7MessageModel getMessageModel(String templateName) {

    String templateFileContent =
        getResourceInHl7Folder(Constants.MESSAGE_BASE_PATH + templateName + ".yml");
    if (StringUtils.isNotBlank(templateFileContent)) {
      try {

        HL7MessageModel rm = ObjectMapperUtil.getYAMLInstance().readValue(templateFileContent,
            HL7MessageModel.class);
        rm.setMessageName(templateName);
        return rm;
      } catch (IOException e) {
        throw new IllegalArgumentException(
            "Error encountered in processing the template" + templateName, e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + templateName);
    }

  }

  public ResourceModel generateResourceModel(String path) {
    Preconditions.checkArgument(StringUtils.isNotBlank(path), "Path for resource cannot be blank");
    String templateFileContent = getResourceInHl7Folder(path + ".yml");

    try {
      HL7DataBasedResourceModel rm = ObjectMapperUtil.getYAMLInstance()
          .readValue(templateFileContent, HL7DataBasedResourceModel.class);
      if (StringUtils.isBlank(rm.getName())
          || StringUtils.equalsIgnoreCase(rm.getName(), "unknown")) {
        rm.setName(path);
      }
      return rm;
    } catch (IOException e) {
      throw new IllegalArgumentException("Error encountered in processing the template" + path, e);
    }


  }

  public static ResourceReader getInstance() {
    if (reader == null) {
      reader = new ResourceReader();
    }
    return reader;
  }



  public String getResourceInHl7Folder(String path) {

    return getResource(Constants.HL7_BASE_PATH + path);
  }



}
