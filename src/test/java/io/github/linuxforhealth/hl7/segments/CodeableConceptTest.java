/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class CodeableConceptTest {
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    private static final String V3_RACE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-Race";

    // These test cover all the paths to create codeableConcepts from CWEs.
    // 
    // Display value is calculated:
    //    1.  If code is in system-known-to-FHIR, use that display value.getValue
    //    2.  Otherwise, if code is not in system-known-to-FHIR, it's a bad code, put an error message in the display string
    //    3.  Otherwise if the system is known-to-us (in CodingSystemMapping ), then we can't tell if the code is valid or not, use the original display text

    // See FHIRExtensionsTest for Test with two races

    // Tests several nearly similar CWE > CodeableConcept patterns.  See comments near arguments.
    // This private method provide parameters to the the test
    private static Stream<Arguments> parmsTestSimilarCWEInputs() {
        return Stream.of(
                //  Arguments are: (PID.10, code, display, system, text)

                // With no system, we move the input code to the code field:
                // "code": {
                //     "coding": [ {
                //       "code": <code-from-input(invalid)>
                //     } ],
                //   },
                Arguments.of("W", "W", null, null, null),

                // With a good code in a registered (to FHIR) system, with orginal text, we should produce:
                // "code": {
                //     "coding": [ {
                //       "system": <known-to-FHIR-system>,
                //       "display": <display value looked up from code>;
                //       "code": <code-from-input(invalid)>
                //     } ],
                //     "text": <original text>;
                //   },
                // Input is special race with good code in known system but non-standard display text
                Arguments.of("2106-3^WHITE^CDCREC", "2106-3", "White", V3_RACE_SYSTEM, "WHITE"),

                // With a system that is unknown, but a code and a display, we should produce:
                // "code": {
                //     "coding": [ {
                //       "system": "urn:id:<system-as-given>",
                //       "display": <original text>;
                //       "code": <code-as-given>
                //     } ],
                //     "text": <original text>;
                //   },
                // Input is special race with unknown system
                Arguments.of("White^WHITE^L", "White", "WHITE", "urn:id:L", "WHITE"),

                // With a good code in a registered (to FHIR) system, with missing orginal text, we should produce:
                // "code": {
                //     "coding": [ {
                //       "system": <known-to-FHIR-system>,
                //       "display": <display value looked up from code>;
                //       "code": <code-from-input(invalid)>
                //     } ]
                //   },    << No TEXT because there is no CWE.2 or CWE.9
                // Input is race with good code in known system but no display text
                Arguments.of("2106-3^^CDCREC", "2106-3", "White", V3_RACE_SYSTEM, null));
    }

    @ParameterizedTest
    @MethodSource("parmsTestSimilarCWEInputs")
    void testSimilarCWEToCodeableConcepts(String cwe, String code, String display, String system, String text) {
        // See inputs and test explanation above
        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR||TestPatientLastName^Jane^|||||" + cwe + "\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        DatatypeUtils.checkCommonCodeableConceptAssertions(ccW, code, display, system, text);

    }

    @Test
    void testCodeableConceptWithBadCodeKnownSystem() {

        // With a bad code in a registered (to FHIR) system, and NO original display value, we should produce:
        // "code": {
        //     "coding": [ {
        //       "system": <known-to-FHIR-system>,
        //       "display": "Invalid input: code: <code> for system: <system> original display: <original-display-value>";
        //     } ],
        //   },

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Race with bad code, but known system
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2186-5^^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        assertThat(ccW.getCoding().size()).isEqualTo(1);
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isFalse();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).containsPattern("Invalid.*2186-5.*" + V3_RACE_SYSTEM);
        assertThat(coding.getSystem()).containsIgnoringCase(V3_RACE_SYSTEM);
    }

    @Test
    void testCodeableConceptWithBadCodeUnknownTextKnownSystem() {

        // With a bad code in a registered (to FHIR) system, we should produce:
        // "code": {
        //     "coding": [ {
        //       "system": <known-to-FHIR-system>,
        //       "display": "Invalid input: code: <code> for system: <system> original display: <original-display-value>";
        //     } ],
        //     "text": <original-display-value>
        //   },

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // This code does not exist in the CDCREC system, and will fail the lookup, resulting in an error message in display.
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2186-5^hispan^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        assertThat(ccW.getCoding().size()).isEqualTo(1);
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isFalse();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).containsPattern("Invalid.*2186-5.*" + V3_RACE_SYSTEM + ".*hispan");
        assertThat(coding.getSystem()).containsIgnoringCase(V3_RACE_SYSTEM);

    }

    @Test
    void testCodeableConceptForNDC() {

        // With a valid (to us) but unregistered (to FHIR) system, we should produce:
        // "code": {
        //     "coding": [ {
        //       "system": <known-to-us-system-via-our-lookup>,
        //       "display": <original-display-value>
        //       "code": <code-from-input>
        //     } ],
        //     "text": <original-display-value>
        //   },

        String medicationRequestWithCodeableConcept = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                // Medication request has unknown code in .2
                + "RXE|^^^20180622230000^^R|73056-017^Test15 SODIUM 100 MG CAPSULE^NDC|100||mg|||||10||5\n";

        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequestWithCodeableConcept);
        assertThat(medReq.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medReq.getMedicationCodeableConcept();
        DatatypeUtils.checkCommonCodeableConceptAssertions(medCC, "73056-017", "Test15 SODIUM 100 MG CAPSULE",
                "http://hl7.org/fhir/sid/ndc", "Test15 SODIUM 100 MG CAPSULE");

    }

    @Test
    void testCodeableConceptForLoincAltenativeI9WithVersions() {

        // With a valid (to us) but unregistered (to FHIR) system, and with a version, we should produce:
        // "code": {
        //     "coding": [ {
        //       "system": <known-to-us-system-via-our-lookup>,  << FIRST CODING
        //       "display": <original-display-value>
        //       "code": <code-from-input>
        //       "version": <version>        
        //     },
        //     {    
        //       "system": <known-to-us-system-via-our-lookup>,  << SECOND (ALTERNATE CODING)
        //       "display": <original-display-value>
        //       "code": <code-from-input>
        //       "version": <version>        
        //     } ],
        //     "text": <original-display-value>
        //   },

        String conditionWithVersionedAlternateCodeableConcept = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PRB|AD|2004062916460000|2148-5^CREATININE^LN^F-11380^CREATININE^I9^474747^22222||||20040629|||||||||20040629";

        Condition condition = ResourceUtils.getCondition(ftv, conditionWithVersionedAlternateCodeableConcept);

        assertThat(condition.hasCode()).isTrue();
        CodeableConcept condCC = condition.getCode();
        assertThat(condCC.hasText()).isTrue();
        assertThat(condCC.getText()).isEqualTo("CREATININE");
        assertThat(condCC.hasCoding()).isTrue();
        assertThat(condCC.getCoding().size()).isEqualTo(2);

        DatatypeUtils.checkCommonCodingAssertions(condCC.getCoding().get(0), "2148-5", "CREATININE", "http://loinc.org",
                "474747");
        DatatypeUtils.checkCommonCodingAssertions(condCC.getCoding().get(1), "F-11380", "CREATININE",
                "http://terminology.hl7.org/CodeSystem/icd9", "22222");
    }

    @Test
    void testCodeableConceptDoubleRaceWithVersionAndAlternate() {

        // This has both a known and an unknown system.
        // "valueCodeableConcept": {
        //     "coding": [ {
        //       "system": <known-to-FHIR>,  << FIRST CODING
        //       "display": <original-display-value>
        //       "code": <code-from-input>
        //       "version": <version>        
        //     },
        //     {    
        //       "system": <unknown, so made up "urn:id:L">,  << SECOND (ALTERNATE CODING)
        //       "display": <original-alternate display-value>
        //       "code": <altenate-code>
        //       "version": <alternate-version>        
        //     } ],
        //     "text": <original-display-value>
        //   },

        String patientWithDoubleRaceWithVersionAndAlternate = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test double race in the SAME CWE (not a second CWE) and versions.  Use made up Cauc to ensure test doesn't mix up whites.
                + "PID|1||12345678^^^^MR||TestPatientLastName^Jane|||||2106-3^White^CDCREC^CA^Caucasian^L^1.1^4|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithDoubleRaceWithVersionAndAlternate);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);

        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        assertThat(ccW.hasText()).isTrue();
        assertThat(ccW.getText()).hasToString("White");

        List<Coding> codings = ccW.getCoding();
        assertThat(codings.size()).isEqualTo(2);

        DatatypeUtils.checkCommonCodingAssertions(codings.get(0), "2106-3", "White", V3_RACE_SYSTEM, "1.1");
        DatatypeUtils.checkCommonCodingAssertions(codings.get(1), "CA", "Caucasian", "urn:id:L", "4");

    }

    @Test
    void checkICD10Coding() {

        String hl7MessageiCD10Coding = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        Condition condition = ResourceUtils.getCondition(ftv, hl7MessageiCD10Coding);
        assertThat(condition.hasCode()).isTrue();
        CodeableConcept condCC = condition.getCode();
        DatatypeUtils.checkCommonCodeableConceptAssertions(condCC, "K80.00", "Cholelithiasis",
                "http://hl7.org/fhir/sid/icd-10-cm", "Cholelithiasis");

    }

    @Test
    void testMultipleCWEsWIthSecondaryCodes() {
        // This test documents behaviors of how multiple CWEs with primary and secondary codings are GROUPED 
        // in the resulting CodeableConcept. 
        // The question is, when there is more than one CodeableConcept in the FHIR resources, should it 
        // be one codeable concept with an array of two code/systems and one text, like this:
        // "sampleCodeableConcept": [ {
        //     "coding": [ {
        //         "system": "http://system.org/AAAA-system",
        //         "code": "AAAA",
        //         "display": "Apples Add Additional Age"
        //      }, {
        //         "system": "http://system.org/BBBB-system",
        //         "code": "BBBB",
        //         "display": "Berries Become Beneficial"
        //      } ],
        //     "text": "Eat Fruits for Health"
        // } ]
        // OR should it be an array two codings each with one code/system and a text, like this:
        // "sampleCodeableConcept": [ {
        //     "coding": [ {
        //        "system": "http://system.org/AAAA-system",
        //        "code": "AAAA",
        //        "display": "Apples Add Additional Age"
        //     } ],
        //     "text": "Apples are Healthy"
        //     }, {
        //     "coding": [ {
        //        "system": "http://system.org/BBBB-system",
        //        "code": "BBBB",
        //        "display": "Berries Become Beneficial"
        //     } ],
        //     "text": "Berries are Healthy"
        //   } ]
        // Both of these are valid in FHIR.
        //
        // ANSWER: it currently depends on whether the multiple codings come in the HL7 in 
        // a repeating field, or in the secondary information of a CWE
        // 
        // Detailed discussion in https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues/363
        // 
        // This test uses a repeating field with two CWE's each with secondary sub-fields
        // to illustrate the way things currently work.

        String hl7message = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                //  ORC.16 key for this test.  It sets a reason code: two codes with two codings each.  See other comments.
                + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|SC||||||||||20170917151717|042^Human immunodeficiency virus [HIV] disease [42]^I9CDX^HIV^HIV/Aids^L~012^Other respiratory tuberculosis^I9CDX^017^Tuberculosis of other organs^I9CDX|||||||||||||||\n"
                //  NOTE: OBR.31 is omitted purposely so the ORC.16 is used for the reason code
                //  NOTE: OBR record required so a ServiceRequest is created
                + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||L|||||||||||||||||||||||||||||||||||||||\n";;
        // Expected output for reasonCode:
        // "reasonCode": [ {
        //     "coding": [ {
        //       "system": "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes",   << FROM FIRST CWE, PRIMARY CODE
        //       "code": "042",
        //       "display": "Human immunodeficiency virus [HIV] disease [42]"
        //     }, {
        //       "system": "urn:id:L", << FROM FIRST CWE, SECONDARY CODE
        //       "code": "HIV",
        //       "display": "HIV/Aids"
        //     } ],
        //     "text": "Human immunodeficiency virus [HIV] disease [42]"
        //   }, {
        //     "coding": [ {
        //       "system": "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes",  << FROM SECOND CWE, PRIMARY CODE
        //       "code": "012",
        //       "display": "Other respiratory tuberculosis"
        //     }, {
        //       "system": "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes",  << FROM SECOND CWE, SECONDARY CODE
        //       "code": "017",
        //       "display": "Tuberculosis of other organs"
        //     } ],
        //     "text": "Other respiratory tuberculosis"
        //   } ]

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv, hl7message);

        List<Resource> serviceRequestList = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
        assertThat(serviceRequestList).hasSize(1);
        ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0),
                ResourceUtils.context);

        // ORC.16 should create a ServiceRequest.reasonCode CWE
        assertThat(serviceRequest.hasReasonCode()).isTrue();
        assertThat(serviceRequest.getReasonCode()).hasSize(2); // There are two reason codes (an array of 2)
        assertThat(serviceRequest.getReasonCode().get(0).getCoding()).hasSize(2); // The first reason code has an array of 2 codings
        assertThat(serviceRequest.getReasonCode().get(1).getCoding()).hasSize(2); // The second reason code has an array of 2 codings
        assertThat(serviceRequest.getReasonCode().get(0).getText())
                .isEqualTo("Human immunodeficiency virus [HIV] disease [42]");
        assertThat(serviceRequest.getReasonCode().get(1).getText()).isEqualTo("Other respiratory tuberculosis");
        // Check the content of the coding sets to see they are as expected
        DatatypeUtils.checkCommonCodingAssertions(serviceRequest.getReasonCode().get(0).getCoding().get(0), "042",
                "Human immunodeficiency virus [HIV] disease [42]",
                "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes", null);
        DatatypeUtils.checkCommonCodingAssertions(serviceRequest.getReasonCode().get(0).getCoding().get(1), "HIV",
                "HIV/Aids", "urn:id:L", null);
        DatatypeUtils.checkCommonCodingAssertions(serviceRequest.getReasonCode().get(1).getCoding().get(0), "012",
                "Other respiratory tuberculosis", "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes", null);
        DatatypeUtils.checkCommonCodingAssertions(serviceRequest.getReasonCode().get(1).getCoding().get(1), "017",
                "Tuberculosis of other organs", "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes", null);

        // There should be one DiagnosticReport
        List<Resource> diagnosticReportList = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticReportList).hasSize(1);

        // There should be one Patient
        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1);

        // Ensure there are no extra resources created
        assertThat(e).hasSize(3);

    }

}
