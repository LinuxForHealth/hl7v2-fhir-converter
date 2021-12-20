/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/**
 * Tests for IN1, Coverage, and related segments
 */
class Hl7FinancialInsuranceTest {
    // Suppress warnings about too many assertions in a test. Justification: creating a FHIR message is very costly; we need to check many asserts per creation for efficiency.  
    @java.lang.SuppressWarnings("squid:S5961")
    @Test
    void testBasicInsuranceCoverageFields() throws IOException {
        // Currently only tests limited items, other fields to be added

        String hl7message = "MSH|^~\\&|||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // IN1 Segment is split and concatenated for easier understanding. (| precedes numbered field.)
                // IN1.2.1, IN1.2.3 to Identifier 1
                // IN1.2.4, IN1.2.6 to Identifier 2
                + "IN1|1|Value1^^System3^Value4^^System6"
                // IN1.3 to Organization Identifier 
                //    IN1.3.1 to Organization Identifier.value
                //    IN1.3.4 to Organization Identifier.system
                //    IN1.3.5 to Organization Identifier.type.code
                //    IN1.3.7 to Organization Identifier.period.start
                //    IN1.3.8 to Organization Identifier.period.end
                + "|IdValue1^^^IdSystem4^IdType5^^20201231145045^20211231145045"
                // IN1.4 to Organization Name
                + "|Large Blue Organization"
                // IN1.5 to Organization Address (All XAD standard fields)
                + "|456 Ultramarine Lane^^Faketown^CA^ZIP5"
                // IN1.6 to Organization Contact Name 
                //    IN1.6.1 to Organization Contact Name .family
                //    IN1.6.2 to Organization Contact Name .given (first)
                //    IN1.6.3 to Organization Contact Name .given (middle)
                //    IN1.6.4 to Organization Contact Name .suffix
                //    IN1.6.5 to Organization Contact Name .prefix
                //    IN1.6.7 to Organization Contact Name .use
                //    IN1.6.12 to Organization Contact Name .period.start
                //    IN1.6.13 to Organization Contact Name .period.end
                + "|LastFake^FirstFake^MiddleFake^III^Dr.^^L^^^^^20201231145045^20211231145045"
                // IN1.7 to Organization Contact telecom  
                //    IN1.7.1 to Organization Contact telecom .value (ONLY when 7.5-7.7 are empty.  See rules in getFormattedTelecomNumberValue.)
                //    IN1.7.2 to Organization Contact telecom .use is "work" 
                //    IN1.7.5 (Country) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.6 (Area) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.7 (Local) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.8 (Extension) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.13 to Organization Contact Name .period.start
                //    IN1.7.14 to Organization Contact Name .period.end
                //    IN1.7.18 to Organization Contact telecom .rank 
                + "|^^^^^333^4444444^^^^^^20201231145045^20211231145045^^^^1"
                // IN1.8 to Coverage.class.value
                // IN1.9.1 to Coverage.class.name
                // IN1.12 to Coverage.period.start
                // IN1.13 to Coverage.period.end
                // IN1.14 to IN1.35 NOT REFERENCED
                + "|UA34567|Blue|||20201231145045|20211231145045||||||||||||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.46 to Identifier 3
                // IN1.47 to IN1.53 NOT REFERENCED
                + "|MEMBER36||||||||||Value46|||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1); // From PV1

        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1); // From PID
        Patient patient = (Patient) patients.get(0);
        String patientId = patient.getId();

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // From Payor created by IN1
        Organization org = (Organization) organizations.get(0);

        // Check organization Id's
        String organizationId = org.getId();
        assertThat(org.getName()).isEqualTo("Large Blue Organization"); // IN1.4
        assertThat(org.getIdentifier()).hasSize(1);
        Identifier orgId1 = org.getIdentifierFirstRep();
        assertThat(orgId1.getValue()).isEqualTo("IdValue1"); // IN1.3.1
        assertThat(orgId1.getSystem()).isEqualTo("urn:id:IdSystem4"); // IN1.3.4
        assertThat(orgId1.getPeriod().getStartElement().toString()).containsPattern("2020-12-31T14:50:45"); // IN1.3.7
        assertThat(orgId1.getPeriod().getEndElement().toString()).containsPattern("2021-12-31T14:50:45"); // IN1.3.8
        DatatypeUtils.checkCommonCodeableConceptAssertions(orgId1.getType(), "IdType5", null, null, null); // IN1.3.5

        // Check the Organization address. IN1.4 is a standard XAD address, which is tested exhaustively in other tests.  
        assertThat(org.getAddress()).hasSize(1);
        assertThat(org.getAddress().get(0).getLine().get(0).getValueAsString())
                .isEqualTo("456 Ultramarine Lane"); // IN1.4.1
        assertThat(org.getAddress().get(0).getCity()).isEqualTo("Faketown"); // IN1.4.3
        assertThat(org.getAddress().get(0).getState()).isEqualTo("CA"); // IN1.4.4
        assertThat(org.getAddress().get(0).getPostalCode()).isEqualTo("ZIP5"); // IN1.4.5

