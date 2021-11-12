/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

// These tests push deep into NTE's and precise creation.
// High level note creation for other messages is done:
// For MDM_T0x in test_mdm_ORDER_with_OBXnotTX

public class Hl7NoteFHIRConverterTest {

    // Tests NTE creation for OBX (Observations) and ORC/OBRs (ServiceRequests)
    // Test associated Practitioners and references created. (NTE.4)
    // Tests messages "ORM^O01" & "ORU^R01^ORU_R01"

    // This private method provide parameters to the the test
    private static Stream<Arguments> provideParmsForNoteCreationServiceRequestMutipleOBX() {
        return Stream.of(
                //  Arguments are: (message, numExpectedDiagnosticReports)
                Arguments.of("ORU^R01^ORU_R01", 1),
                Arguments.of("ORM^O01", 0));
    }

    @ParameterizedTest
    @MethodSource("provideParmsForNoteCreationServiceRequestMutipleOBX")
    public void testNoteCreationServiceRequestMutipleOBX(String message, int numExpectedDiagnosticReports) {
        String hl7ORU = "MSH|^~\\&|||||20180924152907||" + message + "|213|T|2.3.1|||||||||||\n"
                + "PID|||Pract1ID^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // TODO: Future work, handle NTE's on PID
                // "NTE|1|O|TEST NOTE DD line 1|\n" + "NTE|2|O|TEST NOTE DD line 2 |\n" + "NTE|3|O|TEST NOTE D line 3|\n"  
                + "PV1|1|I||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
                + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|||||||||||||||||||||||||||\n"
                + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||||||||||||||||||||||||||||||||||||\n"
                // ServiceRequest NTE has a practitioner reference in NTE.5
                + "NTE|1|O|TEST ORC/OBR NOTE AA line 1||Pract1ID^Pract1Last^Pract1First|\n"
                + "NTE|2|O|TEST NOTE AA line 2|\n"
                + "NTE|3|O|TEST NOTE AA line 3|\n"
                + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F||||||||||||||\n"
                // GLYCOHEMOGLOBIN Observation NTE has a practitioner reference in NTE.5.  Note in second NTE. The first valied NTE.5 is used.
                + "NTE|1|L|TEST OBXa NOTE BB line 1|\n"
                + "NTE|2|L|TEST NOTE BB line 2||Pract2ID^Pract2Last^Pract2First|\n"
                + "NTE|3|L|TEST NOTE BB line 3|\n"
                + "OBX|2|NM|17853^MEAN BLOOD GLUCOSE^LRR^^^^^^MEAN BLOOD GLUCOSE||114.02|mg/dL|||||F||||||||||||||\n"
                // Glucose Observation NTE has no practitioner reference in NTE.5
                + "NTE|1|L|TEST OBXb NOTE CC line 1|\n"
                + "NTE|2|L|TEST NOTE CC line 2|\n"
                + "NTE|3|L| |\n" // Test that blank lines are preserved.
                + "NTE|4|L|TEST NOTE CC line 4|\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7ORU);
        List<Resource> diagnosticReports = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);

        // DiagnosticReport expected for ORU, but not for ORM
        assertThat(diagnosticReports).hasSize(numExpectedDiagnosticReports); // From the OBR in the ORU test case

