/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.VariableUtils;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationParser;


/**
 * Defines Variable object that can be used during the expression evaluation.
 * 
 *
 * @author pbhallam
 */
public class SimpleVariable implements Variable {
  public static final String OBJECT_TYPE = Object.class.getSimpleName();

  private String name;
  private List<String> spec;
  private boolean extractMultiple;
  private boolean combineMultiple;

  public SimpleVariable(String name, List<String> spec) {
    this(name, spec, false, false);
  }

  public SimpleVariable(String name, List<String> spec, boolean extractMultiple,
      boolean combineMultiple) {
    this.name = name;
    this.spec = new ArrayList<>();
    if (spec != null && !spec.isEmpty()) {
      this.spec.addAll(spec);
    }
    this.extractMultiple = extractMultiple;
    this.combineMultiple = combineMultiple;
  }

  @Override
  public List<String> getSpec() {
    return new ArrayList<>(spec);
  }

  @Override
  public String getType() {
    return OBJECT_TYPE;
  }


  public String getName() {
    return name;
  }





  // resolve variable value

  @Override
  public EvaluationResult extractVariableValue(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource) {
    EvaluationResult result;
    if (!this.spec.isEmpty()) {
      List<EvaluationResult> values =
          getValuesFromSpecs(contextValues, dataSource, combineMultiple);
      if (values.isEmpty()) {
        result = null;
      } else if (values.size() == 1) {
        result = values.get(0);
      } else {
        result = generateCombinedValue(values);
      }
    } else {
      result = null;
    }

    return result;

  }


  private static EvaluationResult generateCombinedValue(List<EvaluationResult> values) {
    StringBuilder sb = new StringBuilder();
    for (EvaluationResult value : values) {
      if (value.getValue() != null) {
        sb.append(SimpleDataTypeMapper.getValueResolver("STRING").apply(value.getValue()));
      }
    }
    return EvaluationResultFactory.getEvaluationResult(sb.toString());
  }

  protected EvaluationResult getValueFromSpecs(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource) {
    List<EvaluationResult> values = getValuesFromSpecs(contextValues, dataSource, false);
    if (values.isEmpty()) {
      return null;
    } else {
      return values.get(0);
    }
  }

  protected List<EvaluationResult> getValuesFromSpecs(Map<String, EvaluationResult> contextValues,
      InputDataExtractor dataSource, boolean fetchAll) {
    List<EvaluationResult> combineValue = new ArrayList<>();
    for (String specValue : this.spec) {
      EvaluationResult fetchedValue = null;
      if (VariableUtils.isVar(specValue)) {
        fetchedValue =
            getVariableValueFromVariableContextMap(specValue, ImmutableMap.copyOf(contextValues));
      } else {
        EvaluationResult gen;
        Specification hl7spec = SpecificationParser.parse(specValue, this.extractMultiple, false);

        gen = hl7spec.extractValueForSpec(dataSource, contextValues);

        if (gen != null && !gen.isEmpty()) {
          fetchedValue = gen;
      }
    }
      // break the loop and return
      if (fetchedValue != null) {
        combineValue.add(fetchedValue);
        if (!fetchAll) {
          return combineValue;
        }
      }

  }
    return combineValue;
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




  @Override
  public String getVariableName() {
    return VariableUtils.getVarName(this.name);
  }

  /**
   * Return if variable value should extracted from all repetitions of the spec
   * 
   * @return boolean
   */
  public boolean extractMultiple() {
    return this.extractMultiple;
  }


}
