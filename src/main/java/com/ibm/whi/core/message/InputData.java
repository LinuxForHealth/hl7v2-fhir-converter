/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.message;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Specification;

public interface InputData {

  GenericResult extractValueForSpec(List<Specification> hl7specs,
      Map<String, GenericResult> contextValues);

  GenericResult extractMultipleValuesForSpec(List<Specification> hl7specs,
      Map<String, GenericResult> contextValues);

  GenericResult evaluateJexlExpression(String expression, Map<String, GenericResult> contextValues);


}
