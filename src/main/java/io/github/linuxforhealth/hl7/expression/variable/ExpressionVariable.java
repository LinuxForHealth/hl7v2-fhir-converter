/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;


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
  public ExpressionVariable(String name, String expression, List<String> spec,
      boolean extractMultiple) {
    super(name, spec, extractMultiple, false);
    this.expression = expression;
  }




  // resolve variable value
  @Override
  public EvaluationResult extractVariableValue(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource) {
    EvaluationResult result = null;
    if (!this.getSpec().isEmpty()) {
      result = getValueFromSpecs(contextValues, dataSource);
    }
    if (result == null) {
      result = new EmptyEvaluationResult();
    }

    if (this.expression != null) {
      // resolve expression
      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);

        localContextValues.put(this.getName(), result);

      result = dataSource.evaluateJexlExpression(expression, localContextValues);
    }
    return result;

  }

  /**
   *  @return String representation of expression
   */
  public String getExpression() {
	  return expression;
  }
}
