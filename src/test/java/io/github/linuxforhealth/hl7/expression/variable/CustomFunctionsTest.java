package io.github.linuxforhealth.hl7.expression.variable;

/*
 * (C) Copyright Te Whatu Ora - Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Test to make sure we can put customFunctions into variables in JEXL expressions
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Patient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class CustomFunctionsTest {

    // We're going to play with the configuration for these tests...
    private static final String CONF_PROP_HOME = "hl7converter.config.home";

    @TempDir
    static File folder;

    static String originalConfigHome;

    @BeforeAll
    static void saveConfigHomeProperty() {
        originalConfigHome = System.getProperty(CONF_PROP_HOME);
        folder.setWritable(true);
    }

    @AfterEach
    void reset() {
        System.clearProperty(CONF_PROP_HOME);
        ConverterConfiguration.reset();
        UrlLookup.reset();
        folder.setWritable(true);
    }

    @AfterAll
    static void reloadPreviousConfigurations() {
        if (originalConfigHome != null)
            System.setProperty(CONF_PROP_HOME, originalConfigHome);
        else
            System.clearProperty(CONF_PROP_HOME);
        UrlLookup.reset();
        folder.setWritable(true);
        folder.delete();
    }

    // Don't instantiate the converter quite yet
    private HL7ToFHIRConverter ftv;


    // We're going to write a special config...
    private void writeProperties(File configFile) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "ADT_A09");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.conceptmap.file", "src/test/resources/additional_conceptmap.yml");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
    }

    @ParameterizedTest
    //
    //  ADT_A09 is an "additional" message type - with customPatient  mapping template.
    //
    @ValueSource(strings = { "ADT^A09" })
    void testAdtCustomExtension(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|Smythe||F||||||||||||||||||Y||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r";

        // Let's play with the configuration before we build the Converter
        File configFile = new File(folder, "config.properties");
        writeProperties(configFile);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());

        // re-initialise ConverterConfiguration with the adjusted config
        ConverterConfiguration.reset();
        ConverterConfiguration.getInstance();

        // Now build the Converter
        ftv =  new HL7ToFHIRConverter();

        // Make the FHIR bundle
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv, hl7message);

        // Check things worked ok
        List<Resource> patList = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patList)
            .hasSize(1)
            .element(0).satisfies( pat -> {

                // Did the CustomUtils.ynuCode()  call work ??
                assertThat(((Patient) pat).getExtensionsByUrl("customCitizenship"))
                    .hasSize(1)
                    .element(0).satisfies( cshp -> {
                        assertThat(cshp.getValue()).hasToString("yes");
                    });
            });

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        // Confirm that there are no extra resources
        assertThat(e).hasSize(2);
    }

}
