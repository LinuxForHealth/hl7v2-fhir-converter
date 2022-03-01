/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7RDEMessageTest {

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdeMinimumPatientAndMinimumOrder(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    // In the HL7 spec RXR is required for these messages, however, we can handle having no RXR.
    void testRdeMinimumPatientAndMinimumOrderWithoutRXR(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdePatientWithPatientVisitAndMinimumOrder(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdePatientWithFullPatientVisitWithFullInsuranceAndMinimumOrder(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\r"
                // Minimal Insurance. Minimal Organization for Payor, which is required.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson,
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // from INSURANCE.IN1

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); //from INSURANCE.IN1

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(1); //from INSURANCE.IN2        

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdePatientWithAllergyAndMinimumOrder(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        List<Resource> allergyIntolerances = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntolerances).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdeMinimumPatientAndOrderWithObservation(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(3);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdePatientWithEncounterWithInsuranceAndOrderWithObservation(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                // Minimal Insurance. Minimal Organization for Payor, which is required.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson.
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(1);

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // from INSURANCE.IN1

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); //from INSURANCE.IN1

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(1); //from INSURANCE.IN2        

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(7);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RDE^O11", "RDE^O25"})
    void testRdePatientWithEncounterWithMultipleInsuranceWithAllergyAndOrderWithObservation(String message) throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||"+message+"|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                // Minimal Insurance 1. Minimal Organization for Payor, which is required. IN2.72 creates a RelatedPerson.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                // Minimal Insurance 2.  Minimal Organization for Payor, which is required. IN2.72 creates a RelatedPerson.
                + "IN1|1|Value1b^^System3b^Value4b^^System6b|IdValue1b^^^IdSystem4b^^^^|Large Green Organization|||||||||||\n"
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                // Minimal Insurance 3.  Minimal Organization for Payor, which is required. NO related person.
                + "IN1|1|Value1c^^System3c^Value4c^^System6c|IdValue1c^^^IdSystem4c^^^^|Large Violet Organization|||||||||||\n"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(3); //from INSURANCE.IN1 3x

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(3); //from INSURANCE.IN1 3x

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(2); //from INSURANCE.IN2 2x

        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);

        List<Resource> allergyIntolerances = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntolerances).hasSize(1);

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(1); 

        // Expecting only the above resources, no extras!
        assertThat(e).hasSize(13);
    }

}