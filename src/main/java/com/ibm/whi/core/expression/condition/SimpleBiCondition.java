/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression.condition;

import java.util.Map;
import com.ibm.whi.api.Condition;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.core.expression.VariableUtils;

public class SimpleBiCondition implements Condition {

  private String var1;



  private Object var2;
  private String conditionOperator;


  public SimpleBiCondition(String var1, String var2, String conditionOperator) {
    this.var1 = var1;
    this.var2 = var2;
    this.conditionOperator = conditionOperator;
  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    Object var1Value = null;
    EvaluationResult variable1;
    if (VariableUtils.isVar(var1)) {
      variable1 = contextVariables.get(VariableUtils.getVarName(var1));
      if (variable1 != null && !variable1.isEmpty()) {
        var1Value = variable1.getValue();
      }
    } else {
      throw new IllegalArgumentException("First value should be a variable");
    }


    Object var2Value = getValue(contextVariables);

    if (var1Value != null && var2Value != null) {
    
      ConditionPredicateEnum condEnum = ConditionPredicateEnum
          .getConditionPredicate(this.conditionOperator, variable1.getName());
      if (condEnum != null) {
        return condEnum.getPredicate().test(var1Value, var2Value);
      }

    }
    return false;
  }



  private Object getValue(Map<String, EvaluationResult> contextVariables) {
    Object var2Value = null;
    if (var2 instanceof String && VariableUtils.isVar((String) var2)) {
      EvaluationResult variable = contextVariables.get(VariableUtils.getVarName((String) var2));
      if (variable != null && !variable.isEmpty()) {
        var2Value = variable.getValue();
      }
    } else {
      var2Value = var2;
    }
    return var2Value;
  }


  public String getVar1() {
    return var1;
  }



  public Object getVar2() {
    return var2;
  }



  public String getConditionOperator() {
    return conditionOperator;
  }



}
