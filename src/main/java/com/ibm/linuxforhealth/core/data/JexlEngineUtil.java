/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.ibm.linuxforhealth.hl7.exception.NoMoreRepititionException;

public final class JexlEngineUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(JexlEngineUtil.class);
  private static final List<String> OPERATORS =
      Lists.newArrayList(">", "<", "==", "!=", ">=", "<=");

  private JexlEngine jexl;
  private Map<String, Object> functions = new HashMap<>();



  public JexlEngineUtil() {
    jexl = new JexlBuilder().silent(false).debug(true).strict(true).create();
    LOGGER.info("silent:{} , strict :{} ", jexl.isSilent(), jexl.isStrict());
    functions.put(StringUtils.class.getSimpleName(), StringUtils.class);
    functions.put(NumberUtils.class.getSimpleName(), NumberUtils.class);
    functions.put(String.class.getSimpleName(), String.class);
    functions.put(UUID.class.getSimpleName(), UUID.class);

  }

  public JexlEngineUtil(Map<String, Object> functions) {
    this();
    functions.putAll(functions);

  }


  public JexlEngineUtil(String name, Object function) {
    this();
    functions.put(name, function);

  }

  public Object evaluate(String jexlExp, Map<String, Object> context) {
    Preconditions.checkArgument(StringUtils.isNotBlank(jexlExp), "jexlExp cannot be blank");
    Preconditions.checkArgument(context != null, "context cannot be null");
    String trimedJexlExp = StringUtils.trim(jexlExp);
    // ensure that expression
    validateExpression(trimedJexlExp);

    LOGGER.info("Evaluating expression : {}", trimedJexlExp);
    Map<String, Object> localContext = new HashMap<>(functions);
    localContext.putAll(context);

    JexlExpression exp = jexl.createExpression(trimedJexlExp);
    JexlContext jc = new MapContext();
    localContext.entrySet().forEach(e -> jc.set(e.getKey(), e.getValue()));
    // Now evaluate the expression, getting the result
    try {
      Object obj = exp.evaluate(jc);
      LOGGER.info("Evaluated expression : {}, returning object {}", trimedJexlExp, obj);
      return obj;
    } catch (JexlException e) {
      if (e.getCause() instanceof NoMoreRepititionException) {
        throw new NoMoreRepititionException(
            e.getCause().getMessage() + ": expression" + trimedJexlExp,
            e.getCause());
      } else {
        throw e;
      }
    }

  }



  public boolean evaluateCondition(String jexlExp, Map<String, Object> context) {
    Preconditions.checkArgument(StringUtils.isNotBlank(jexlExp), "jexlExp cannot be blank");
    Preconditions.checkArgument(context != null, "context cannot be null");
    String trimedJexlExp = StringUtils.trim(jexlExp);
    // ensure that expression
    validateCondition(trimedJexlExp);

    LOGGER.info("Evaluating condiitional expression : {}", trimedJexlExp);
    Map<String, Object> localContext = new HashMap<>(functions);
    localContext.putAll(context);

    JexlExpression exp = jexl.createExpression(trimedJexlExp);
    JexlContext jc = new MapContext();
    localContext.entrySet().forEach(e -> jc.set(e.getKey(), e.getValue()));
    // Now evaluate the expression, getting the result

    boolean obj = (boolean) exp.evaluate(jc);
      LOGGER.info("Evaluated expression : {}, returning object {}", trimedJexlExp, obj);
      return obj;


  }


  static void validateCondition(String input) {
    boolean isValid = false;
    StringTokenizer strtoken = new StringTokenizer(input, " ").setIgnoreEmptyTokens(true);
    if (strtoken.getTokenList().size() == 3) {
      String var1 = strtoken.nextToken();
      String operator = strtoken.nextToken();
      String var2 = strtoken.nextToken();
      if (StringUtils.isAlphanumeric(var1) && StringUtils.isAlphanumeric(var2)
          && OPERATORS.contains(operator)) {
        isValid = true;
      }
    }
    if (!isValid) {

      throw new IllegalArgumentException(
          "Condition not supported, only the following format is supported, value1 <conditionOperator> value2, input:"
              + input);
    }
  }

  private void validateExpression(String jexlExp) {

    StringTokenizer stk = new StringTokenizer(jexlExp, ".").setIgnoreEmptyTokens(true);
    String tok = stk.nextToken();
    // format of expressions should be Function.method
    // if only one part is specified like Function then this is not a valid expression.
    if (stk.getTokenList().size() < 2 || functions.get(tok) == null) {
      throw new IllegalArgumentException("Expression has unsupported function: " + tok);
    }
    if (jexlExp.contains(";")) {
      throw new IllegalArgumentException(
          "Expression cannot contain character ; Expression: " + jexlExp);
    }

  }
}
