/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.message.InputData;

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
   * @return {@link GenericResult}
   */
  GenericResult getDefaultValue();

  /**
   * List of Specification values that would be passed to the Evaluator to extract value.
   * 
   * @return
   */
  List<Specification> getspecs();

  /**
   * Evaluates the expression and returns the GenericResult Object
   * 
   * @param {@link DataSource} input data
   * @param contextValues - Map of values for variables
   * @return {@link GenericResult}
   */
  GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues);


  boolean isConditionSatisfied(Map<String, GenericResult> contextValues);



}
