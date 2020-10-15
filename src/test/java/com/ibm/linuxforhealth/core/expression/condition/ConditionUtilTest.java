/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class ConditionUtilTest {



  @Test
  public void simple_equals_string_condition() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getVar1()).isEqualTo("$var1");
    assertThat(simplecondition.getVar2()).isEqualTo("abc");
    assertThat(simplecondition.getConditionOperator()).isEqualTo("EQUALS");
  }



  @Test
  public void simple_greaterthan_condition() {
    String condition = "$var1 GREATER_THAN 4";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getVar1()).isEqualTo("$var1");
    assertThat(simplecondition.getVar2()).isEqualTo("4");
    assertThat(simplecondition.getConditionOperator()).isEqualTo("GREATER_THAN");
  }



  @Test
  public void multiple_or_condition() {
    String condition = "$var1 EQUALS abc || $var1 EQUALS xyz";
    CompountORCondition simplecondition =
        (CompountORCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getConditions()).hasSize(2);

  }

  @Test
  public void multiple_and_condition() {
    String condition = "$var1 EQUALS abc && $var1 EQUALS xyz";
    CompountAndCondition simplecondition =
        (CompountAndCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getConditions()).hasSize(2);

  }


  @Test
  public void notnull_condition() {
    String condition = "$var1 NOT_NULL";
    CheckNotNull simplecondition = (CheckNotNull) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getVar1()).isEqualTo("$var1");

  }

  @Test
  public void null_condition() {
    String condition = "$var1 NULL";
    CheckNull simplecondition = (CheckNull) ConditionUtil.createCondition(condition);
    assertThat(simplecondition).isNotNull();
    assertThat(simplecondition.getVar1()).isEqualTo("$var1");

  }


}
