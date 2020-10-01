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
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.api.Condition;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.Expression;
import com.ibm.whi.api.InputData;
import com.ibm.whi.api.Specification;
import com.ibm.whi.api.Variable;
import com.ibm.whi.core.data.DataTypeUtil;
import com.ibm.whi.core.expression.EmptyEvaluationResult;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.expression.VariableUtils;
import com.ibm.whi.core.expression.condition.ConditionUtil;
import com.ibm.whi.core.resource.ResourceValue;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.exception.RequiredConstraintFailureException;
import com.ibm.whi.hl7.expression.variable.VariableGenerator;

public abstract class AbstractExpression implements Expression {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpression.class);
  public static final String OBJECT_TYPE = Object.class.getSimpleName();
  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private final String type;
  private final EvaluationResult defaultValue;
  private final boolean required;
  private final List<Specification> hl7specs;
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
      this.defaultValue = EvaluationResultFactory.getEvaluationResult(defaultValue);
    } else {
      this.defaultValue = null;
    }

    this.required = required;
    this.hl7specs = getHl7SpecList(hl7spec);
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
  public EvaluationResult getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  @Override
  public List<Specification> getspecs() {
    return new ArrayList<>(this.hl7specs);
  }

  public List<Variable> getVariables() {
    return new ArrayList<>(variables);
  }




  /**
   * Evaluates the expression and generated single or multiple resources based on the expression
   * values. If expression (reference and resource) ends with * then for that expression a the
   * Generic result includes list of values.
   * 
   * @see com.ibm.whi.api.Expression#evaluate(com.ibm.whi.api.InputData,
   *      java.util.Map)
   */
  public EvaluationResult evaluate(InputData dataSource, Map<String, EvaluationResult> contextValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    EvaluationResult result;
    LOGGER.info("Started Evaluating expression {} ", this);
    if (this.isMultiple) {
      result = evaluateMultiple(dataSource, contextValues);
    } else {
      result = evaluateSingle(dataSource, contextValues);
    }

    LOGGER.info("Completed Evaluating the expression {} returned result {}", this, result);
    if (this.isRequired() && (result == null || result.isEmpty())) {
      String stringRep = this.toString();
      RuntimeException e = new RequiredConstraintFailureException(
          "Failure in Evaluating expression   :" + stringRep);
      LOGGER.info("Failure encountered during evaluation of expression {} , exception {}", this, e);
      throw e;
    } else {
      return result;
    }
  }



  private EvaluationResult evaluateSingle(InputData dataSource,
      Map<String, EvaluationResult> contextValues) {

    EvaluationResult hl7Value = dataSource.extractValueForSpec(this.getspecs(),
          ImmutableMap.copyOf(contextValues));

    LOGGER.debug("Evaluating expression {} returned hl7 value {} ", this, hl7Value);
    return generateValue(dataSource, contextValues, hl7Value);


  }

  private EvaluationResult evaluateMultiple(InputData dataSource,
      Map<String, EvaluationResult> contextValues) {

    EvaluationResult hl7Values = dataSource.extractMultipleValuesForSpec(this.getspecs(),
        ImmutableMap.copyOf(contextValues));
    List<Object> result = new ArrayList<>();
    List<ResourceValue> additionalresourcesresult = new ArrayList<>();
    if (hl7Values != null && hl7Values.getValue() instanceof List) {
      List<Object> baseHl7Specvalues = (List<Object>) hl7Values.getValue();
      for (Object o : baseHl7Specvalues) {
        EvaluationResult gen = generateValue(dataSource, contextValues,
            EvaluationResultFactory.getEvaluationResult(o));
        if (gen != null && gen.getValue() != null && !gen.isEmpty()) {
          result.add(gen.getValue());
          additionalresourcesresult.addAll(gen.getAdditionalResources());

        }
      }
    } else {
      EvaluationResult gen =
          generateValue(dataSource, contextValues, new EmptyEvaluationResult());
      if (gen != null && gen.getValue() != null && !gen.isEmpty()) {
        result.add(gen.getValue());
        additionalresourcesresult.addAll(gen.getAdditionalResources());

    }

    }


    if (!result.isEmpty()) {
      return EvaluationResultFactory.getEvaluationResult(result, additionalresourcesresult);
    } else {
      return new EmptyEvaluationResult();
    }


  }

  private EvaluationResult generateValue(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValue) {
    LOGGER.debug("Evaluating expression {} for spec value {}  ", this, hl7SpecValue);
    // resolve variables
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    if (hl7SpecValue != null && hl7SpecValue.getValue() != null) {
      localContextValues.put(DataTypeUtil.getDataType(hl7SpecValue.getValue()), hl7SpecValue);
    }
    localContextValues.putAll(
        resolveVariables(this.getVariables(), ImmutableMap.copyOf(localContextValues), dataSource));

    if (this.isConditionSatisfied(localContextValues)) {
      EvaluationResult gen = evaluateExpression(dataSource, ImmutableMap.copyOf(localContextValues),
          hl7SpecValue);
      // Use the default value if the generated value is null and provided default value is not
      // null
      if (gen == null && this.getDefaultValue() != null && !this.getDefaultValue().isEmpty()) {
        gen = EvaluationResultFactory.getEvaluationResult(this.getDefaultValue().getValue());
      }

      if (this.isRequired() && (gen == null || gen.isEmpty())) {
        LOGGER.warn("Failure in Evaluating expression {} for hl7spec obj {} value generated {}",
            this, hl7SpecValue, gen);
        return new EmptyEvaluationResult();
      } else {
        return gen;
      }
    }
    return new EmptyEvaluationResult();
  }



  private static Map<String, EvaluationResult> resolveVariables(List<Variable> variables,
      Map<String, EvaluationResult> contextValues, InputData dataSource) {

    Map<String, EvaluationResult> localVariables = new HashMap<>();

    for (Variable var : variables) {
      try {
        EvaluationResult value =
            var.extractVariableValue(ImmutableMap.copyOf(contextValues), dataSource);
        if (value != null) {

          localVariables.put(VariableUtils.getVarName(var.getVariableName()),
              EvaluationResultFactory.getEvaluationResult(value.getValue()));
        } else {
          // enclose null in GenericParsingResult
          localVariables.put(VariableUtils.getVarName(var.getVariableName()),
              new EmptyEvaluationResult());
        }
      } catch (DataExtractionException e) {
        LOGGER.error("cannot extract value for variable {} ", var.getVariableName(), e);
      }
    }
    return localVariables;
  }


  protected abstract EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> resolvedVariables, EvaluationResult hl7Values);




  @Override
  public boolean isConditionSatisfied(Map<String, EvaluationResult> contextValues) {
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



  private static List<Specification> getHl7SpecList(String inputString) {
    final boolean extractMultiple;
    String hl7SpecExpression = inputString;
    if (StringUtils.endsWith(inputString, "*")) {
      hl7SpecExpression = StringUtils.removeEnd(inputString, "*");
      extractMultiple = true;
    } else {
      extractMultiple = false;
    }

    hl7SpecExpression = StringUtils.strip(hl7SpecExpression);
    List<Specification> specs = new ArrayList<>();
    if (StringUtils.isNotBlank(hl7SpecExpression)) {
      StringTokenizer st = new StringTokenizer(hl7SpecExpression, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      st.getTokenList().forEach(s -> specs.add(HL7Specification.parse(s, extractMultiple)));
    }

    return specs;
  }



}
