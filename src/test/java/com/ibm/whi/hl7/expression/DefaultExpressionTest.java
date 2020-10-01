/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.core.expression.SimpleEvaluationResult;
import com.ibm.whi.hl7.expression.util.TestBlankInputData;

public class DefaultExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";
  private static final InputData data = new TestBlankInputData();

  @Test
  public void test_constant() {

    SimpleExpression exp = new SimpleExpression(SOME_VALUE);
    Map<String, EvaluationResult> context = new HashMap<>();

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable() {

    SimpleExpression exp = new SimpleExpression("$var1");
    Map<String, EvaluationResult> context = new HashMap<>();

    context.put("var1", new SimpleEvaluationResult(SOME_VALUE));

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable_invalid_var() {

    SimpleExpression exp = new SimpleExpression("$");

    Map<String, EvaluationResult> context = new HashMap<>();

    context.put("", new SimpleEvaluationResult(SOME_VALUE));

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo("$");
  }


  @Test
  public void test_variable_no_context() {

    SimpleExpression exp = new SimpleExpression("$var1");
    Map<String, EvaluationResult> context = new HashMap<>();



    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context));
    assertThat(value).isNull();
  }

  @Test
  public void test_blank() {

    SimpleExpression exp = new SimpleExpression("");
    Map<String, EvaluationResult> context = new HashMap<>();


    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo("");
  }

}
