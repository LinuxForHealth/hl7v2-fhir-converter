/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class HL7ConditionFHIRConversionTest {

    // --------------------- DIAGNOSIS UNIT TESTS (DG1) ---------------------

    // Tests the DG1 segment (diagnosis) with all supported message types.
    // This tests all the fields in the happy path.
    // Suppress warnings about too many assertions in a test.  Justification: creating a FHIR message is very costly; we need to check many asserts per creation for efficiency.  
    @java.lang.SuppressWarnings("squid:S5961")
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
            // "MSH|^~\\&|||||||ADT^A03|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
            // "MSH|^~\\&|||||||ADT^A04|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
            "MSH|^~\\&|||||||ADT^A08|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
    // "MSH|^~\\&|||||||ADT^A28^ADT^A28|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
    // "MSH|^~\\&|||||||ADT^A31|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
    // "MSH|^~\\&|||||||ORM^O01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
    // PPR_PC1, PPR_PC2, and PPR_PC3 create Conditions but they don't have a DG1 segment so they are tested in a different testcase.
    })
    void validateDiagnosis(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||\r"
                // DG1.3 to Code
                // DG1.5 to Onset (DateTime)
                // DG1.6 type required 
                // DG1.15 to Rank
                // DG1.16 to Practitioner  
                // DG1.19 to RecordedDate
                // DG1.20 to Identifier
                + "DG1|1||C56.9^Ovarian Cancer^I10||20210322154449|A|||||||||1|123^DOE^JOHN^A^|||20210322154326|V45|||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Verify no extraneous resources
        // Expect encounter, patient, practitioner, condition
        assertThat(e).hasSize(4);

        // --- CONDITION TESTS ---

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify we have 3 identifiers
        // NOTE: The other identifiers, not related to condition, are tested deeply in the
        // identifier suite of unit tests.
        assertThat(condition.getIdentifier()).hasSize(3);
        // First identifier is the Visit Number, and is a codeable concept
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getIdentifier().get(0).getType(), "VN",
                "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
        // Second identifier  
        assertThat(condition.getIdentifier().get(1).getSystem()).isEqualTo("urn:id:extID");
        assertThat(condition.getIdentifier().get(1).getValue()).isEqualTo("C56.9-I10");
        // The 3rd identifier value should be from DG1.20
        assertThat(condition.getIdentifier().get(2).getValue()).isEqualTo("V45"); // DG1.20

        // Verify asserter reference to Practitioner exists
        assertThat(condition.getAsserter().getReference().substring(0, 13)).isEqualTo("Practitioner/");

        // Verify recorded date is set correctly.
        assertThat(condition.getRecordedDateElement().toString()).containsPattern("2021-03-22T15:43:26"); // DG1.19

        // Verify onset date time is set correctly.
        assertThat(condition.getOnset().toString()).containsPattern("2021-03-22T15:44:49"); // DG1.5

        // Verify code text and coding are set correctly.
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCode(), "C56.9", "Ovarian Cancer",
                "http://hl7.org/fhir/sid/icd-10-cm", "Ovarian Cancer"); // DG1.3

        // Verify encounter reference exists
        assertThat(condition.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

        // Verify subject reference to Patient exists
        assertThat(condition.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");

        // Verify category text and coding are set correctly.
        assertThat(condition.getCategory()).hasSize(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCategoryFirstRep(), "encounter-diagnosis",
                "Encounter Diagnosis",
                "http://terminology.hl7.org/CodeSystem/condition-category", "Encounter Diagnosis"); // DG1.3

        // --- ENCOUNTER TESTS ---

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);
        Encounter encounter = (Encounter) encounterResource.get(0);

        // Encounter should have a reference to the conditions (only 1 in this unit test)
        assertThat(encounter.getReasonReference()).hasSize(1);
        assertThat(encounter.getReasonReference().get(0).getReference().substring(0, 10))
                .isEqualTo("Condition/");

        // Verify encounter diagnosis condition, use, and rank are set correctly
        assertThat(encounter.getDiagnosis()).hasSize(1);
        assertThat(encounter.getDiagnosisFirstRep().getCondition().getReference().substring(0, 10))
                .isEqualTo("Condition/");
        DatatypeUtils.checkCommonCodeableConceptAssertions(encounter.getDiagnosisFirstRep().getUse(), "AD",
                "Admission diagnosis",
                "http://terminology.hl7.org/CodeSystem/diagnosis-role", null);
        assertThat(encounter.getDiagnosisFirstRep().getRank()).isEqualTo(1); // DG1.15

        // --- PRACTIONER TESTS ---

        // Find the asserter (practitioner)  resource from the FHIR bundle.
        List<Resource> practitionerResource = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitionerResource).hasSize(1);
        Practitioner practitioner = (Practitioner) practitionerResource.get(0);

        // Verify name text, family, and given are set correctly.
        assertThat(practitioner.getName()).hasSize(1);
        assertThat(practitioner.getNameFirstRep().getText()).isEqualTo("JOHN A DOE"); // DG1.16
        assertThat(practitioner.getNameFirstRep().getFamily()).isEqualTo("DOE"); // DG1.16.2
        assertThat(practitioner.getNameFirstRep().getGivenAsSingleString()).isEqualTo("JOHN A"); // DG1.16.3

        // Verify asserter (practitioner) identifier is set correctly.
        assertThat(practitioner.getIdentifier()).hasSize(1);
        assertThat(practitioner.getIdentifierFirstRep().getValue()).isEqualTo("123"); // DG1.16.1

    }

    // Tests the DG1 segment (diagnosis) with a full Entity Identifier (EI).
    // These values come from DG1.20.
    @Test
    void validateDiagnosisWithEIIdentifiers() {

        String hl7message = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                // DG1.3 to identifiers (required)
                // DG1.6 type required
                // DG1.20 to identifers
                + "DG1|1||B45678|||A||||||||||||||one^https://terminology.hl7.org/CodeSystem/two^three^https://terminology.hl7.org/CodeSystem/four||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify we have 3 identifiers
        // NOTE: The first identifier which is not related to condition is tested in the
        // identifier suite of unit tests.
        assertThat(condition.getIdentifier()).hasSize(3);
        // First identifier 
        assertThat(condition.getIdentifier().get(0).getValue()).isEqualTo("B45678"); //DG1.3
        assertThat(condition.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:extID"); // DG1.3 (assumed)
        // Test 2nd identifier
        assertThat(condition.getIdentifier().get(1).getValue()).isEqualTo("one"); //DG1.20.1
        assertThat(condition.getIdentifier().get(1).getSystem())
                .isEqualTo("https://terminology.hl7.org/CodeSystem/two"); // DG1.20.2
        // Test 3rd identifier.
        assertThat(condition.getIdentifier().get(2).getValue()).isEqualTo("three"); //DG1.20.3
        assertThat(condition.getIdentifier().get(2).getSystem())
                .isEqualTo("https://terminology.hl7.org/CodeSystem/four"); // DG1.20.4
    }

    // Tests multiple DG1 segments to verify we get multiple conditions with
    // references to the encounter.
    @Test
    void validateEncounterMultipleDiagnoses() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||\r"
                + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\r"
                + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\r"
                + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);

        //Verify we have 3 conditions. 1 for each DG1.
        assertThat(conditionResource).hasSize(3);

        // Check the Code of each condition
        Condition condition = (Condition) conditionResource.get(0);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCode(), "V72.83",
                "Other specified pre-operative examination",
                "http://terminology.hl7.org/CodeSystem/icd9", "Other specified pre-operative examination");

        condition = (Condition) conditionResource.get(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCode(), "R00.0", "Tachycardia, unspecified",
                "http://hl7.org/fhir/sid/icd-10-cm", "Tachycardia, unspecified");

        condition = (Condition) conditionResource.get(2);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCode(), "R06.02", "Shortness of breath",
                "http://hl7.org/fhir/sid/icd-10-cm", "Shortness of breath");

        // Verify encounter reference exists
        assertThat(condition.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

        // --- ENCOUNTER TESTS ---

        // Find the encounter from the FHIR bundle.
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); //Verify we only have 1 encounter
        Encounter encounter = (Encounter) encounterResource.get(0);

        // Verify encounter.reasonReference has a reference for each of the 3 conditions.
        assertThat(encounter.getReasonReference()).hasSize(3);

        // Check the references are referencing conditions
        for (int i = 0; i < 3; i++) {
            assertThat(encounter.getReasonReference().get(i).getReference().substring(0, 10))
                    .isEqualTo("Condition/");
        }

    }

    // Tests that the Encounter has the full aray of condition references in both
    // diagnosis and reasonReference.
    @Test
    void validateEncounterMultipleDiagnosesTestingMultipleDiagnosisAndReasonReferences() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A04||||||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1492|||||||||||||||||||||||||\r"
                + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A|||||||||8|\r"
                + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A|||||||||8|\r"
                + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A|||||||||8|\r"
                + "DG1|4|D4|Q99.9^Chromosomal abnormality, unspecified^ICD-10^^^|Chromosomal abnormality, unspecified||A|||||||||8|\r"
                + "DG1|5|D5|I34.8^Arteriosclerosis^ICD-10^^^|Arteriosclerosis||A|||||||||8|\r"
                + "DG1|6|D6|I34.0^Mitral valve regurgitation^ICD-10^^^|Mitral valve regurgitation||A|||||||||8|\r"
                + "DG1|6|D7|I05.9^Mitral valve disorder in childbirth^ICD-10^^^|Mitral valve disorder in childbirth||A|||||||||8|\r"
                + "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A|||||||||8|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the conditions from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);

        // We should have 1 condition for each diagnosis therefore 8 for this message.
        assertThat(conditionResource).hasSize(8);

        // Find the encounter from the FHIR bundle.
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); //Verify we only have 1 encounter
        Encounter encounter = (Encounter) encounterResource.get(0);

        // Verify reasonReference has a reference for each condition. Therefore there should be 8.
        assertThat(encounter.getReasonReference()).hasSize(8);
        // Check the references are referencing conditions
        for (int i = 0; i < 8; i++) {
            assertThat(encounter.getReasonReference().get(i).getReference().substring(0, 10))
                    .isEqualTo("Condition/");
        }

        // Verify there is encounter.diagnosis for every diagnosis. Therefore there should be 8.
        assertThat(encounter.getDiagnosis()).hasSize(8);

        // Verify each diagnosis is set correctly.
        for (int i = 0; i < 8; i++) {
            // Diagnosis requires a reference to condition.
            assertThat(encounter.getDiagnosis().get(i).getCondition().getReference().substring(0, 10))
                    .isEqualTo("Condition/");
            // Verify Use coding of each diagnosis
            DatatypeUtils.checkCommonCodeableConceptAssertions(encounter.getDiagnosis().get(i).getUse(), "AD",
                    "Admission diagnosis",
                    "http://terminology.hl7.org/CodeSystem/diagnosis-role", null);
            // Verify encounter diagnosis rank is set correctly.
            assertThat(encounter.getDiagnosis().get(i).getRank()).isEqualTo(8); // DG1.15
        }

    }

    /**
     * ICD-10-CM system for diagnosis
     */
    @Test
    void validateConditionICD10CM() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A01||||||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1492|||||||||||||||||||||||||\r"
                + "DG1|1|ICD-10-CM|M54.5^Low back pain^ICD-10-CM|Low back pain|20210407191342|A\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the conditions from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);

        // We should have 1 condition for each diagnosis therefore 1 for this message.
        assertThat(conditionResource).hasSize(1);

        Condition c = (Condition) conditionResource.get(0);
        assertEquals("M54.5", c.getCode().getCoding().get(0).getCode());
        assertEquals("Low back pain", c.getCode().getCoding().get(0).getDisplay());
        assertEquals("http://hl7.org/fhir/sid/icd-10-cm", c.getCode().getCoding().get(0).getSystem());
    }

    // --------------------- PROBLEM UNIT TESTS (PRB) ---------------------

    // Tests the PRB segment (problem) with all supported message types. This tests
    // all the fields in the happy path (PART 1).
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC2|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC3|331|P|2.3.1||\r",
    })
    void validateProblemHappyTestOne(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB broken into multiple lines for readability
                // PRB.1 to PRB.4 required
                // PRB.2 to Recorded DateTime
                // PRB.7 for extension assertionDate
                // PRB.9 to Abatement DateTime
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||20100907175347||20180310074000|||"
                // PRB.13 for verificationStatus
                + "|confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status||"
                // PRB.16 prereq for PRB.17
                // PRB.17 for Onset detail
                // PRB.26 for severity
                + "|20170102074000|textual representation of the time when the problem began|||||||||some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify recorded date is set correctly.
        assertThat(condition.getRecordedDateElement().toString()).containsPattern("2017-01-10T07:40:00"); // PRB.2

        // Verify abatement date is set correctly.
        assertThat(condition.getAbatement().toString()).containsPattern("2018-03-10T07:40:00"); // PRB.9

        // Verify verificationStatus text and coding
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getVerificationStatus(), "confirmed", "Confirmed",
                "http://terminology.hl7.org/CodeSystem/condition-ver-status", "Confirmed"); // PRB.13

        // Verify onset string is set correctly (PRB.17). Only present if PRB.16 is
        // present. In this test case it is.
        assertThat(condition.getOnset().toString()).containsPattern("2017-01-02T07:40:00"); // PRB.17

        // Verify encounter reference exists
        assertThat(condition.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

        // Verify subject reference to Patient exists
        assertThat(condition.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");

        // Verify category text and coding are set correctly.
        assertThat(condition.getCategory()).hasSize(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCategoryFirstRep(), "problem-list-item",
                "Problem List Item",
                "http://terminology.hl7.org/CodeSystem/condition-category", "Problem List Item"); // PRB.3

        // Verify extension is set correctly.

        assertThat(condition.getExtension()).hasSize(1);
        assertThat(condition.getExtension().get(0).getUrl())
                .isEqualTo("http://hl7.org/fhir/StructureDefinition/condition-assertedDate");
        assertThat(condition.getExtension().get(0).getValue().toString()).containsPattern("2010-09-07T17:53:47"); // PRB.7

        // Verify severity code is set correctly.
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getSeverity(), "some prb detail", null, null,
                null); // PRB.26

    }

    // Tests the PRB segment (problem) with all supported message types. This tests
    // all the fields in the happy path (Part 2).
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC2|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC3|331|P|2.3.1||\r",
    })
    void validateProblemHappyTestTwo(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // // PRB broken into multiple lines for readability
                // // PRB.1 to PRB.4 required
                // // PRB.14 for clinical Status
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the conditions from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify code is set correctly.
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCode(), "K80.00", "Cholelithiasis",
                "http://hl7.org/fhir/sid/icd-10-cm", "Cholelithiasis"); // PRB.3

        // Verify clinicalStatus is set correctly
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getClinicalStatus(), "remission", "Remission",
                "http://terminology.hl7.org/CodeSystem/condition-clinical", "Remission"); //PRB.14

    }

    // This tests verificationStatus and clinicalStatus when they use a CWE but the code is
    // invalid or missing. We should be discarding these and not populating the optional field.
    @ParameterizedTest
    @ValueSource(strings = {
            // CASE 1:
            // PRB.1 to PRB.4 required
            // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
            // PRB.14 is INVALID with 'BAD' so clinicalStatus is omitted
            // PRB.13 is EMPTY so verificationStatus is omitted
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||BAD^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|||||||||||||\r",
            // CASE 2: Similar to CASE 1, but specific instance that caused problems in a test message.
            // For verificationStatus 'C^Confirmed^Confirmation Status List':
            // because 'C' is not a valid code we should be omitting this optional field.
            // PRB.1 to PRB.4 required
            // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
            // PRB.14 is INVALID with 'C' so clinicalStatus is omitted
            // PRB.13 is EMPTY so verificationStatus is omitted
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||C^Confirmed^Confirmation Status List|||||||||||||\r",
            // This tests verificationStatus and clinicalStatus when they only have one value
            // and it is invalid. We should be throwing these out and not populating these
            // optional fields.
            // CASE 3:
            // PRB.1 to PRB.4 required
            // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
            // PRB.14 is INVALID so clinicalStatus is omitted
            // PRB.13 is INVALID "BAD" so verificationStatus id omitted
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||||||BAD|INVALID|||||||||||||\r"
    })
    void validateProblemWithEmptyAndInvalidValues(String problemSegment) {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + problemSegment;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify verification status is omitted.
        assertThat(condition.hasVerificationStatus()).isFalse(); // PRB.13

        // Verify clinical status is omitted.
        assertThat(condition.hasClinicalStatus()).isFalse(); // PRB.14 (& EMPTY PRB.9)

    }

    // This tests verificationStatus and clinicalStatus when they only have one value
    // (for example 'confirmed'). This should work and create a coding for this field
    // but there will be no text because the HL7 message isn't passing a display in.
    @Test
    void validateProblemTestingOneWordGoodStatus() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r" + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
                // PRB.13 to verificationStatus
                // PRB.14 to clinicalStatus
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||||||confirmed|remission|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify verificationStatus has a coding but no text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getVerificationStatus(), "confirmed", "Confirmed",
                "http://terminology.hl7.org/CodeSystem/condition-ver-status", null); // PRB.13

        // Verify clinicalStatus has a coding but no text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getClinicalStatus(), "remission", "Remission",
                "http://terminology.hl7.org/CodeSystem/condition-clinical", null); // PRB.14

    }

    // This tests verificationStatus 'C^Confirmed^Confirmation Status List' which is in one our test messages.
    // But I edited the code to be correct in this case to verify we handle this stuation correctly.
    // Since display is in HL7 message we should have a text field with that value.
    @Test
    void validateProblemTestingGoodClinicalStatusTestData() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
                // PRB.13 to verificationStatus
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||||||confirmed^Confirmed^Confirmation Status List||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify verificationStatus has a coding and text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getVerificationStatus(), "confirmed", "Confirmed",
                "http://terminology.hl7.org/CodeSystem/condition-ver-status", "Confirmed"); // PRB.13

    }

    // This tests verificationStatus and clinicalStatus when the system is wrong.
    // We should ignore this bad system and use the correct one.
    @Test
    void validateProblemTestingBadSystem() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                // PRB.9 is EMPTY so PRB.14 does not activate a Resolved clinicalStatus
                // PRB.13 to verificationStatus
                // PRB.14 to clinicalStatus
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||||||confirmed^Confirmed^BADCLINCALSTATUSSYSTEM|remission^Remission^BADVERIFICATIONSTATUSSYSTEM|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify verificationStatus has a coding with correct system and text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getVerificationStatus(), "confirmed", "Confirmed",
                "http://terminology.hl7.org/CodeSystem/condition-ver-status", "Confirmed"); // PRB.13

        // Verify clinicalStatus has a coding with correct system  and text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getClinicalStatus(), "remission", "Remission",
                "http://terminology.hl7.org/CodeSystem/condition-clinical", "Remission"); // PRB.14

    }

    // Tests a particular PRB segment that wasn't working properly recently regards
    // to condition category.
    // Specifically that this HL7 messagee would create a code field with all 3
    // coding values (code, display, system) instead of seperating them out.
    //
    // "category": [
    //   {
    //   "coding": [
    //     {
    //       "code":
    //          "http://terminology.hl7.org/CodeSystem/condition-category,problem-list-item,Problem
    //           List Item"
    //     }
    //     ],
    //   "text": "problem-list-item"
    //   }
    // ]
    @Test
    void validateOverloadedCodeField() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                + "PRB|AD|20210101000000|G47.31^Primary central sleep apnea^ICD-10-CM|28827016|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify there is only one category and it has a coding with correct system and text
        assertThat(condition.getCategory()).hasSize(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getCategoryFirstRep(), "problem-list-item",
                "Problem List Item",
                "http://terminology.hl7.org/CodeSystem/condition-category", "Problem List Item");

    }

    // Tests multiple PRB segments to verify we get multiple conditions.
    @Test
    void validateProblemMultipleProblems() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|N39.0^Urinary Tract Infection^I9|53957|E2|1|20090907175347|20150907175347||||||||||||||||||\r"
                + "PRB|AD|20170110074000|C56.9^Ovarian Cancer^I10|53958|E3|2|20110907175347|20160907175347||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(2);
    }

    // Tests that onset[x] is set to PRB.16 if it is present AND
    // Tests that onset[x] is set to PRB.16 if it is present and PRB.17 is present.
    @ParameterizedTest
    @ValueSource(strings = {
            // PRB.1 to PRB.4 required
            // PRB.16 to onset date
            // PRB.17 purposely empty 
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||||20180310074000||\r",
            // PRB.1 to PRB.4 required
            // PRB.16 to onset date
            // PRB.17 present
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956||||||||||||20180310074000|textual representation of the time when the problem began|\r" })
    void validateProblemWithOnsetDateTimeWithNoOnsetString(String segmentPRB) {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + segmentPRB;
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Condition condition = (Condition) conditionResource.get(0);

        // Verify onset is set correctly to PRB.16
        assertThat(condition.getOnset().toString()).containsPattern("2018-03-10T07:40:00"); // PRB.16

    }

    // Tests (1) that onset[x] is correctly set to PRB.17 if we have no PRB.16
    // Tests (2) that if PRB.9 is set and PRB.14 is null, default Condition.clinicalStatus to "resolved"
    @Test
    void validateProblemWithOnsetStringAndNoOnsetdateClinicalStatusResolved() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                // Important fields:
                // PRB.1 to PRB.4 required
                // PRB.9 Actual Problem Resolution Date used in connection with EMPTY PRB.14 to trigger Resolved clinicalStatus
                // PRB.17 to Onset
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||20150907175347||||||||textual representation of the time when the problem began|||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify clinicalStatus has a Resolved coding with correct system and text
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getClinicalStatus(), "resolved", "Resolved",
                "http://terminology.hl7.org/CodeSystem/condition-clinical", null); // PRB.9 && EMPTY PRB.14

        // Verify onset is set correctly to PRB.17
        assertThat(condition.getOnset()).hasToString("textual representation of the time when the problem began"); // PRB.17
    }

    // Tests that if PRB.9 is set and PRB.14 is INVALID, default Condition.clinicalStatus to "resolved"
    @Test
    void validateProblemWithInvalidClinicalStatusAndResolutionDate() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                // Important fields:
                // PRB.1 to PRB.4 required
                // PRB.9 Actual Problem Resolution Date used in connection with INVALID PRB.14 to trigger Resolved clinicalStatus
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|||||20150907175347|||||INVALID|||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);

        // Verify clinicalStatus has a Resolved coding with correct system and text (based on PRB.9 and INVALID PRB.14)
        DatatypeUtils.checkCommonCodeableConceptAssertions(condition.getClinicalStatus(), "resolved", "Resolved",
                "http://terminology.hl7.org/CodeSystem/condition-clinical", null);
    }

}