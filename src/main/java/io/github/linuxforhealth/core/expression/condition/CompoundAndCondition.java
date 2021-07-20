/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;

public class CompoundAndCondition implements Condition {

  private List<Condition> conditions;


  public CompoundAndCondition(List<Condition> conditions) {
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
