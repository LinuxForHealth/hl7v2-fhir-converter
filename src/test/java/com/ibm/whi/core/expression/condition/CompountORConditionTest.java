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
public class CompountORConditionTest {

  @Test
  public void compount_condition_evaluated_to_true() {
    String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
    CompountORCondition simplecondition =
        (CompountORCondition) ConditionUtil.createCondition(condition);

    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void compount_condition_evaluated_to_false() {
    String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
    CompountORCondition simplecondition =
        (CompountORCondition) ConditionUtil.createCondition(condition);

    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("tuv"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }

}
