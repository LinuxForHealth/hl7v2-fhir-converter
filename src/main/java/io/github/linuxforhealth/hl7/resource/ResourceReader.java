/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static ResourceReader reader;

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReader.class);


  public String getResource(String filePath) {
    LOGGER.info("Attempting to read resource {}", filePath);
    String basePath = ConverterConfiguration.getInstance().getResourceFolder();
    File f = new File(basePath, filePath);
    boolean resourcefromClassPath = ConverterConfiguration.getInstance().isResourcefromClassPath();
    String resource;
    try {
      if (resourcefromClassPath) {
        LOGGER.info("Loading resource {}", f.getPath());

        try (InputStream inputStream = this.getClass().getResourceAsStream(f.getPath())) {
          resource = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
      } else {
        resource = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
      }
      return resource;
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      throw new IllegalArgumentException("IOexception encountered:" + f.getPath(), e);
    }

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

        HL7MessageModel rm =
            ObjectMapperUtil.getYAMLInstance().readValue(templateFileContent,
                HL7MessageModel.class);
        rm.setMessageName(templateName);
        return rm;
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
        throw new IllegalArgumentException(
            "Error encountered in processing the template" + templateName, e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + templateName);
    }

  }

  public ResourceModel generateResourceModel(String path) {

    String templateFileContent = getResourceInHl7Folder(path + ".yml");

    if (StringUtils.isNotBlank(templateFileContent)) {
      try {
        HL7DataBasedResourceModel rm = ObjectMapperUtil.getYAMLInstance()
            .readValue(templateFileContent, HL7DataBasedResourceModel.class);
        if (StringUtils.isBlank(rm.getName())
            || StringUtils.equalsIgnoreCase(rm.getName(), "unknown")) {
          rm.setName(path);
        }
        return rm;
      } catch (IOException e) {
        throw new IllegalArgumentException("Error encountered in processing the template" + path,
            e);
      }
    } else {
      throw new IllegalArgumentException("File not present:" + path);
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
