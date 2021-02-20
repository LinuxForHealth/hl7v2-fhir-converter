/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConverterConfiguration.class);

  private static final String SUPPORTED_HL7_MESSAGES = "supported.hl7.messages";
  private static final String BASE_PATH_RESOURCE = "base.path.resource";
  private static final String DEFAULT_ZONE_ID = "default.zoneid";
  private static final String CONFIG_PROPERTIES = "config.properties";
  private static final String ADDITIONAL_CONCEPT_MAPS_FILE = "additional.conceptmap.file";

  private static ConverterConfiguration configuration;

  private String resourceFolder;
  private boolean resourcefromClassPath;
  private List<String> supportedMessageTemplates;
  private ZoneId zoneId;
  private String additionalConceptmapFile;
  private ConverterConfiguration() {
    try {
      
      List<FileLocationStrategy> subs =
          Arrays.asList(new ConfigDirectoryLocationStrategy(),
              new ClasspathLocationStrategy());
      FileLocationStrategy strategy = new CombinedLocationStrategy(subs);

      Parameters params = new Parameters();

      FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
          new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)

              .configure(
                  params.properties().setFileName(CONFIG_PROPERTIES)
                      .setThrowExceptionOnMissing(true).setLocationStrategy(strategy)
                  .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
      Configuration config = builder.getConfiguration();

      String resourceLoc = config.getString(BASE_PATH_RESOURCE);
      if (StringUtils.isNotBlank(resourceLoc)) {
        resourceFolder = resourceLoc;
      } else {
        resourceFolder = "";
        resourcefromClassPath = true;
      }

      // get list of supported messages
      List<Object> values = config.getList(SUPPORTED_HL7_MESSAGES);

      supportedMessageTemplates =
          values.stream().filter(v -> v != null && StringUtils.isNotBlank(v.toString()))
              .map(v -> v.toString()).collect(Collectors.toList());


      // get default zone
      String zoneText = config.getString(DEFAULT_ZONE_ID, null);
      if (StringUtils.isNotBlank(zoneText)) {
        getZoneId(zoneText);
      }

      // get additional concept map
      additionalConceptmapFile = config.getString(ADDITIONAL_CONCEPT_MAPS_FILE, null);


    } catch (ConfigurationException e) {
      throw new IllegalStateException("Cannot read configuration for resource location", e);
    }
  }


  private void getZoneId(String zoneText) {
    try {
    zoneId = ZoneId.of(zoneText);
    } catch (DateTimeException e) {
      LOGGER.warn("Cannot create ZoneId from :" + zoneText, e);
      zoneId = null;
    }
  }


  public static ConverterConfiguration getInstance() {
    if (configuration == null) {
      configuration = new ConverterConfiguration();
    }
    return configuration;
  }


  public static void reset() {
    configuration = new ConverterConfiguration();
  }


  public String getResourceFolder() {
    return resourceFolder;
  }


  public boolean isResourcefromClassPath() {
    return resourcefromClassPath;
  }


  public void setResourcefromClassPath(boolean resourcefromClassPath) {
    this.resourcefromClassPath = resourcefromClassPath;
  }


  public List<String> getSupportedMessageTemplates() {
    return supportedMessageTemplates;
  }


  public ZoneId getZoneId() {
    return zoneId;
  }


  public String getAdditionalConceptmapFile() {
    return additionalConceptmapFile;
  }


}
