package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.eval.WHIAJexlEngine;
import com.ibm.whi.core.message.InputData;


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
  public GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");

    Map<String, Object> localContext = new HashMap<>();

    Map<String, GenericResult> resolvedVariables = new HashMap<>(contextValues);
    resolvedVariables.putAll(
        dataSource.resolveVariables(this.getVariables(),
            ImmutableMap.copyOf(contextValues)));
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
