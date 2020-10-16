package com.ibm.linuxforhealth.hl7.expression.specification;

import java.util.List;
import java.util.Map;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.api.InputDataExtractor;
import com.ibm.linuxforhealth.api.Specification;
import com.ibm.linuxforhealth.core.Constants;
import com.ibm.linuxforhealth.core.expression.EmptyEvaluationResult;
import com.ibm.linuxforhealth.core.expression.SimpleEvaluationResult;
import com.ibm.linuxforhealth.core.expression.VariableUtils;

public class ContextMapData implements InputDataExtractor {




  @Override
  public EvaluationResult evaluateJexlExpression(String expression,
      Map<String, EvaluationResult> contextValues) {
    throw new IllegalStateException("No supported for this input source type");
  }

  @Override
  public String getName() {
    return "ContextMap";
  }

  @Override
  public String getId() {
    throw new IllegalStateException("No supported for this input source type");
  }

  @Override
  public EvaluationResult extractValueForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    EvaluationResult fetchedValue = this.extractMultipleValuesForSpec(spec, contextValues);
    if (fetchedValue != null && !fetchedValue.isEmpty()) {
      return new SimpleEvaluationResult<>(getSingleValue(fetchedValue.getValue()));
    } else {
      return new EmptyEvaluationResult();
    }
  }

  @Override
  public EvaluationResult extractMultipleValuesForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    SimpleSpecification simpleSpec = (SimpleSpecification) spec;
    EvaluationResult res = contextValues
        .get(getKeyName(contextValues, VariableUtils.getVarName(simpleSpec.getVariable())));
    if (res != null && !res.isEmpty()) {
      return res;
    } else {
      return new EmptyEvaluationResult();
    }

  }


  private static Object getSingleValue(Object object) {
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


  private static String getGroupId(Map<String, EvaluationResult> localContext) {
    EvaluationResult result = localContext.get(Constants.GROUP_ID);
    if (result != null) {
      return (String) result.getValue();
    }
    return null;
  }


  private static String getKeyName(Map<String, EvaluationResult> contextValues, String key) {
    boolean useGroup = false;
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
}
