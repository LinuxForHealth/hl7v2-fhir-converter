package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.model.DefaultExpression;

public class DefaultExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";


  @Test
  public void test_constant() {

    DefaultExpression exp = new DefaultExpression(SOME_VALUE);
    Map<String, GenericResult> context = new HashMap<>();
    Map<String, ?> executable = new HashMap<>();
    GenericResult value = exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable() {

    DefaultExpression exp = new DefaultExpression("$var1");
    Map<String, GenericResult> context = new HashMap<>();
    Map<String, ?> executable = new HashMap<>();
    context.put("var1", new GenericResult(SOME_VALUE));

    GenericResult  value = exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable_invalid_var() {

    DefaultExpression exp = new DefaultExpression("$");

    Map<String, GenericResult> context = new HashMap<>();
    Map<String, ?> executable = new HashMap<>();
    context.put("", new GenericResult(SOME_VALUE));

    GenericResult  value = exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo("$");
  }


  @Test
  public void test_variable_no_context() {

    DefaultExpression exp = new DefaultExpression("$var1");
    Map<String, GenericResult> context = new HashMap<>();
    Map<String, ?> executable = new HashMap<>();


    GenericResult  value = exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
    assertThat(value).isNull();
  }

  @Test
  public void test_blank() {

    DefaultExpression exp = new DefaultExpression("");
    Map<String, GenericResult> context = new HashMap<>();
    Map<String, ?> executable = new HashMap<>();


    GenericResult value = exp.execute(ImmutableMap.copyOf(executable), ImmutableMap.copyOf(context));
    assertThat(value.getValue()).isEqualTo("");
  }

}
