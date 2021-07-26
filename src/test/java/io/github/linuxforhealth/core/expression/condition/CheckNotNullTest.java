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
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.core.expression.condition.CheckNotNull;
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;

public class CheckNotNullTest {

  private static final String VAR1_NOT_NULL = "$var1 NOT_NULL";


  @Test
  public void not_null_condition_with_is_evaluated_True() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void not_null_condition_with_is_evaluated_false() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new EmptyEvaluationResult());
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void not_null_condition_is_evaluated_false_if_var_is_not_present() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

}