        // One ServiceRequest contains NTE for ORC/OBR
        List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequests).hasSize(1);
        ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequests.get(0),
                ResourceUtils.context);
        assertThat(serviceRequest.hasNote()).isTrue();
        assertThat(serviceRequest.getNote()).hasSize(1);
        // Processing adds "  \n" two spaces and a line feed between each line.
        // NOTE: the note contains an Annotation, which contains a MarkdownType that has the string.
        // Must use getTextElement().getValueAsString() to see untrimmed contents.
        assertThat(serviceRequest.getNote().get(0).getTextElement().getValueAsString()).isEqualTo(
                "TEST ORC/OBR NOTE AA line 1  \nTEST NOTE AA line 2  \nTEST NOTE AA line 3");
        assertThat(serviceRequest.getNote().get(0).hasAuthorReference()).isTrue();
        String practitionerServReqRefId = serviceRequest.getNote().get(0).getAuthorReference().getReference();

        // Two observations.  One has GLYCOHEMOGLOBIN and notes BB, One has GLUCOSE and notes CC
        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(2); // Should be 2 for ORU and ORM
        Observation obsGlucose = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);
        Observation obsHemoglobin = ResourceUtils.getResourceObservation(observations.get(1), ResourceUtils.context);
        // Figure out which is first and reassign if needed for testing
        if (obsGlucose.getCode().getText() != "MEAN BLOOD GLUCOSE") {
            Observation temp = obsGlucose;
            obsGlucose = obsHemoglobin;
            obsHemoglobin = temp;
        }
        // Validate the note contents and references 
        assertThat(obsHemoglobin.hasNote()).isTrue();
        assertThat(obsHemoglobin.getNote()).hasSize(1);
        assertThat(obsHemoglobin.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST OBXa NOTE BB line 1  \nTEST NOTE BB line 2  \nTEST NOTE BB line 3");
        assertThat(obsHemoglobin.getNote().get(0).hasAuthorReference()).isTrue();
        String practitionerObsHemoglobinRefId = obsHemoglobin.getNote().get(0).getAuthorReference().getReference();

        assertThat(obsGlucose.hasNote()).isTrue();
        assertThat(obsGlucose.getNote()).hasSize(1);
        assertThat(obsGlucose.getNote().get(0).getTextElement().getValueAsString()).isEqualTo(
                "TEST OBXb NOTE CC line 1  \nTEST NOTE CC line 2  \n   \nTEST NOTE CC line 4"); // Test that blank lines are preserved.
        assertThat(obsGlucose.getNote().get(0).hasAuthorReference()).isFalse();

        // Two Practitioners, one for the serviceRequest, one for the GLYCOHEMOGLOBIN Observation
        List<Resource> practitioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitioners).hasSize(2);
        Practitioner practitionerServReq = ResourceUtils.getResourcePractitioner(practitioners.get(0),
                ResourceUtils.context);
        Practitioner practitionerObsHemoglobin = ResourceUtils.getResourcePractitioner(practitioners.get(1),
                ResourceUtils.context);
        // Adjust to correct practitioner if needed        
        if (!practitionerServReq.getIdentifierFirstRep().getValue().contentEquals("Pract1ID")) {
            Practitioner temp = practitionerObsHemoglobin;
            practitionerObsHemoglobin = practitionerServReq;
            practitionerServReq = temp;
        }
        // Check the values for the Practitioners and validate match to references.             
        assertThat(practitionerServReq.getIdentifier()).hasSize(1);
        assertThat(practitionerServReq.getIdentifierFirstRep().getValue()).isEqualTo("Pract1ID");
        assertThat(practitionerServReq.getName()).hasSize(1);
        assertThat(practitionerServReq.getNameFirstRep().getText()).isEqualTo("Pract1First Pract1Last");
        assertThat(practitionerServReq.getId()).isEqualTo(practitionerServReqRefId); // Check the cross-reference
        // Sanity check to confirm data corruption in meta content has not returned.
        CodeableConcept ccSourceEventTrigger = (CodeableConcept) practitionerServReq.getMeta()
                .getExtensionByUrl("http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger").getValue();
        assertThat(ccSourceEventTrigger.hasText()).isFalse();

        assertThat(practitionerObsHemoglobin.getIdentifier()).hasSize(1);
        assertThat(practitionerObsHemoglobin.getIdentifierFirstRep().getValue()).isEqualTo("Pract2ID");
        assertThat(practitionerObsHemoglobin.getName()).hasSize(1);
        assertThat(practitionerObsHemoglobin.getNameFirstRep().getText()).isEqualTo("Pract2First Pract2Last");
        assertThat(practitionerObsHemoglobin.getId()).isEqualTo(practitionerObsHemoglobinRefId); // Check the cross-reference  
    }

    // Tests NTE creation for OMP/RDE (MedicationRequest) messages and OBX (Observations) 
    // Test associated Practitioners and references created. (NTE.4)
    // This private method provide parameters to the the test
    private static Stream<Arguments> parmsTestMedicationRequestNoteCreation() {
        return Stream.of(
                //  Arguments are: (message, medicalRequestSegments)
                Arguments.of("OMP^O09",
                        "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||||||G||4||0|||||||||||\n"),
                // NOTE: RDE processing prioritizes RXE segments and ignores RXO segments and their associated NTE's and NTE practitioner references.
                // They are included below to show that no extra MedicationRequests, NTEs or Practitioners are added.
                Arguments.of("RDE^O11",
                        "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||||||G||4||0|||||||||||\n"
                                + "NTE|1|O|TEST ORC/OBR NOTE DDD line 1||Pract3ID^Pract3Last^Pract3First|\n"
                                + "RXE|^Q24H&0600^^20210407191342^^ROU|0169-6339-10^insulin aspart (NOVOLOG) subcutaneous injection (CORRECTION SCALE INSULIN) 0-16 Units^NDC|0||Units|47^SOLN|||||||\n"),
                Arguments.of("RDE^O25",
                        "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||||||G||4||0|||||||||||\n"
                                + "NTE|1|O|TEST ORC/OBR NOTE DDD line 1||Pract3ID^Pract3Last^Pract3First|\n"
                                + "RXE|^Q24H&0600^^20210407191342^^ROU|0169-6339-10^insulin aspart (NOVOLOG) subcutaneous injection (CORRECTION SCALE INSULIN) 0-16 Units^NDC|0||Units|47^SOLN|||||||\n"));
    }

    @ParameterizedTest
    @MethodSource("parmsTestMedicationRequestNoteCreation")
    public void testMedicationRequestNoteCreation(String message, String medicalRequestSegments) {
        // Minimal valid ORC message.  Requires RXO and RXR segments.
        String hl7message = "MSH|^~\\&||||IBM|20210101000000||" + message + "|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                + "ORC|OP|1234|1234|0827||||||||||||||||||||\n"
                + medicalRequestSegments // May be RXO or RXO+RXE values from parmsTestMedicationRequestNoteCreation
                + "NTE|1|O|TEST MedReq NOTE AA line 1||Pract1ID^Pract1Last^Pract1First|\n"
                + "NTE|2|O|TEST NOTE AA line 2|\n"
                + "NTE|3|O|TEST NOTE AA line 3|\n"
                + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F||||||||||||||\n"
                // GLYCOHEMOGLOBIN Observation NTE has a practitioner reference in NTE.5.  Note in second NTE. The first valid NTE.5 is used.
                + "NTE|1|L|TEST OBXa NOTE BB line 1|\n"
                + "NTE|2|L|TEST NOTE BB line 2||Pract2ID^Pract2Last^Pract2First|\n"
                + "NTE|3|L|TEST NOTE BB line 3|\n"
                + "OBX|2|NM|17853^MEAN BLOOD GLUCOSE^LRR^^^^^^MEAN BLOOD GLUCOSE||114.02|mg/dL|||||F||||||||||||||\n"
                // Glucose Observation NTE has no practitioner reference in NTE.5
                + "NTE|1|L|TEST OBXb NOTE CC line 1|\n"
                + "NTE|2|L|TEST NOTE CC line 2|\n"
                + "NTE|3|L| |\n" // Test that blank lines are preserved.
                + "NTE|4|L|TEST NOTE CC line 4|\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expect MedicationRequest containing NTE for RXO or RXE
        List<Resource> medicationRequests = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequests).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequests.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.hasNote()).isTrue();
        assertThat(medicationRequest.getNote()).hasSize(1);
        // NOTE: the note contains an Annotation, which contains a MarkdownType that has the string.
        // Must use getTextElement().getValueAsString() to see untrimmed contents.
        assertThat(medicationRequest.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST MedReq NOTE AA line 1  \nTEST NOTE AA line 2  \nTEST NOTE AA line 3");
        assertThat(medicationRequest.getNote().get(0).hasAuthorReference()).isTrue();
        String practitionerServReqRefId = medicationRequest.getNote().get(0).getAuthorReference().getReference();

        // Two observations.  One has GLYCOHEMOGLOBIN and notes BB, One has GLUCOSE and notes CC
        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(2); // Should be 2 for ORU and ORM
        Observation obsGlucose = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);
        Observation obsHemoglobin = ResourceUtils.getResourceObservation(observations.get(1), ResourceUtils.context);
        // Figure out which is first and reassign if needed for testing
        if (obsGlucose.getCode().getText() != "MEAN BLOOD GLUCOSE") {
            Observation temp = obsGlucose;
            obsGlucose = obsHemoglobin;
            obsHemoglobin = temp;
        }
        // Validate the note contents and references 
        assertThat(obsHemoglobin.hasNote()).isTrue();
        assertThat(obsHemoglobin.getNote()).hasSize(1);
        assertThat(obsHemoglobin.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST OBXa NOTE BB line 1  \nTEST NOTE BB line 2  \nTEST NOTE BB line 3");
        assertThat(obsHemoglobin.getNote().get(0).hasAuthorReference()).isTrue();
        String practitionerObsHemoglobinRefId = obsHemoglobin.getNote().get(0).getAuthorReference().getReference();

        assertThat(obsGlucose.hasNote()).isTrue();
        assertThat(obsGlucose.getNote()).hasSize(1);
        assertThat(obsGlucose.getNote().get(0).getTextElement().getValueAsString()).isEqualTo(
                "TEST OBXb NOTE CC line 1  \nTEST NOTE CC line 2  \n   \nTEST NOTE CC line 4"); // Test that blank lines are preserved.
        assertThat(obsGlucose.getNote().get(0).hasAuthorReference()).isFalse();

        // Two Practitioners, one for the serviceRequest, one for the GLYCOHEMOGLOBIN Observation
        List<Resource> practitioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitioners).hasSize(2);
        Practitioner practitionerServReq = ResourceUtils.getResourcePractitioner(practitioners.get(0),
                ResourceUtils.context);
        Practitioner practitionerObsHemoglobin = ResourceUtils.getResourcePractitioner(practitioners.get(1),
                ResourceUtils.context);
        // Adjust to correct practitioner if needed        
        if (!practitionerServReq.getIdentifierFirstRep().getValue().contentEquals("Pract1ID")) {
            Practitioner temp = practitionerObsHemoglobin;
            practitionerObsHemoglobin = practitionerServReq;
            practitionerServReq = temp;
        }
        // Check the values for the Practitioners and validate match to references.             
        assertThat(practitionerServReq.getIdentifier()).hasSize(1);
        assertThat(practitionerServReq.getIdentifierFirstRep().getValue()).isEqualTo("Pract1ID");
        assertThat(practitionerServReq.getName()).hasSize(1);
        assertThat(practitionerServReq.getNameFirstRep().getText()).isEqualTo("Pract1First Pract1Last");
        assertThat(practitionerServReq.getId()).isEqualTo(practitionerServReqRefId); // Check the cross-reference
        // Sanity check to confirm data corruption in meta content has not returned.
        CodeableConcept ccSourceEventTrigger = (CodeableConcept) practitionerServReq.getMeta()
                .getExtensionByUrl("http://ibm.com/fhir/cdm/StructureDefinition/source-event-trigger").getValue();
        assertThat(ccSourceEventTrigger.hasText()).isFalse();

        assertThat(practitionerObsHemoglobin.getIdentifier()).hasSize(1);
        assertThat(practitionerObsHemoglobin.getIdentifierFirstRep().getValue()).isEqualTo("Pract2ID");
        assertThat(practitionerObsHemoglobin.getName()).hasSize(1);
        assertThat(practitionerObsHemoglobin.getNameFirstRep().getText()).isEqualTo("Pract2First Pract2Last");
        assertThat(practitionerObsHemoglobin.getId()).isEqualTo(practitionerObsHemoglobinRefId); // Check the cross-reference  
    }

    // Test that multiple problems (PRB) each with multiple notes (NTE) are associated with the correct NTE / PRB 
    // and that there is no "bleed"
    @Test
    public void testNoteCreationMutiplePPR() throws IOException {
        // TODO: Add PC2 and PC3 tests in future            
        String message = "PPR^PC1";
        String hl7message = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message
                + "|1|P^I|2.6||||||ASCII||\n"
                + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\n"
                + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\n"
                + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n"
                + "NTE|1|O|TEST PRBa NOTE AA line 1|\n"
                + "NTE|2|O|TEST NOTE AA line 2|\n"
                + "NTE|3|O|TEST NOTE AA line 3|\n"
                + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F||||||||||||||\n"
                + "NTE|1|L|TEST OBXb NOTE BB line 1|\n"
                + "NTE|2|L|TEST NOTE BB line 2|\n"
                + "NTE|3|L|TEST NOTE BB line 3|\n"
                + "OBX|2|NM|17853^MEAN BLOOD GLUCOSE^LRR^^^^^^MEAN BLOOD GLUCOSE||114.02|mg/dL|||||F||||||||||||||\n"
                + "NTE|1|L|TEST OBXc NOTE CC line 1|\n"
                + "NTE|2|L|TEST NOTE CC line 2|\n"
                + "NTE|3|L|TEST NOTE CC line 3|\n"
                + "PRB|AD|200603150625|I47.2^Ventricular tachycardia^ICD-10-CM|53692||2||200603150625\n"
                + "NTE|1|O|TEST PRBd NOTE DD line 1|\n"
                + "NTE|2|O|TEST NOTE DD line 2|\n"
                + "NTE|3|O|TEST NOTE DD line 3|\n"
                + "OBX|1|NM|8595^BP Mean|1|88|MM HG|||||F|||20180520230000|||\n"
                + "NTE|1|L|TEST OBXe NOTE EE line 1|\n"
                + "NTE|2|L|TEST NOTE EE line 2|\n"
                + "NTE|3|L|TEST NOTE EE line 3|\n"
                + "OBX|2|NM|7302^Resp Rate|1|19||||||F|||20180520230000|||\n"
                + "NTE|1|L|TEST OBXf NOTE FF line 1|\n"; // Single NTE to ensure it is created correctly

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1);

        // Two Conditions from two PRBs. One has "... stenosis" and notes AA, One has "... Tachycardia" and notes DD
        List<Resource> conditions = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditions).hasSize(2);
        Condition condStenosis = ResourceUtils.getResourceCondition(conditions.get(0),
                ResourceUtils.context);
        Condition condTachy = ResourceUtils.getResourceCondition(conditions.get(1),
                ResourceUtils.context);
        // Figure out which is first and reassign if needed for testing        
        if (!condStenosis.getCode().getCodingFirstRep().getCode().contains("aortic stenosis")) {
            Condition temp = condStenosis;
            condStenosis = condTachy;
            condTachy = temp;
        }
        // Test the conditions have the correct NTE's associated and have correct content.
        // NOTE: the note contains an Annotation, which contains a MarkdownType that has the string.
        // Must use getTextElement().getValueAsString() to see untrimmed contents.
        assertThat(condStenosis.hasNote()).isTrue();
        assertThat(condStenosis.getNote()).hasSize(1);
        assertThat(condStenosis.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST PRBa NOTE AA line 1  \nTEST NOTE AA line 2  \nTEST NOTE AA line 3");
        assertThat(condTachy.hasNote()).isTrue();
        assertThat(condTachy.getNote()).hasSize(1);
        assertThat(condTachy.getNote().get(0).getTextElement().getValueAsString())
                .isEqualTo("TEST PRBd NOTE DD line 1  \nTEST NOTE DD line 2  \nTEST NOTE DD line 3");

        // Four observations.  Two associated with the first problem and two with the second
        // This map tells us what Annotation text is associated with an Observation code
        Map<String, String> matchObsCodeToNotes = new HashMap<>();
        matchObsCodeToNotes.put("17985",
                "TEST OBXb NOTE BB line 1  \nTEST NOTE BB line 2  \nTEST NOTE BB line 3");
        matchObsCodeToNotes.put("17853",
                "TEST OBXc NOTE CC line 1  \nTEST NOTE CC line 2  \nTEST NOTE CC line 3");
        matchObsCodeToNotes.put("8595",
                "TEST OBXe NOTE EE line 1  \nTEST NOTE EE line 2  \nTEST NOTE EE line 3");
        matchObsCodeToNotes.put("7302", "TEST OBXf NOTE FF line 1");

        // This map tells us what Parent should be associated with an Observation code
        Map<String, String> matchObsCodeToParent = new HashMap<>();
        matchObsCodeToParent.put("17985", "aortic stenosis");
        matchObsCodeToParent.put("17853", "aortic stenosis");
        matchObsCodeToParent.put("8595", "I47.2");
        matchObsCodeToParent.put("7302", "I47.2");

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(4);
        int observationsVerified = 0;
        // Dig through the Conditions, find the referenced Observations, find the matching Observation resource
        // and ensure it has the expected Notes and referenced parent.
        // For the list of Conditions
        for (int condIndex = 0; condIndex < conditions.size(); condIndex++) { // condIndex is index for condition
            // Get the list of Observation references
            Condition cond = ResourceUtils.getResourceCondition(conditions.get(condIndex), ResourceUtils.context);
            List<ConditionEvidenceComponent> evidences = cond.getEvidence();
            for (int evidenceIndex = 0; evidenceIndex < evidences.size(); evidenceIndex++) {
                // Get the evidence Observation reference
                String obsReferenceId = evidences.get(evidenceIndex).getDetailFirstRep().getReference();
                // Find the referenced observation
                for (int obsIndex = 0; obsIndex < observations.size(); obsIndex++) {
                    // If the Id's match
                    if (obsReferenceId.contains(observations.get(obsIndex).getId())) {
                        // Check the contents and the parent
                        Observation obs = ResourceUtils.getResourceObservation(observations.get(obsIndex),
                                ResourceUtils.context);
                        String code = obs.getCode().getCodingFirstRep().getCode().toString();
                        // The Annotation text should match the mapped text for this key
                        assertThat(obs.getNoteFirstRep().getText()).hasToString(matchObsCodeToNotes.get(code));
                        // The parent Condition code.coding.code should match the expected mapped code for this key
                        assertThat(cond.getCode().getCodingFirstRep().getCode())
                                .hasToString(matchObsCodeToParent.get(code));
                        observationsVerified++;
                        break;
                    }
                }
            }
        }
        // This confirms ALL of the observations were checked.
        assertThat(observationsVerified).isEqualTo(4);
    }

}
