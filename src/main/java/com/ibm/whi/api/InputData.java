/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.List;
import java.util.Map;

public interface InputData {

  EvaluationResult extractValueForSpec(List<Specification> hl7specs,
      Map<String, EvaluationResult> contextValues);

  EvaluationResult extractMultipleValuesForSpec(List<Specification> hl7specs,
      Map<String, EvaluationResult> contextValues);

  EvaluationResult evaluateJexlExpression(String expression, Map<String, EvaluationResult> contextValues);


}
