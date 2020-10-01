/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.Map;
import com.ibm.whi.core.resource.ResourceResult;

/**
 * Represents FHIR resource that needs to be generated based on the provided expressions
 * 
 *
 * @author pbhallam
 */
public interface ResourceModel {

  ResourceResult evaluate(InputData dataExtractor, Map<String, EvaluationResult> contextValues,
      EvaluationResult baseValue);

  Map<String, Expression> getExpressions();


  int getOrder();

  String getSpec();


  String getName();


}
