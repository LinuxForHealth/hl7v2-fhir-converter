/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;


@JsonIgnoreProperties(ignoreUnknown = true)
public class JEXLExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(JEXLExpression.class);

  @JsonCreator
  public JEXLExpression(ExpressionAttributes expAttr) {
    super(expAttr);

  }



  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
    for (Variable v : this.getVariables()) {
      if (!localContextValues.containsKey(v.getVariableName())) {
        localContextValues.put(v.getVariableName(), new EmptyEvaluationResult());
      }
    }
    LOGGER.info("Evaluating expression");
    LOGGER.debug("Evaluating value of {}", this.getExpressionAttr().getValueOf());
    return dataSource.evaluateJexlExpression(this.getExpressionAttr().getValueOf(), contextValues);
  }



}
