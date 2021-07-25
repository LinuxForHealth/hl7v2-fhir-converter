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

public class FHIRExtensionsTest {

    @Test
    public void test_that_extension_has_mothers_maiden_name_and_religion() {

      String patientWithDataForExtensions =
          "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
              // Test for mother's maiden name and religion
              + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|MotherMaiden^Mickette|20060504080400|M|Alias^Alias|2028-9^Asian^HL70005~2106-3^White^HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|LUT^Christian: Lutheran^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";
      String patientWithNoExtensionData =
          "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
              // Test for missing mother's maiden name and unknown religion
              + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|^Mickette|20060504080400|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|ZZZ^No stated religion^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";

      Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
      assertThat(patient.hasExtension()).isTrue();
      Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
      assertThat(ext).isNotNull();
      assertThat(ext.getValue()).hasToString("MotherMaiden");
      ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
      assertThat(ext).isNotNull();
      CodeableConcept cc = (CodeableConcept) ext.getValue();
      assertThat(cc.getText()).hasToString("Lutheran");



    }



    @Test
    public void test_that_race_extension_is_added_for_single_rep() {

      String patientWithDataForExtensions =
          "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
              // Test for mother's maiden name and religion
              + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|MotherMaiden^Mickette|20060504080400|M|Alias^Alias|2028-9^Asian^HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|LUT^Christian: Lutheran^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";

      Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
      assertThat(patient.hasExtension()).isTrue();
      Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
      assertThat(ext).isNotNull();
      assertThat(ext.getValue()).hasToString("MotherMaiden");
      ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
      assertThat(ext).isNotNull();
      CodeableConcept cc = (CodeableConcept) ext.getValue();
      assertThat(cc.getText()).hasToString("Lutheran");

      // Look at the Race
      ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("race"));
      assertThat(ext).isNotNull();
      List<Extension> subExts = ext.getExtensionsByUrl("ombCategory");
      assertThat(subExts).isNotEmpty().hasSize(1);

      Coding coding = (Coding) subExts.get(0).getValue();
      assertThat(coding.getDisplay()).hasToString("Asian");
      assertThat(coding.getCode()).hasToString("2028-9");
      assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v3-Race");

      subExts = ext.getExtensionsByUrl("text");
      assertThat(subExts).isNotEmpty().hasSize(1);

      String text = (String) subExts.get(0).getValue().toString();
      assertThat(text).isEqualTo("Asian");



    }


    @Test
    public void test_that_race_extension_is_added_for_multiple_rep() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|MotherMaiden^Mickette|20060504080400|M|Alias^Alias|2028-9^Asian^HL70005~2106-3^White^HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|LUT^Christian: Lutheran^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();
        Extension ext = patient
            .getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
        assertThat(ext).isNotNull();
        assertThat(ext.getValue()).hasToString("MotherMaiden");
        ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.getText()).hasToString("Lutheran");

                // Look at the Race
        ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(ext).isNotNull();
        List<Extension> subExts = ext.getExtensionsByUrl("ombCategory");
        assertThat(subExts).isNotEmpty().hasSize(2);
        assertThat(subExts).extracting(e -> getDisplay(e)).contains("White", "Asian");

        Coding coding1 = (Coding) subExts.get(0).getValue();
        Coding coding2 = (Coding) subExts.get(1).getValue();
        assertThat(coding1.getDisplay()).isNotEqualToIgnoringCase(coding2.getDisplay());
        Coding asianCoding;
        Coding whiteCoding;
        if (coding1.getDisplay().equals("Asian")) {
          asianCoding = coding1;
          whiteCoding = coding2;
        } else {
          asianCoding = coding2;
          whiteCoding = coding1;
        }


        assertThat(asianCoding.getDisplay()).hasToString("Asian");
        assertThat(asianCoding.getCode()).hasToString("2028-9");
        assertThat(asianCoding.getSystem())
            .hasToString("http://terminology.hl7.org/CodeSystem/v3-Race");
        assertThat(whiteCoding.getDisplay()).hasToString("White");
        assertThat(whiteCoding.getCode()).hasToString("2106-3");
        assertThat(whiteCoding.getSystem())
            .hasToString("http://terminology.hl7.org/CodeSystem/v3-Race");




    }

    private String getDisplay(Extension e) {
      Coding c = (Coding) e.getValue();
      return c.getDisplay();
    }

}
