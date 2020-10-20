/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import java.util.Map;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.VariableUtils;

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
