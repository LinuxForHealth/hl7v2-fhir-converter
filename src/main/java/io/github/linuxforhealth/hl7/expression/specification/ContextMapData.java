package io.github.linuxforhealth.hl7.expression.specification;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.core.expression.VariableUtils;

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
  //BJCBJC
  @Override
  public EvaluationResult extractMultipleValuesForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    SimpleSpecification simpleSpec = (SimpleSpecification) spec;
    if (simpleSpec.getVariable().contains("servation")) {
      List multiple = new ArrayList();
      for (Map.Entry<String, EvaluationResult> entry : contextValues.entrySet()) {
        if (entry.getKey().contains("Observation")){
          multiple.add(entry.getValue());
        }
      }
      if (multiple.size()>0){
        return new SimpleEvaluationResult<>(multiple);
      }
    } 
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
