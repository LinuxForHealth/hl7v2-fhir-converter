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

public class CheckNull implements Condition {
  public static final String NULL = "NULL";
  private String var1;


  public CheckNull(String var1) {
    this.var1 = var1;

  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    EvaluationResult variable1 = contextVariables.get(VariableUtils.getVarName(var1));
    
    return variable1 == null || variable1.isEmpty();
  }


  public String getVar1() {
    return var1;
  }



}
