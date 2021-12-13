/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7RelatedGeneralUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7RelatedGeneralUtilsTest.class);

    @Test
    public void testConcatenateWithChar() {

        ArrayList<Object> objects = new ArrayList<Object>();
        String st = "AA";
        objects.add(st);
        st = "BB";
        objects.add(st);

        String concatted = Hl7RelatedGeneralUtils.concatenateWithChar(objects, "  \n");
        assertThat(concatted).isEqualTo("AA  \nBB");

        // Simulate the input from YAML
        // YAML doesn't reduce the '\n' to linefeed, but inputs 92 78, a literal backslash n
        // For this test, we force the string to contain 32 32 92 78 by using a double backslash
        concatted = Hl7RelatedGeneralUtils.concatenateWithChar(objects, "  \\n");
        assertThat(concatted).isEqualTo("AA  \nBB");

        // Simulate the input from YAML
        // For this test, we force the string to contain 32 32 92 92 78 by using a two double backslashes
        // We want, in this case to assure that a double backslash input will be taken literally
        concatted = Hl7RelatedGeneralUtils.concatenateWithChar(objects, "  \\\\n");
        assertThat(concatted).isEqualTo("AA  \\\\nBB");

        // Simulate the input from YAML
        // For this test, we force the string to contain 32 92 78 32 92 78 32 by using double backslash
        // This tests that both intended linefeeds in input " \n \n " are handled.
        concatted = Hl7RelatedGeneralUtils.concatenateWithChar(objects, " \\n \\n ");
        assertThat(concatted).isEqualTo("AA \n \n BB");
    }

    @Test
    public void test_generate_name() {
        String name = Hl7RelatedGeneralUtils.generateName("prefix", "first", "M", "family", "suffix");
        assertThat(name).isEqualTo("prefix first M family suffix");
        LOGGER.debug("name=" + name);
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
        assertThat(diff).isZero();
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
                "2007-11-04T01:33:06.345+20:00");
        assertThat(diff).isNull();
    }

    @Test
    public void test_date_diff_null_values() {

        Long diff = Hl7RelatedGeneralUtils.diffDateMin(null, "2007-11-04T01:33:06.345+09:00");
        assertThat(diff).isNull();
    }

    @Test
    public void test_date_diff_incorrect_date_format() {

        Long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:33:06890",
                "2007-11-04T01:33:06.345+09:00");
        assertThat(diff).isNull();
    }

    @Test
    public void test_splitting_values() {

        String val = Hl7RelatedGeneralUtils.split("5.9-8.4", "-", 0);
        assertThat(val).isEqualTo("5.9");
    }

    // ExtractHigh and ExtractLow assumes if there are two or more values, the first is low, the second is high,
    // but if there is only one value, it is the high
    @Test
    public void testExtractHigh() {

        String aString = "27.0-55.2";
        String resultValue = Hl7RelatedGeneralUtils.extractHigh(aString);
        assertThat(resultValue).isEqualTo("55.2");

        aString = "47.47";
        resultValue = Hl7RelatedGeneralUtils.extractHigh(aString);
        assertThat(resultValue).isEqualTo("47.47");

        aString = "<0.50 IU/mL";
        resultValue = Hl7RelatedGeneralUtils.extractHigh(aString);
        assertThat(resultValue).isEqualTo("0.50");

        aString = "something111another222more333done";
        resultValue = Hl7RelatedGeneralUtils.extractHigh(aString);
        assertThat(resultValue).isEqualTo("222");

        aString = "Normal";
        resultValue = Hl7RelatedGeneralUtils.extractHigh(aString);
        assertThat(resultValue).isNull();

    }

    // ExtractHigh and ExtractLow assumes if there are two or more numbers (with or without decimal points), 
    // the first is low, the second is high, but if there is only one value, it is the high
    // See extensive notes near the methods. 
    @Test
    public void testExtractLow() {

        String aString = "27.0-55.2";
        String resultValue = Hl7RelatedGeneralUtils.extractLow(aString);
        assertThat(resultValue).isEqualTo("27.0");

        aString = "47.47";
        resultValue = Hl7RelatedGeneralUtils.extractLow(aString);
        assertThat(resultValue).isNull();

        aString = "<0.50 IU/mL";
        resultValue = Hl7RelatedGeneralUtils.extractLow(aString);
        assertThat(resultValue).isNull();

        aString = "something111another222more333done";
        resultValue = Hl7RelatedGeneralUtils.extractLow(aString);
        assertThat(resultValue).isEqualTo("111");

        aString = "Normal";
        resultValue = Hl7RelatedGeneralUtils.extractLow(aString);
        assertThat(resultValue).isNull();

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
        assertThat(stringArray.size()).isZero();
    }

    @Test
    public void test_getAddressUse() {
        String ANYTHING = "anything";
        // Inputs are XAD.7 Type, XAD.16 Temp Indicator, XAD.17 Bad address indicator
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", "", ANYTHING)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", "", null)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", null, ANYTHING)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", null, null)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, "Y", ANYTHING)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, "Y", null)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, "Y", ANYTHING)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, "Y", null)).isEqualTo("temp");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", ANYTHING, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("C", ANYTHING, null)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", ANYTHING, "")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", ANYTHING, null)).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", null, "")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", null, null)).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, ANYTHING, "Y")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, ANYTHING, "Y")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, null, "Y")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, null, "Y")).isEqualTo("old");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", ANYTHING, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BA", null, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("H", ANYTHING, ANYTHING)).isEqualTo("home");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("H", null, ANYTHING)).isEqualTo("home");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("H", ANYTHING, null)).isEqualTo("home");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("H", null, null)).isEqualTo("home");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("B", ANYTHING, ANYTHING)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("B", ANYTHING, null)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("B", null, ANYTHING)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("B", null, null)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("O", ANYTHING, ANYTHING)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("O", ANYTHING, null)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("O", null, ANYTHING)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("O", null, null)).isEqualTo("work");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BI", ANYTHING, ANYTHING)).isEqualTo("billing");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BI", null, ANYTHING)).isEqualTo("billing");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BI", ANYTHING, null)).isEqualTo("billing");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse("BI", null, null)).isEqualTo("billing");
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, ANYTHING, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, ANYTHING, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, null, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(ANYTHING, ANYTHING, null)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressUse(null, null, null)).isEmpty();

    }

    @Test
    public void test_getAddressType() {
        String ANYTHING = "anything";
        // Inputs are XAD.7 Type, XAD.18 Type 
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "M")).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType(null, "M")).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("M", "")).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("M", null)).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("M", ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "V")).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType(null, "V")).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("SH", "")).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("SH", null)).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("SH", ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, null)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(null, ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(null, null)).isEmpty();

    }

    @Test
    public void test_getAddressDistrict() {
        String ANYTHING = "anything";

        // Inputs are XAD.7 Type, XAD.18 Type 
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "M")).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("M", "")).isEqualTo("postal");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("M", ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, "V")).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("SH", "")).isEqualTo("physical");
        assertThat(Hl7RelatedGeneralUtils.getAddressType("SH", ANYTHING)).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.getAddressType(ANYTHING, ANYTHING)).isEmpty();
    }
    // Note: Utility  Hl7RelatedGeneralUtils.getAddressDistrict is more effectively tested as part of Patient Address testing

    @Test
    public void getFormattedTelecomNumberValue() {
        // Empty values return nothing
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("", "", "", "", "", "")).isEmpty();
        // Everything empty except XTN1 returns XTN1.
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "", "", "")).isEqualTo("111");
        // Everything empty except XTN12 returns XTN12.
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("", "", "", "", "", "112")).isEqualTo("112");
        // XTN12 takes priority over XTN1
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "", "", "112"))
                .isEqualTo("112");
        // Country, Area, and Extension are ignored if there is no Local number
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("", "22", "333", "", "555", "")).isEmpty();
        // XTN12 and XTN1 will be used if no local number
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "", "555", ""))
                .isEqualTo("111");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("", "22", "333", "", "555", "112"))
                .isEqualTo("112");
        // Country, Area, and Extension are used if there is a Local number and they exist, and XTN1 and XTN12 are ignored  
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .isNotEmpty();
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .contains("+22");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .contains("333");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .contains("4444");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .contains("ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "555", "112"))
                .isEqualTo("+22 333 444 4444 ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "22", "333", "4444444", "", "112"))
                .isEqualTo("+22 333 444 4444"); // Same rule without extension
        // Area, and Extension are used if there is a Local number, and XTN1 and XTN12 are ignored  
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "555", "112"))
                .isNotEmpty();
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "555", "112"))
                .contains("333");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "555", "112"))
                .contains("4444");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "555", "112"))
                .contains("ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "555", "112"))
                .isEqualTo("(333) 444 4444 ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "333", "4444444", "", "112"))
                .isEqualTo("(333) 444 4444"); // Same rule without extension
        // If local and country but no area, country is not prepended,only local is returned; XTN1 and XTN12 are ignored  
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "4444444", "555", "112"))
                .isNotEmpty();
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "4444444", "555", "112"))
                .contains("4444");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "4444444", "555", "112"))
                .contains("ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "4444444", "555", "112"))
                .isEqualTo("444 4444 ext. 555");
        assertThat(Hl7RelatedGeneralUtils.getFormattedTelecomNumberValue("111", "", "", "4444444", "", "112"))
                .isEqualTo("444 4444"); // Same rule without extension
    }

    @Test
    public void testGetFormatAsId() {

        // Inputs are any string
        assertThat(Hl7RelatedGeneralUtils.formatAsId("Mayo Clinic")).isEqualTo("mayo.clinic");
        assertThat(Hl7RelatedGeneralUtils.formatAsId("OMC")).isEqualTo("omc");
        assertThat(Hl7RelatedGeneralUtils.formatAsId("   4 5 6  ")).isEqualTo("4.5.6");

        // Edge cases (if these occur we might have name space collisions)
        // The input is trimmed so totally blank input becomes empty
        assertThat(Hl7RelatedGeneralUtils.formatAsId(" ")).isEmpty();
        assertThat(Hl7RelatedGeneralUtils.formatAsId("")).isEmpty();
        // Null in becomes null out
        assertThat(Hl7RelatedGeneralUtils.formatAsId(null)).isNull();

    }

}