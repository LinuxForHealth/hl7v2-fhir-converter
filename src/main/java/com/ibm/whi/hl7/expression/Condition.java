package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.expression.GenericResult;

public class Condition {
  private static final WHIAJexlEngine JEXL = new WHIAJexlEngine();
  private String conditionExpression;


  public Condition(String input) {
    Preconditions.checkArgument(StringUtils.isNotBlank(input), "Conditiion cannot be blank");
    WHIAJexlEngine.validateCondition(input);
    this.conditionExpression = input;
  }



  public boolean evaluateCondition(Map<String, GenericResult> contextValues) {
    Map<String, Object> localContext = new HashMap<>();
    contextValues.forEach((key, value) -> localContext.put(key, value.getValue()));
    return JEXL.evaluateCondition(conditionExpression, localContext);
  }



  public String getConditionExpression() {
    return conditionExpression;
  }


}
