/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

class Hl7ORMMessageTest {

    @Test
    void testORMO01NoPatientOrderOnly() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(1);

    }

    @Test
    void testORMO01NoPatientOrderPlusDetail() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(2);

    }

    @Test
    void testORMO01PatientOrderPlusDetail() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r"
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(3);

    }

    @Test
    void testORMO01PatientEncounterInsuranceOrder() throws IOException {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|IBMWATSON_LAB|IBM|20210407191758||ORM^O01|MSGID_e30a3471-7afd-4aa2-a3d5-e93fd89d24b3|T|2.3\n"
                + "PID|1||0a1f7838-4230-4752-b8f6-948b07c38b25^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator||9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900|||||Account_0a1f7838-4230-4752-b8f6-948b07c38b25|123-456-7890||||BIRTH PLACE\n"
                + "PV1||IP|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting|IP^I|Visit_0a1f7838-4230-4752-b8f6-948b07c38b25|||||||||||||||||||||||||20210407191758\n"
                + "PV2|||^|||X-5546||20210407191758|||||||||||||||\n"
                // Minimal Insurance. Minimal Organization for Payor, which is required.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                // IN2.72 creates a RelatedPerson,
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                + "ORC|SN|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|2950|||||20210407191758|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191758||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN\n"
                + "OBR|1|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|4916^Diffusion-weighted imaging||20210331214400|20210407191758|20210407191758||||||20210331214600||1234^SOURCE^SPECIMEN^LNAME^FNAME^^^^^^^^^LABNAME||||W18562||||P|||^^^^^POCPR|660600^Doctor^FYI||||Result Interpreter\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        List<Resource> practitioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitioners).hasSize(5);

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(2); // from Practitioner and INSURANCE.IN1

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); //from INSURANCE.IN1

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(1); //from INSURANCE.IN2

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(12);
    }

    @Test
    void testORMO01PatientWithPatientVisitMultipleInsuranceOrder() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
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

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(2); //from INSURANCE.IN1 2x

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(2); //from INSURANCE.IN1 2x

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(2); //from INSURANCE.IN2 2x

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(10);

    }

    @Test
    void testORMO01PatientWithPatientVisitSingleInsuranceMultipleAllergyOrder() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
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

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); //from INSURANCE.IN1

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(1); //from INSURANCE.IN1

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(1); //from INSURANCE.IN2

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(10);
    }

    @Test
    void testORMO01NoPatientOrderWithMultipleObservationsWithOBXnonTX() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
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

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(2);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(4);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(8);

    }

    @Test
    void testORMO01PatientOrderWithMultipleObservationsWithOBXnonTX() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
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

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(2);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(4);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(9);

    }

    @Test
    void testORMO01NoPatientOrderWithMultipleObservationsWithOBXtypeTX() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                // first order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|TX|||Report line 1|||||||X\r"
                + "OBX|2|TX|||Report line 2|||||||X\r"
                // second order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r"
                + "OBX|1|TX|1234^some text^SCT||First line: Sodium Report||||||F||\n"
                + "OBX|2|TX|1234^some text^SCT||Second line: Sodium REPORT||||||F||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(2);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(5);

    }

    @Test
    void testORMO01PatientOrderWithMultipleObservationsWithOBXtypeTX() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r" //Even though PID is optional, I had to provide it in order to get results from the converter
                // first order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|50111032701^hydrALAZINE HCl 25 MG Oral Tablet^NDC^^^^^^hydrALAZINE (APRESOLINE) 25 MG TABS|||||||||||||||||||||||\r"
                + "OBX|1|TX|||Report line 1|||||||X\r"
                + "OBX|2|TX|||Report line 2|||||||X\r"
                // second order group
                + "ORC|OP|1000|9999999||||^3 times daily^^20210401\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r"
                + "OBX|1|TX|1234^some text^SCT||First line: Sodium Report||||||F||\n"
                + "OBX|2|TX|1234^some text^SCT||Second line: Sodium REPORT||||||F||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(2);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(6);

    }

    @Test
    void testORMO01PatientMultipleInsuranceMultipleOrdersWithAndWithoutOBXtypeTX() throws IOException {
        String hl7message = "MSH|^~\\&|||||20210407191342||ORM^O01|MSGID_bae9ce6a-e35d-4ff5-8d50-c5dde19cc1aa|T|2.5.1\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\r"
                // Minimal Insurance 1. Minimal Organization for Payor, which is required. IN2.72 creates a RelatedPerson.
                + "IN1|1|Value1^^System3^Value4^^System6|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||\n"
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                // Minimal Insurance 2.  Minimal Organization for Payor, which is required. IN2.72 creates a RelatedPerson.
                + "IN1|1|Value1b^^System3b^Value4b^^System6b|IdValue1b^^^IdSystem4b^^^^|Large Green Organization|||||||||||\n"
                + "IN2||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||04|\n"
                // Minimal Insurance 3.  Minimal Organization for Payor, which is required. NO related person.
                + "IN1|1|Value1c^^System3c^Value4c^^System6c|IdValue1c^^^IdSystem4c^^^^|Large Violet Organization|||||||||||\n"
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

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(3); //from INSURANCE.IN1 3x

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(3); //from INSURANCE.IN1 3x

        List<Resource> relatedResource = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedResource).hasSize(2); //from INSURANCE.IN2 2x

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(3);

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(3);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(3);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1);

        // Confirm that there are no extra resources created
        assertThat(e).hasSize(19);
    }

}
