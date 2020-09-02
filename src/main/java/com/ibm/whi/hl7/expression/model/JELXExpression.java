package com.ibm.whi.hl7.expression.model;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.python.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.hl7.expression.eval.WHIAJexlEngine;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JELXExpression extends AbstractExpression {
  private static final WHIAJexlEngine JEXL = new WHIAJexlEngine();
  private String evaluate;




  @JsonCreator
  public JELXExpression(@JsonProperty("type") String type,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("hl7spec") String hl7spec, @JsonProperty("evaluate") String evaluate,
      @JsonProperty("var") Map<String, String> variables) {
    super(type, defaultValue, required, hl7spec, variables);
    Preconditions.checkArgument(StringUtils.isNotBlank(evaluate), "evaluate file cannot be blank");


    this.evaluate = evaluate;


  }


  public JELXExpression(String evaluate, Map<String, String> variables) {
    this("String", null, false, null, evaluate, variables);


  }


  @Override
  public Object execute(Map<String, Object> context) {
    Map<String, Object> localContext = new HashMap<>(context);
    localContext.putAll(resolveVariables(this.getVariables(), localContext));

    Object obj = JEXL.evaluate(this.evaluate, localContext);
    if (obj != null) {
      return obj;
    } else {
      return this.getDefaultValue();
    }
  }




}
