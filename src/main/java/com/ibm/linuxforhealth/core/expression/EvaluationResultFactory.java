/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.expression;

import java.util.List;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.api.ResourceValue;


public class EvaluationResultFactory {

  private EvaluationResultFactory() {}

  public static <V> EvaluationResult getEvaluationResult(V value,
      List<ResourceValue> additionalResources) {
    if (value != null) {
      return new SimpleEvaluationResult<>(value, additionalResources);
    } else {
      return new EmptyEvaluationResult();
    }
  }

  public static <V> EvaluationResult getEvaluationResult(V value) {
    if (value != null) {
      return new SimpleEvaluationResult<>(value);
    } else {
      return new EmptyEvaluationResult();
    }
  }


}


