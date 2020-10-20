package io.github.linuxforhealth.hl7.expression.specification;

import java.util.List;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;

public class SpecificationUtil {

  private SpecificationUtil() {}
  public static EvaluationResult extractValueForSpec(List<Specification> hl7specs, InputDataExtractor source,
      Map<String, EvaluationResult> contextValues) {

    EvaluationResult fetchedValue = null;
    for (Specification spec : hl7specs) {

      fetchedValue = spec.extractValueForSpec(source, contextValues);
        // break the loop and return
        if (fetchedValue != null && !fetchedValue.isEmpty()) {
        return fetchedValue;
        }

    }
    return new EmptyEvaluationResult();


  }




  public static EvaluationResult extractMultipleValuesForSpec(List<Specification> hl7specs,
      InputDataExtractor source, Map<String, EvaluationResult> contextValues) {
    EvaluationResult fetchedValue = null;
    for (Specification spec : hl7specs) {

      fetchedValue = spec.extractMultipleValuesForSpec(source, contextValues);
        // break the loop and return
        if (fetchedValue != null && !fetchedValue.isEmpty()) {
          return fetchedValue;
        }

    }
    return new EmptyEvaluationResult();

  }
}
