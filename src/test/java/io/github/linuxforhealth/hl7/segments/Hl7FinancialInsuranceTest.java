/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coverage.ClassComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
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
        // Tests fields listed below.  
        String hl7message = "MSH|^~\\&|||||20151008111200||DFT^P03^DFT_P03|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // FT1 added for completeness; required in specification, but not used (ignored) by templates
                // FT1.4 is required transaction date (currently not used)
                // FT1.6 is required transaction type (currently not used)
                // FT1.7 is required transaction code (currently not used)
                + "FT1||||20201231145045||CG|FAKE|||||||||||||||||||||||||||||||||||||\n"
                // IN1 Segment is split and concatenated for easier understanding. (| precedes numbered field.)
                // IN1.2.1, IN1.2.3 to Identifier 1
                // IN1.2.4, IN1.2.6 to Identifier 2
                + "IN1|1|Value1^^System3^Value4^^System6"
                // Thorough organization testing.
                // IN1.3 to Organization Identifier 
                //    IN1.3.1 to Organization Identifier.value   NOTE: no external TENANT set. 
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
                //    IN1.7.2 is not mapped.  Purposely contains possible valid HL7 value to show it is ignored.
                //    IN1.7.5 (Country) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.6 (Area) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.7 (Local) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.8 (Extension) to Organization Contact telecom .value (as input to getFormattedTelecomNumberValue)
                //    IN1.7.13 to Organization Contact Name .period.start
                //    IN1.7.14 to Organization Contact Name .period.end
                //    IN1.7.18 to Organization Contact telecom .rank 
                + "|^WPN^^^^333^4444444^^^^^^20201231145045^20211231145045^^^^1"
                // IN1.8 to list element Coverage.class.value
                // IN1.9.1 (1) to list element Coverage.class.name
                // IN1.9.10 (1) to list element Coverage.class.value 
                // IN1.9.1 (2) to list element Coverage.class.name and value (because IN1.9.10 (2) does not exist)
                + "|UA34567|NameBlue^^^^^^^^^IDBlue~NameGreen||"
                // IN1.12 to Coverage.period.start
                // IN1.13 to Coverage.period.end
                // IN1.17 is purposely empty - no Related Person should be created.
                // IN1.22 to Coverage.order takes priority over IN1.1
                // IN1.23 through IN1.35 NOT REFERENCED
                + "|20201231145045|20211231145045|||||||||5|||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.36 also to subscriberId
                // IN1.46 to Identifier 3
                // IN1.47 through IN1.53 NOT REFERENCED
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
        // Note: in the next assert, no TENANT value set; confirm default (nothing) was used.  See testInsuranceCoverageOfSelfAndTenant for test of setting TENANT.
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
        assertThat(contactPoint.hasUseElement()).isFalse(); // IN1.7.2 is not mapped (ignored)
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

        // Confirm SubscriberId
        assertThat(coverage.getSubscriberId()).isEqualTo("MEMBER36"); // IN1.36

        // Confirm coverage.Order
        assertThat(coverage.getOrder()).isEqualTo(5); // IN1.22 takes priority over IN1.1

        // Confirm Coverage Beneficiary references to Patient, and Payor references to Organization
        assertThat(coverage.getBeneficiary().getReference()).isEqualTo(patientId);
        assertThat(coverage.getPayorFirstRep().getReference()).isEqualTo(organizationId);
        // Only one Coverage Class expected.  (getClass_ is correct name for method)
        assertThat(coverage.getClass_()).hasSize(3);
        checkCoverageClassExistsWithCorrectValueAndName(coverage.getClass_(), "UA34567", null); // IN1.8  Only has value
        checkCoverageClassExistsWithCorrectValueAndName(coverage.getClass_(), "IDBlue", "NameBlue"); // IN1.9 (1)
        checkCoverageClassExistsWithCorrectValueAndName(coverage.getClass_(), "NameGreen", "NameGreen"); // IN1.9 (2) Name is also used for value

        // Confirm Coverage period
        assertThat(coverage.getPeriod().getStartElement().toString()).containsPattern("2020-12-31T14:50:45"); // IN1.12
        assertThat(coverage.getPeriod().getEndElement().toString()).containsPattern("2021-12-31T14:50:45"); // IN1.13

        // Expect no RelatedPerson because IN1.17 was empty
        List<Resource> relatedPersons = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedPersons).isEmpty(); // IN1.17 empty

        // Confirm there are no unaccounted for resources
        // Expected: Coverage, Organization, Patient, Encounter
        assertThat(e).hasSize(4);
    }

    // Checks that a Coverage.Class with the right Value and Name and Type exist in the list
    private void checkCoverageClassExistsWithCorrectValueAndName(List<ClassComponent> classes_, String value,
            String name) {
        for (ClassComponent c : classes_) {
            // Find our class in the list
            if (c.getValue().equals(value)) {
                // Value is required
                assertThat(c.getValue()).isEqualTo(value);
                if (name != null) {
                    assertThat(c.getName()).isEqualTo(name);
                } else {
                    assertThat(c.getName()).isNull();
                }
                DatatypeUtils.checkCommonCodeableConceptVersionedAssertions(c.getType(), "group",
                        "Group", "http://terminology.hl7.org/CodeSystem/coverage-class", null, null);
                return;
            }
        }
        // if our class is not in the list, fail the check
        fail("No Coverage.Class with value: " + value);
    }

    // Suppress warnings about too many assertions in a test.  Justification: creating a FHIR message is very costly; we need to check many asserts per creation for efficiency.  
    @java.lang.SuppressWarnings("squid:S5961")
    @ParameterizedTest
    // Tests IN1 for different message types. 
    // The breadth of this test is sufficent for multiple message type coverage, so other tests are not parameterized.
    @ValueSource(strings = {
            "DFT^P03^DFT_P03", "ADT^A01^ADT_A01"
    })
    // Tests IN1.17 coverage by related person. A related person should be created and cross-referenced.
    // Also tests backup field for coverage.order

    void testInsuranceCoverageByRelatedFields(String messageType) throws IOException {
        String hl7message = "MSH|^~\\&|||||20151008111200||"+messageType+"|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
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
                // Organization deep test in testBasicInsuranceCoverageFields
                // IN1.3 to Organization Identifier 
                //    IN1.3.1 to Organization Identifier.value  (No TENANT set; uses default (nothing).)
                //    IN1.3.4 to Organization Identifier.system
                // INI.4 to Organization Name (required to inflate organization)
                // IN1.5 to 15 NOT REFERENCED (See test testBasicInsuranceCoverageFields)
                + "|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||"
                // IN1.16 to RelatedPerson.name
                //    IN1.16.1 to RelatedPerson Name .family
                //    IN1.16.2 to RelatedPerson Name .given (first)
                //    IN1.16.5 to RelatedPerson Name .prefix
                // IN1.17 to Coverage.relationship and RelatedPerson.relationship. 
                // IN1.18 to RelatedPerson.birthDate
                // IN1.19 to RelatedPerson.address (All XAD standard fields)
                // IN1.22 purposely empty to show that IN1.1 is secondary for Coverage.order
                // IN1.23 through IN1.35 NOT REFERENCED
                + "|DoeFake^Judy^^^Rev.|PAR|19780429|19 Rose St^^Faketown^CA^ZIP5||||||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.43 to RelatedPerson.gender
                // IN1.46 to Identifier 3
                // IN1.49 to RelatedPerson.identifier
                //    IN1.49.1 to RelatedPerson.identifier.value
                //    IN1.49.4 to RelatedPerson.identifier.system
                //    IN1.49.5 to RelatedPerson.identifier.type.code (must be from terminology.hl7.org/3.0.0/CodeSystem-v2-0203.html)
                //      NOTE: Purposely XX to ensure we are doing a lookup, and not getting bleed from other hard-coded XV uses
                // IN1.50 through IN1.53 NOT REFERENCED
                + "|MEMBER36|||||||F|||Value46|||J494949^^^Large HMO^XX||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1); // From PV1

        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1); // From PID
        Patient patient = (Patient) patients.get(0);
        String patientId = patient.getId();

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // From Payor created by IN1

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment
        Coverage coverage = (Coverage) coverages.get(0);

        // Confirm Coverage Identifiers
        assertThat(coverage.getIdentifier()).hasSize(4);
        // Coverage Identifiers deep check in testBasicInsuranceCoverageFields

        // Confirm Coverage Beneficiary references to Patient, and Payor references to Organization
        assertThat(coverage.getBeneficiary().getReference()).isEqualTo(patientId);
        assertThat(coverage.getPayorFirstRep().getReference()).isEqualTo(organizations.get(0).getId());

        // Expect one RelatedPerson
        List<Resource> relatedPersons = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedPersons).hasSize(1); // From IN1.16 through IN1.19; IN1.43; INI.49 
        RelatedPerson related = (RelatedPerson) relatedPersons.get(0);

        // Check RelatedPerson identifier
        assertThat(related.getIdentifier()).hasSize(1);
        assertThat(related.getIdentifier().get(0).getValue()).isEqualTo("J494949"); // IN1.49.1
        assertThat(related.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:Large_HMO"); // IN1.49.4
        DatatypeUtils.checkCommonCodeableConceptAssertions(related.getIdentifier().get(0).getType(), "XX", // IN1.49.5
                "Organization identifier", // Display value looked up from code 'XX'
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Check RelatedPerson name. IN1.16 name is standard XPN, tested exhaustively in other tests.        
        assertThat(related.getName()).hasSize(1);
        HumanName relatedName = related.getName().get(0);
        assertThat(relatedName.getFamily()).isEqualTo("DoeFake"); // IN1.16.1
        assertThat(relatedName.getGiven().get(0).getValueAsString()).isEqualTo("Judy"); // IN1.16.2
        assertThat(relatedName.getPrefixAsSingleString()).hasToString("Rev."); // IN1.16.6
        assertThat(relatedName.getText()).isEqualTo("Rev. Judy DoeFake"); // from IN1.16 aggregate

        // Check RelatedPerson birth and gender
        assertThat(related.getBirthDateElement().toString()).containsPattern("1978-04-29"); // IN1.18
        Enumerations.AdministrativeGender gen = related.getGender();
        assertThat(gen).isNotNull().isEqualTo(Enumerations.AdministrativeGender.FEMALE); // IN1.43

        // Check the RelatedPerson address. IN1.19 is a standard XAD address, which is tested exhaustively in other tests.  
        assertThat(related.getAddress()).hasSize(1);
        assertThat(related.getAddress().get(0).getLine().get(0).getValueAsString())
                .isEqualTo("19 Rose St"); // IN1.19.1
        assertThat(related.getAddress().get(0).getCity()).isEqualTo("Faketown"); // IN1.19.3
        assertThat(related.getAddress().get(0).getState()).isEqualTo("CA"); // IN1.19.4
        assertThat(related.getAddress().get(0).getPostalCode()).isEqualTo("ZIP5"); // IN1.19.5

        // Check coverage relationship
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getRelationship(), "child",
                "Child",
                "http://terminology.hl7.org/CodeSystem/subscriber-relationship", null); // IN1.17

        // Check relatedPerson relationship
        assertThat(related.getRelationship()).hasSize(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(related.getRelationship().get(0), "PRN",
                "parent",
                "http://terminology.hl7.org/CodeSystem/v3-RoleCode", null); // IN1.17

        // Confirm the Coverage (subscriber) references the RelatedPerson
        assertThat(coverage.getSubscriber().getReference()).isEqualTo(related.getId());
        // Confirm the RelatedPerson references the Patient
        assertThat(related.getPatient().getReference()).isEqualTo(patientId);

        // Confirm coverage.Order
        assertThat(coverage.getOrder()).isEqualTo(1); // IN1.1 backup for missing IN1.22

        // Confirm there are no unaccounted for resources
        // Expected: Coverage, Organization, Patient, Encounter, RelatedPerson
        assertThat(e).hasSize(5);
    }

    @Test
    // Tests IN1.17 coverage by one's self. A related person should not be created.  But the patient should be referenced.
    // Tests Organization ID created with a TENANT prepend.
    void testInsuranceCoverageOfSelfAndTenant() throws IOException {

        String hl7message = "MSH|^~\\&|||||20151008111200||DFT^P03^DFT_P03|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // FT1 added for completeness; required in specification, but not used (ignored) by templates
                // FT1.4 is required transaction date (currently not used)
                // FT1.6 is required transaction type (currently not used)
                // FT1.7 is required transaction code (currently not used)
                + "FT1||||20201231145045||CG|FAKE|||||||||||||||||||||||||||||||||||||\n"
                // IN1 Segment is split and concatenated for easier understanding. (| precedes numbered field.)
                // IN1.2.1, IN1.2.3 to Identifier 1
                // IN1.2.4, IN1.2.6 to Identifier 2
                + "IN1|1|Value1^^System3^Value4^^System6"
                // Minimal Organization to test TENANT prepend. 
                // IN1.3 to Organization Identifier 
                //    IN1.3.1 to Organization Identifier.value
                //    IN1.3.4 to Organization Identifier.system, will be prepended by TENANT from options
                // IN1.4 to Organization Name
                // IN1.5 to 15 NOT REFERENCED (Tested in testBasicInsuranceCoverageFields)
                + "|IdValue1^^^IdSystem4^^^^|Large Blue Organization|||||||||||"
                // IN1.16 empty because IN1.17 is SEL
                // IN1.17 to Coverage.relationship.  SEL (self) should create relationship of ONESELF, and reference to patient. 
                // IN1.18 through IN1.35 NOT REFERENCED
                + "||SEL||||||||||||||||||"
                // IN1.36 to Identifier 4
                // IN1.46 to Identifier 3
                // IN1.47 through IN1.53 NOT REFERENCED
                + "|MEMBER36||||||||||Value46|||||||\n";

        // TENANT prepend is passed through the options.  
        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint().withProperty("TENANT", "TenantId").build();        
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message, customOptionsWithTenant);

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
        assertThat(org.getName()).isEqualTo("Large Blue Organization"); // IN1.4
        assertThat(org.getIdentifier()).hasSize(1);
        Identifier orgId1 = org.getIdentifierFirstRep();
        assertThat(orgId1.getValue()).isEqualTo("TenantId.IdValue1"); // Options TENANT + IN1.3.1
        assertThat(orgId1.getSystem()).isEqualTo("urn:id:IdSystem4"); // IN1.3.4
        assertThat(orgId1.hasPeriod()).isFalse(); // IN1.3.7 & IN1.3.7 empty
        assertThat(orgId1.hasType()).isFalse(); // IN1.3.5 empty

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment
        Coverage coverage = (Coverage) coverages.get(0);

        // Confirm Coverage Identifiers
        assertThat(coverage.getIdentifier()).hasSize(4);
        // Coverage Identifiers deep check in testBasicInsuranceCoverageFields

        // Confirm Coverage Subscriber references to Patient
        assertThat(coverage.getSubscriber().getReference()).isEqualTo(patientId);
        // Confirm Coverage Beneficiary references to Patient, and Payor references to Organization
        assertThat(coverage.getBeneficiary().getReference()).isEqualTo(patientId);
        assertThat(coverage.getPayorFirstRep().getReference()).isEqualTo(organizations.get(0).getId());

        // Expect no RelatedPerson because IN1.17 was self
        List<Resource> relatedPersons = ResourceUtils.getResourceList(e, ResourceType.RelatedPerson);
        assertThat(relatedPersons).isEmpty(); // No related person should be created because IN1.17 was SEL

        // Check coverage.relationship (from SubscriberRelationship mapping)
        DatatypeUtils.checkCommonCodeableConceptAssertions(coverage.getRelationship(), "self",
                "Self",
                "http://terminology.hl7.org/CodeSystem/subscriber-relationship", null); // IN1.17

        // Confirm there are no unaccounted for resources
        // Expected: Coverage, Organization, Patient, Encounter
        assertThat(e).hasSize(4);
    }

    @Test
    // Confirms that telecom values are processed in special case.
    // Background: a testcase with IN1 was failing to create the telecom.  It didn't seem unusual because there is a phone number in IN1.7,
    // and an organization was created, but for some reason the telecom was not created.
    // Period and rank were not the problem cause.
    // This was fixed by a change to Organization.yml contact: in PR #405
    // This test is to see the problem does not return.  It fails without PR #405, and is successful with it. 
    void testFailingOrganizationTelecom() throws IOException {

        String hl7message = "MSH|^~\\&|||||20211214105741||DFT^P03|1760487765|P|2.6|||||||||\n"
                + "EVN|P03|20211214105741\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // FT1 added for completeness; required in specification, but not used (ignored) by templates
                // FT1.4 is required transaction date (currently not used)
                // FT1.6 is required transaction type (currently not used)
                // FT1.7 is required transaction code (currently not used)
                + "FT1||||20201231145045||CG|FAKE|||||||||||||||||||||||||||||||||||||\n"
                // These fields in IN1 with these values cause the failure, leaving some of the field empty may be contribute.
                // IN1.2 to Identifier 1
                // IN1.3 to Organization Identifier
                // IN1.4 to Organization Name
                // IN1.7 to Organization Telecom
                // IN1.8 to 15 NOT REFERENCED (Tested in testBasicInsuranceCoverageFields)
                + "IN1|1|PLAN001|210012|GOLD CHOICE PLUS|||^^^^^800^3334444||||||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1); // From PV1

        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1); // From PID

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // From Payor created by IN1
        Organization org = (Organization) organizations.get(0);

        // Check organization Id's
        assertThat(org.getName()).isEqualTo("GOLD CHOICE PLUS"); // IN1.4
        assertThat(org.getIdentifier()).hasSize(1);
        Identifier orgId1 = org.getIdentifierFirstRep();
        assertThat(orgId1.getValue()).isEqualTo("210012"); // IN1.3.1
        assertThat(orgId1.hasSystem()).isFalse(); // IN1.3.4 empty
        assertThat(orgId1.hasPeriod()).isFalse(); // IN1.3.7 & IN1.3.7 empty
        assertThat(orgId1.hasType()).isFalse(); // IN1.3.5 empty

        // Check Organization contact telecom.  IN1.7 is standard XTN, tested exhaustively in other tests.
        assertThat(org.getContact().get(0).getTelecom()).hasSize(1);
        ContactPoint contactPoint = org.getContact().get(0).getTelecomFirstRep(); // telecom is type ContactPoint
        assertThat(contactPoint.getSystemElement().getCode()).hasToString("phone"); // default type hardcoded.
        assertThat(contactPoint.hasUseElement()).isFalse(); // IN1.7.2 is not mapped 
        assertThat(contactPoint.getValue()).hasToString("(800) 333 4444"); // IN1.7.6, IN1.7.7 via getFormattedTelecomNumberValue

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment

        // Confirm there are no unaccounted for resources
        // Expected: Coverage, Organization, Patient, Encounter
        assertThat(e).hasSize(4);
    }

}
