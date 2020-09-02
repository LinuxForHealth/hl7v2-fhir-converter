package com.ibm.whi.hl7.expression.model;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.python.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.GenericResult;
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
  public GenericResult execute(ImmutableMap<String, ?> executables,
      ImmutableMap<String, GenericResult> variables) {
    Map<String, Object> localContext = new HashMap<>(executables);

    Map<String, GenericResult> resolvedVariables = new HashMap<>(variables);
    resolvedVariables.putAll(resolveVariables(this.getVariables(), executables, variables));
    resolvedVariables
        .forEach((key, value) -> localContext.put(key, value.getValue()));
    Object obj = JEXL.evaluate(this.evaluate, localContext);
    if (obj != null) {
      return new GenericResult(obj);
    } else {
      return new GenericResult(this.getDefaultValue());
    }
  }




}
