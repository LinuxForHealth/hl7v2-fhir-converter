/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;


@JsonIgnoreProperties(ignoreUnknown = true)
public class JEXLExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(JEXLExpression.class);

  private String evaluate;



  @JsonCreator
  public JEXLExpression(ExpressionAttributes expAttr) {
    super(expAttr);
    this.evaluate = expAttr.getEvaluate();
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
    return dataSource.evaluateJexlExpression(this.evaluate, contextValues);
  }


  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .append(TemplateFieldNames.EVALUATE, this.evaluate).build();
  }


}
