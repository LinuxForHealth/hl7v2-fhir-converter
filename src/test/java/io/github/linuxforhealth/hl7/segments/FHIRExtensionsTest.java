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
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

class FHIRExtensionsTest {

    @Test
    void testExtensionMothersMaidenNameReligion() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion and two race variants
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|TestMaidenName^Jane||||2028-9^Asian^HL70005~2106-3^White^HL70005|||||||LUT^Christian: Lutheran^|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();
        Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
        assertThat(ext).isNotNull();
        assertThat(ext.getValue()).hasToString("TestMaidenName");
        ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.getText()).hasToString("Lutheran");
    }

    @Test
    void testTextOnlyReligion() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only religion
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName||||||||||||Methodist|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();

        Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.hasCoding()).isTrue();
        Coding coding = cc.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isFalse();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isFalse();
        assertThat(coding.getCode()).hasToString("Methodist");
    }

    @Test
    void testExtensionTwoRaces() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion and two race variants
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|TestMaidenName^Jane||||2028-9^Asian^HL70005~2106-3^White^HL70005|||||||LUT^Christian: Lutheran^|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(2);
        assertThat(extensions.get(0).hasValue()).isTrue();
        assertThat(extensions.get(1).hasValue()).isTrue();
        // check for Asian & White.  Guess at order, switch if incorrect
        CodeableConcept ccAsian = (CodeableConcept) extensions.get(0).getValue();
        CodeableConcept ccWhite = (CodeableConcept) extensions.get(1).getValue();
        assertThat(ccAsian.hasText()).isTrue();
        // Switch if guess was not right. (If not reversed, then something is wrong)
        if (!ccAsian.getText().equalsIgnoreCase("Asian")) {
            ccAsian = (CodeableConcept) extensions.get(1).getValue();
            ccWhite = (CodeableConcept) extensions.get(0).getValue();
        }
        assertThat(ccAsian.getText()).hasToString("Asian");
        assertThat(ccAsian.hasCoding()).isTrue();
        Coding coding = ccAsian.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("Asian");
        assertThat(coding.getCode()).hasToString("2028-9");
        assertThat(coding.getSystem()).containsIgnoringCase("terminology.hl7.org/CodeSystem/v3-Race");

        assertThat(ccWhite.getText()).hasToString("White");
        assertThat(ccWhite.hasCoding()).isTrue();
        coding = ccWhite.getCodingFirstRep();
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getDisplay()).hasToString("White");
        assertThat(coding.getCode()).hasToString("2106-3");
        assertThat(coding.getSystem()).containsIgnoringCase("terminology.hl7.org/CodeSystem/v3-Race");
    }

    // See CodeableConceptText.java for more tests on CodeableConcepts

}
