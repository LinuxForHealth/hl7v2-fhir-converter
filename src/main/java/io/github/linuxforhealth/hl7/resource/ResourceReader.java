/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.message.HL7FHIRResourceTemplate;
import io.github.linuxforhealth.hl7.message.HL7FHIRResourceTemplateAttributes;
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
   * Loads a file based resource using a three pass approach. The first pass attempts to load the
   * resource from the file system. The second attempts to load it from the alternate resource path.
   * on the file system. If the file is not found on the file system, the resource is
   * loaded from the classpath.
   * 
   * @param resourcePath The relative path to the resource (hl7/, fhir/, etc)
   * @return The resource as a String
   */
  public String getResource(String resourcePath) {
    String resourceFolderName = converterConfig.getResourceFolder();
    String additionalResourcesFolderName = converterConfig.getAdditionalResourcesLocation();
    String resource = null;

    try {
      if (resourceFolderName != null) {
        Path resourceFolderFilePath = Paths.get(resourceFolderName, resourcePath);
        if (resourceFolderFilePath.toFile().exists()) {
          resource = loadFileResource(resourceFolderFilePath.toFile());
        }
      }

      if (resource == null && additionalResourcesFolderName != null) {
        Path alternateResourceFolderFilePath = Paths.get(additionalResourcesFolderName, resourcePath);

        if (alternateResourceFolderFilePath.toFile().exists()) {
          resource = loadFileResource(alternateResourceFolderFilePath.toFile());
        }
      }

      if (resource == null) {
        resource = loadClassPathResource(resourcePath);
      }
    } catch (IOException ioEx) {
      String msg = "Unable to load resource " + resourcePath;
      throw new IllegalArgumentException(msg, ioEx);
    }
    return resource;
  }

  /**
   * Returns all message templates in the configured location(s)
   * Relies on the values in config.properties.
   * 
   * @return Map of messages, by message title.
   */
  public Map<String, HL7MessageModel> getMessageTemplates() {
    Map<String, HL7MessageModel> messagetemplates = new HashMap<>();
    List<String> supportedMessageTemplates = ConverterConfiguration.getInstance().getSupportedMessageTemplates();
    if (hasWildcard(supportedMessageTemplates)) {
      // Code currently assumes we do no use the list of supported messages, once we see an *.
      // In future if needed to merge, it would go here.
      supportedMessageTemplates.clear();
      supportedMessageTemplates = findAllMessageTemplateNames();
    }
    for (String template : supportedMessageTemplates) {
      HL7MessageModel rm = getMessageModel(template);
      messagetemplates.put(com.google.common.io.Files.getNameWithoutExtension(template),
          rm);
    }
    return messagetemplates;
  }

  private boolean hasWildcard(List<String> supportedMessageTemplates) {
    for (String template : supportedMessageTemplates) {
      if (template.contains("*")) {
        return true;
      }
    }
    return false;
  }

  private List<String> findAllMessageTemplateNames() {

    // Get the base templates
    List<String> foundTemplates = new ArrayList<>();
    File folder = new File(
        converterConfig.getResourceFolder() + '/' + Constants.HL7_BASE_PATH + Constants.MESSAGE_BASE_PATH);
    File[] listOfFiles = folder.listFiles();
    for (int i = 0; listOfFiles != null && i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        foundTemplates.add(listOfFiles[i].getName());
      }
    }

    // Add the extended templates
    // Current code assumes the extended templates are exclusive of the base templates.
    // In future, if there is to be a priority, or one can override the other, changes needed here to merge extended templates.
    folder = new File(
        converterConfig.getAdditionalResourcesLocation() + '/' + Constants.HL7_BASE_PATH + Constants.MESSAGE_BASE_PATH);
    listOfFiles = folder.listFiles();
    for (int i = 0; listOfFiles != null && i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        foundTemplates.add(listOfFiles[i].getName());
      }
    }

    return foundTemplates;
  }

  private HL7MessageModel getMessageModel(String templateName) {
    // Allow for names that already have .yml extension
    String yamlizedTemplateName = templateName.endsWith(".yml") ? templateName : templateName + ".yml";
    String templateFileContent = getResourceInHl7Folder(Constants.MESSAGE_BASE_PATH + yamlizedTemplateName);
    if (StringUtils.isNotBlank(templateFileContent)) {
      try {

        JsonNode parent = ObjectMapperUtil.getYAMLInstance().readTree(templateFileContent);
        Preconditions.checkState(parent != null, "Parent node from template file cannot be null");

        JsonNode resourceNodes = parent.get("resources");
        Preconditions.checkState(resourceNodes != null && !resourceNodes.isEmpty(),
            "List of resources from Parent node from template file cannot be null or empty");
        List<HL7FHIRResourceTemplateAttributes> templateAttributes = ObjectMapperUtil.getYAMLInstance().convertValue(
            resourceNodes,
            new TypeReference<List<HL7FHIRResourceTemplateAttributes>>() {
            });

        List<HL7FHIRResourceTemplate> templates = new ArrayList<>();

        templateAttributes.forEach(t -> templates.add(new HL7FHIRResourceTemplate(t)));
        Preconditions.checkState(templateAttributes != null && !templateAttributes.isEmpty(),
            "TemplateAttributes generated from template file cannot be null or empty");
        return new HL7MessageModel(templateName, templates);

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
      InjectableValues injValues = new InjectableValues.Std().addValue("resourceName", path);
      return ObjectMapperUtil.getYAMLInstance().setInjectableValues(injValues)
          .readValue(templateFileContent, HL7DataBasedResourceModel.class);

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

  public static void reset() {
    reader = null;
  }

  public String getResourceInHl7Folder(String path) {
    return getResource(Constants.HL7_BASE_PATH + path);
  }

}
