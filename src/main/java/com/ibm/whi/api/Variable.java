/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.List;
import java.util.Map;


/**
 * Defines Variable object that can be used during the expression evaluation.
 * 
 *
 * @author pbhallam
 */
public interface Variable {

  /**
   * Return the list of specs that should be used for evaluating the variable. If the list of specs
   * is empty then variable value will be extracted from contextValues. If the list of specs is not
   * empty then data source will be used to extract the value of the specs.
   * 
   * @return
   */
  List<String> getSpec();

  /**
   * Return the Class type for the value to be extracted.
   * 
   * @return
   */
  String getType();

  /**
   * Return the name of the variable
   * 
   * @return
   */
  String getVariableName();

  /**
   * Evaluates the variable and extracts the value based on the provided context values and input
   * data source.
   * 
   * @param contextValues
   * @param dataSource
   * @return {@link EvaluationResult}
   */
  EvaluationResult extractVariableValue(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource);


}
