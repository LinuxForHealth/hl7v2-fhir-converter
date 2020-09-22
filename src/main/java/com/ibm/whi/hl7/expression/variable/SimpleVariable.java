/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression.variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.core.message.InputData;


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

  public SimpleVariable(String name, List<String> spec) {
    this(name, spec, false);
  }

  public SimpleVariable(String name, List<String> spec, boolean extractMultiple) {
    this.name = name;
    this.spec = new ArrayList<>();
    if (spec != null && !spec.isEmpty()) {
      this.spec.addAll(spec);
    }
    this.extractMultiple = extractMultiple;
  }

  public List<String> getSpec() {
    return new ArrayList<>(spec);
  }

  public String getType() {
    return OBJECT_TYPE;
  }


  public String getName() {
    return name;
  }





  // resolve variable value

  public GenericResult extractVariableValue(Map<String, GenericResult> contextValues,
      InputData dataSource) {
    GenericResult result;
    if (!this.spec.isEmpty()) {
      result = getValueFromSpecs(contextValues, dataSource);
    } else {
      result = null;
    }

    return result;

  }


  protected GenericResult getValueFromSpecs(Map<String, GenericResult> contextValues,
      InputData dataSource) {
    GenericResult fetchedValue = null;
    for (String specValue : this.spec) {
      if (VariableUtils.isVar(specValue)) {
        fetchedValue =
            getVariableValueFromVariableContextMap(specValue, ImmutableMap.copyOf(contextValues));
      } else {
        GenericResult gen;
        if (this.extractMultiple) {
          gen =
              dataSource.extractMultipleValuesForSpec(Lists.newArrayList(specValue), contextValues);
        } else {
          gen = dataSource.extractSingleValueForSpec(Lists.newArrayList(specValue), contextValues);
        }
        if (gen != null && !gen.isEmpty()) {
          fetchedValue = gen;
      }
    }
      // break the loop and return
      if (fetchedValue != null) {
        return fetchedValue;
      }

  }
    return fetchedValue;
  }


  private static GenericResult getVariableValueFromVariableContextMap(String varName,
      ImmutableMap<String, GenericResult> varables) {
    if (StringUtils.isNotBlank(varName)) {
      GenericResult fetchedValue;
      fetchedValue = varables.get(varName.replace("$", ""));
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
   * Return if variable value should extracted from all repititions of the spec
   * 
   * @return
   */
  public boolean extractMultiple() {
    return this.extractMultiple;
  }


}
