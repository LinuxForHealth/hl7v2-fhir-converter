package com.ibm.linuxforhealth.hl7.expression.specification;

import java.util.List;
import java.util.Map;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.api.InputDataExtractor;
import com.ibm.linuxforhealth.api.Specification;
import com.ibm.linuxforhealth.core.expression.EmptyEvaluationResult;

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
