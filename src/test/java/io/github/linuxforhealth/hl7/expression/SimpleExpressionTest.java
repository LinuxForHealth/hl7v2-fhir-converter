/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.hl7.expression.util.TestBlankInputData;

public class SimpleExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";
  private static final InputDataExtractor data = new TestBlankInputData();

  @Test
  public void test_constant() {
    ExpressionAttributes attr = new ExpressionAttributes.Builder().withValue(SOME_VALUE).build();
    SimpleExpression exp = new SimpleExpression(attr);
    Map<String, EvaluationResult> context = new HashMap<>();

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context), new EmptyEvaluationResult());
    assertThat((String) value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable() {
    ExpressionAttributes attr = new ExpressionAttributes.Builder().withValue(SOME_VALUE).build();
    SimpleExpression exp = new SimpleExpression(attr);
    Map<String, EvaluationResult> context = new HashMap<>();

    context.put("var1", new SimpleEvaluationResult(SOME_VALUE));

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context), new EmptyEvaluationResult());
    assertThat((String) value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable_invalid_var() {
    ExpressionAttributes attr = new ExpressionAttributes.Builder().withValue("$").build();
    SimpleExpression exp = new SimpleExpression(attr);

    Map<String, EvaluationResult> context = new HashMap<>();

    context.put("", new SimpleEvaluationResult(SOME_VALUE));

    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context), new EmptyEvaluationResult());
    assertThat((String) value.getValue()).isEqualTo("$");
  }


  @Test
  public void test_variable_no_context() {
    ExpressionAttributes attr = new ExpressionAttributes.Builder().withValue("$var1").build();
    SimpleExpression exp = new SimpleExpression(attr);

    Map<String, EvaluationResult> context = new HashMap<>();



    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context), new EmptyEvaluationResult());
    assertThat(value).isNull();
  }

  @Test
  public void test_blank() {
    ExpressionAttributes attr = new ExpressionAttributes.Builder().withValue("").build();
    SimpleExpression exp = new SimpleExpression(attr);
    Map<String, EvaluationResult> context = new HashMap<>();


    EvaluationResult value =
        exp.evaluate(data, ImmutableMap.copyOf(context), new EmptyEvaluationResult());
    assertThat((String) value.getValue()).isEqualTo("");
  }

}
