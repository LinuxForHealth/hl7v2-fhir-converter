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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

class Hl7ConditionalMappingTest {

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


    // Conditional Segment test - Custom SEGMENT value   --  ZSG
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZSG|L2\r,1", 
                 "ADT^A11,ZSG|A3\r,0",
                 "ADT^A11,ZSG|L2|Two\r,1",                    // L2 is a FIELD value, but SEGMENT comparison will return first field
                 "ADT^A11,ZSG|L2^Lookup List 2^WebPAS\r,0"    // L2 is a COMPONENT value so SEGMENT comparison will fail
                })
    void testConditionalSegmentValueIsSegment(String messageType, String zSegment, int aiCount) throws IOException {

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

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(aiCount);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(2 + aiCount);
    }

    // Conditional Segment test - Custom FIELD Value  -  ZFD-2
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZFD|1|L2\r,1", 
                 "ADT^A11,ZFD|1|A3\r,0",
                 "ADT^A11,ZFD|1|L2|Three|Four\r,1",
                 "ADT^A11,ZFD|L2\r,0",                         // L2 is a SEGMENT value so FIELD comparison will fail
                 "ADT^A11,ZFD|1|L2^Lookup List 2^WebPAS\r,0"   // L2 is a COMPONENT value so FIELD comparison will fail
                })
    void testConditionalSegmentValueIsField(String messageType, String zSegment, int aiCount) throws IOException {

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

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(aiCount);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(2 + aiCount);
    }

    // Conditional Segment test - Custom COMPONENT Value  -  ZCP.2.1
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZCP|1|A3^Allergies^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H1^Drug Reactions^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H3^Other Allergies^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H2^Health Conditions^WebPAS\r,0",             // H2 not IN set
                 "ADT^A11,ZCP|1|H4^Infection Prevention Alerts^WebPAS\r,0",   // H4 not IN set
                 "ADT^A11,ZCP|1|H3|three|four\r,1"                            // H3 in FIELD, yet COMPONENT comparison succeeds
                })
    void testConditionalSegmentValueIsComponent(String messageType, String zSegment, int aiCount) throws IOException {

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

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(aiCount);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(2 + aiCount);
    }

    // Conditional Segment test - Custom SUB COMPONENT Value  - ZSC-2.1.2
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZSC|1|pre&A3^Allergies^WebPAS\r,1", 
                 "ADT^A11,ZSC|1|pre&H1^Drug Reactions^WebPAS\r,1", 
                 "ADT^A11,ZSC|1|pre&H3^Other Allergies^WebPAS\r,1", 
                 "ADT^A11,ZSC|1|pre&H2^Health Conditions^WebPAS\r,0",             // H2 not IN set
                 "ADT^A11,ZSC|1|pre&H4^Infection Prevention Alerts^WebPAS\r,0",   // H4 not IN set
                 "ADT^A11,ZSC|1|H3|three|four\r,0",                               // H3 in FIELD so SUB COMPONENT comparison will fail
                 "ADT^A11,ZSC|1|H3^Other Allergies^WebPAS|three|four\r,0"         // H3 in COMPONENT, so SUB COMPONENT comparison will fail
                })
    void testConditionalSegmentValueIsSubComponent(String messageType, String zSegment, int aiCount) throws IOException {

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

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(aiCount);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(2 + aiCount);
    }


    // Conditional Segment test - repetition -  ZCR.2(1).1    -- repetitions start at Zero
     @ParameterizedTest
    @CsvSource({ "ADT^A11,ZCP|1|A3^Allergies^WebPAS~H2^Other Allergies^WebPAS\r,0", 
                 "ADT^A11,ZCP|1|H1^Drug Reactions^WebPAS~A3^Allergies^WebPAS\r,1" })
    void testConditionalSegmentRepetitions(String messageType, String zSegment, int aiCount) throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        // An empty AL1 Segment...
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + messageType + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "ZCP|1|A3^Allergies^WebPAS\r"                      // included
                + "ZCP|2|H1^Drug Reactions^WebPAS\r"                 // included
                + "ZCP|3|H2^Health Conditions^WebPAS\r"              // dropped
                + "ZCP|4|H3^Other Allergies^WebPAS\r"                // included
                + "ZCP|5|H4^Infection Prevention Alerts^WebPAS\r";   // dropped

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(3);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(5);
    }

    // Conditional Segment test - multiple segments -  make sure we don't get confusion in which segment we're testing
    @ParameterizedTest
    @CsvSource({ "ADT^A11,ZCP|1|A3^Allergies^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H1^Drug Reactions^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H2^Health Conditions^WebPAS\r,0", 
                 "ADT^A11,ZCP|1|H3^Other Allergies^WebPAS\r,1", 
                 "ADT^A11,ZCP|1|H4^Infection Prevention Alerts^WebPAS\r,0"})
    void testConditionalSegmentMultiples(String messageType, String zSegment, int aiCount) throws IOException {

        // Set up the config file
        commonConfigFileSetup();

        // An empty AL1 Segment...
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + messageType + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "ZCP|1|A3^Allergies^WebPAS\r"                      // included
                + "ZCP|2|H1^Drug Reactions^WebPAS\r"                 // included
                + "ZCP|3|H2^Health Conditions^WebPAS\r"              // dropped
                + "ZCP|4|H3^Other Allergies^WebPAS\r"                // included
                + "ZCP|5|H4^Infection Prevention Alerts^WebPAS\r";   // dropped

        List<BundleEntryComponent> e = getBundleEntryFromHL7Message(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(3);   // How many AllergyIntolerance segments ?

        // Confirm that there are no extra resources
        assertThat(e).hasSize(5);
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
