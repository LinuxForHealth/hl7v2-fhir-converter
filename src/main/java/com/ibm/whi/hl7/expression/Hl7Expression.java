/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.whi.hl7.expression;

import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.core.expression.EmptyEvaluationResult;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.data.ValueExtractor;



/**
 * Represents the HL7 expression which can generate the extraction string that HAPI terser can
 * evaluate. Supports the following structure <br>
 * 
 *
 * @author pbhallam
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hl7Expression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7Expression.class);


  public Hl7Expression(@JsonProperty("type") String type, @JsonProperty("hl7spec") String hl7spec) {
    this(type, hl7spec, null, false, null, null);
  }

  @JsonCreator
  public Hl7Expression(@JsonProperty("type") String type, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition) {
    super(type, defaultValue, required, hl7spec, variables, condition);

  }

  @Override
  public EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");


    Object hl7Value = hl7SpecValues.getValue();
    Object resolvedValue = null;

    ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
    if (resolver != null && hl7Value != null) {
      resolvedValue = resolver.apply(hl7Value);
    }
    if (resolvedValue != null) {
      return EvaluationResultFactory.getEvaluationResult(resolvedValue);
    } else {
      return new EmptyEvaluationResult();
    }
  }

  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this).append("hl7spec", this.getspecs())
        .append("isMultiple", this.isMultiple()).append("variables", this.getVariables())
        .toString();
  }
}
