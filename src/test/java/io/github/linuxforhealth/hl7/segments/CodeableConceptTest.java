/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Condition;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.segments.util.*;

class CodeableConceptTest {

    // See FHIRExtensionsTest for Test with two races

    @Test
    void testCodeableConceptNoSystem() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||W\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isFalse();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isFalse();
        assertThat(coding.getCode()).hasToString("W");
    }

    @Test
    void testCodeableConceptWithCDCRECSystem() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2106-3^White^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        assertThat(ccW.hasText()).isTrue();
        assertThat(ccW.getText()).hasToString("White");
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("White");
        assertThat(coding.getCode()).hasToString("2106-3");
        assertThat(coding.getSystem()).containsIgnoringCase("terminology.hl7.org/CodeSystem/v3-Race");

    }

    @Test
    void testCodeableConceptWithCustomText() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with good code in known system but non-standard display text
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2106-3^WHITE^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("White");
        assertThat(coding.getCode()).hasToString("2106-3");
        assertThat(coding.getSystem()).containsIgnoringCase("terminology.hl7.org/CodeSystem/v3-Race");

    }

    @Test
    void testCodeableConceptWithUnknownEncoding() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with unknown system
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||White^WHITE^L\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("WHITE");
        assertThat(coding.getCode()).hasToString("White");
        assertThat(coding.getSystem()).containsIgnoringCase("urn:id:L");

    }

    @Test
    void testCodeableConceptWithMissingDisplayText() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Race with good code in known system but no display text
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2106-3^^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("White");
        assertThat(coding.getCode()).hasToString("2106-3");
        assertThat(coding.getSystem()).containsIgnoringCase("terminology.hl7.org/CodeSystem/v3-Race");

    }

    @Test
    void testCodeableConceptWithBadCodeKnownSystem() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Race with bad code, but known system
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2186-5^^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isFalse();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).containsPattern("Invalid.*2186-5.*CDCREC");
        assertThat(coding.getSystem()).containsIgnoringCase("http://terminology.hl7.org/CodeSystem/v3-Race");
    }

    @Test
    void testCodeableConceptWithBadCodeUnknownTextKnownSystem() {

        String patientWithCodeableConcept = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with bad code, known system, unknown display value
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2186-5^hispan^CDCREC\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithCodeableConcept);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(1);
        assertThat(extensions.get(0).hasValue()).isTrue();
        CodeableConcept ccW = (CodeableConcept) extensions.get(0).getValue();
        assertThat(ccW.hasCoding()).isTrue();
        Coding coding = ccW.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isFalse();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).containsPattern("Invalid.*2186-5.*CDCREC.*hispan");
        assertThat(coding.getSystem()).containsIgnoringCase("http://terminology.hl7.org/CodeSystem/v3-Race");

    }

    @Test
    void testCodeableConceptForNDC() {

        String medicationRequestWithCodeableConcept = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                // Medication request has unknown code in .2
                + "RXE|^^^20180622230000^^R|73056-017^Test15 SODIUM 100 MG CAPSULE^NDC|100||mg|||||10||5\n";

        MedicationRequest medReq = ResourceUtils.getMedicationRequest(medicationRequestWithCodeableConcept);

        assertThat(medReq.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medReq.getMedicationCodeableConcept();
        assertThat(medCC.hasText()).isTrue();
        assertThat(medCC.getText()).isEqualTo("Test15 SODIUM 100 MG CAPSULE");
        assertThat(medCC.hasCoding()).isTrue();

        Coding medCoding = medCC.getCoding().get(0);
        assertThat(medCoding.hasSystem()).isTrue();
        assertThat(medCoding.getSystem()).isEqualTo("http://hl7.org/fhir/sid/ndc");
        assertThat(medCoding.hasCode()).isTrue();
        assertThat(medCoding.getCode()).isEqualTo("73056-017");
        assertThat(medCoding.hasDisplay()).isFalse();

    }

    @Test
    public void testCodeableConceptForI9() {
        String conditionWithCodeableConcept = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PRB|AD|2004062916460000|596.5^BLADDER DYSFUNCTION^I9||||20040629||||||ACTIVE|||20040629";

        // With a valid (to us) but unregistered (to FHIR) system, we should produce:
        // "code": {
        //     "coding": [ {
        //       "system": <known-to-us-system-via-our-lookup>,
        //       "code": <code-from-input>
        //     } ],
        //     "text": <original-display-value>
        //   },

        Condition condition = ResourceUtils.getCondition(conditionWithCodeableConcept);

        assertThat(condition.hasCode()).isTrue();
        CodeableConcept condCC = condition.getCode();
        assertThat(condCC.hasText()).isTrue();
        assertThat(condCC.getText()).isEqualTo("BLADDER DYSFUNCTION");
        assertThat(condCC.hasCoding()).isTrue();

        Coding condCoding = condCC.getCoding().get(0);
        assertThat(condCoding.hasSystem()).isTrue();
        assertThat(condCoding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/icd9");
        assertThat(condCoding.hasCode()).isTrue();
        assertThat(condCoding.getCode()).isEqualTo("596.5");
        assertThat(condCoding.hasDisplay()).isFalse();
    }

}
