/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.hl7.expression.specification.SimpleSpecification;
import io.github.linuxforhealth.hl7.util.ExpressionUtility;

public class ContextValueUtils {



  public static Object getSingleValue(Object object) {
    if (object instanceof List) {
      List value = (List) object;
      if (value.isEmpty()) {
        return null;
      } else {
        return value.get(0);
      }

    }
    return object;
  }


  public static String getGroupId(Map<String, EvaluationResult> localContext) {
    EvaluationResult result = localContext.get(Constants.GROUP_ID);
    if (result != null) {
      return (String) result.getValue();
    }
    return null;
  }


  public static String getKeyName(Map<String, EvaluationResult> contextValues, String key,
      boolean useGroup) {

    EvaluationResult result = contextValues.get(Constants.USE_GROUP);
    if (result != null && !result.isEmpty()) {
      useGroup = result.getValue();
    }
    if (useGroup) {
      String groupId = getGroupId(contextValues);
      return key + "_" + groupId;
    } else {
      return key;
    }

  }




  public static EvaluationResult getVariableValuesFromVariableContextMap(String varName,
      Map<String, EvaluationResult> contextValues, boolean isUseGroup, boolean fuzzyMatch) {
    Preconditions.checkArgument(!(isUseGroup && fuzzyMatch),
        "Both use group and fuzzyMatch cannot be true");
    if (StringUtils.isNotBlank(varName)) {
      EvaluationResult fetchedValue;
      if (varName.startsWith("$") && varName.contains(":")) {
        return fetchValueFromVar(varName, contextValues, isUseGroup);
      } else {

        if (fuzzyMatch) {
          fetchedValue = getPrefixedValues(VariableUtils.getVarName(varName), contextValues);
        } else if (isUseGroup) {
          String keyname = getKeyName(contextValues, VariableUtils.getVarName(varName), isUseGroup);
          fetchedValue = contextValues.get(keyname);
          if (fetchedValue == null) {
            fetchedValue = contextValues.get(VariableUtils.getVarName(varName));
          }
        } else {

          fetchedValue = contextValues.get(VariableUtils.getVarName(varName));
        }
      }


      return fetchedValue;
    } else {
      return null;
    }
  }

  private static EvaluationResult getPrefixedValues(String keyname,
      Map<String, EvaluationResult> contextValues) {
    List<Object> obj = contextValues.entrySet().stream()
        .filter(
            e -> e.getKey().startsWith(keyname) && e.getValue() != null && !e.getValue().isEmpty())
        .map(e -> e.getValue().getValue()).collect(Collectors.toList());
    return EvaluationResultFactory.getEvaluationResult(obj);
  }

  private static EvaluationResult fetchValueFromVar(String varName,
      Map<String, EvaluationResult> contextValues, boolean isUseGroup) {
    String[] tokens = StringUtils.split(varName, ":", 2);
    ImmutablePair<String, String> fetch = ImmutablePair.of(tokens[0], tokens[1]);
    String keyname = getKeyName(contextValues, VariableUtils.getVarName(fetch.left), isUseGroup);

    EvaluationResult resource = contextValues.get(keyname);

    return ExpressionUtility.extractComponent(fetch, resource);
  }


  public static EvaluationResult getVariableValueFromVariableContextMap(
      SimpleSpecification simpleSpec, Map<String, EvaluationResult> contextValues) {

    return getVariableValuesFromVariableContextMap(simpleSpec.getVariable(), contextValues,
        simpleSpec.isUseGroup(), simpleSpec.isFuzzyMatch());
  }






}
