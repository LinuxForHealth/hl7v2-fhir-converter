/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.Map;

/**
 * Represents class that encapsulates how to extract information from a particular source.
 * 
 *
 * @author pbhallam
 */
public interface InputDataExtractor {

  /**
   * Extract the single value from the input for the given specification.
   * 
   * @param spec - List of specifications example: PID.3
   * @param contextValues - Map of key value pair
   * @return {@link EvaluationResult}
   */
  EvaluationResult extractValueForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues);

  /**
   * Extract the multiple values from the input for the given specification.
   * 
   * @param specs - List of specifications example: PID.3
   * @param contextValues - Map of key value pair
   * @return {@link EvaluationResult}
   */
  EvaluationResult extractMultipleValuesForSpec(Specification specs,
      Map<String, EvaluationResult> contextValues);

  /**
   * Evaluate JEXL Expression that handles extracting data from this data source.
   * 
   * @param expression - example:
   * @param contextValues - Map of key value pair
   * @return {@link EvaluationResult}
   */
  EvaluationResult evaluateJexlExpression(String expression,
      Map<String, EvaluationResult> contextValues);


  /**
   * Return the name /identifier of this resource Example: for ADT_A01 message, return the message
   * type.
   */
  String getName();

  /**
   * Return the unique identifier of this resource Example: for a ADT_A01 message, return the
   * message id.
   */
  String getId();
}
