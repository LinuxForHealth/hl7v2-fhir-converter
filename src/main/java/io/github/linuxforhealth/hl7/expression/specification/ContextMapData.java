package io.github.linuxforhealth.hl7.expression.specification;

import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.expression.ContextValueUtils;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;

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
      return new SimpleEvaluationResult<>(ContextValueUtils.getSingleValue(fetchedValue.getValue()));
    } else {
      return new EmptyEvaluationResult();
    }
  }

  @Override
  public EvaluationResult extractMultipleValuesForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    EvaluationResult res;
    if (spec instanceof SimpleSpecification) {
    SimpleSpecification simpleSpec = (SimpleSpecification) spec;
      res = ContextValueUtils.getVariableValueFromVariableContextMap(simpleSpec, contextValues);
    } else {
      res = null;
    }
    if (res != null && !res.isEmpty()) {
      return res;
    } else {
      return new EmptyEvaluationResult();
    }

  }


 


 
}