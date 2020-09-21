/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import com.ibm.whi.core.data.JexlEngineUtil;


public class JexlEngineUtilTest {


  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void test() {
    JexlEngineUtil engine= new JexlEngineUtil();
    Map<String, Object> context = new HashMap<>();
    context.put("var1", "s");
    context.put("var2", "t");
    context.put("var3", "u");

    String value = (String) engine.evaluate("String.join(\" \",  var1,var2, var3)", context);
    assertThat(value).isEqualTo("s t u");
  }


  @Test
  public void blank_expression_throws_exception() {
    JexlEngineUtil wex = new JexlEngineUtil();
    exception.expect(IllegalArgumentException.class);
    wex.evaluate("", new HashMap<>());
  }



  @Test
  public void non_supported_expression_throws_exception() {
    JexlEngineUtil wex = new JexlEngineUtil();
    exception.expect(IllegalArgumentException.class);
    wex.evaluate("System.currentTimeMillis()", new HashMap<>());
  }



  @Test
  public void non_supported_expression_combining_lines_throws_exception() {
    JexlEngineUtil wex = new JexlEngineUtil();
    exception.expect(IllegalArgumentException.class);
    wex.evaluate("String.toString();System.exit(1); ", new HashMap<>());
  }

  @Test
  public void non_supported_expression_throws_exception_2() {
    JexlEngineUtil wex = new JexlEngineUtil();
    exception.expect(IllegalArgumentException.class);
    wex.evaluate("String", new HashMap<>());
  }


  @Test
  public void valid_expression_returns_value() {
    JexlEngineUtil wex = new JexlEngineUtil();
    Object b = wex.evaluate("String.toString() ", new HashMap<>());
    assertThat(b).isEqualTo(String.class.toString());
  }


  @Test
  public void valid_NumUtils_expression_returns_value() {
    JexlEngineUtil wex = new JexlEngineUtil();
    Object b = wex.evaluate("NumberUtils.createFloat(\"1.2\")", new HashMap<>());
    assertThat(b).isEqualTo(NumberUtils.createFloat("1.2"));
  }


}
