/*
 * (C) Copyright IBM Corp. 2020
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
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;
import io.github.linuxforhealth.core.expression.condition.SimpleBiCondition;

public class SimpleBiConditionTest {

  // Test EQUALS exhaustively

  @Test
  public void simple_EQUALS_condition_is_evaluated_true() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_EQUALS_condition_with_two_variables_is_evaluated_true() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
public void simple_EQUALS_condition_with_two_variables_is_evaluated_false_when_condition_is_not_satisfied() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("xyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  @Test
  public void simple_EQUALS_condition_is_evaluated_false() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcdf"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  @Test
  public void simple_EQUALS_condition_is_evaluated_false_when_var_not_found() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // NOT_EQUALS tested exhaustively

  @Test
  public void simple_NOT_EQUALS_condition_is_evaluated_true() {
    String condition = "$var1 NOT_EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcd"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_NOT_EQUALS_condition_with_two_variables_is_evaluated_true() {
    String condition = "$var1 NOT_EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("abcd"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
public void simple_NOT_EQUALS_condition_with_two_variables_is_evaluated_false_when_condition_is_not_satisfied() {
    String condition = "$var1 NOT_EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("abc"));  // Because the same, fails NOT_EQUALS
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  @Test
  public void simple_NOT_EQUALS_condition_is_evaluated_false() {
    String condition = "$var1 NOT_EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc")); // Because the same, NOT_EQUALS false
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  @Test
  public void simple_NOT_EQUALS_condition_is_evaluated_false_when_var_not_found() {
    String condition = "$var1 NOT_EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // CONTAINS test one true and one false

  @Test
  public void simple_CONTAINS_condition_is_evaluated_true() {
    String condition = "$var1 CONTAINS bcd";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcde"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_CONTAINS_condition_is_evaluated_false() {
    String condition = "$var1 CONTAINS def";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcde"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // NOT_CONTAINS test one true and one false

  @Test
  public void simple_NOT_CONTAINS_condition_is_evaluated_true() {
    String condition = "$var1 NOT_CONTAINS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("defgh"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_NOT_CONTAINS_condition_is_evaluated_false() {
    String condition = "$var1 NOT_CONTAINS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcdef"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // ENDS_WITH test one true and one false

  @Test
  public void simple_ENDS_WITH_condition_is_evaluated_true() {
    String condition = "$var1 ENDS_WITH xyz";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_ENDS_WITH_condition_is_evaluated_false() {
    String condition = "$var1 ENDS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // NOT_ENDS_WITH test one true and one false

  @Test
  public void simple_NOT_ENDS_WITH_condition_is_evaluated_true() {
    String condition = "$var1 NOT_ENDS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_NOT_ENDS_WITH_condition_is_evaluated_false() {
    String condition = "$var1 NOT_ENDS_WITH xyz";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // STARTS_WITH test one true and one false

  @Test
  public void simple_STARTS_WITH_condition_is_evaluated_true() {
    String condition = "$var1 STARTS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcdefg"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_STARTS_WITH_condition_is_evaluated_false() {
    String condition = "$var1 STARTS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

  // NOT_STARTS_WITH test one true and one false

  @Test
  public void simple_NOT_STARTS_WITH_condition_is_evaluated_true() {
    String condition = "$var1 NOT_STARTS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("uvwxyz"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_NOT_STARTS_WITH_condition_is_evaluated_false() {
    String condition = "$var1 NOT_STARTS_WITH abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcdefg"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

}
