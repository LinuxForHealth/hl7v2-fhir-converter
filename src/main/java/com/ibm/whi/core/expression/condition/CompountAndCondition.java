/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.Condition;
import com.ibm.whi.api.EvaluationResult;

public class CompountAndCondition implements Condition {

  private List<Condition> conditions;


  public CompountAndCondition(List<Condition> conditions) {
    Preconditions.checkArgument(conditions != null && !conditions.isEmpty(),
        "conditions cannot be null or empty");
    this.conditions = conditions;
  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    for (Condition c : conditions) {
      if (!c.test(contextVariables)) {
        return false;
      }
    }
    return true;
  }



  public List<Condition> getConditions() {
    return new ArrayList<>(conditions);
  }




}
