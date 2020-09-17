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
public class CheckNullTest {

  @Test
  public void null_condition_with_is_evaluated_false() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void null_condition_with_is_evaluated_true() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult(null));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void null_condition_with_is_evaluated_true_if_var_is_not_in_context_map() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

}
