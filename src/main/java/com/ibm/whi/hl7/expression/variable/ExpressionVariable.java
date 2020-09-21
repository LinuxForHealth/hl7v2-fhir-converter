/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;


/**
 * Defines Variable object that can be used during the expression evaluation.
 * 
 *
 * @author pbhallam
 */
public class ExpressionVariable extends SimpleVariable {

  private String expression;

  /**
   * Constructor for Variable with default type: Object
   * 
   * @param name
   * @param spec
   */
  public ExpressionVariable(String name, String expression, List<String> spec) {
    super(name, spec);
    this.expression = expression;
  }




  // resolve variable value
  @Override
  public GenericResult extractVariableValue(Map<String, GenericResult> contextValues,
      InputData dataSource) {
    GenericResult result;
    if (!this.getSpec().isEmpty()) {
      result = getValueFromSpecs(contextValues, dataSource);
    } else {
      result = null;
    }

    if (this.expression != null) {
      // resolve expression
      Map<String, GenericResult> localContextValues = new HashMap<>(contextValues);
      if (result != null) {
        localContextValues.put(this.getName(), result);
      }
      result = dataSource.evaluateJexlExpression(expression, localContextValues);
    }
    return result;

  }




}
