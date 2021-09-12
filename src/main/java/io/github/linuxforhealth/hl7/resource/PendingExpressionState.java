/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

import java.util.HashMap;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;

public class PendingExpressionState {


  private Map<String, Expression> expressions;
  private Map<String, EvaluationResult> contextValues;

  public PendingExpressionState(Map<String, Expression> expressionsToEvaluateLater,
      Map<String, EvaluationResult> contextValues) {
    this.expressions = expressionsToEvaluateLater;
    this.contextValues = contextValues;
  }

  public Map<String, Expression> getExpressions() {
    return expressions;
  }

  public Map<String, EvaluationResult> getContextValues() {
    return contextValues;
  }

  public boolean isEmpty() {
    return expressions.isEmpty();
  }


  public static PendingExpressionState emptyPendingExpressionState() {
    return new PendingExpressionState(new HashMap<>(), new HashMap<>());
  }

}
