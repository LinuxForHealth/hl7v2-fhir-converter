/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.EmptyEvaluationResult;

/**
 * Each expression defines how to extract the value for a field. The execute method defines the
 * extraction process.
 * 
 *
 * @author pbhallam
 */

public interface Expression {
  /**
   * Defines the return type of the object value evaluated/extracted.
   * 
   * @return
   */
  String getType();

  /**
   * Specifies the default value for the field that should be returned if the evaluated value is
   * null
   * 
   * @return {@link EvaluationResult}
   */
  default EvaluationResult getDefaultValue() {
    return new EmptyEvaluationResult();
  }

  /**
   * List of Specification values that would be passed to the Evaluator to extract value.
   * 
   * @return List {@link Specification}
   */
  List<Specification> getspecs();

  /**
   * Evaluates the expression and returns the GenericResult Object
   * 
   * @param primaryDataSource {@link DataSource} input data
   * @param contextValues - Map of values for variables
   * @param baseValue {@link EvaluationResult}
   * @return {@link EvaluationResult}
   */
  EvaluationResult evaluate(InputDataExtractor primaryDataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue);

  /**
   * Evaluates an expression if the condition is satisfied
   * 
   * @param contextValues {@link Map}
   * @return true if condition satisfied otherwise returns false;
   */
  default boolean isConditionSatisfied(Map<String, EvaluationResult> contextValues) {
    return true;
  }


  /**
   * List of Variables values that would be evaluated prior to evaluating the expression and these
   * values extracted from variables will be utilized during expression evaluation. Extracted
   * variables are added to context map.
   * 
   * @return List {@link Variable}
   */
  default List<Variable> getVariables() {
    return new ArrayList<>();
  }


  /**
   * List of constant string values that would be added to context map prior to the evaluating the
   * variables.
   * 
   * @return List {@link String}
   */
  default Map<String, String> getConstants() {
    return new HashMap<>();
  }


}
