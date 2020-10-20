/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.core.expression.condition.CheckNull;
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;
public class CheckNullTest {

  @Test
  public void null_condition_with_is_evaluated_false() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void null_condition_with_is_evaluated_true() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new EmptyEvaluationResult());
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void null_condition_with_is_evaluated_true_if_var_is_not_in_context_map() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

}
