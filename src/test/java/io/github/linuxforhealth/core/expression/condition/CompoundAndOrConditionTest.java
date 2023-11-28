package io.github.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;

public class CompoundAndOrConditionTest {
    @Test
    void compound_condition_1() {
        String condition = "$var1 EQUALS abc || $var2 EQUALS xyz && $var1 NOT_NULL";
        CompoundAndOrCondition simplecondition = (CompoundAndOrCondition) ConditionUtil.createCondition(condition);

        Map<String, EvaluationResult> contextVariables = new HashMap<>();
        contextVariables.put("var1", new SimpleEvaluationResult<>("abc"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("zyx"));
        assertThat(simplecondition.test(contextVariables)).isTrue();

        contextVariables.put("var1", new SimpleEvaluationResult<>("cba"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("xyz"));
        assertThat(simplecondition.test(contextVariables)).isTrue();

        contextVariables.put("var1", new SimpleEvaluationResult<>("null"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("xyz"));
        assertThat(simplecondition.test(contextVariables)).isFalse();
    }

    @Test
    void compound_condition_2() {
        String condition = "($var1 EQUALS abc || $var2 EQUALS xyz) && $var1 NOT_NULL";
        CompoundAndOrCondition simplecondition = (CompoundAndOrCondition) ConditionUtil.createCondition(condition);

        Map<String, EvaluationResult> contextVariables = new HashMap<>();
        contextVariables.put("var1", new SimpleEvaluationResult<>("abc"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("xyz"));
        assertThat(simplecondition.test(contextVariables)).isTrue();

        contextVariables.put("var1", new SimpleEvaluationResult<>("null"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("xyz"));
        assertThat(simplecondition.test(contextVariables)).isFalse();

        contextVariables.put("var1", new SimpleEvaluationResult<>("123"));
        contextVariables.put("var2", new SimpleEvaluationResult<>("321"));
        assertThat(simplecondition.test(contextVariables)).isFalse();
    }
}
