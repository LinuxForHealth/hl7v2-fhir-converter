/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.variable;

import java.util.List;
import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.data.ValueExtractor;


/**
 * Defines variable type that supports extracting values from spec to a particular data type.
 * Supported data types are listed here {@link SimpleDataTypeMapper}
 *
 * @author pbhallam
 */
public class DataTypeVariable extends SimpleVariable {

  private String valueType;
  private ValueExtractor<Object, ?> resolver;


  /**
   * 
   * @param name
   * @param valueType
   * @param spec
   */

  public DataTypeVariable(String name, String valueType, List<String> spec,
      boolean extractMultiple) {
    super(name, spec, extractMultiple);

    this.valueType = valueType;
    this.resolver = SimpleDataTypeMapper.getValueResolver(this.valueType);
  }



  // resolve variable value
  @Override
  public EvaluationResult extractVariableValue(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource) {
    EvaluationResult result;
    if (!this.getSpec().isEmpty()) {
      result = getValueFromSpecs(contextValues, dataSource);
    } else {
      result = null;
    }
    EvaluationResult resolvedvalue = null;
    if (result != null && this.resolver != null) {
      resolvedvalue =
          EvaluationResultFactory.getEvaluationResult(this.resolver.apply(result.getValue()));
    } else {
      resolvedvalue = result;
    }

    return resolvedvalue;

  }

  public String getValueType() {
    return valueType;
  }



  public ValueExtractor<Object, ?> getResolver() {
    return resolver;
  }



}
