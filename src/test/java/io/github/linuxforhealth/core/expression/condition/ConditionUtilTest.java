/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConditionUtilTest {

    private static Stream<Arguments> parmsTestValueComparisionValueSimpleConditions() {
        return Stream.of(
                //  Arguments are: (condition, val1, val2, operator)

                // simple_equals_string_condition
                Arguments.of("$var1 EQUALS abc", "$var1", "abc", "EQUALS"),

                // simple_greaterthan_condition
                Arguments.of("$var1 GREATER_THAN 4", "$var1", "4", "GREATER_THAN"),

                // simpleQuotedCharactersParsing 
                Arguments.of("$var1 EQUALS ':'", "$var1", ":", "EQUALS"),

                // another simpleQuotedCharactersParsing
                Arguments.of("$var1 EQUALS '/'", "$var1", "/", "EQUALS"));
    }

    @ParameterizedTest
    @MethodSource("parmsTestValueComparisionValueSimpleConditions")
    void testValueComparisionValueSimpleConditions(String condition, String val1, String val2, String operator) {
        SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getVar1()).isEqualTo(val1);
        assertThat(simplecondition.getVar2()).isEqualTo(val2);
        assertThat(simplecondition.getConditionOperator()).isEqualTo(operator);
    }

    @Test
    void multiple_or_condition() {
        String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
        CompoundORCondition simplecondition = (CompoundORCondition) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getConditions()).hasSize(2);
    }

    @Test
    void multiple_and_condition() {
        String condition = "$var1 EQUALS abc && $var1 EQUALS xyz";
        CompoundAndCondition simplecondition = (CompoundAndCondition) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getConditions()).hasSize(2);
    }

    @Test
    void notnull_condition() {
        String condition = "$var1 NOT_NULL";
        CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getVar1()).isEqualTo("$var1");
    }

    @Test
    void null_condition() {
        String condition = "$var1 NULL";
        CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
        assertThat(simplecondition).isNotNull();
        assertThat(simplecondition.getVar1()).isEqualTo("$var1");
    }

}
