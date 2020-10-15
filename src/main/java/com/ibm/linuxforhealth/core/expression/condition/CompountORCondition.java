/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.expression.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import com.ibm.linuxforhealth.api.Condition;
import com.ibm.linuxforhealth.api.EvaluationResult;

public class CompountORCondition implements Condition {

  private List<Condition> conditions;



  public CompountORCondition(List<Condition> conditions) {
    Preconditions.checkArgument(conditions != null && !conditions.isEmpty(),
        "onditions cannot be null or empty");
    this.conditions = new ArrayList<>(conditions);
  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    for (Condition c : conditions) {
      if (c.test(contextVariables)) {
        return true;
      }
    }
    return false;
  }



  public List<Condition> getConditions() {
    return new ArrayList<>(conditions);
  }





}
