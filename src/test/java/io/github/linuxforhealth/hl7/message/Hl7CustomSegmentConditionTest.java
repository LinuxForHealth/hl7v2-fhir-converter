/*
 * (c) Te Whatu Ora, Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 * 
 * @author Stuart McGrigor
 */

package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

// This class uses the ability to create ADDITIONAL HL7 messages to convert weird HL7 messages
// that exercise the new conditional Resource Template functionality
//
// In these tests, the additional message definitions for (entirely ficticious) ADT^A11 messages
// are placed in src/test/resources/additional_resources/hl7/message/ADT_A11.yml
//
// ... and the mappings are placed in src/test/resources/hl7/resource/AllergyIntoleranceZ*.yml
//
//  We are verifying that we can extract FIELD, COMPONENT and SUB COMPONENT values from custom segments and use those
//  values in conditions.

class Hl7CustomSegmentConditionTest {

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


    // Custom Segment Field Condition Test ZFD
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZFD|1|L2|H1|four\r,ALLERGY",      // ZFD.3 = H1 --> allergy
                 "ADT^A11,ZFD|1|L2|H2|four\r,ALLERGY",      // ZFD.3 = H2 --> allergy
                 "ADT^A11,ZFD|1|L2|H3|four\r,INTOLERANCE",  // ZFD.3 = H3 --> intolerance
                })
    void testCustomSegmentFieldInCondition(String messageType, String zSegment, String aiType) throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        // An empty AL1 Segment...
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + messageType + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + zSegment;

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> allergyIntoleranceResources = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResources).hasSize(1)   // from ZFD segment
            .element(0).satisfies(ai -> {

                // Make sure the type field is present & correct
                assertThat(((AllergyIntolerance) ai).hasType()).isTrue();
                assertThat(((AllergyIntolerance) ai).getType()).hasToString(aiType);
            });



        // Confirm that there are no extra resources
        assertThat(e).hasSize(3);
    }


    // Custom Segment COMPONENT Condition Test ZCP
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZCP|1|A3^yy|xx^H1|four\r,ALLERGY",      // ZCP.3.2 = H1 --> allergy
                 "ADT^A11,ZCP|1|A3^yy|xx^H2|four\r,ALLERGY",      // ZCP.3.2 = H2 --> allergy
                 "ADT^A11,ZCP|1|A3^yy|xx^H3|four\r,INTOLERANCE",  // ZCP.3.2 = H3 --> intolerance
                })
    void testCustomSegmentComponentInCondition(String messageType, String zSegment, String aiType) throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        // An empty AL1 Segment...
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + messageType + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + zSegment;

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> allergyIntoleranceResources = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResources).hasSize(1)   // from ZFD segment
            .element(0).satisfies(ai -> {

                // Make sure the type field is present & correct
                assertThat(((AllergyIntolerance) ai).hasType()).isTrue();
                assertThat(((AllergyIntolerance) ai).getType()).hasToString(aiType);
            });



        // Confirm that there are no extra resources
        assertThat(e).hasSize(3);
    }


    // Custom Segment SUBCOMPONENT Condition Test ZCP
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZSC|1|zz&A3^yy|xx^ww&H1|four\r,ALLERGY",      // ZCP.3.2.2 = H1 --> allergy
                 "ADT^A11,ZSC|1|zz&A3^yy|xx^ww&H2|four\r,ALLERGY",      // ZCP.3.2.2 = H2 --> allergy
                 "ADT^A11,ZSC|1|zz&A3^yy|xx^ww&H3|four\r,INTOLERANCE",  // ZCP.3.2.2 = H3 --> intolerance
                })
    void testCustomSegmentSubComponentInCondition(String messageType, String zSegment, String aiType) throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        // An empty AL1 Segment...
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + messageType + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + zSegment;

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> allergyIntoleranceResources = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResources).hasSize(1)   // from ZFD segment
            .element(0).satisfies(ai -> {

                // Make sure the type field is present & correct
                assertThat(((AllergyIntolerance) ai).hasType()).isTrue();
                assertThat(((AllergyIntolerance) ai).getType()).hasToString(aiType);
            });



        // Confirm that there are no extra resources
        assertThat(e).hasSize(3);
    }



    private static void commonConfigFileSetup() throws IOException {
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "ADT_A11"); // Must need to define our weird ADT message
        prop.put("default.zoneid", "+08:00");
        // Location of custom (or merely additional) resources
        prop.put("additional.resources.location",  "src/test/resources/additional_resources");
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
