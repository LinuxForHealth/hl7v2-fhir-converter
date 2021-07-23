/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import java.util.Map;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.VariableUtils;

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
          .getConditionPredicate(this.conditionOperator, variable1.getIdentifier());
      if (condEnum != null) {
        // if var2 is a string and must be converted to an integer to test
        if (var2Value.getClass().getTypeName().equalsIgnoreCase("java.lang.String") 
            && condEnum.getKlassU().getTypeName().equalsIgnoreCase("java.lang.Integer")) {
          var2Value = Integer.parseInt((String)var2Value);
        }
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
