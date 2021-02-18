/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.variable;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.Variable;

public class VariableGenerator {

  private static final int COMPONENT_LENGTH_FOR_VAR_EXPRESSION = 2;


  private VariableGenerator() {}



  public static Variable parse(String varName, String variableExpression) {
    Preconditions.checkArgument(StringUtils.isNotBlank(varName), "varName string cannot be null");
    Preconditions.checkArgument(StringUtils.isNotBlank(variableExpression),
        "rawVariable string cannot be null");
    boolean extractMultiple = false;
    if (StringUtils.endsWith(variableExpression, "*")) {
      extractMultiple = true;
    }
    String rawVariable = StringUtils.removeEnd(variableExpression, "*");
    rawVariable = StringUtils.strip(rawVariable);
    if (StringUtils.contains(rawVariable, "GeneralUtils")) {
      String[] values = rawVariable.split(",", 2);
      if (values.length == COMPONENT_LENGTH_FOR_VAR_EXPRESSION) {
        List<String> specs = getTokens(values[0]);
        return new ExpressionVariable(varName, values[1], specs, extractMultiple);
      }
      throw new IllegalArgumentException("rawVariable not in correct format ");
    } else if (StringUtils.contains(rawVariable, ",")) {
      String[] values = rawVariable.split(",", 2);
      if (values.length == COMPONENT_LENGTH_FOR_VAR_EXPRESSION) {
        List<String> specs = getTokens(values[1]);
        return new DataTypeVariable(varName, values[0], specs, extractMultiple);
      }
      throw new IllegalArgumentException("rawVariable not in correct format ");
    } else {
      boolean combineValues = false;
      if (StringUtils.contains(rawVariable, "+")) {
        combineValues = true;
      }
      List<String> specs = getTokens(rawVariable);
      return new SimpleVariable(varName, specs, extractMultiple, combineValues);
    }
  }


  private static List<String> getTokens(String value) {
    if (StringUtils.contains(value, "+")) {
      StringTokenizer st = new StringTokenizer(value, "+").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }
    if (StringUtils.isNotBlank(value)) {
      StringTokenizer st = new StringTokenizer(value, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }

    return new ArrayList<>();
  }


}
