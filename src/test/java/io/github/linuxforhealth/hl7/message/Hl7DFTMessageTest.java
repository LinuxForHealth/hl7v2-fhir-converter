/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

class Hl7DFTMessageTest {

    @Test
    void testResourceCreationFromDFT() throws IOException {
        // Tests structure of DFT_P03 message type and DFT messages in general
        // DFT_P03 message types and IN1 are tested in detail in Hl7FinancialInsuranceTest

        String hl7message = "MSH|^~\\&|||||20151008111200||DFT^P03^DFT_P03|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // Minimal ORC for ServiceRequest
                + "ORC|NW|||||||||||||||\n"
                // Minimal OBR for ServiceRequest
                + "OBR|1||||||||||||||||||||||||||||||||\n"
                + "NTE|1|O|TEST ORC/OBR NOTE AA|||\n"
                // OBX is type ST so an observation will be created
                + "OBX|1|ST|100||This is content|||||||X\n"
                + "NTE|1|L|TEST OBXa NOTE|\n"
                // FT1 added for completeness; required in specification, but not used (ignored) by templates
                // FT1.4 is required transaction date (currently not used)
                // FT1.6 is required transaction type (currently not used)
                // FT1.7 is required transaction code (currently not used)
                + "FT1||||20201231145045||CG|FAKE|||||||||||||||||||||||||||||||||||||\n"
                // IN1 Segment is split and concatenated for easier understanding. (| precedes numbered field.)
                // IN1.2.1, IN1.2.3 to Identifier 1
                // IN1.2.4, IN1.2.6 to Identifier 2
                + "IN1|1|Value1^^System3^Value4^^System6"
                // Minimal Organization. Required for Payor, which is required.
                // IN1.3 to Organization Identifier 
                // INI.4 to Organization Name (required to inflate organization)
                // IN1.5 to 15 NOT REFERENCED (See test testBasicInsuranceCoverageFields)
                + "|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||"
                // IN1.16 to RelatedPerson.name
                // IN1.17 to Coverage.relationship and RelatedPerson.relationship. 
                // IN1.22 purposely empty to show that IN1.1 is secondary for Coverage.order
                // IN1.18 through IN1.35 NOT REFERENCED
                + "|DoeFake^Judy^^^Rev.|PAR||||||||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.46 to Identifier 3
                // IN1.49 to RelatedPerson.identifier
                // IN1.50 through IN1.53 NOT REFERENCED
                + "|MEMBER36||||||||||Value46|||J494949||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1); // From PV1

        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1); // From PID

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // From Payor created by IN1

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment

        List<Resource> relatedPersons = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedPersons).hasSize(1); // From IN1.16 through IN1.19; IN1.43; INI.49 

        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);  // From ORC / OBR
        ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequests.get(0),
                ResourceUtils.context);
        assertThat(serviceRequest.getNote()).hasSize(1);
        assertThat(serviceRequest.getNote().get(0).getTextElement().getValueAsString()).isEqualTo(
                "TEST ORC/OBR NOTE AA");

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(1); // From OBX
        Observation observation = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);
        assertThat(observation.getNote()).hasSize(1);
        assertThat(observation.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST OBXa NOTE");

        // Confirm there are no unaccounted for resources
        // Expected: Coverage, Organization, Patient, Encounter, RelatedPerson, ServiceRequest, Observation
        assertThat(e).hasSize(7);
    }
}
