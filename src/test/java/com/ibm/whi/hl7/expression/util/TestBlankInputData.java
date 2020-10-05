/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.util;

import java.util.List;
import java.util.Map;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.api.Specification;

public class TestBlankInputData implements InputData {


  @Override
  public EvaluationResult extractMultipleValuesForSpec(List<Specification> hl7specs,
      Map<String, EvaluationResult> contextValues) {
    return null;
  }

  @Override
  public EvaluationResult evaluateJexlExpression(String expression,
      Map<String, EvaluationResult> contextValues) {

    return null;
  }

  @Override
  public EvaluationResult extractValueForSpec(List<Specification> hl7specs,
      Map<String, EvaluationResult> contextValues) {

    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

}
