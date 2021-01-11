/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;


/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */

public class ValueExtractionGeneralExpression extends AbstractExpression {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ValueExtractionGeneralExpression.class);


  private ImmutablePair<String, String> fetch;

  private boolean isUseGroup;


  @JsonCreator
  public ValueExtractionGeneralExpression(ExpressionAttributes expAttr) {
    super(expAttr);
    this.fetch = expAttr.getFetch();
    this.isUseGroup = expAttr.isUseGroup();
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

    if (this.isUseGroup) {
      String groupId = getGroupId(contextValues);
      return fetch.getKey() + "_" + groupId;
    } else {
      return fetch.getKey();
    }

  }



}
