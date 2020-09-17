/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.resource;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.message.InputData;


public interface ResourceModel {

  ResourceResult evaluateSingle(InputData dataExtractor, Map<String, GenericResult> contextValues,
      GenericResult baseValue);

  ResourceResult evaluateMultiple(InputData dataExtractor, Map<String, GenericResult> contextValues,
      List<GenericResult> baseValues, List<Variable> variables);

  Map<String, Expression> getExpressions();


  int getOrder();

  String getSpec();


  String getName();




}
