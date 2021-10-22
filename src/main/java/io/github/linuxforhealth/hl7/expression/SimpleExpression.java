/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import io.github.linuxforhealth.hl7.util.ExpressionUtility;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleExpression.class);

  private String value;
  private ImmutablePair<String, String> fetch;


  @JsonCreator
  public SimpleExpression(ExpressionAttributes expAttr) {
    super(expAttr);
    this.value = expAttr.getValue();
    if (StringUtils.isBlank(value)) {
      this.value = expAttr.getValueOf();
    }
    if (this.value != null && this.value.startsWith("$") && this.value.contains(":")) {
      String[] tokens = StringUtils.split(this.value, ":", 2);
      this.fetch = ImmutablePair.of(tokens[0], tokens[1]);
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
    
    /**
     * If the expression has fetch pair populated then use that for evaluation.
     */
    if(this.fetch!=null) {
      return evaluateExpressionForFetch(localContextValues, baseValue);
    }
    
    
    Object resolvedValue = null;
    if (VariableUtils.isVar(value)) {
      if (this.getExpressionAttr().isFuzzyGroup()){
        LOGGER.debug("Getting Value for varname {}",value);
      }
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


  /**
   * Any expression that needs to extract value from another object will use fetch field.
   * example:'$ref-type:id' The expression will try and extract the value of the variable $ref-type
   * from the context.
   * 
   * 
   */


  private EvaluationResult evaluateExpressionForFetch(Map<String, EvaluationResult> contextValues,
      EvaluationResult basevalue) {


    EvaluationResult resource;
    if (Constants.BASE_VALUE_NAME.equals(fetch.getKey())) {
      resource = basevalue;
    } else {
      resource = contextValues.get(getKeyName(contextValues));
    }
    return ExpressionUtility.extractComponent(fetch, resource);


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


  private String getKeyName(Map<String, EvaluationResult> contextValues) {

    if (this.getExpressionAttr().isUseGroup()) {
      String groupId = getGroupId(contextValues);
      return VariableUtils.getVarName(fetch.getKey()) + "_" + groupId;
    } else {
      return VariableUtils.getVarName(fetch.getKey());
    }

  }


}
