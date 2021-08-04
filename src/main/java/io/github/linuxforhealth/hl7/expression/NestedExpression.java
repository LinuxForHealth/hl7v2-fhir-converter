/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.hl7.resource.ResourceEvaluationResult;
import io.github.linuxforhealth.hl7.resource.deserializer.HL7DataBasedResourceDeserializer;
import io.github.linuxforhealth.hl7.util.ExpressionUtility;

/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */

public class NestedExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(NestedExpression.class);

  private Map<String, Expression> childexpressions;
  private boolean generateMap;
  public NestedExpression(ExpressionAttributes attr) {
    super(attr);
    this.childexpressions = new HashMap<>();
    if (attr.getExpressions() != null) {
      int index = 0;
      for (ExpressionAttributes nestedattrs : attr.getExpressions()) {
        Expression e = HL7DataBasedResourceDeserializer.generateExpression(nestedattrs);
        if (e != null) {
          this.childexpressions.put("key" + index, e);
          index++;
        }
      }

      this.generateMap = false;
    } else if (attr.getExpressionsMap() != null) {

      for (Entry<String, ExpressionAttributes> nestedattrs : attr.getExpressionsMap().entrySet()) {
        Expression e = HL7DataBasedResourceDeserializer.generateExpression(nestedattrs.getValue());
        if (e != null) {
          this.childexpressions.put(nestedattrs.getKey(), e);

        }
      }
      this.generateMap = true;
    }
    Preconditions.checkState(!childexpressions.isEmpty(),
        "childexpressions cannot be null or empty");
  }



  @Override
  protected EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    LOGGER.info("Evaluating child expressions {}", this.getExpressionAttr().getName());
    ResourceEvaluationResult result =
        ExpressionUtility.evaluate(dataSource, contextValues, baseValue, this.childexpressions);
    if (result.getResolveValues() == null || result.getResolveValues().isEmpty()) {
      return EvaluationResultFactory.getEvaluationResult(null);
    } else {
      if (this.generateMap) {
        return EvaluationResultFactory.getEvaluationResult(result.getResolveValues());
      } else {
      return EvaluationResultFactory.getEvaluationResult(
            new ArrayList<>(result.getResolveValues().values()),
            result.getAdditionalResolveValues());
      }
    }
  }



}
