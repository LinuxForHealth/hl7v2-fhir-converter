/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;

public class HL7ResourceReaderTest {

	private static final String CONF_PROP_HOME = "hl7converter.config.home";

  @TempDir
  static File folder;
  
  static String originalConfigHome;
  
  @BeforeAll
  public static void saveConfigHomeProperty() {
    originalConfigHome = System.getProperty(CONF_PROP_HOME);
  }

  @AfterEach
  public void reset() {
      System.clearProperty(CONF_PROP_HOME);
      ConverterConfiguration.reset();
      ResourceReader.reset();
  }

  @AfterAll
  public static void reloadPreviousConfigurations() {
    if (originalConfigHome != null)
      System.setProperty(CONF_PROP_HOME, originalConfigHome);
    else
      System.clearProperty(CONF_PROP_HOME);
  }

  // This tests that messagetemplates are still loaded the old way via class path
  // Create a config without base.path.resource and additional.resources.location properties forcing the files to be found via classpath
  @Test
  public void testGetMessageTemplatesViaClasspath() throws IOException {
    try {
      // Set up the config file
      File configFile = new File(folder, "config.properties");
      Properties prop = new Properties();
      prop.put("supported.hl7.messages", "ADT_A01, ORU_R01, PPR_PC1, VXU_V04");
      prop.put("default.zoneid", "+08:00");
      prop.store(new FileOutputStream(configFile), null);
      System.setProperty(CONF_PROP_HOME, configFile.getParent());

      // Get the templates
      Map<String, HL7MessageModel> messagetemplates = ResourceReader.getInstance().getMessageTemplates();
      assertThat(messagetemplates.containsKey("ORU_R01")).isTrue();
      assertThat(messagetemplates.containsKey("ADT_A09")).isFalse();
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Failure to initialize the templates for the converter.", e);
    }

  }

  // This tests that messagetemplates are loaded the new way via configured path + alternate path
  @Test
  public void testGetMessageTemplatesViaAdditionalLocation() throws IOException {
    try {
      // Set up the config file
      File configFile = new File(folder, "config.properties");
      Properties prop = new Properties();
      prop.put("base.path.resource", "src/main/resources");
      prop.put("supported.hl7.messages", "*");
      prop.put("default.zoneid", "+08:00");
      prop.put("additional.resources.location", "src/test/resources/additional_resources");
      prop.store(new FileOutputStream(configFile), null);
      System.setProperty(CONF_PROP_HOME, configFile.getParent());

      // Get the templates ORU_R01 will be found in the base path and ADT_A09 will be found in the additional path
      Map<String, HL7MessageModel> messagetemplates = ResourceReader.getInstance().getMessageTemplates();
      assertThat(messagetemplates.containsKey("ORU_R01")).isTrue(); // found in the base path
      assertThat(messagetemplates.containsKey("ADT_A09")).isTrue(); // found in the additional path
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Failure to initialize the templates for the converter.", e);
    }

  }

    // This tests that messagetemplates are loaded the new way via configured path + alternate path
    // AND that they are found when supported.hl7.messages is omitted and defaults to *
    @Test
    public void testGetMessageTemplatesViaAdditionalLocationWithDefaultSupportedList() throws IOException {
      try {
        // Set up the config file
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
  
        // Get the templates ORU_R01 will be found in the base path and ADT_A09 will be found in the additional path
        Map<String, HL7MessageModel> messagetemplates = ResourceReader.getInstance().getMessageTemplates();
        assertThat(messagetemplates.containsKey("ORU_R01")).isTrue(); // found in the base path
        assertThat(messagetemplates.containsKey("ADT_A09")).isTrue(); // found in the additional path
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Failure to initialize the templates for the converter.", e);
      }
  
    }




}

