/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public class DatatypeUtils {

    // Common check for values of a codeable concept.  Null in any input indicates it should check False
    // Assumes 1 coding and only checks the first one.
    public static void checkCommonCodeableConceptAssertions(CodeableConcept cc, String code, String display,
            String system, String text) {
        if (text == null) {
            assertThat(cc.hasText()).isFalse();
        } else {
            assertThat(cc.hasText()).isTrue();
            assertThat(cc.getText()).isEqualTo(text);
        }

        if (code == null && display == null && system == null) {
            assertThat(cc.hasCoding()).isFalse();
        } else {
            assertThat(cc.hasCoding()).isTrue();
            assertThat(cc.getCoding().size()).isEqualTo(1);
            Coding coding = cc.getCoding().get(0);
            checkCommonCodingAssertions(coding, code, display, system, null);
        }
    }

    // Checks a single coding element. Null in any input indicates it should check False
    public static void checkCommonCodingAssertions(Coding coding, String code, String display,
            String system, String version) {
        assertThat(coding).isNotNull();
        // assertThat(c).isGreaterThan(index-1);

        if (code == null) {
            assertThat(coding.hasCode()).isFalse();
        } else {
            assertThat(coding.hasCode()).isTrue();
            assertThat(coding.getCode()).isEqualTo(code);
        }
        if (display == null) {
            assertThat(coding.hasDisplay()).isFalse();
        } else {
            assertThat(coding.hasDisplay()).isTrue();
            assertThat(coding.getDisplay()).isEqualTo(display);
        }
        if (system == null) {
            assertThat(coding.hasSystem()).isFalse();
        } else {
            assertThat(coding.hasSystem()).isTrue();
            assertThat(coding.getSystem()).isEqualTo(system);
        }
        if (version == null) {
            assertThat(coding.hasVersion()).isFalse();
        } else {
            assertThat(coding.hasVersion()).isTrue();
            assertThat(coding.getVersion()).isEqualTo(version);
        }
        
    }
}
