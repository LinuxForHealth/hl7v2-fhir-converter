/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.expression.condition;

import java.util.Map;
import com.ibm.linuxforhealth.api.Condition;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.core.expression.VariableUtils;

public class CheckNotNull implements Condition {

  public static final String NOT_NULL = "NOT_NULL";
  private String var1;



  public CheckNotNull(String var1) {
    this.var1 = var1;

  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    EvaluationResult variable1 = contextVariables.get(VariableUtils.getVarName(var1));
    
    return variable1 != null && !variable1.isEmpty();
  }



  public String getVar1() {
    return var1;
  }



}
