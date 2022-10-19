/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

// This shows how and tests the ability to create a custom Hl7 class
// Detailed documentation about how this works is found here: 
// http://javadox.com/ca.uhn.hapi/hapi-base/2.1/ca/uhn/hl7v2/parser/DefaultModelClassFactory.html#packageList(java.lang.String)
// In this test, the custom class which HL7 uses for validation is placed in src/test/java/org/foo/hl7/custom/message/CUSTOM_PAT.java
// The custom message is placed in src/test/resources/additional_custom_resources/hl7/message/CUSTOM_PAT.yml
// The custom packages class is placed in src/test/java/custom_packages/2.6 and references the custom package /org/foo/hl7/custom/

class Hl7CustomMessageTest {

    // # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
    // NOTE VALIDATION IS INTENTIONALLY NOT USED BECAUSE WE ARE CREATING RESOURCES THAT ARE NOT STANDARD
    private static final ConverterOptions OPTIONS = new Builder().withPrettyPrint().build();
    // # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

    private static final String CONF_PROP_HOME = "hl7converter.config.home";

    @TempDir
    static File folder;

    static String originalConfigHome;

    @BeforeAll
    static void saveConfigHomeProperty() {
        originalConfigHome = System.getProperty(CONF_PROP_HOME);
        ConverterConfiguration.reset();
        ResourceReader.reset();
        folder.setWritable(true);
    }

    @AfterEach
    void reset() {
        System.clearProperty(CONF_PROP_HOME);
        ConverterConfiguration.reset();
        ResourceReader.reset();
    }

    @AfterAll
    static void reloadPreviousConfigurations() {
        if (originalConfigHome != null)
            System.setProperty(CONF_PROP_HOME, originalConfigHome);
        else
            System.clearProperty(CONF_PROP_HOME);
        folder.setWritable(true);
    }

    @Test
    void testCustomPatMessage() throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        String hl7message = "MSH|^~\\&|||||20211005105125||CUSTOM^PAT|1a3952f1-38fe-4d55-95c6-ce58ebfc7f10|P|2.6\n"
                + "PID|1|100009^^^FAC^MR|100009^^^FAC^MR||DOE^JANE||195001010000|M|||||5734421788|||U\n"
                + "PRB|1|20211005|10281^LYMPHOID LEUKEMIA NEC^ICD9||||201208061011||201208061011|||||||201208061011\n"
                + "PRB|2|20211005|11334^ABNORMALITIES OF HAIR^ICD9||||201208071000||201208071000|||||||201208071000\n"
                + "AL1|50|DA|penicillin|MO||20210629\n"
                + "AL1|50|MA|cat dander|SV|hives\\R\\ difficult breathing|20210629\n";

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        // Check for the expected resources 1 patient, 2 conditions, 2 allergies
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // From PID

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(2); // From 2x PRB

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2); // From 2x AL1

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(5);
    }

    private static void commonConfigFileSetup() throws IOException {
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "*"); // Must use wild card so the custom resources are found.
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_custom_resources"); // Location of custom resources
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());
    }

    // Need custom convert sequence with options that turn off FHIR validation.
    private static List<BundleEntryComponent> getBundleEntryFromHL7Message(String hl7message) {
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter(); // Testing loading of config which happens once per instantiation
        String json = ftv.convert(hl7message, OPTIONS); // Need custom options that turn off FHIR validation.
        assertThat(json).isNotNull();
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        return b.getEntry();
    }

}
