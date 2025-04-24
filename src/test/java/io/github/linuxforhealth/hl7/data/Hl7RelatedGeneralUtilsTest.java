/*
 * (C) Copyright IBM Corp. 2020, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_VISIT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import io.github.linuxforhealth.hl7.data.date.DateUtil;

class Hl7RelatedGeneralUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7RelatedGeneralUtilsTest.class);

    @Test
    void testConcatenateWithChar() {

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
    void test_generate_name() {
        String name = Hl7RelatedGeneralUtils.generateName("prefix", "first", "M", "family", "suffix");
        assertThat(name).isEqualTo("prefix first M family suffix");
        LOGGER.debug("name=" + name);
    }

    @Test
    void test_generate_name_prefix_suffix_missing() {
        String name = Hl7RelatedGeneralUtils.generateName(null, "first", "M", "family", null);
        assertThat(name).isEqualTo("first M family");
    }

    @Test
    void test_get_encounter_var1() {
        // if var1 is not null then EncounterStatus.FINISHED
        String name = Hl7RelatedGeneralUtils.getEncounterStatus("var1", "var2", "var3");
        assertThat(name).isEqualTo(EncounterStatus.FINISHED.toCode());
    }

    @Test
    void test_get_encounter_var2() {

        String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, "var2", "var3");
        assertThat(name).isEqualTo(EncounterStatus.ARRIVED.toCode());
    }

    @Test
    void test_get_encounter_var3() {

        String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, null, "var3");
        assertThat(name).isEqualTo(EncounterStatus.CANCELLED.toCode());
    }

    @Test
    void test_get_encounter_all_vars_null() {

        String name = Hl7RelatedGeneralUtils.getEncounterStatus(null, null, null);
        assertThat(name).isEqualTo(EncounterStatus.UNKNOWN.toCode());
    }

    @Test
    void test_date_diff_valid_values_same_dates() {

        long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:32:06.345+09:00",
                "2007-11-04T01:32:06.345+09:00");
        assertThat(diff).isZero();
    }

    @Test
    void test_date_diff_valid_values_1min_ahead() {

        long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:32:06.345+09:00",
                "2007-11-04T01:33:06.345+09:00");
        assertThat(diff).isEqualTo(1);
    }

    @Test
    void test_date_diff_valid_values_no_min() {

        Long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-05",
                "2007-11-04T01:33:06.345+20:00");
        assertThat(diff).isNull();
    }

    @Test
    void test_date_diff_null_values() {

        Long diff = Hl7RelatedGeneralUtils.diffDateMin(null, "2007-11-04T01:33:06.345+09:00");
        assertThat(diff).isNull();
    }

    @Test
    void test_date_diff_incorrect_date_format() {

        Long diff = Hl7RelatedGeneralUtils.diffDateMin("2007-11-04T01:33:06890",
                "2007-11-04T01:33:06.345+09:00");
        assertThat(diff).isNull();
    }

    @Test
    void test_splitting_values() {

        String val = Hl7RelatedGeneralUtils.split("5.9-8.4", "-", 0);
        assertThat(val).isEqualTo("5.9");
    }

    // ExtractHigh and ExtractLow assumes if there are two or more values, the first is low, the second is high,
    // but if there is only one value, it is the high
    @Test
    void testExtractHigh() {

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
    void testExtractLow() {

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
    void test_makeStringArray() {
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
    void test_getAddressUse1() {
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
        // More tests of getAddressUse in test_getAddressUse2
    }

    @Test
    void test_getAddressUse2() {
        String ANYTHING = "anything";
        // Inputs are XAD.7 Type, XAD.16 Temp Indicator, XAD.17 Bad address indicator
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
    void test_getAddressType() {
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
    void test_getAddressDistrict() {
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
    void getFormattedTelecomNumberValue() {
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
    void getPV1DurationLength() throws DataTypeException {

        ArrayList<String> timeZoneIds = Lists.newArrayList("", "+03:00", "Europe/Paris");

        for (String timeZoneId : timeZoneIds) {
            // Get a PV1
            ORU_R01 message = new ORU_R01();
            ORU_R01_PATIENT_RESULT patientResult = message.getPATIENT_RESULT();
            ORU_R01_PATIENT patient = patientResult.getPATIENT();
            ORU_R01_VISIT visit = patient.getVISIT();
            PV1 pv1 = visit.getPV1();

            // Admit and Discharge are not yet set; they are still empty
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength(pv1, timeZoneId)).isNull();

            // Admit set, but Discharge not yet set
            pv1.getAdmitDateTime().setValue("20161013154626");
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength(pv1, timeZoneId)).isNull();

            // Admit and Discharge set to valid values
            pv1.getAdmitDateTime().setValue("20161013154626");
            pv1.getDischargeDateTime().setValue("20161013164626");
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength(pv1, timeZoneId)).isEqualTo("60");

            // Admit and Discharge set to valid values less that one minute apart
            pv1.getAdmitDateTime().setValue("20161013154626");
            pv1.getDischargeDateTime().setValue("20161013154628");
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength(pv1, timeZoneId)).isEqualTo("0");

            // Admit and Discharge set to insufficient detail values (have no minutes) return null
            pv1.getAdmitDateTime().setValue("20161013");
            pv1.getDischargeDateTime().setValue("20161013");
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength(pv1, timeZoneId)).isNull();

            // Other input types, such as a string, are not valid and null is returned
            assertThat(Hl7RelatedGeneralUtils.pv1DurationLength("A string", timeZoneId)).isNull();
        }

    }

    @Test
    void get_datetime_value_valid() {
        String gen = "20110613122406";
        assertThat(Hl7RelatedGeneralUtils.dateTimeWithZoneId(gen,"")).isNotNull();
        assertThat(Hl7RelatedGeneralUtils.dateTimeWithZoneId(gen,"")).isEqualTo(DateUtil.formatToDateTimeWithZone(gen,""));

        // Test DateTime adjusts for milliseconds
        gen = "20110613122406.637";
        assertThat(Hl7RelatedGeneralUtils.dateTimeWithZoneId(gen,"")).isNotNull();
        assertThat(Hl7RelatedGeneralUtils.dateTimeWithZoneId(gen,"")).isEqualTo(DateUtil.formatToDateTimeWithZone(gen,""));
    }

    @Test
    void get_datetime_value_null() {
        assertThat(Hl7RelatedGeneralUtils.dateTimeWithZoneId(null,null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "UUID", "OID", ""})
    void getResourceIdWithType(String type) {
        String resourceId = Hl7RelatedGeneralUtils.generateResourceId(type);
        System.out.println("ResourceId: " + resourceId);
        assertThat(resourceId).isNotNull();
        if(type.equals("UUID")) {
            assertThat(resourceId).startsWith("urn:uuid:");
        } else if (type.equals("OID")) {
            assertThat(resourceId).startsWith("urn:oid:");
        } else {
            assertThat(resourceId).contains(".");
        }
    }
}
