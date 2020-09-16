package com.ibm.whi.core.expression.condition;

import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.expression.GenericResult;

public class CompountAndCondition implements Condition {

  private List<Condition> conditions;


  public CompountAndCondition(List<Condition> conditions) {
    Preconditions.checkArgument(conditions != null && !conditions.isEmpty(),
        "conditions cannot be null or empty");
    this.conditions = conditions;
  }



  @Override
  public boolean test(Map<String, GenericResult> contextVariables) {
    for (Condition c : conditions) {
      if (!c.test(contextVariables)) {
        return false;
      }
    }
    return true;
  }





}
