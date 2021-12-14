/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.terminology.UrlLookup;

class ConverterConfigurationTest {

    private static final String CONF_PROP_HOME = "hl7converter.config.home";

    @TempDir
    static File folder;

    static String originalConfigHome;

    @BeforeAll
    static void saveConfigHomeProperty() {
        originalConfigHome = System.getProperty(CONF_PROP_HOME);
    }

    @AfterEach
    void reset() {
        System.clearProperty(CONF_PROP_HOME);
        ConverterConfiguration.reset();
        UrlLookup.reset();
    }

    @AfterAll
    static void reloadPreviousConfigurations() {
        if (originalConfigHome != null)
            System.setProperty(CONF_PROP_HOME, originalConfigHome);
        else
            System.clearProperty(CONF_PROP_HOME);
        UrlLookup.reset();
    }

    @Test
    void test_that_additional_conceptmap_values_are_loaded() throws IOException {
        File configFile = new File(folder, "config.properties");
        writeProperties(configFile);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
        ConverterConfiguration.reset();
        UrlLookup.reset(Constants.CODING_SYSTEM_MAPPING);
        String url = UrlLookup.getSystemUrl("LN");
        assertThat(url).isEqualTo("http://loinc-additional.org");
        UrlLookup.reset(Constants.EXTENSION_URL_MAPPING);
        url = UrlLookup.getExtensionUrl("mothersMaidenName");
        assertThat(url).isEqualTo("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    }

    @Test
    void testConfigurationValuesAreSetAndRetrieved() throws IOException {
        // Create our own properties file
        File configFile = new File(folder, "config.properties");
        writeProperties(configFile);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
        ConverterConfiguration.reset();
        ConverterConfiguration theConvConfig = ConverterConfiguration.getInstance();
        assertThat(theConvConfig.getResourceFolder()).isEqualTo("src/main/resources");
        assertThat(theConvConfig.getSupportedMessageTemplates()).hasSize(4); // Four messages supported.  (Proves we're using our created file, not the default.)
        assertThat(theConvConfig.getZoneId().getId()).isEqualTo("+08:00");
        assertThat(theConvConfig.getAdditionalConceptmapFile())
                .isEqualTo("src/test/resources/additional_conceptmap.yml");
        assertThat(theConvConfig.getAdditionalResourcesLocation()).isEqualTo("src/test/resources/additional_resources");
    }

    private void writeProperties(File configFile) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "ADT_A01, ORU_R01, PPR_PC1, VXU_V04");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.conceptmap.file", "src/test/resources/additional_conceptmap.yml");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
    }

    @Test
    void testConfigurationDefaultsAreUsed() throws IOException {
        File configFile = new File(folder, "config.properties");
        writePropertiesDefaultMessages(configFile);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
        ConverterConfiguration.reset();
        ConverterConfiguration theConvConfig = ConverterConfiguration.getInstance();
        assertThat(theConvConfig.getResourceFolder()).isEmpty(); // No resource folder specified
        assertThat(theConvConfig.getSupportedMessageTemplates()).hasSize(1); // Because there was an *, there is only one message template configured
        assertThat(theConvConfig.getSupportedMessageTemplates().get(0)).contains("*"); // * indicates search for templates.
        assertThat(theConvConfig.getAdditionalConceptmapFile()).isNull();
        assertThat(theConvConfig.getAdditionalResourcesLocation()).isNull();
    }

    private void writePropertiesDefaultMessages(File configFile) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.put("default.zoneid", "+08:00");
        prop.store(new FileOutputStream(configFile), null);
    }

    /** Test will run 2nd due to alphabetical order, this order is important **/
    @Test
    void test_that_config_reset_reloads_configuration() throws IOException {
        UrlLookup.reset(Constants.CODING_SYSTEM_MAPPING);
        String url = UrlLookup.getSystemUrl("LN");
        assertThat(url).isEqualTo("http://loinc.org");

        UrlLookup.reset(Constants.EXTENSION_URL_MAPPING);
        url = UrlLookup.getExtensionUrl("mothersMaidenName");
        assertThat(url).isEqualTo("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    }

}
