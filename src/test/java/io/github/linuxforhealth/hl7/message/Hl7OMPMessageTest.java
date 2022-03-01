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
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7OMPMessageTest {

    @Test
    void testOMPO09MinimumPatientAndMinimumOrderGroups() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(2);

    }

    @Test
    void testOMPO09PatientWithPatientVisitAndMultipleInsuranceAndMinimumOrderGroups() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
                // Minimal Insurance 1. Minimal Organization for Payor, which is required.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson,
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                // Minimal Insurance 2.  Minimal Organization for Payor, which is required.
                + "IN1|1|Value1b^^System3b^Value4b^^System6b|IdValue1b^^^IdSystem4b^^^^|Large Green Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson,
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(2); //from INSURANCE.IN1 2x

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(2); //from INSURANCE.IN1 2x

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(2); //from INSURANCE.IN1 2x

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(9);

    }

    @Test
    void testOMPO09FullPatientWithPatientVisitAndInsuranceAndMinimumOrderGroups() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
                + "PV2|||chortles|||||||||||||||||||||||||||||||||\r"
                // Minimal Insurance. Minimal Organization for Payor, which is required.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson,
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
                + "AL1|2|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "AL1|3|BRANDNAME4|00008604^LEVAQUIN||RASH ITCHING\r"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> allergyResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyResource).hasSize(3);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); //from INSURANCE.IN1

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(1); //from INSURANCE.IN1

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(1); //from INSURANCE.IN1

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(9);
    }

    @Test
    void testOMPO09OrderWithMultipleObservationsWithOBXnonTX() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r" //Even though PID is optional, I had to provide it in order to get results from the converter
                //1st order
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
                + "OBX|2|ST|0135-4^TotalProtein||6.4|gm/dl|5.9-8.4||||F||||||\r"
                + "OBX|3|CE|30945-0^Contraindication^LN||21^acute illness^NIP^^^|||||||F| \r"
                //2nd order
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r"
                + "OBX|1|ST|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(4);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(7);

    }

    @Test
    void testOMPO09OrderWithMultipleObservationsWithOBXtypeTX() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r" //Even though PID is optional, I had to provide it in order to get results from the converter
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|TX|||Report line 1|||||||X\r"
                + "OBX|2|TX|||Report line 2|||||||X\r"
                //2nd order
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r"
                + "OBX|1|TX|1234^some text^SCT||First line: Sodium Report||||||F||\n"
                + "OBX|2|TX|1234^some text^SCT||Second line: Sodium REPORT||||||F||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).isEmpty();//TODO: This should be 2 when card 864 is completed

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(3); //TODO: This should be 5 when card 864 is completed

    }

    @Test
    void testOMPO09withMultipleOrdersWithAndWithoutOBXtypeTX() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210407191342|9022934|OMP^O09|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r" //Even though PID is optional, I had to provide it in order to get results from the converter
                // first order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|TX|||Report line 1|||||||X\r"
                + "OBX|2|TX|||Report line 2|||||||X\r"
                // second order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\r"
                + "OBX|2|ST|0135-4^TotalProtein||6.4|gm/dl|5.9-8.4||||F||||||\r"
                + "OBX|3|CE|30945-0^Contraindication^LN||21^acute illness^NIP^^^|||||||F| \r"
                // third order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(3);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(3);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).isEmpty(); //TODO: This should be 1 when card 864 is completed

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(7); //TODO: This should be 8 when card 864 is completed

    }

}
