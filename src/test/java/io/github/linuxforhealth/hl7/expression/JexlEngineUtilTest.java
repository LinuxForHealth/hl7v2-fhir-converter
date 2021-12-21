/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.core.data.JexlEngineUtil;

class JexlEngineUtilTest {

    @Test
    void test() {
        JexlEngineUtil engine = new JexlEngineUtil();
        Map<String, Object> context = new HashMap<>();
        context.put("var1", "s");
        context.put("var2", "t");
        context.put("var3", "u");

        String value = (String) engine.evaluate("String.join(\" \",  var1,var2, var3)", context);
        assertThat(value).isEqualTo("s t u");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Blank / empty should throw an exception
            "",
            // Non-supported expression should throw an exception
            "System.currentTimeMillis()",
            // Multiple lines in the expression should throw exception
            "String.toString();System.exit(1); ",
            // An expression that is an incomplete statement should throw an exception.
            "String"
    })
    void testConditionsThatThrowExceptions(String evaluationString) {
        JexlEngineUtil wex = new JexlEngineUtil();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            wex.evaluate(evaluationString, new HashMap<>());
        });
    }

    @Test
    void valid_expression_returns_value() {
        JexlEngineUtil wex = new JexlEngineUtil();
        Object b = wex.evaluate("String.toString() ", new HashMap<>());
        assertThat(b).isEqualTo(String.class.toString());
    }

    @Test
    void valid_NumUtils_expression_returns_value() {
        JexlEngineUtil wex = new JexlEngineUtil();
        Object b = wex.evaluate("NumberUtils.createFloat(\"1.2\")", new HashMap<>());
        assertThat(b).isEqualTo(NumberUtils.createFloat("1.2"));
    }

}
