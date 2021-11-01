/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import java.util.Map;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.ContextValueUtils;
import io.github.linuxforhealth.core.expression.VariableUtils;

public class CheckNull implements Condition {
  public static final String NULL = "NULL";
  private String var1;
  private boolean useGroup;

  public CheckNull(String var1, boolean useGroup) {
    this.var1 = var1;
    this.useGroup = useGroup;

  }



  @Override
  public boolean test(Map<String, EvaluationResult> contextVariables) {
    EvaluationResult variable1 = ContextValueUtils.getVariableValuesFromVariableContextMap(var1,
        contextVariables, this.useGroup, VariableUtils.isFuzzyMatch(var1));

    return variable1 == null || variable1.isEmpty();
  }


  public String getVar1() {
    return var1;
  }



}
