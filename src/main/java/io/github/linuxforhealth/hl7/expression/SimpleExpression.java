/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.VariableUtils;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.data.ValueExtractor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleExpression.class);

  private String value;



  @JsonCreator
  public SimpleExpression(ExpressionAttributes expAttr) {
    super(expAttr);
    this.value = expAttr.getValue();
    if (StringUtils.isBlank(value)) {
      this.value = expAttr.getValueOf();
    }
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
    Object resolvedValue = null;
    if (VariableUtils.isVar(value)) {
      EvaluationResult obj =
          getVariableValueFromVariableContextMap(value, ImmutableMap.copyOf(localContextValues));
      if (obj != null && !obj.isEmpty()) {
        resolvedValue = obj.getValue();
      }

    } else {
      LOGGER.debug("Evaluated {} returning value enclosed as GenericResult.", this.value);
      resolvedValue = this.value;
    }
    LOGGER.debug("Evaluated value {} to {} ", this.value, resolvedValue);
    if (resolvedValue != null) {

      return getValueOfSpecifiedType(resolvedValue);
    } else {
      LOGGER.debug("Evaluated {} returning null", this.value);
      return null;
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


}
