/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.List;
import java.util.Map;

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
  EvaluationResult getDefaultValue();

  /**
   * List of Specification values that would be passed to the Evaluator to extract value.
   * 
   * @return List {@link Specification}
   */
  List<Specification> getspecs();

  /**
   * Evaluates the expression and returns the GenericResult Object
   * 
   * @param dataSource {@link DataSource} input data
   * @param contextValues - Map of values for variables
   * @return {@link EvaluationResult}
   */
  EvaluationResult evaluate(InputData dataSource, Map<String, EvaluationResult> contextValues);

  /**
   * Evaluates an expression if the condition is satisfied
   * 
   * @param contextValues {@link Map}
   * @return true if condition satisfied otherwise returns false;
   */
  boolean isConditionSatisfied(Map<String, EvaluationResult> contextValues);



}
