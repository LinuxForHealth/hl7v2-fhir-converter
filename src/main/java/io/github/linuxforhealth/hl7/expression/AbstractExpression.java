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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
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
import io.github.linuxforhealth.hl7.expression.specification.SpecificationUtil;

public abstract class AbstractExpression implements Expression {
  private static final String RESOURCE = "Resource";


  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpression.class);


  private ExpressionAttributes attr;
  private String originalContext;
  private boolean conditionSatisfiedState;
  public AbstractExpression(ExpressionAttributes attr) {
    this.attr = attr;
  }



  @Override
  public String getType() {
    return this.attr.getType();
  }


  public ExpressionAttributes getExpressionAttr() {
    return this.attr;
  }

  @Override
  public boolean isEvaluateLater() {
    return this.attr.isEvaluateLater();

  }

  @Override
  public EvaluationResult getDefaultValue() {
    return EvaluationResultFactory.getEvaluationResult(this.attr.getDefaultValue());
  }

  public boolean isRequired() {
    return this.attr.isRequired();
  }

  @Override
  public List<Specification> getspecs() {
    return this.attr.getSpecs();
  }

  @Override
  public List<Variable> getVariables() {
    return this.attr.getVariables();
  }


  /**
   * Evaluates the expression and generated single or multiple resources based on the expression
   * values. If expression (reference and resource) ends with * then for that expression the Generic
   * result includes list of values.
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
      setLoggingContext();

      LOGGER.debug("Started Evaluating with baseValue {} expression {} ", baseValue, this);


      Map<String, EvaluationResult> localContextValues =
          new HashMap<>(ImmutableMap.copyOf(contextValues));

      if (!baseValue.isEmpty()) {
        localContextValues.put(baseValue.getIdentifier(), baseValue);
      }

      result = evaluateValueOfExpression(dataSource, localContextValues, baseValue);

      LOGGER.debug("Completed Evaluating returned value  {} ----  for  expression {} ", result, this);

      if (this.conditionSatisfiedState && this.isRequired()
          && (result == null || result.isEmpty())) {

        String stringRep = this.toString();
        throw new RequiredConstraintFailureException(
            "Resource Constraint condition not satisfied for expression   :" + stringRep);

      } else {
        return result;
      }
    } catch (DataExtractionException | IllegalArgumentException e) {
      LOGGER.warn("Failure encountered during evaluation of expression {}", 
          this.attr.getName());
      return null;
    } finally {
      resetLoggingContext();
    }
  }



  private void setLoggingContext() {
    originalContext = MDC.get(RESOURCE);
    MDC.put(RESOURCE, originalContext + "-> Field:" + this.getExpressionAttr().getName());
  }

  private void resetLoggingContext() {
    MDC.put(RESOURCE, originalContext);
  }


  private EvaluationResult evaluateValueOfExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseinputValue) {
    /**
     * Steps:
     * <ul>
     * <li>Add all constants to the context map</li>
     * <li>Evaluate the specs</li>
     * <li>Evaluate the variables</li>
     * <li>Apply the condition</li>
     * <li>If condition is satisfies then evaluate the valueOf/value attribute.</li>
     * </ul>
     * Note: If generateMultiple is set that all the spec values are used for generating list value
     * from the expression.
     * 
     */

    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    // Add constants to the context map
    this.attr.getConstants().entrySet().forEach(e -> localContextValues.put(e.getKey(),
        EvaluationResultFactory.getEvaluationResult(e.getValue())));

    List<Object> result = new ArrayList<>();
    List<ResourceValue> additionalresourcesresult = new ArrayList<>();
    List<Object> baseSpecvalues =
        getSpecValues(dataSource, localContextValues, baseinputValue, this.getspecs());
    LOGGER.debug("Base values evaluated {} -----  values {} ", this, baseSpecvalues);


    if (!baseSpecvalues.isEmpty()) {
      for (Object o : baseSpecvalues) {
        EvaluationResult gen = generateValue(dataSource, localContextValues,
            EvaluationResultFactory.getEvaluationResult(o));

        if (gen != null && gen.getValue() != null && !gen.isEmpty()) {
          if (gen.getValue() instanceof List) {
            result.addAll(gen.getValue());
          } else {
            result.add(gen.getValue());
          }
          additionalresourcesresult.addAll(gen.getAdditionalResources());
        }

        if (!this.attr.isGenerateMultiple() && !result.isEmpty()) {
          break;
        }

      }
    } else {
      EvaluationResult gen = generateValue(dataSource, localContextValues, baseinputValue);
      if (gen != null && gen.getValue() != null && !gen.isEmpty()) {
        if (gen.getValue() instanceof List) {
          result.addAll(gen.getValue());
        } else {
          result.add(gen.getValue());
        }
        additionalresourcesresult.addAll(gen.getAdditionalResources());

      }

    }


    return getResult(result, additionalresourcesresult);


  }

  private EvaluationResult getResult(List<Object> result,
      List<ResourceValue> additionalresourcesresult) {
    if (!result.isEmpty() && !this.attr.isGenerateMultiple()) {
      return EvaluationResultFactory.getEvaluationResult(result.get(0), additionalresourcesresult);
    } else if (!result.isEmpty()) {
      return EvaluationResultFactory.getEvaluationResult(result, additionalresourcesresult);
    } else if (!this.getDefaultValue().isEmpty()) {
      return this.getDefaultValue();
    } else {
      return null;
    }

  }


  protected static List<Object> getSpecValues(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseinputValue,
      List<Specification> specs) {
    List<Object> baseHl7Specvalues = new ArrayList<>();
    EvaluationResult specValues;
    if (specs == null || specs.isEmpty()) {
      specValues = baseinputValue;
    } else {
      specValues = SpecificationUtil.extractMultipleValuesForSpec(specs, dataSource,
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
      conditionSatisfiedState = true;
      return evaluateExpression(dataSource, ImmutableMap.copyOf(localContextValues), baseValue);

    }
    return null;
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
        LOGGER.error("Cannot extract value for variable {} ", var.getVariableName());
        LOGGER.debug("Cannot extract value for variable {} ", var.getVariableName(), e);
      }
    }
    return localVariables;
  }


  protected abstract EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> resolvedVariables, EvaluationResult baseValue);



  @Override
  public boolean isConditionSatisfied(Map<String, EvaluationResult> contextValues) {
    if (this.attr.getFilter() != null) {
      return this.attr.getFilter().test(contextValues);
    } else {
      return true;
    }
  }


  @Override
  public Map<String, String> getConstants() {
    return this.attr.getConstants();
  }



  protected static String getGroupId(Map<String, EvaluationResult> localContext) {
    EvaluationResult result = localContext.get(Constants.GROUP_ID);
    if (result != null) {
      return (String) result.getValue();
    }
    return null;
  }

  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.SIMPLE_STYLE);
    return new ToStringBuilder(this).append("Expression Attributes", this.attr).build();
  }

}
