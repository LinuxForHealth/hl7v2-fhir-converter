/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.message;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;

public interface InputData {




  Map<String, GenericResult> resolveVariables(List<Variable> variables,
      Map<String, GenericResult> contextValues);

  GenericResult extractSingleValueForSpec(List<String> hl7specs,
      Map<String, GenericResult> contextValues);

  GenericResult extractMultipleValuesForSpec(List<String> hl7specs,
      Map<String, GenericResult> contextValues);




}
