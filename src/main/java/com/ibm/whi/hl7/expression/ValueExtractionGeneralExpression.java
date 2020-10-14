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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputDataExtractor;
import com.ibm.whi.api.ResourceValue;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.expression.EmptyEvaluationResult;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.hl7.resource.deserializer.TemplateFieldNames;


/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueExtractionGeneralExpression extends AbstractExpression {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueExtractionGeneralExpression.class);


  private ImmutablePair<String, String> fetch;


  /**
   * 
   * @param type
   * @param fetch
   * @param specs
   * @param defaultValue
   * @param required
   * @param variables
   * @param condition
   * @param constants
   * @param useGroup
   */
  @JsonCreator
  public ValueExtractionGeneralExpression(@JsonProperty(TemplateFieldNames.TYPE) String type,
      @JsonProperty(TemplateFieldNames.FETCH) String fetch,
      @JsonProperty(TemplateFieldNames.SPEC) String specs,
      @JsonProperty(TemplateFieldNames.DEFAULT_VALUE) String defaultValue,
      @JsonProperty(TemplateFieldNames.REQUIRED) boolean required,
      @JsonProperty(TemplateFieldNames.VARIABLES) Map<String, String> variables,
      @JsonProperty(TemplateFieldNames.CONDITION) String condition,
      @JsonProperty(TemplateFieldNames.CONSTANTS) Map<String, String> constants,
      @JsonProperty(TemplateFieldNames.USE_GROUP) boolean useGroup) {
    super(type, defaultValue, required, specs, variables, condition, constants, useGroup);

    this.fetch = get(fetch);


  }

  /**
   * 
   * @param type
   * @param fetch
   * @param hl7spec
   */

  public ValueExtractionGeneralExpression(String type, String fetch, String hl7spec) {
    this(type, fetch, hl7spec, null, false, null, null, null, false);
  }



  /**
   * Any expression that needs to extract value from another object will use fetch field.
   * example:'$ref-type:id' The expression will try and extract the value of the variable $ref-type
   * from the context.
   * 
   * 
   */

  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult basevalue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");

    Map<String, EvaluationResult> resolvedVariables = new HashMap<>(contextValues);
    EvaluationResult resource;
    if (Constants.BASE_VALUE_NAME.equals(fetch.getKey())) {
      resource = basevalue;
    } else {
      resource = resolvedVariables.get(getKeyName(contextValues));
    }
    if (resource != null && resource.getValue() instanceof Map) {
      Map<String, Object> resourceMap = (Map<String, Object>) resource.getValue();
      return EvaluationResultFactory.getEvaluationResult(resourceMap.get(fetch.getValue()));
    } else if (resource != null && resource.getValue() instanceof ResourceValue) {
      ResourceValue rv = resource.getValue();
      Map<String, Object> resourceMap = rv.getResource();
      return EvaluationResultFactory.getEvaluationResult(resourceMap.get(fetch.getValue()));
    } else {
      return new EmptyEvaluationResult();
      }


  }

  private String getKeyName(Map<String, EvaluationResult> contextValues) {

    if (this.isUseGroup()) {
      String groupId = getGroupId(contextValues);
      return fetch.getKey() + "_" + groupId;
    } else {
      return fetch.getKey();
    }

  }



  private static ImmutablePair<String, String> get(String tok) {
    String[] token = tok.split(":");
    if (token.length == 2) {
      return new ImmutablePair<>(VariableUtils.getVarName(token[0]), token[1]);
    } else if (token.length == 1) {
      return new ImmutablePair<>(Constants.BASE_VALUE_NAME, token[0]);
    }
    throw new IllegalArgumentException(
        "fetch token not in correct format, expected format $varName:key, input" + tok);
  }
  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .append(TemplateFieldNames.FETCH, this.fetch)
        .append(TemplateFieldNames.USE_GROUP, this.isUseGroup()).build();
  }



}
