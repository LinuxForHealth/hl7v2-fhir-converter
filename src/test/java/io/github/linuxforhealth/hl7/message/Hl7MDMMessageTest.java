/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7MDMMessageTest {

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
    public void test_mdm_mininum_with_OBXtypeTXtoDocumenReference(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "TXA|1|OP^Operative Report|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\r"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA, OBX(type TX)

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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(1); // from OBX(not type TX)

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_mininum_with_multiple_OBXtypeTXtoDocumentReference(String message) throws IOException {
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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA, OBX(type TX)

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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(3); // from OBX (not type TX)

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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1); // from ORC, OBR

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA, OBX(type TX)

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "MDM^T02", "MDM^T06" })
    public void test_mdm_ORDER_with_OBXnotTX(String message) throws IOException {
        // Also check NTE working for MDM messages.
        String hl7message =
            "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||" + message + "|<MESSAGEID>|P|2.6\r"
            + "EVN||20150502090000|\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1||O||||||||||||||||||||||||||||||||||||||||||199501102300\r"
            + "ORC|NW|622470H432|||||^^^^^R||||||||||||||\r"
            + "OBR|1|622470H432|102397CE432|||20170725143849|20180102|||||||||||||||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1||||\r"
            // ServiceRequest NTE has a practitioner reference in NTE.4
            + "NTE|1|O|TEST ORC/OBR NOTE AA line 1||Pract1ID^Pract1Last^Pract1First|\n" 
            + "NTE|2|O|TEST NOTE AA line 2|\n"
            + "TXA|1|HP^History and physical examination|TX||||201801171442||||||||||||AV|||||\r"
            + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
            // Observation NTE has a practitioner reference in second NTE.4.
            + "NTE|1|L|TEST OBX NOTE BB line 1|\n" 
            + "NTE|2|L|TEST NOTE BB line 2||Pract2ID^Pract2Last^Pract2First|\n"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1); // from ORC, OBR
        // Light check that ServiceRequest contains NTE for ORC/OBR;  Deep check of NTE in Hl7NoteFHIRConverterTest.
        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);
        ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequests.get(0),
                ResourceUtils.context);
        assertThat(serviceRequest.hasNote()).isTrue();
        assertThat(serviceRequest.getNote()).hasSize(1);
        assertThat(serviceRequest.getNote().get(0).getText())
                .isEqualTo("TEST ORC/OBR NOTE AA line 1  \\nTEST NOTE AA line 2  \\n");
        assertThat(serviceRequest.getNote().get(0).hasAuthorReference()).isTrue();

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA

        List<Resource> observationResources = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResources).hasSize(1); // from OBX(not type TX)
        // Light check that Observation contains OBX for ORC/OBR;  Deep check of NTE in Hl7NoteFHIRConverterTest.
        Observation observation = ResourceUtils.getResourceObservation(observationResources.get(0), ResourceUtils.context);
        // Validate the note contents and reference existance.
        assertThat(observation.hasNote()).isTrue();
        assertThat(observation.getNote()).hasSize(1);
        assertThat(observation.getNote().get(0).getText())
                .isEqualTo("TEST OBX NOTE BB line 1  \\nTEST NOTE BB line 2  \\n");
        assertThat(observation.getNote().get(0).hasAuthorReference()).isTrue(); 

        // Two Practitioners, one for the serviceRequest, one for the Observation
        List<Resource> practitioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitioners).hasSize(2); // from NTE.4 references

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(7);
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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(3); // from ORC, OBR in ORDER groups

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA, OBX(type TX)

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

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(3); // from ORC, OVR in ORDER groups

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).hasSize(1); // from TXA

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(3); // from OBX (not TX type)

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(9);
    }

}
