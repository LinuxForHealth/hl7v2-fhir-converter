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
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.data.ValueExtractor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleExpression.class);

  private String value;


  @JsonCreator
  public SimpleExpression(String var) {
    this("String", var, new HashMap<>(), null);

  }

  @JsonCreator
  public SimpleExpression(@JsonProperty("type") String type, @JsonProperty("value") String value,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition) {
    super(type, null, false, "", variables, condition);
    this.value = value;
  }


  public String getValue() {
    return value;
  }


  @Override
  public EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValues) {

    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    if (hl7SpecValues != null && !hl7SpecValues.isEmpty()) {
      localContextValues.put(hl7SpecValues.getName(), hl7SpecValues);
    }

    if (VariableUtils.isVar(value)) {
      EvaluationResult obj =
          getVariableValueFromVariableContextMap(value, ImmutableMap.copyOf(localContextValues));
      LOGGER.debug("Evaluated value {} to {} ", this.value, obj);
      if (obj != null) {
        LOGGER.debug("Evaluated value {} to {} type {} ", this.value, obj, obj.getClass());
        ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
        return EvaluationResultFactory.getEvaluationResult(resolver.apply(obj.getValue()));
      }
      LOGGER.debug("Evaluated {} returning null", this.value);
      return null;
    } else {
      LOGGER.debug("Evaluated {} returning value enclosed as GenericResult.", this.value);
      return EvaluationResultFactory.getEvaluationResult(this.value);
    }
  }

  private static EvaluationResult getVariableValueFromVariableContextMap(String varName,
      ImmutableMap<String, EvaluationResult> contextValues) {
    if (StringUtils.isNotBlank(varName)) {
      EvaluationResult fetchedValue;
      fetchedValue = contextValues.get(VariableUtils.getVarName(varName));
      return fetchedValue;
    } else {
      return null;
    }
  }



  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this.getClass().getSimpleName()).append("hl7spec", this.getspecs())
        .append("isMultiple", this.isMultiple()).append("variables", this.getVariables())
        .append("value", this.value).build();
  }
}
