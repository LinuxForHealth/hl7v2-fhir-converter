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
    void test_that_extension_has_mothers_maiden_name_religion_and_two_races() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion and two race variants
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|MotherMaiden^Mickette|20060504080400|M|Alias^Alias|2028-9^Asian^HL70005~2106-3^White^HL70005||USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|LUT^Christian: Lutheran^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC||Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();
        Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
        assertThat(ext).isNotNull();
        assertThat(ext.getValue()).hasToString("MotherMaiden");
        ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.getText()).hasToString("Lutheran");

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

    @Test
    void test_that_extension_handles_text_only_religion_correctly() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|||USAA|||english|married|Methodist|||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

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
    void test_that_extension_handles_text_only_race_correctly() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|W||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
    void test_that_extension_handles_alternate_CDCREC_race_encoding() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only race
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|2106-3^White^CDCREC||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
    void test_that_extension_handles_alternate_CDCREC_race_encoding_with_custom_text() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with good code in known system but non-standard display text
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|2106-3^WHITE^CDCREC||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
    void test_that_extension_handles_unknown_encoding() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with unknown system
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|White^WHITE^L||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
    void test_that_extension_with_valid_system_handles_missing_text_encoding() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with good code in known system but no display text
                + "PID|1||12345678|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|2106-3^^CDCREC||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
    void test_that_extension_with_invalid_code_and_valid_system_produces_failsafe_encoding() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Special race with bad code, but known system
                + "PID|1||12345678|ALTID|Mouse^Mickey^J^III^^^|||M|Alias^Alias|2186-5^^CDCREC||USAA|||english|married||||DL00003333|||Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
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
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getCode()).hasToString("2186-5");
        assertThat(coding.getSystem()).containsIgnoringCase("urn:id:CDCREC");

    }

}
