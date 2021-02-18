/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.core.terminology;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
public class TerminologyLookupTest {

  @Test
  public void test() {
    SimpleCode code =
        TerminologyLookup.lookup("v2-0396", "ICD10GM2012");
    assertThat(code).isNotNull();
    assertThat(code.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0396");
    assertThat(code.getCode()).isEqualTo("ICD10GM2012");
    assertThat(code.getDisplay()).isEqualTo("ICD 10 Germany v2012");
  }



}
