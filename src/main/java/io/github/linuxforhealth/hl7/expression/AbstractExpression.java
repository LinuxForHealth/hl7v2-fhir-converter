/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

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
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.data.DataTypeUtil;
import io.github.linuxforhealth.core.exception.DataExtractionException;
import io.github.linuxforhealth.core.exception.RequiredConstraintFailureException;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.VariableUtils;
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationParser;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationUtil;
import io.github.linuxforhealth.hl7.expression.variable.VariableGenerator;

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
  private Map<String, String> constants;
  private boolean useGroup;

  public AbstractExpression(String type, String defaultValue, boolean required, String specs,
      Map<String, String> rawvariables, String condition, Map<String, String> constants,
      boolean useGroup) {
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
    this.useGroup = useGroup;
    this.required = required;
    this.hl7specs = getSpecList(specs, useGroup);
    if (StringUtils.isNotBlank(condition)) {
      this.condition = ConditionUtil.createCondition(condition);
    }


    this.constants = new HashMap<>();
    if (constants != null && !constants.isEmpty()) {
      this.constants.putAll(constants);
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

  @Override
  public List<Variable> getVariables() {
    return new ArrayList<>(variables);
  }




  /**
   * Evaluates the expression and generated single or multiple resources based on the expression
   * values. If expression (reference and resource) ends with * then for that expression a the
   * Generic result includes list of values.
   * 
   * @see io.github.linuxforhealth.api.Expression#evaluate(io.github.linuxforhealth.api.InputDataExtractor,
   *      java.util.Map, EvaluationResult)
   */
  @Override
  public EvaluationResult evaluate(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Preconditions.checkArgument(baseValue != null, "baseValue cannot be null");
    EvaluationResult result;
    try {
      LOGGER.debug("Started Evaluating expression {} ", this);
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    if (!baseValue.isEmpty()) {
      localContextValues.put(baseValue.getIdentifier(), baseValue);
    }
    this.constants.entrySet().forEach(e -> localContextValues.put(e.getKey(),
        EvaluationResultFactory.getEvaluationResult(e.getValue())));
    if (this.isMultiple()) {
      result = evaluateMultiple(dataSource, localContextValues, baseValue);
    } else {
      result = evaluateSingle(dataSource, localContextValues, baseValue);
    }

      LOGGER.debug("Completed Evaluating the expression {} returned result {}", this, result);
    if (this.isRequired() && (result == null || result.isEmpty())) {
      String stringRep = this.toString();
      RuntimeException e = new RequiredConstraintFailureException(
          "Failure in Evaluating expression   :" + stringRep);
        LOGGER.warn("Failure encountered during evaluation of expression {} , exception {}", this,
            e);
      throw e;
    } else {
      return result;
    }
    } catch (DataExtractionException e) {
      LOGGER.error("Failure encountered during evaluation of expression {} , exception {}", this,
          e);
      return null;
    }
  }



  private EvaluationResult evaluateSingle(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseInputValue) {
    EvaluationResult baseValue;
    if (this.getspecs() == null || this.getspecs().isEmpty()) {
      baseValue = baseInputValue;
    } else {
      baseValue = SpecificationUtil.extractValueForSpec(this.getspecs(), dataSource,
          ImmutableMap.copyOf(contextValues));
    }

    LOGGER.debug("Evaluating Single value for expression  {} returned hl7 value {} ", this,
        baseValue);
    return generateValue(dataSource, contextValues, baseValue);


  }

  private EvaluationResult evaluateMultiple(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseinputValue) {


    List<Object> result = new ArrayList<>();
    List<ResourceValue> additionalresourcesresult = new ArrayList<>();
    List<Object> baseHl7Specvalues = getBaseValues(dataSource, contextValues, baseinputValue);
    LOGGER.debug("Base values evaluated {} values {} ", this, baseHl7Specvalues);
    if (!baseHl7Specvalues.isEmpty()) {
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
          generateValue(dataSource, contextValues, baseinputValue);
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


  private List<Object> getBaseValues(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseinputValue) {
    List<Object> baseHl7Specvalues = new ArrayList<>();
    EvaluationResult specValues;
    if (this.getspecs() == null || this.getspecs().isEmpty()) {
      specValues = baseinputValue;
    } else {
      specValues =
        SpecificationUtil.extractMultipleValuesForSpec(this.getspecs(), dataSource,
          ImmutableMap.copyOf(contextValues));
    }


    if (specValues != null && specValues.getValue() instanceof List) {
      baseHl7Specvalues.addAll((List<Object>) specValues.getValue());
    } else if (specValues != null) {
      baseHl7Specvalues.add(specValues.getValue());
    }

    return baseHl7Specvalues;
  }

  private EvaluationResult generateValue(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {

    // resolve variables
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    if (baseValue != null && baseValue.getValue() != null) {
      localContextValues.put(DataTypeUtil.getDataType(baseValue.getValue()), baseValue);
    }
    localContextValues.putAll(
        resolveVariables(this.getVariables(), ImmutableMap.copyOf(localContextValues), dataSource));

    if (this.isConditionSatisfied(localContextValues)) {
      EvaluationResult gen =
          evaluateExpression(dataSource, ImmutableMap.copyOf(localContextValues),
              baseValue);
      // Use the default value if the generated value is null and provided default value is not
      // null
      if (gen == null && this.getDefaultValue() != null && !this.getDefaultValue().isEmpty()) {
        gen = EvaluationResultFactory.getEvaluationResult(this.getDefaultValue().getValue());
      }

      if (this.isRequired() && (gen == null || gen.isEmpty())) {
        LOGGER.warn("Failure in Evaluating expression {} for hl7spec obj {} value generated {}",
            this, baseValue, gen);
        return new EmptyEvaluationResult();
      } else {
        return gen;
      }
    }
    return new EmptyEvaluationResult();
  }



  private static Map<String, EvaluationResult> resolveVariables(List<Variable> variables,
      Map<String, EvaluationResult> contextValues, InputDataExtractor dataSource) {

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


  protected abstract EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> resolvedVariables, EvaluationResult baseValue);




  @Override
  public boolean isConditionSatisfied(Map<String, EvaluationResult> contextValues) {
    if (this.condition != null) {
      return this.condition.test(contextValues);
    } else {
      return true;
    }
  }

  public boolean isMultiple() {
    return false;
  }




  private static List<Specification> getSpecList(String inputString, boolean useGroup) {
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
      st.getTokenList()
          .forEach(s -> specs.add(SpecificationParser.parse(s, extractMultiple, useGroup)));
    }

    return specs;
  }

  @Override
  public Map<String, String> getConstants() {
    return this.constants;
  }



  public boolean isUseGroup() {
    return useGroup;
  }

  protected static String getGroupId(Map<String, EvaluationResult> localContext) {
    EvaluationResult result = localContext.get(Constants.GROUP_ID);
    if (result != null) {
      return (String) result.getValue();
    }
    return null;
  }

}
