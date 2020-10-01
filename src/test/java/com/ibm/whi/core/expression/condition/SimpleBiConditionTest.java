/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.core.expression.SimpleEvaluationResult;

public class SimpleBiConditionTest {

  @Test
  public void simple_condition_is_evaluated_true() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void simple_condition_with_two_variables_is_evaluated_true() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_condition_with_two_variables_is_evaluated_false_when_condition_is_not_satisfied() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    contextVariables.put("var2", new SimpleEvaluationResult("xyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }



  @Test
  public void simple_condition_is_evaluated_false() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abcdf"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void simple_condition_is_evaluated_false_when_var_not_found() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isFalse();
  }
}
