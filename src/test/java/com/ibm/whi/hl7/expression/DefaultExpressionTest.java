package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DefaultExpressionTest {

  private static final String SOME_VALUE = "SOME_VALUE";


  @Test
  public void test_constant() {

    DefaultExpression exp = new DefaultExpression(SOME_VALUE);
    Map<String, Object> context = new HashMap<>();
    Object value = exp.execute(context);
    assertThat(value).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable() {

    DefaultExpression exp = new DefaultExpression("$var1");
    Map<String, Object> context = new HashMap<>();
    context.put("var1", SOME_VALUE);

    Object value = exp.execute(context);
    assertThat(value).isEqualTo(SOME_VALUE);
  }


  @Test
  public void test_variable_invalid_var() {

    DefaultExpression exp = new DefaultExpression("$");
    Map<String, Object> context = new HashMap<>();
    context.put("", SOME_VALUE);

    Object value = exp.execute(context);
    assertThat(value).isEqualTo("$");
  }


  @Test
  public void test_variable_no_context() {

    DefaultExpression exp = new DefaultExpression("$var1");
    Map<String, Object> context = new HashMap<>();

    Object value = exp.execute(context);
    assertThat(value).isNull();
  }

  @Test
  public void test_blank() {

    DefaultExpression exp = new DefaultExpression("");
    Map<String, Object> context = new HashMap<>();

    Object value = exp.execute(context);
    assertThat(value).isEqualTo("");
  }

}
