/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.message.HL7FHIRResourceTemplate;
import io.github.linuxforhealth.hl7.message.HL7FHIRResourceTemplateAttributes;
import io.github.linuxforhealth.hl7.message.HL7MessageModel;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.segments.util.AllergyUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7AllergyFHIRConversionTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();


  @Test
  public void test_allergy_single() {
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|^PENICILLIN|MI|PRODUCES HIVES~RASH|MI\r" //
            + "AL1|2|AA|^CAT DANDER|SV";

    AllergyIntolerance allergy =  AllergyUtils.createAllergyFromHl7Segment(hl7message);
    assertThat(allergy.getCriticality().toCode()).isEqualTo("low");
    assertThat(allergy.getCategory().get(0).getCode()).isEqualTo("medication");
    assertThat(allergy.getCode().getText()).isEqualTo("PENICILLIN");
    assertThat(allergy.getReaction().get(0).getManifestation()).extracting(m -> m.getText())
            .containsExactlyInAnyOrder("PRODUCES HIVES", "RASH");


  }

  @Test
  public void test_allergy_no_severity(){
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r";

    AllergyIntolerance resource =  AllergyUtils.createAllergyFromHl7Segment(hl7message);
    assertThat(resource.hasCriticality()).isFalse();

  }
}
