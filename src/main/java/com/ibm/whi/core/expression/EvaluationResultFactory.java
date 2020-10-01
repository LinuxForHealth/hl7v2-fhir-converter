/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression;

import java.util.List;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.core.resource.ResourceValue;

public class EvaluationResultFactory {

  private EvaluationResultFactory() {}

  public static EvaluationResult getEvaluationResult(Object value,
      List<ResourceValue> additionalResources) {
    if (value != null) {
      return new SimpleEvaluationResult(value, additionalResources);
    } else {
      return new EmptyEvaluationResult();
    }
  }

  public static EvaluationResult getEvaluationResult(Object value) {
    if (value != null) {
      return new SimpleEvaluationResult(value);
    } else {
      return new EmptyEvaluationResult();
    }
  }


}


