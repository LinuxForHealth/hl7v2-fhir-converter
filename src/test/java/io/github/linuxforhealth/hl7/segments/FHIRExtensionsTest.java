/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

public class FHIRExtensionsTest {

    @Test
    public void simple_extensions_test() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|MotherMaiden^Mickette|20060504080400|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|LUT^Christian: Lutheran^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";
        String patientWithNoExtensionData = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for missing mother's maiden name and unknown religion
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|^Mickette|20060504080400|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|ZZZ^No stated religion^|1234567_account|111-22-3333|DL00003333||2186-5^not Hispanic or Latino^CDCREC|Orlando Disney Hospital|Y|2|USA||||\n";

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

        patient = PatientUtils.createPatientFromHl7Segment(patientWithNoExtensionData);
        assertThat(patient.hasExtension()).isFalse();

    }

}
