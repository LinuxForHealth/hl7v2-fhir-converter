/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.util;

import java.util.Map;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputDataExtractor;
import com.ibm.whi.api.Specification;

public class TestBlankInputData implements InputDataExtractor {

  @Override
  public EvaluationResult extractValueForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EvaluationResult extractMultipleValuesForSpec(Specification specs,
      Map<String, EvaluationResult> contextValues) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EvaluationResult evaluateJexlExpression(String expression,
      Map<String, EvaluationResult> contextValues) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

}
