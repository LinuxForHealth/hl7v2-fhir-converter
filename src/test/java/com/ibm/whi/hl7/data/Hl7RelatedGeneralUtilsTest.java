/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.junit.Test;

public class Hl7RelatedGeneralUtilsTest {

  @Test
  public void test_generate_name() {
    String name = Hl7RelatedGeneralUtils.generateName("prefix", "given", "family", "suffix");
    assertThat(name).isEqualTo("prefix given family suffix");
  }


  @Test
  public void test_generate_name_prefix_suffix_missing() {
    String name = Hl7RelatedGeneralUtils.generateName(null, "given", "family", null);
    assertThat(name).isEqualTo("given family");
  }



  @Test
  public void test_get_encounter_var1() {
    // if var1 is not null then EncounterStatus.FINISHED
    String name = Hl7RelatedGeneralUtils.getEncounterStatus("var1", "var2", "var3");
    assertThat(name).isEqualTo(EncounterStatus.FINISHED.toCode());
  }


  @Test
  public void test_get_encounter_var2() {

    String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, "var2", "var3");
    assertThat(name).isEqualTo(EncounterStatus.ARRIVED.toCode());
  }


  @Test
  public void test_get_encounter_var3() {

    String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, null, "var3");
    assertThat(name).isEqualTo(EncounterStatus.CANCELLED.toCode());
  }

  @Test
  public void test_get_encounter_all_vars_null() {

    String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, null, null);
    assertThat(name).isEqualTo(EncounterStatus.UNKNOWN.toCode());
  }


  @Test
  public void test_date_diff_valid_values_same_dates() {

    long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:32:06.345+09:00",
        "2007-11-04T01:32:06.345+09:00");
    assertThat(diff).isEqualTo(0);
  }


  @Test
  public void test_date_diff_valid_values_1min_ahead() {

    long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:32:06.345+09:00",
        "2007-11-04T01:33:06.345+09:00");
    assertThat(diff).isEqualTo(1);
  }


  @Test
  public void test_date_diff_valid_values_no_min() {

    Long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-05",
        "2007-11-04T01:33:06.345+09:00");
    assertThat(diff).isNull();
  }


  @Test
  public void test_date_diff_null_values() {

    Long diff = Hl7RelatedGeneralUtils.diffDateMin(null, "2007-11-04T01:33:06.345+09:00");
    assertThat(diff).isNull();
  }


  @Test
  public void test_date_diff_incorrect_date_format() {

    Long diff =
        Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:33:06890",
            "2007-11-04T01:33:06.345+09:00");
    assertThat(diff).isNull();
  }


  @Test
  public void test_splitting_values() {

    String val = Hl7RelatedGeneralUtils.split("5.9-8.4", "-", 0);
    assertThat(val).isEqualTo("5.9");
  }

}
