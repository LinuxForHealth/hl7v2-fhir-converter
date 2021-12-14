/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;

class CompoundORConditionTest {

    @Test
    void compound_condition_evaluated_to_true() {
        String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
        CompoundORCondition simplecondition = (CompoundORCondition) ConditionUtil.createCondition(condition);

        Map<String, EvaluationResult> contextVariables = new HashMap<>();
        contextVariables.put("var1", new SimpleEvaluationResult<String>("abc"));
        assertThat(simplecondition.test(contextVariables)).isTrue();
    }

    @Test
    void compound_condition_evaluated_to_false() {
        String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
        CompoundORCondition simplecondition = (CompoundORCondition) ConditionUtil.createCondition(condition);

        Map<String, EvaluationResult> contextVariables = new HashMap<>();
        contextVariables.put("var1", new SimpleEvaluationResult<String>("tuv"));
        assertThat(simplecondition.test(contextVariables)).isFalse();
    }

}
