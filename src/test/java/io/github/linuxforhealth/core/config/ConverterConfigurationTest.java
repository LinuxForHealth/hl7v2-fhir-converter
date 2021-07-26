/*
 * (C) Copyright IBM Corp. 2020
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.terminology.UrlLookup;

public class ConverterConfigurationTest {

    @TempDir
    static File folder;

    @AfterEach
    public void reset() {
        System.clearProperty("config.home");
        ConverterConfiguration.reset();
        UrlLookup.reset();
    }

    @Test
    public void test_that_additional_conceptmap_values_are_loaded() throws IOException {
    	File configFile = new File(folder, "config.properties");
        writeProperties(configFile);
        System.setProperty("config.home", configFile.getParent());
        ConverterConfiguration.reset();
        UrlLookup.reset(Constants.CODING_SYSTEM_MAPPING);
        String url = UrlLookup.getSystemUrl("LN");
        assertThat(url).isEqualTo("http://loinc-additional.org");
        UrlLookup.reset(Constants.EXTENSION_URL_MAPPING);
        url = UrlLookup.getExtensionUrl("mothersMaidenName");
        assertThat(url).isEqualTo("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    }

    private void writeProperties(File configFile) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.put("supported.hl7.messages", "ADT_A01, ORU_R01, PPR_PC1, VXU_V04");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.conceptmap.file", "src/test/resources/additional_conceptmap.yml");
        prop.store(new FileOutputStream(configFile), null);
    }

    /** Test will run 2nd due to alphabetical order, this order is important **/
    @Test
    public void test_that_config_reset_reloads_configuration() throws IOException {
        UrlLookup.reset(Constants.CODING_SYSTEM_MAPPING);
        String url = UrlLookup.getSystemUrl("LN");
        assertThat(url).isEqualTo("http://loinc.org");

        UrlLookup.reset(Constants.EXTENSION_URL_MAPPING);
        url = UrlLookup.getExtensionUrl("mothersMaidenName");
        assertThat(url).isEqualTo("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
    }

}
