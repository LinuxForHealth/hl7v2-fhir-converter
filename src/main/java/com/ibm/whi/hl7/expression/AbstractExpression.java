/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.data.DataTypeUtil;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.core.expression.condition.Condition;
import com.ibm.whi.core.expression.condition.ConditionUtil;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.core.resource.ResourceValue;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.exception.RequiredConstraintFailureException;
import com.ibm.whi.hl7.expression.variable.VariableGenerator;

public abstract class AbstractExpression implements Expression {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpression.class);
  public static final String OBJECT_TYPE = Object.class.getSimpleName();
  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private final String type;
  private final GenericResult defaultValue;
  private final boolean required;
  private final List<String> hl7specs;
  private List<Variable> variables;
  private Condition condition;
  private boolean isMultiple;

  public AbstractExpression(String type, Object defaultValue, boolean required, String hl7spec,
      Map<String, String> rawvariables, String condition) {
    if (type == null) {
      this.type = OBJECT_TYPE;
    } else {
      this.type = type;
    }
    if (defaultValue != null) {
      this.defaultValue = new GenericResult(defaultValue);
    } else {
      this.defaultValue = null;
    }

    this.required = required;
    this.hl7specs = getTokens(hl7spec);
    if (StringUtils.isNotBlank(condition)) {
      this.condition = ConditionUtil.createCondition(condition);
    }

    initVariables(rawvariables);

  }

  private void initVariables(Map<String, String> rawvariables) {
    this.variables = new ArrayList<>();
    if (rawvariables != null) {
      for (Entry<String, String> e : rawvariables.entrySet()) {

        this.variables.add(VariableGenerator.parse(e.getKey(), e.getValue()));


      }

    }
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public GenericResult getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  @Override
  public List<String> getspecs() {
    return new ArrayList<>(this.hl7specs);
  }

  public List<Variable> getVariables() {
    return new ArrayList<>(variables);
  }





  public GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    if (this.isMultiple) {
      return evaluateMultiple(dataSource, contextValues);
    } else {
      return evaluateSingle(dataSource, contextValues);
    }
  }



  private GenericResult evaluateSingle(InputData dataSource,
      Map<String, GenericResult> contextValues) {
    String stringRep = this.toString();
    // resolve hl7spec
    LOGGER.info("Evaluating expression {}", stringRep);
    GenericResult hl7Value = dataSource.extractSingleValueForSpec(this.getspecs(),
        ImmutableMap.copyOf(contextValues));
    LOGGER.info("Evaluating expression {} returned hl7 value {} ", stringRep, hl7Value);
    GenericResult gen = generateValue(dataSource, contextValues, hl7Value);
      if (this.isRequired() && (gen == null || gen.isEmpty())) {
        throw new RequiredConstraintFailureException(
          "Failure in Evaluating expression   :" + stringRep);
      } else {
        return gen;
      }

  }

  private GenericResult evaluateMultiple(InputData dataSource,
      Map<String, GenericResult> contextValues) {
    String stringRep = this.toString();
    // resolve hl7spec
    LOGGER.info("Evaluating multiple values for expression {} ", stringRep);
    GenericResult hl7Values = dataSource.extractMultipleValuesForSpec(this.getspecs(),
        ImmutableMap.copyOf(contextValues));
    if (hl7Values == null || hl7Values.getValue() == null
        || !(hl7Values.getValue() instanceof List)) {
      return null;
    }


    List<Object> result = new ArrayList<>();
    List<ResourceValue> additionalresourcesresult = new ArrayList<>();
    List<Object> baseHl7Specvalues = (List<Object>) hl7Values.getValue();
    for (Object o : baseHl7Specvalues) {
      GenericResult gen = generateValue(dataSource, contextValues, new GenericResult(o));
      if (gen != null && gen.getValue() != null && !gen.isEmpty()) {
        result.add(gen.getValue());
        if (gen.getAdditionalResources() != null && !gen.getAdditionalResources().isEmpty()) {
          additionalresourcesresult.addAll(gen.getAdditionalResources());
        }
      }
    }


    if (!result.isEmpty()) {
    return new GenericResult(result, additionalresourcesresult);
    } else {
      return null;
    }


  }

  private GenericResult generateValue(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult obj) {
    String stringRep = this.toString();
    LOGGER.info("Evaluating expression {} for spec value {}  ", stringRep, obj);
    // resolve variables
    Map<String, GenericResult> localContextValues = new HashMap<>(contextValues);
    if (obj != null && obj.getValue() != null) {
      localContextValues.put(DataTypeUtil.getDataType(obj.getValue()), obj);
    }
    localContextValues.putAll(
        resolveVariables(this.getVariables(), ImmutableMap.copyOf(localContextValues), dataSource));

    if (this.isConditionSatisfied(localContextValues)) {
      GenericResult gen = evaluateExpression(dataSource, ImmutableMap.copyOf(localContextValues),
          obj);
      // Use the default value if the generated value is null and provided default value is not
      // null
      if (gen == null && this.getDefaultValue() != null && !this.getDefaultValue().isEmpty()) {
        gen = new GenericResult(this.getDefaultValue().getValue());
      }

      if (this.isRequired() && (gen == null || gen.isEmpty())) {
        LOGGER.warn("Failure in Evaluating expression {} for hl7spec obj {} value generated {}",
            stringRep, obj, gen);
        return null;
      } else {
        return gen;
      }
    }
    return null;
  }



  private static Map<String, GenericResult> resolveVariables(List<Variable> variables,
      Map<String, GenericResult> contextValues, InputData dataSource) {

    Map<String, GenericResult> localVariables = new HashMap<>();

    for (Variable var : variables) {
      try {
        GenericResult value =
            var.extractVariableValue(ImmutableMap.copyOf(contextValues), dataSource);
        if (value != null) {

          localVariables.put(VariableUtils.getVarName(var.getVariableName()),
              new GenericResult(value.getValue()));
        } else {
          // enclose null in GenericParsingResult
          localVariables.put(VariableUtils.getVarName(var.getVariableName()),
              new GenericResult(null));
        }
      } catch (DataExtractionException e) {
        LOGGER.error("cannot extract value for variable {} ", var.getVariableName(), e);
      }
    }
    return localVariables;
  }


  protected abstract GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> resolvedVariables, GenericResult hl7Values);



  protected static boolean isVar(String spec) {
    return StringUtils.isNotBlank(spec) && spec.startsWith("$") && spec.length() > 1;
  }


  @Override
  public boolean isConditionSatisfied(Map<String, GenericResult> contextValues) {
    if (this.condition != null) {
      return this.condition.test(contextValues);
    } else {
      return true;
    }
  }

  public boolean isMultiple() {
    return isMultiple;
  }

  public void setMultiple() {
    this.isMultiple = true;
  }

  static Object getSingleValue(GenericResult hl7SpecValues) {
    Object hl7Value = null;
    if (hl7SpecValues != null && !hl7SpecValues.isEmpty()) {
      Object valList = hl7SpecValues.getValue();
      if (valList instanceof List && !((List) valList).isEmpty()) {
        hl7Value = ((List) valList).get(0);
      } else {
        hl7Value = hl7SpecValues.getValue();
      }
    }
    return hl7Value;
  }


  private static List<String> getTokens(String value) {
    if (StringUtils.isNotBlank(value)) {
      StringTokenizer st = new StringTokenizer(value, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }

    return new ArrayList<>();
  }



  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

}