        // Check Organization contact name.  IN1.6 name is standard XPN, tested exhaustively in other tests.
        HumanName contactName = org.getContact().get(0).getName();
        assertThat(org.getContact()).hasSize(1);
        assertThat(contactName.getFamily()).isEqualTo("LastFake"); // IN1.6.1
        assertThat(contactName.getGiven().get(0).getValueAsString()).isEqualTo("FirstFake"); // IN1.6.2
        assertThat(contactName.getGiven().get(1).getValueAsString()).isEqualTo("MiddleFake"); // IN1.6.3
        assertThat(contactName.getPrefixAsSingleString()).hasToString("Dr."); // IN1.6.6
        assertThat(contactName.getSuffixAsSingleString()).hasToString("III"); // IN1.6.5
        assertThat(contactName.getText()).isEqualTo("Dr. FirstFake MiddleFake LastFake III"); // from IN1.6 aggregate
        assertThat(contactName.getUseElement().getCode()).hasToString("official"); // IN1.6.7
        assertThat(contactName.getPeriod().getStartElement().toString()).containsPattern("2020-12-31T14:50:45"); // IN1.6.12
        assertThat(contactName.getPeriod().getEndElement().toString()).containsPattern("2021-12-31T14:50:45"); // IN1.6.13
        assertThat(org.getContact().get(0).hasPurpose()).isFalse(); // Check there is no purpose. Don't need one, here.

        // Check Organization contact telecom.  IN1.7 is standard XTN, tested exhaustively in other tests.
        assertThat(org.getContact().get(0).getTelecom()).hasSize(1);
        ContactPoint contactPoint = org.getContact().get(0).getTelecomFirstRep(); // telecom is type ContactPoint
        assertThat(contactPoint.getSystemElement().getCode()).hasToString("phone"); // default type hardcoded.
        assertThat(contactPoint.getUseElement().getCode()).hasToString("work"); // IN1.7.2
        assertThat(contactPoint.getValue()).hasToString("(333) 444 4444"); // IN1.7.6, IN1.7.7 via getFormattedTelecomNumberValue
        assertThat(contactPoint.getPeriod().getStartElement().toString()).containsPattern("2020-12-31T14:50:45"); // IN1.7.13
        assertThat(contactPoint.getPeriod().getEndElement().toString()).containsPattern("2021-12-31T14:50:45"); // IN1.7.14
        assertThat(contactPoint.getRank()).isEqualTo(1); // IN1.7.18

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment
        Coverage coverage = (Coverage) coverages.get(0);

        // Confirm Coverage Identifiers
        assertThat(coverage.getIdentifier()).hasSize(4);
        assertThat(coverage.getIdentifier().get(0).getValue()).isEqualTo("Value1"); // IN1.2.1
        assertThat(coverage.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:System3"); // IN1.2.3
        assertThat(coverage.getIdentifier().get(0).getUse()).isNull(); // No use, here
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getIdentifier().get(0).getType(), "XV",
                "Health Plan Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
        assertThat(coverage.getIdentifier().get(1).getValue()).isEqualTo("Value4"); // IN1.2.4
        assertThat(coverage.getIdentifier().get(1).getSystem()).isEqualTo("urn:id:System6"); // IN1.2.6
        assertThat(coverage.getIdentifier().get(1).getUse()).isNull(); // No use, here
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getIdentifier().get(1).getType(), "XV",
                "Health Plan Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
        assertThat(coverage.getIdentifier().get(2).getValue()).isEqualTo("Value46"); // IN1.46
        assertThat(coverage.getIdentifier().get(2).getSystem()).isNull(); // No system, here
        assertThat(coverage.getIdentifier().get(2).getUseElement().getCode()).hasToString("old"); // Use is enumeration "old"
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getIdentifier().get(2).getType(), "XV",
                "Health Plan Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
        assertThat(coverage.getIdentifier().get(3).getValue()).isEqualTo("MEMBER36"); // IN1.36
        assertThat(coverage.getIdentifier().get(3).getSystem()).isNull(); // No system, here
        assertThat(coverage.getIdentifier().get(3).getUse()).isNull(); // No use, here
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getIdentifier().get(3).getType(), "MB",
                "Member Number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Confirm Coverage Beneficiary references to Patient, and Payor references to Organization
        assertThat(coverage.getBeneficiary().getReference()).isEqualTo(patientId);
        assertThat(coverage.getPayorFirstRep().getReference()).isEqualTo(organizationId);

        // Only one Coverage Class expected.  (getClass_ is correct name for method)
        assertThat(coverage.getClass_()).hasSize(1);
        assertThat(coverage.getClass_FirstRep().getName()).isEqualTo("Blue"); // IN1.9.1
        assertThat(coverage.getClass_FirstRep().getValue()).isEqualTo("UA34567"); // IN1.8
        DatatypeUtils.checkCommonCodeableConceptVersionedAssertions(coverage.getClass_FirstRep().getType(), "group",
                "Group", "http://terminology.hl7.org/CodeSystem/coverage-class", null, null);

        // Confirm Coverage period
        assertThat(coverage.getPeriod().getStartElement().toString()).containsPattern("2020-12-31T14:50:45"); // IN1.12
        assertThat(coverage.getPeriod().getEndElement().toString()).containsPattern("2021-12-31T14:50:45"); // IN1.13

        // Confirm there are no unaccounted for resources
        assertThat(e).hasSize(4);
    }

}
