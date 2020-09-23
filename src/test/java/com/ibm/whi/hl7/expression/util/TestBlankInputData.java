/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.util;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Specification;
import com.ibm.whi.core.message.InputData;

public class TestBlankInputData implements InputData {


  @Override
  public GenericResult extractMultipleValuesForSpec(List<Specification> hl7specs,
      Map<String, GenericResult> contextValues) {
    return null;
  }

  @Override
  public GenericResult evaluateJexlExpression(String expression,
      Map<String, GenericResult> contextValues) {

    return null;
  }

  @Override
  public GenericResult extractValueForSpec(List<Specification> hl7specs,
      Map<String, GenericResult> contextValues) {

    return null;
  }

}
