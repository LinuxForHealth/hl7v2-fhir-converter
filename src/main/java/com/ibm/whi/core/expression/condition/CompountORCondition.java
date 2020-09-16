package com.ibm.whi.core.expression.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.expression.GenericResult;

public class CompountORCondition implements Condition {

  private List<Condition> conditions;



  public CompountORCondition(List<Condition> conditions) {
    Preconditions.checkArgument(conditions != null && !conditions.isEmpty(),
        "onditions cannot be null or empty");
    this.conditions = new ArrayList<>(conditions);
  }



  @Override
  public boolean test(Map<String, GenericResult> contextVariables) {
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
