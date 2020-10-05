/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
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
import com.ibm.whi.core.expression.EmptyEvaluationResult;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.expression.VariableUtils;


/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueExtractionGeneralExpression extends AbstractExpression {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueExtractionGeneralExpression.class);


  private String fetch;



  /**
   * 
   * @param type
   * @param reference
   * @param hl7prefix
   * @param defaultValue
   * @param required
   * @param variables
   * @param constants
   */
  @JsonCreator
  public ValueExtractionGeneralExpression(@JsonProperty("type") String type,
      @JsonProperty("fetch") String fetch, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition,
      @JsonProperty("constants") Map<String, String> constants) {
    super(type, defaultValue, required, hl7spec, variables, condition, constants);
    Preconditions.checkArgument(fetch != null && fetch.split(":").length >= 2,
        "value of fetch should include name of the resource and field. ");


    this.fetch = fetch;

  }

  /**
   * 
   * @param type
   * @param fetch
   * @param hl7spec
   */

  public ValueExtractionGeneralExpression(String type, String fetch, String hl7spec) {
    this(type, fetch, hl7spec, null, false, null, null, null);
  }



  /**
   * Any expression that needs to extract value from another object will use fetch field.
   * example:'$ref-type:id' The expression will try and extract the value of the variable $ref-type
   * from the context.
   * 
   * 
   * @see com.ibm.whi.api.Expression#execute(java.util.Map)
   */

  @Override
  public EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");

    Map<String, EvaluationResult> resolvedVariables = new HashMap<>(contextValues);

    Map<String, Object> localContext = new HashMap<>();
    resolvedVariables
        .forEach((key, value) -> localContext.put(key, value.getValue()));
    String[] token = fetch.split(":");
    if (token.length == 2) {
      Object resource = localContext.get(VariableUtils.getVarName(token[0]));
    if (resource instanceof Map) {
      Map<String, Object> resourceMap = (Map<String, Object>) resource;
        return EvaluationResultFactory.getEvaluationResult(resourceMap.get(token[1]));
      }
    }
    return new EmptyEvaluationResult();
  }

  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this.getClass().getSimpleName()).append("hl7spec", this.getspecs())
        .append("isMultiple", this.isMultiple()).append("variables", this.getVariables())
        .append("fetch", this.fetch).build();
  }
}
