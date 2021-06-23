/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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


  @Test
  public void test_makeStringArray() {
    // Test for 2
    List<String> stringArray = Hl7RelatedGeneralUtils.makeStringArray("banana", "peach");
    assertThat(stringArray.size()).isEqualTo(2);
    assertThat(stringArray.get(0)).isEqualTo("banana");
    // Test for 3
    stringArray = Hl7RelatedGeneralUtils.makeStringArray("apple", "banana", "peach");
    assertThat(stringArray.size()).isEqualTo(3);
    assertThat(stringArray.get(0)).isEqualTo("apple");
    // Test for 0; expect an empty array
    stringArray = Hl7RelatedGeneralUtils.makeStringArray();
    assertThat(stringArray.size()).isEqualTo(0);
  }

  @Test
  public void test_getAddressUse() {
    String ANYTHING = "anything";
    // Inputs are XAD.7 Type, XAD.16 Temp Indicator, XAD.17 Bad address indicator
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("C","", ANYTHING)).isEqualTo("temp");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING,"Y", ANYTHING)).isEqualTo("temp");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("C",ANYTHING, ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA",ANYTHING, "")).isEqualTo("old");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING,ANYTHING, "Y")).isEqualTo("old");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA",ANYTHING, ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("H",ANYTHING, ANYTHING)).isEqualTo("home");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("B",ANYTHING, ANYTHING)).isEqualTo("work");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("O",ANYTHING, ANYTHING)).isEqualTo("work");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse("BI",ANYTHING, ANYTHING)).isEqualTo("billing");
    assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING,ANYTHING, ANYTHING)).isEqualTo("");

  }

  @Test
  public void test_getAddressType() {
    String ANYTHING = "anything";
    // Inputs are XAD.7 Type, XAD.18 Type 
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "M")).isEqualTo("postal");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("M","")).isEqualTo("postal");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("M",ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "V")).isEqualTo("physical");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("SH","")).isEqualTo("physical");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("SH",ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING,ANYTHING)).isEqualTo("");
  }

  @Test
  public void test_getAddressDistrict() {
    String ANYTHING = "anything";
    
    // Inputs are XAD.7 Type, XAD.18 Type 
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "M")).isEqualTo("postal");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("M","")).isEqualTo("postal");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("M",ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "V")).isEqualTo("physical");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("SH","")).isEqualTo("physical");
    assertThat(Hl7RelatedGeneralUtils.getAddressType("SH",ANYTHING)).isEqualTo("");
    assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING,ANYTHING)).isEqualTo("");
  }
  // Note: Utility  Hl7RelatedGeneralUtils.getAddressDistrict is more effectively tested as part of Patient Address testing

}

