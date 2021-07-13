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
    String name = Hl7RelatedGeneralUtils.generateName("prefix", "first", "M", "family", "suffix");
    assertThat(name).isEqualTo("prefix first M family suffix");
    System.out.println(name);
  }


  @Test
  public void test_generate_name_prefix_suffix_missing() {
    String name = Hl7RelatedGeneralUtils.generateName(null, "first", "M", "family", null);
    assertThat(name).isEqualTo("first M family");
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
    System.out.println(diff);
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

  @Test
  public void getFormattedTelecomNumberValue() {  
    // Empty values return nothing
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("","","","","","")).isEmpty();
    // Everything empty except XTN1 returns XTN1.
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","","","")).isEqualTo("111");
    // Everything empty except XTN12 returns XTN12.
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("","","","","","112")).isEqualTo("112");
    // XTN12 takes priority over XTN1
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","","","112")).isEqualTo("112");
    // Country, Area, and Extension are ignored if there is no Local number
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("","22","333","","555","")).isEmpty();
    // XTN12 and XTN1 will be used if no local number
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","","555","")).isEqualTo("111");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("","22","333","","555","112")).isEqualTo("112");
    // Country, Area, and Extension are used if there is a Local number and they exist, and XTN1 and XTN12 are ignored  
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).isNotEmpty();
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).contains("+22");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).contains("333");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).contains("4444");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).contains("ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","555","112")).isEqualTo("+22 333 444 4444 ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","22","333","4444444","","112")).isEqualTo("+22 333 444 4444");  // Same rule without extension
    // Area, and Extension are used if there is a Local number, and XTN1 and XTN12 are ignored  
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","555","112")).isNotEmpty();
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","555","112")).contains("333");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","555","112")).contains("4444");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","555","112")).contains("ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","555","112")).isEqualTo("(333) 444 4444 ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","333","4444444","","112")).isEqualTo("(333) 444 4444");  // Same rule without extension
    // If local and country but no area, country is not prepended,only local is returned; XTN1 and XTN12 are ignored  
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","4444444","555","112")).isNotEmpty();
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","4444444","555","112")).contains("4444");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","4444444","555","112")).contains("ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","4444444","555","112")).isEqualTo("444 4444 ext. 555");
    assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111","","","4444444","","112")).isEqualTo("444 4444");  // Same rule without extension
  }

}