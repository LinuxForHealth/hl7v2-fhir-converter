/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

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
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.VariableUtils;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.data.ValueExtractor;
import io.github.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleExpression.class);

  private String value;


  @JsonCreator
  public SimpleExpression(String var) {
    this("String", var, new HashMap<>(), null, false);

  }

  /**
   * 
   * @param type
   * @param value
   * @param variables
   * @param condition
   */
  @JsonCreator
  public SimpleExpression(@JsonProperty(TemplateFieldNames.TYPE) String type,
      @JsonProperty(TemplateFieldNames.VALUE) String value,
      @JsonProperty(TemplateFieldNames.VARIABLES) Map<String, String> variables,
      @JsonProperty(TemplateFieldNames.CONDITION) String condition,
      @JsonProperty(TemplateFieldNames.USE_GROUP) boolean useGroup) {
    super(type, null, false, "", variables, condition, null, useGroup);
    this.value = value;
  }


  public String getValue() {
    return value;
  }


  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {

    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    if (baseValue != null && !baseValue.isEmpty()) {
      localContextValues.put(baseValue.getIdentifier(), baseValue);
      localContextValues.put(Constants.BASE_VALUE_NAME, baseValue);
    }

    if (VariableUtils.isVar(value)) {
      EvaluationResult obj =
          getVariableValueFromVariableContextMap(value, ImmutableMap.copyOf(localContextValues));
      LOGGER.debug("Evaluated value {} to {} ", this.value, obj);
      if (obj != null) {

        return getValueOfSpecifiedType(obj.getValue());
      } else {
        LOGGER.debug("Evaluated {} returning null", this.value);
        return null;
      }

    } else {
      LOGGER.debug("Evaluated {} returning value enclosed as GenericResult.", this.value);
      return getValueOfSpecifiedType(this.value);
    }
  }

  private EvaluationResult getValueOfSpecifiedType(Object obj) {
    if (obj != null) {
      LOGGER.debug("Evaluated value {} to {} type {} ", this.value, obj, obj.getClass());
      ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
      return EvaluationResultFactory.getEvaluationResult(resolver.apply(obj));
    } else {
      LOGGER.debug("Evaluated {} returning null", this.value);
      return null;
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
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .append(TemplateFieldNames.VALUE, this.value).build();
  }



}
