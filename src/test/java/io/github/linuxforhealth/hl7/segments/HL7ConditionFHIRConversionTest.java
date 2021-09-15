/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class HL7ConditionFHIRConversionTest {

    // --------------------- DIAGNOSIS UNIT TESTS (DG1) ---------------------

    // Tests the DG1 segment (diagnosis) with all supported message types. This
    // tests all the fields in the happy path.
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
    // "MSH|^~\\&|||||||ADT^A03|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
    // "MSH|^~\\&|||||||ADT^A04|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
    // "MSH|^~\\&|||||||ADT^A08|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
    // "MSH|^~\\&|||||||ADT^A28^ADT^A28|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
    // "MSH|^~\\&|||||||ADT^A31|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
    // "MSH|^~\\&|||||||ORM^O01|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r"
    })
    public void validateDiagnosis(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||\r"
                + "DG1|1|ICD10|C56.9^Ovarian Cancer^I10|Test|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|V45|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // --- CONDITION TESTS ---

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get condition Resource
        Resource condition = conditionResource.get(0);

        // Get condition identifiers
        Property identifierProperty = condition.getNamedProperty("identifier");
        List<Base> identifierList = identifierProperty.getValues();

        // Verify we have 3 identifiers
        // NOTE: The other identifiers, not related to condition, are tested in the
        // identifier suite of unit tests.
        assertThat(identifierList).hasSize(3);

        // The 3rd identifier value should be from DG1.20
        String thirdIdentiferValue = ResourceUtils.getValueAsString(identifierList.get(2), "value");
        assertThat(thirdIdentiferValue).isEqualTo("V45");

        // Verify asserter reference to Practitioner exists
        Base asserter = ResourceUtils.getValue(condition, "asserter");
        assertThat(ResourceUtils.getValueAsString(asserter, "reference").substring(0, 13)).isEqualTo("Practitioner/");

        // Verify recorded date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "recordedDate"))
                .isEqualTo("DateTimeType[2021-03-22T15:43:26+08:00]");

        // Verify onset date time is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "onsetDateTime"))
                .isEqualTo("DateTimeType[2021-03-22T15:44:49+08:00]");

        // Verify code text is set correctly.
        Base code = ResourceUtils.getValue(condition, "code");
        assertThat(ResourceUtils.getValueAsString(code, "text")).isEqualTo("Ovarian Cancer");

        // Verify code coding is set correctly.
        Base coding = ResourceUtils.getValue(code, "coding");
        // change from http://hl7.org/fhir/sid/icd-10 to
        // http://hl7.org/fhir/sid/icd-10-cm temporarily, see Issue #189
        assertThat(ResourceUtils.getValueAsString(coding, "system"))
                .isEqualTo("UriType[http://hl7.org/fhir/sid/icd-10-cm]");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("C56.9");
        assertThat(ResourceUtils.getValueAsString(coding, "display")).isEqualTo("Ovarian Cancer");

        // Verify encounter reference exists
        Base encounterProp = ResourceUtils.getValue(condition, "encounter");
        assertThat(ResourceUtils.getValueAsString(encounterProp, "reference").substring(0, 10)).isEqualTo("Encounter/");

        // Verify subject reference to Patient exists
        Base subject = ResourceUtils.getValue(condition, "subject");
        assertThat(ResourceUtils.getValueAsString(subject, "reference").substring(0, 8)).isEqualTo("Patient/");

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("Encounter Diagnosis");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("encounter-diagnosis");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Encounter Diagnosis");

        // --- ENCOUNTER TESTS ---

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Get the encounter resource
        Base encounter = encounterResource.get(0);

        // Verify reasonReference to condition exists
        Base reasonReference = ResourceUtils.getValue(encounter, "reasonReference");
        assertThat(ResourceUtils.getValueAsString(reasonReference, "reference").substring(0, 10))
                .isEqualTo("Condition/");

        // Verify encounter diagnosis use is set correctly
        Base diagnosis = ResourceUtils.getValue(encounter, "diagnosis");
        Base use = ResourceUtils.getValue(diagnosis, "use");
        assertThat(ResourceUtils.getValueAsString(use, "text")).isEqualTo("A");
        Base diagCoding = ResourceUtils.getValue(use, "coding");
        assertThat(ResourceUtils.getValueAsString(diagCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/diagnosis-role]");
        assertThat(ResourceUtils.getValueAsString(diagCoding, "code")).isEqualTo("AD");
        assertThat(ResourceUtils.getValueAsString(diagCoding, "display")).isEqualTo("Admission diagnosis");

        // Verify encounter diagnosis rank is set correctly.
        assertThat(ResourceUtils.getValueAsString(diagnosis, "rank")).isEqualTo("PositiveIntType[1]");

        // Diagnosis requires a reference to condition.
        Base conditionRef = ResourceUtils.getValue(encounter, "reasonReference");
        assertThat(ResourceUtils.getValueAsString(conditionRef, "reference").substring(0, 10)).isEqualTo("Condition/");

        // --- PRACTIONER TESTS ---

        // Find the practitioner resource from the FHIR bundle.
        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(1);

        // Get practitioner Resource
        Resource practitioner = practitionerResource.get(0);

        // Verify name text, family, and given are set correctly.
        Base name = ResourceUtils.getValue(practitioner, "name");
        assertThat(ResourceUtils.getValueAsString(name, "text")).isEqualTo("JOHN A DOE");
        assertThat(ResourceUtils.getValueAsString(name, "family")).isEqualTo("DOE");
        assertThat(ResourceUtils.getValueAsString(name, "given")).isEqualTo("JOHN");

        // Verify asserter identifier is set correctly.
        Base identifier = ResourceUtils.getValue(practitioner, "identifier");
        assertThat(ResourceUtils.getValueAsString(identifier, "value")).isEqualTo("123");

    }

    // Tests the DG1 segment (diagnosis) with a full Entity Identifier (EI).
    // These values come from DG1.20.
    @Test
    public void validateDiagnosisWithEIIdentifiers() {

        String hl7message = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|one^https://terminology.hl7.org/CodeSystem/two^three^https://terminology.hl7.org/CodeSystem/four|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get condition Resource
        Resource condition = conditionResource.get(0);

        // Get condition identifiers
        Property identifierProperty = condition.getNamedProperty("identifier");
        List<Base> identifierList = identifierProperty.getValues();

        // Verify we have 3 identifier
        // NOTE: The first identifier which is not related to condition is tested in the
        // identifier suite of unit tests.
        assertThat(identifierList).hasSize(3);

        // Test 2nd identifier
        Base identifierTwo = identifierList.get(1);
        // value = DG1.20.1
        assertThat(ResourceUtils.getValueAsString(identifierTwo, "value")).isEqualTo("one");
        // system = DG1.20.2
        assertThat(ResourceUtils.getValueAsString(identifierTwo, "system"))
                .isEqualTo("UriType[https://terminology.hl7.org/CodeSystem/two]");

        // Test 3rd identifier.
        Base identifierThree = identifierList.get(2);
        // value = DG1.20.3
        assertThat(ResourceUtils.getValueAsString(identifierThree, "value")).isEqualTo("three");
        // system = DG1.20.4
        assertThat(ResourceUtils.getValueAsString(identifierThree, "system"))
                .isEqualTo("UriType[https://terminology.hl7.org/CodeSystem/four]");

    }

    // Tests multiple DG1 segments to verify we get multiple conditions with
    // references to the encounter.
    @Test
    public void validateEncounterMultipleDiagnoses() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV2|||||||||||||||||||||||||||||||RR|Y|2|Y|Y|N|N|\r"
                + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\r"
                + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\r"
                + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\r"
                + "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(4);

        Condition condition = (Condition) conditionResource.get(1);
        assertThat(condition.hasCode()).isTrue();
        CodeableConcept condCC = condition.getCode();
        assertThat(condCC.hasText()).isTrue();
        assertThat(condCC.getText()).isEqualTo("Tachycardia, unspecified");
        assertThat(condCC.hasCoding()).isTrue();
        assertThat(condCC.getCoding().size()).isEqualTo(1);

        Coding condCoding = condCC.getCoding().get(0);
        assertThat(condCoding.hasSystem()).isTrue();
        // change from http://hl7.org/fhir/sid/icd-10 to
        // http://hl7.org/fhir/sid/icd-10-cm temporarily, see Issue #189
        assertThat(condCoding.getSystem()).isEqualTo("http://hl7.org/fhir/sid/icd-10-cm");
        assertThat(condCoding.hasCode()).isTrue();
        assertThat(condCoding.getCode()).isEqualTo("R00.0");
        assertThat(condCoding.hasDisplay()).isTrue();
        assertThat(condCoding.getDisplay()).isEqualTo("Tachycardia, unspecified");
        assertThat(condCoding.hasVersion()).isFalse();

    }

    // Tests that the Encounter has the full aray of condition references in both
    // diagnosis and reasonReference.
    @Test
    public void validateEncounterMultipleDiagnosesTestingMultipleDiagnosisAndReasonReferences() {

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

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the conditions from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // We should have 1 condition for each diagnosis therefore 8 for this message.
        assertThat(conditionResource).hasSize(8);

        // Find the encounter from the FHIR bundle.
        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Get the encounter resource
        Base encounter = encounterResource.get(0);

        // Verify reasonReference has a reference for each condition.
        List<Base> reasonReferences = encounter.getNamedProperty("reasonReference").getValues();
        // Therefore there should be 8.
        assertThat(reasonReferences).hasSize(8);

        // And the references should be referencing conditions
        for (Base reasonReference : reasonReferences) {
            assertThat(ResourceUtils.getValueAsString(reasonReference, "reference").substring(0, 10))
                    .isEqualTo("Condition/");
        }

        // Verify there is encounter.diagnosis for every diagnosis
        List<Base> diagnosises = encounter.getNamedProperty("diagnosis").getValues();
        // Therefore there should be 8.
        assertThat(diagnosises).hasSize(8);

        // Verify each diagnosis is set correctly.
        for (Base diagnosis : diagnosises) {

            Base use = ResourceUtils.getValue(diagnosis, "use");
            assertThat(ResourceUtils.getValueAsString(use, "text")).isEqualTo("A");
            Base diagCoding = ResourceUtils.getValue(use, "coding");
            assertThat(ResourceUtils.getValueAsString(diagCoding, "system"))
                    .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/diagnosis-role]");
            assertThat(ResourceUtils.getValueAsString(diagCoding, "code")).isEqualTo("AD");
            assertThat(ResourceUtils.getValueAsString(diagCoding, "display")).isEqualTo("Admission diagnosis");

            // Verify encounter diagnosis rank is set correctly.
            assertThat(ResourceUtils.getValueAsString(diagnosis, "rank")).isEqualTo("PositiveIntType[8]");

            // Diagnosis requires a reference to condition.
            Base conditionRef = ResourceUtils.getValue(encounter, "reasonReference");
            assertThat(ResourceUtils.getValueAsString(conditionRef, "reference").substring(0, 10))
                    .isEqualTo("Condition/");
        }

    }

    // --------------------- PROBLEM UNIT TESTS (PRB) ---------------------

    // Tests the PRB segment (problem) with all supported message types. This tests
    // all the fields in the happy path (PART 1).
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC2|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC3|331|P|2.3.1||\r",
    })
    public void validateProblemHappyTestOne(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);
        ;

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify recorded date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "recordedDate"))
                .isEqualTo("DateTimeType[2017-01-10T07:40:00+08:00]");

        // Verify abatement date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "abatementDateTime"))
                .isEqualTo("DateTimeType[2018-03-10T07:40:00+08:00]");

        // Verify verification status text is set correctly.
        Base verificationStatus = ResourceUtils.getValue(condition, "verificationStatus");
        assertThat(ResourceUtils.getValueAsString(verificationStatus, "text")).isEqualTo("Confirmed");

        // Verify verification status coding is set correctly
        Base coding = ResourceUtils.getValue(verificationStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-ver-status]");
        assertThat(ResourceUtils.getValueAsString(coding, "display")).isEqualTo("Confirmed");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("confirmed");

        // Verify onset string is set correctly (PRB.17). Only present if PRB.16 is
        // present. In this test case it is.
        assertThat(ResourceUtils.getValueAsString(condition, "onset"))
                .isEqualTo("DateTimeType[2017-01-02T07:40:00+08:00]");

        // Verify encounter reference exists
        Base encounter = ResourceUtils.getValue(condition, "encounter");
        assertThat(ResourceUtils.getValueAsString(encounter, "reference").substring(0, 10)).isEqualTo("Encounter/");

        // Verify subject reference to Patient exists
        Base subject = ResourceUtils.getValue(condition, "subject");
        assertThat(ResourceUtils.getValueAsString(subject, "reference").substring(0, 8)).isEqualTo("Patient/");

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("Problem List Item");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Problem List Item");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("problem-list-item");

        // Verify extension is set correctly.
        Base extension = ResourceUtils.getValue(condition, "extension");
        assertThat(ResourceUtils.getValueAsString(extension, "url"))
                .isEqualTo("UriType[http://hl7.org/fhir/StructureDefinition/condition-assertedDate]");
        assertThat(ResourceUtils.getValueAsString(extension, "valueDateTime"))
                .isEqualTo("DateTimeType[2010-09-07T17:53:47+08:00]");

        // Verify severity code is set correctly.
        Base severity = ResourceUtils.getValue(condition, "severity");
        Base sevCoding = ResourceUtils.getValue(severity, "coding");
        assertThat(ResourceUtils.getValueAsString(sevCoding, "code")).isEqualTo("some prb detail");

    }

    // Tests the PRB segment (problem) with all supported message types. This tests
    // all the fields in the happy path (Part 2).
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC2|331|P|2.3.1||\r",
    // "MSH|^~\\&|||||20040629164652|1|PPR^PC3|331|P|2.3.1||\r",
    })
    public void validateProblemHappyTestTwo(String msh) {

        String hl7message = msh + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify code text is set correctly.
        Base code = ResourceUtils.getValue(condition, "code");
        assertThat(ResourceUtils.getValueAsString(code, "text")).isEqualTo("Cholelithiasis");

        // Verify code coding fields are set correctly.
        Base codeCoding = ResourceUtils.getValue(code, "coding");
        // change from http://hl7.org/fhir/sid/icd-10 to
        // http://hl7.org/fhir/sid/icd-10-cm temporarily, see Issue #189
        assertThat(ResourceUtils.getValueAsString(codeCoding, "system"))
                .isEqualTo("UriType[http://hl7.org/fhir/sid/icd-10-cm]");
        assertThat(ResourceUtils.getValueAsString(codeCoding, "code")).isEqualTo("K80.00");

        // Verify clinicalStatus text is set correctly
        Base clinicalStatus = ResourceUtils.getValue(condition, "clinicalStatus");
        assertThat(ResourceUtils.getValueAsString(clinicalStatus, "text")).isEqualTo("Remission");

        // Verify clinicalStatus coding is set correctly
        Base clinCoding = ResourceUtils.getValue(clinicalStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-clinical]");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "code")).isEqualTo("remission");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "display")).isEqualTo("Remission");

    }

    // This tests verificationStatus and clincalStatus when they use a CWE but the
    // code is invalid. We should be discarding these and not populating the
    // optional field.
    @Test
    public void validateProblemWithInvalidValues() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||BAD^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|INVALID^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status is omitted.
        assertThat(condition.listChildrenByName("verificationStatus")).isEmpty();

        // Verify clinical status is omitted.
        assertThat(condition.listChildrenByName("clinicalStatus")).isEmpty();

    }

    // This tests verificationStatus and clincalStatus when they only have one value
    // (for example 'confirmed'). This should work and create a coding for this
    // field
    // but there will be no text because the HL7 message isn't passing a display in.
    @Test
    public void validateProblemTestingOneWordGoodStatus() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r" + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||confirmed|remission|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status text is omitted.
        Base verificationStatus = ResourceUtils.getValue(condition, "verificationStatus");
        assertThat(verificationStatus.listChildrenByName("text")).isEmpty();

        // Verify verification status coding is set correctly
        Base coding = ResourceUtils.getValue(verificationStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-ver-status]");
        assertThat(ResourceUtils.getValueAsString(coding, "display")).isEqualTo("Confirmed");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("confirmed");

        // Verify clinical status text is omitted.
        Base clinicalStatus = ResourceUtils.getValue(condition, "clinicalStatus");
        assertThat(clinicalStatus.listChildrenByName("text")).isEmpty();

        // Verify clinical status coding is set correctly
        Base clinCoding = ResourceUtils.getValue(clinicalStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-clinical]");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "code")).isEqualTo("remission");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "display")).isEqualTo("Remission");

    }

    // This tests verifcationStatus 'C^Confirmed^Confirmation Status List' which is
    // in one our test messages.
    // Because 'C' is not a valid code we should be omitting this optional field.
    @Test
    public void validateProblemTestingBadClinicalStatusTestData() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||C^Confirmed^Confirmation Status List||textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status is omitted.
        assertThat(condition.listChildrenByName("verificationStatus")).isEmpty();

    }

    // This tests verificationStatus 'C^Confirmed^Confirmation Status List' which is
    // in one our test messages.
    // But I editted the code to be correct in this case to verify we handle this
    // stuation correctly.
    // Since display is in HL7 message we should have a text field with that value.
    @Test
    public void validateProblemTestingGoodClinicalStatusTestData() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||confirmed^Confirmed^Confirmation Status List||textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status text is set correctly.
        Base verificationStatus = ResourceUtils.getValue(condition, "verificationStatus");
        assertThat(ResourceUtils.getValueAsString(verificationStatus, "text")).isEqualTo("Confirmed");

        // Verify verification status coding is set correctly
        Base coding = ResourceUtils.getValue(verificationStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-ver-status]");
        assertThat(ResourceUtils.getValueAsString(coding, "display")).isEqualTo("Confirmed");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("confirmed");

    }

    // This tests verificationStatus and clincalStatus when they only have one value
    // and it is invalid. We should be throwing these out and not populating these
    // optional fields.
    @Test
    public void validateProblemTestingOneWordBadStatus() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||BAD|INVALID|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status is omitted.
        assertThat(condition.listChildrenByName("verificationStatus")).isEmpty();

        // Verify clinical status is omitted.
        assertThat(condition.listChildrenByName("clinicalStatus")).isEmpty();
    }

    // This tests verificationStatus and clincalStatus when the system is wrong.
    // We should ignore this bad system and use the correct one.
    @Test
    public void validateProblemTestingBadSystem() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^BADCLINCALSTATUSSYSTEM|remission^Remission^BADVERIFICATIONSTATUSSYSTEM|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify verification status text is set correctly.
        Base verificationStatus = ResourceUtils.getValue(condition, "verificationStatus");
        assertThat(ResourceUtils.getValueAsString(verificationStatus, "text")).isEqualTo("Confirmed");

        // Verify verification status coding is set correctly
        Base coding = ResourceUtils.getValue(verificationStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-ver-status]");
        assertThat(ResourceUtils.getValueAsString(coding, "display")).isEqualTo("Confirmed");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("confirmed");

        // Verify clinicalStatus text is set correctly
        Base clinicalStatus = ResourceUtils.getValue(condition, "clinicalStatus");
        assertThat(ResourceUtils.getValueAsString(clinicalStatus, "text")).isEqualTo("Remission");

        // Verify clinicalStatus coding is set correctly
        Base clinCoding = ResourceUtils.getValue(clinicalStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-clinical]");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "code")).isEqualTo("remission");
        assertThat(ResourceUtils.getValueAsString(clinCoding, "display")).isEqualTo("Remission");
    }

    // Tests a particular PRB segment that wasn't working properly recently regards
    // to condition category.
    // Specifically that this HL7 messagee would create a code field with all 3
    // coding values (code, display, system) instead of seperating them out.
    //
    // "category": [
    // {
    // "coding": [
    // {
    // "code":
    // "http://terminology.hl7.org/CodeSystem/condition-category,problem-list-item,Problem
    // List Item"
    // }
    // ],
    // "text": "problem-list-item"
    // }
    // ]
    @Test
    public void validateOverloadedCodeField() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20210101000000|G47.31^Primary central sleep apnea^ICD-10-CM|28827016|||20210101000000|20210101000000|||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status||20210101000000|20210101000000\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("Problem List Item");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Problem List Item");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("problem-list-item");

    }

    // Tests multiple PRB segments to verify we get multiple conditions.
    @Test
    public void validateProblemMultipleProblems() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|N39.0^Urinary Tract Infection^I9|53957|E2|1|20090907175347|20150907175347||||||||||||||||||\r"
                + "PRB|AD|20170110074000|C56.9^Ovarian Cancer^I10|53958|E3|2|20110907175347|20160907175347||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(2);
    }

    // Tests that onset[x] is set to PRB.16 if it is present AND
    // Tests that onset[x] is set to PRB.16 if it is present and PRB.17 is present.
    @ParameterizedTest
    @ValueSource(strings = {
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||||20180310074000|20180310074000||1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r",
            "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||||20180310074000|20180310074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r" })
    public void validateProblemWithOnsetDateTimeWithNoOnsetString() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||||20180310074000|20180310074000||1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify onset is set correctly to PRB.16
        assertThat(ResourceUtils.getValueAsString(condition, "onset"))
                .isEqualTo("DateTimeType[2018-03-10T07:40:00+08:00]");

    }

    // Tests that onset[x] is correctly set to PRB.17 if we have no PRB.16
    @Test
    public void validateProblemWithOnsetStringAndNoOnsetdate() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|||||||20180310074000||textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        List<BundleEntryComponent> e = ResourceUtils.createHl7Segment(hl7message);

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify onset is set correctly to PRB.17
        assertThat(ResourceUtils.getValueAsString(condition, "onset"))
                .isEqualTo("textual representation of the time when the problem began");
    }

}