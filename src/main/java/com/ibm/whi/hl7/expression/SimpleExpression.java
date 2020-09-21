/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.core.message.InputData;
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
  public GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult hl7SpecValues) {

    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Map<String, GenericResult> localContextValues = new HashMap<>(contextValues);
    if (hl7SpecValues != null && !hl7SpecValues.isEmpty()) {
      localContextValues.put(hl7SpecValues.getKlassName(), hl7SpecValues);
    }
    LOGGER.info("Evaluating {}", this.value);
    if (isVar(value)) {
      GenericResult obj =
          getVariableValueFromVariableContextMap(value, ImmutableMap.copyOf(localContextValues));
      LOGGER.info("Evaluated value {} to {} ", this.value, obj);
      if (obj != null) {
        LOGGER.info("Evaluated value {} to {} type {} ", this.value, obj, obj.getClass());
        ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
        return new GenericResult(resolver.apply(obj.getValue()));
      }
      LOGGER.info("Evaluated {} returning null", this.value);
      return null;
    } else {
      LOGGER.info("Evaluated {} returning value enclosed as GenericResult.", this.value);
      return new GenericResult(this.value);
    }
  }

  private static GenericResult getVariableValueFromVariableContextMap(String varName,
      ImmutableMap<String, GenericResult> contextValues) {
    if (StringUtils.isNotBlank(varName)) {
      GenericResult fetchedValue;
      fetchedValue = contextValues.get(VariableUtils.getVarName(varName));
      return fetchedValue;
    } else {
      return null;
    }
  }


}
