package com.ibm.whi.core.expression.condition;

import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.util.GeneralUtil;

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
  public boolean test(Map<String, GenericResult> contextVariables) {
    Object var1Value = null;
    GenericResult variable1;
    if (GeneralUtil.isVar(var1)) {
      variable1 = contextVariables.get(GeneralUtil.getVarName(var1));
      if (variable1 != null && !variable1.isEmpty()) {
        var1Value = variable1.getValue();
      }
    } else {
      throw new IllegalArgumentException("First value should be a variable");
    }


    Object var2Value = getValue(contextVariables);

    if (var1Value != null && var2Value != null) {
    
      ConditionPredicateEnum condEnum = ConditionPredicateEnum
          .getConditionPredicate(this.conditionOperator, variable1.getKlassName());
      if (condEnum != null) {
        return condEnum.getPredicate().test(var1Value, var2Value);
      }

    }
    return false;
  }



  private Object getValue(Map<String, GenericResult> contextVariables) {
    Object var2Value = null;
    if (var2 instanceof String && GeneralUtil.isVar((String) var2)) {
      GenericResult variable = contextVariables.get(GeneralUtil.getVarName((String) var2));
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
