/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.linuxforhealth.hl7.expression;

import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.api.InputDataExtractor;
import com.ibm.linuxforhealth.core.expression.EmptyEvaluationResult;
import com.ibm.linuxforhealth.core.expression.EvaluationResultFactory;
import com.ibm.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import com.ibm.linuxforhealth.hl7.data.ValueExtractor;
import com.ibm.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;



/**
 * Represents the HL7 expression linuxforhealthch can generate the extraction string that HAPI
 * terser can evaluate. Supports the following structure <br>
 * 
 *
 * @author pbhallam
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hl7Expression extends AbstractExpression {
  public Hl7Expression(String type, String hl7spec) {
    this(type, hl7spec, null, false, null, null, null, false);
  }

  /**
   * 
   * @param type
   * @param specs
   * @param defaultValue
   * @param required
   * @param variables
   * @param condition
   * @param constants
   * @param useGroup
   */
  @JsonCreator
  public Hl7Expression(@JsonProperty(TemplateFieldNames.TYPE) String type,
      @JsonProperty(TemplateFieldNames.SPEC) String specs,
      @JsonProperty(TemplateFieldNames.DEFAULT_VALUE) String defaultValue,
      @JsonProperty(TemplateFieldNames.REQUIRED) boolean required,
      @JsonProperty(TemplateFieldNames.VARIABLES) Map<String, String> variables,
      @JsonProperty(TemplateFieldNames.CONDITION) String condition,
      @JsonProperty(TemplateFieldNames.CONSTANTS) Map<String, String> constants,
      @JsonProperty(TemplateFieldNames.USE_GROUP) boolean useGroup) {
    super(type, defaultValue, required, specs, variables, condition, constants, useGroup);

  }

  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");


    Object hl7Value = baseValue.getValue();
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
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .toString();
  }
}
