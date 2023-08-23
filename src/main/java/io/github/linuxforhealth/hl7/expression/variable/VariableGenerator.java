/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.variable;

import java.util.ArrayList;
import java.util.List;

import io.github.linuxforhealth.hl7.expression.ExpressionAttributes;
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

    // Extract the modifiers such as '*' and '&' from the expression
    ExpressionAttributes.ExpressionModifiers exp =
        ExpressionAttributes.extractExpressionModifiers(variableExpression, false);

    String rawVariable = exp.getExpression();
    // Are they putting a java GeneralUtils fn result into a variable? 
    //   - It might not just be GeneralUtils - It could be one of their own custom fns (which **MUST** end in Utils)
    //   - TODO: Go find the Map of customFunctions, and check all the keys specifically
    if (StringUtils.contains(rawVariable, "Utils.")) { 
      String[] values = rawVariable.split(",", 2);
      // Handle * in combination with GeneralUtils function
      exp = ExpressionAttributes.extractExpressionModifiers(values[0], false);
      if (values.length == COMPONENT_LENGTH_FOR_VAR_EXPRESSION) {
        List<String> specs = getTokens(exp.getExpression());
        return new ExpressionVariable(varName, values[1], specs, exp.getExtractMultiple(), exp.getRetainEmpty());
      }
      throw new IllegalArgumentException("rawVariable not in correct format ");
    } else if (StringUtils.contains(rawVariable, ",")) {
      String[] values = rawVariable.split(",", 2);
      if (values.length == COMPONENT_LENGTH_FOR_VAR_EXPRESSION) {
        List<String> specs = getTokens(values[1]);
        return new DataTypeVariable(varName, values[0], specs, exp.getExtractMultiple());
      }
      throw new IllegalArgumentException("rawVariable not in correct format ");
    } else {
      boolean combineValues = false;
      if (StringUtils.contains(rawVariable, "+")) {
        combineValues = true;
      }
      List<String> specs = getTokens(rawVariable);
      return new SimpleVariable(varName, specs, exp.getExtractMultiple(), combineValues, exp.getRetainEmpty());
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
