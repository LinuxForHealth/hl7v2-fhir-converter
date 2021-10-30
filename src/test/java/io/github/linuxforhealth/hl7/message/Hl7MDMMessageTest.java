/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7MDMMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder().withBundleType(BundleType.COLLECTION)
        .withValidateResource().withPrettyPrint().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MDMMessageTest.class);

    //An example message for reference:
    // "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||MDM^T06|<MESSAGEID>|P|2.6\n"
    // + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
    // + "PV1|1|O|2GY^2417^W||||ATT_ID^ATT_LN^ATT_MI^ATT_FN^^^MD|REF_ID^REF_LN^REF_MI^REF_FN^^^MD|CONSULTING_ID^CONSULTING_LN^CONSULTING_MI^CONSULTING_FN^^^MD||||||||ADM_ID^ADM_LN^ADM_MI^ADM_FN^^^MD|OTW|<HospitalID>|||||||||||||||||||||||||20180115102400|20180118104500\n"
    // + "ORC|NW|622470H432|||||^^^^^R|||||123456789^MILLER^BOB|123D432^^^Family Practice Clinic||||||||FAMILY PRACTICE CLINIC\n"
    // + "OBR|1|622470H432|102397CE432|LAMIKP^AMIKACIN LEVEL, PEAK^83718||20170725143849|20180102||||L|||||123456789^MILLER^BOB|||REASON_TEXT_1|||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1|RESP_ID1&RESP_FAMILY_1&RESP_GIVEN1||TECH_1_ID&TECH_1_FAMILY&TECH_1_GIVEN|TRANS_1_ID&TRANS_1_FAMILY&TRANS_1_GIVEN\n"
    // + "ORC|NW|9494138H600|||||^^^^^R|||||1992779250^TEST^DOCTOR\n"
    // + "OBR|1|9494138H600^ORDER_PLACER_NAMESPACE_2|1472232CE600|83718^HIGH-DENSITY LIPOPROTEIN (HDL)^NAMING2||20150909170243|||||L|||||1992779250^TEST^DOCTOR||||||||CAT|A||^^^20180204^^R||||REASON_ID_2^REASON_TEXT_2|RESP_ID2&RESP_FAMILY_2&RESP_GIVEN2||TECH_2_ID&TECH_2_FAMILY&TECH_2_GIVEN|TRANS_2_ID&TRANS_2_FAMILY&TRANS_2_GIVEN\n"
    // + "TXA|1|05^Operative Report|TX|201801171442|5566^PAPLast^PAPFirst^J^^MD|201801171442|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>||4466^TRANSCLast^TRANSCFirst^J^^MD|<MESSAGEID>||P||AV\n"
    // + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\n"
    // + "OBX|2|TX|05^Operative Report||                             <HOSPITAL ADDRESS2>||||||P\n"
    // + "OBX|3|TX|05^Operative Report||                              <HOSPITAL ADDRESS2>||||||P\n";

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_mininum_with_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "TXA|1|OP^Operative Report|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_mininum_with_OBXnotTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "TXA|1|HP^History and physical examination|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        List<Resource> observationResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_mininum_with_multiple_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "TXA|1|OP^Operative Report|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\r"
            + "OBX|2|TX|05^Operative Report||                             <HOSPITAL ADDRESS>||||||P\r"
            + "OBX|3|TX|05^Operative Report||                             <HOSPITAL ADDRESS2>||||||P\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_mininum_with_multipleOBXnotTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "TXA|1|HP^History and physical examination|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
            + "OBX|2|ST|100||This is content|||||||X\r"
            + "OBX|3|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        List<Resource> observationResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(3);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_ORDER_group_and_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "TXA|1|OP^Operative Report|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> serviceRequestResource =
        e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_ORDER_with_OBXnotTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "TXA|1|HP^History and physical examination|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> serviceRequestResource =
        e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(1);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        List<Resource> observationResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_multiple_ORDERs_and_multiple_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "TXA|1|OP^Operative Report|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\r"
            + "OBX|2|TX|05^Operative Report||                             <HOSPITAL ADDRESS>||||||P\r"
            + "OBX|3|TX|05^Operative Report||                             <HOSPITAL ADDRESS2>||||||P\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> serviceRequestResource =
        e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(3);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_multiple_ORDERs_and_multiple_OBXnotTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            + "TXA|1|HP^History and physical examination|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
            + "OBX|2|ST|100||This is content|||||||X\r"
            + "OBX|3|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);

        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> serviceRequestResource =
        e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(3);

        List<Resource> documentReferenceResource =
        e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentReferenceResource).hasSize(1);

        List<Resource> observationResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(3);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(9);
    }

}
