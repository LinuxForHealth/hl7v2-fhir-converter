/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.api.Variable;
import com.ibm.whi.core.expression.EmptyEvaluationResult;


@JsonIgnoreProperties(ignoreUnknown = true)
public class JELXExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(JELXExpression.class);

  private String evaluate;




  @JsonCreator
  public JELXExpression(@JsonProperty("type") String type,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("hl7spec") String hl7spec, @JsonProperty("evaluate") String evaluate,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition,
      @JsonProperty("constants") Map<String, String> constants) {
    super(type, defaultValue, required, hl7spec, variables, condition, constants);
    Preconditions.checkArgument(StringUtils.isNotBlank(evaluate), "evaluate file cannot be blank");

    this.evaluate = evaluate;


  }


  public JELXExpression(String evaluate, Map<String, String> variables) {
    this("String", null, false, null, evaluate, variables, null, null);


  }


  @Override
  public EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValues) {
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    for (Variable v : this.getVariables()) {
      if (!localContextValues.containsKey(v.getVariableName())) {
        localContextValues.put(v.getVariableName(), new EmptyEvaluationResult());
      }
    }
    return dataSource.evaluateJexlExpression(this.evaluate, contextValues);
  }


  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this.getClass().getSimpleName()).append("hl7spec", this.getspecs())
        .append("isMultiple", this.isMultiple()).append("variables", this.getVariables())
        .append("evaluate", this.evaluate).build();
  }


}
