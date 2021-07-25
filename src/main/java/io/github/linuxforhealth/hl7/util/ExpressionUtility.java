/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.exception.DataExtractionException;
import io.github.linuxforhealth.core.exception.RequiredConstraintFailureException;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.hl7.resource.ResourceEvaluationResult;


public class ExpressionUtility {

  private static final String KEY_NAME_SUFFIX = "KEY_NAME_SUFFIX";


  private static final String EVALUATING = "Evaluating {} {}";


  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionUtility.class);

  private ExpressionUtility() {}

  /**
   * Evaluates map of expression and generates ResourceEvaluationResult object.
   * 
   * @param dataSource
   * @param context
   * @param baseValue
   * @param expressionMap
   * @return {@link ResourceEvaluationResult}
   */
  public static ResourceEvaluationResult evaluate(InputDataExtractor dataSource,
      Map<String, EvaluationResult> context, EvaluationResult baseValue,
      Map<String, Expression> expressionMap) {

    try {


      Map<String, EvaluationResult> localContext = new HashMap<>(context);
      // initialize the map and list to collect values
      List<ResourceValue> additionalResolveValues = new ArrayList<>();
      Map<String, Object> resolveValues = new HashMap<>();


      for (Entry<String, Expression> entry : expressionMap.entrySet()) {

        Expression exp = entry.getValue();
        LOGGER.debug(EVALUATING, entry.getKey(), entry.getValue());
        EvaluationResult obj = exp.evaluate(dataSource, localContext, baseValue);
        LOGGER.debug("Evaluated {} {} value returned {} ", entry.getKey(), entry.getValue(), obj);

        if (obj != null && !obj.isEmpty()) {
          String keyNameSuffix = getKeyNameSuffix(localContext);
          // Check if the key already exist in the HashMap, if found append, do not replace
          if (!resolveValues.containsKey(getKeyName(entry.getKey(), keyNameSuffix))) {
            resolveValues.put(getKeyName(entry.getKey(), keyNameSuffix), obj.getValue());
          } else {
            Object existing = resolveValues.get(getKeyName(entry.getKey(), keyNameSuffix));
            if (existing instanceof List) {
              if (obj.getValue() instanceof List) {
                ((List<Object>) existing).addAll(obj.getValue());
              } else {
                ((List<Object>) existing).add(obj.getValue());
              }
            }
          }
          if (obj.getAdditionalResources() != null && !obj.getAdditionalResources().isEmpty()) {
            additionalResolveValues.addAll(obj.getAdditionalResources());
          }
        }
      }



      resolveValues.values().removeIf(Objects::isNull);

      return new ResourceEvaluationResult(resolveValues, additionalResolveValues);


    } catch (RequiredConstraintFailureException e) {
      LOGGER.warn("Resource Constraint condition not satisfied , exception {}", e.getMessage());
      LOGGER.debug("Resource Constraint condition not satisfied, exception", e);
      return null;

    } catch (IllegalArgumentException | IllegalStateException | DataExtractionException e) {
      LOGGER.error("Exception during  resource evaluation reason ", e);
      return null;

    }

  }





  private static String getKeyName(String key, String suffix) {
    String[] keyComponents = StringUtils.split(key, "_", 2);
    if (keyComponents.length == 2 && KEY_NAME_SUFFIX.equalsIgnoreCase(keyComponents[1])) {
      return keyComponents[0] + suffix;
    } else {
      return keyComponents[0];
    }



  }



  private static String getKeyNameSuffix(Map<String, EvaluationResult> localContext) {
    EvaluationResult res = localContext.get(KEY_NAME_SUFFIX);
    if (res == null || res.isEmpty()) {
      return null;
    }
    return res.getValue();
  }


  public static EvaluationResult extractComponent(ImmutablePair<String, String> fetch,
      EvaluationResult resource) {
    if (resource != null && resource.getValue() instanceof ResourceValue) {
      ResourceValue rv = resource.getValue();
      Map<String, Object> resourceMap = rv.getResource();
      return EvaluationResultFactory.getEvaluationResult(resourceMap.get(fetch.getValue()));
    } else if (resource != null && resource.getValue() instanceof Map) {
      Map<String, Object> resourceMap = (Map<String, Object>) resource.getValue();
      return EvaluationResultFactory.getEvaluationResult(resourceMap.get(fetch.getValue()));
    } else if (resource != null && !resource.isEmpty()) {
      Map<String, Object> resourceMap =
          ObjectMapperUtil.getJSONInstance().convertValue(resource.getValue(), Map.class);
      return EvaluationResultFactory.getEvaluationResult(resourceMap.get(fetch.getValue()));
    } else {
      return new EmptyEvaluationResult();
    }
  }



}
