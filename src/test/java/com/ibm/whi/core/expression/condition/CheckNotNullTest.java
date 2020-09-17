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
import com.ibm.whi.core.expression.GenericResult;

public class CheckNotNullTest {

  private static final String VAR1_NOT_NULL = "$var1 NOT_NULL";


  @Test
  public void not_null_condition_with_is_evaluated_True() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void not_null_condition_with_is_evaluated_false() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult(null));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void not_null_condition_is_evaluated_false_if_var_is_not_present() {
    String condition = VAR1_NOT_NULL;
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

}
