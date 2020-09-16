package com.ibm.whi.core.expression.condition;

import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;

public class CheckNull implements Condition {
  public static final String NULL = "NULL";
  private String var1;


  public String getVar1() {
    return var1;
  }



  public CheckNull(String var1) {
    this.var1 = var1;

  }



  @Override
  public boolean test(Map<String, GenericResult> contextVariables) {
    GenericResult variable1=contextVariables.get(var1);
    
    return variable1 == null || variable1.isEmpty();
  }





}
