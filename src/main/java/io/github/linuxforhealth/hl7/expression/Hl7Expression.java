/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.expression;

import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.data.ValueExtractor;
import io.github.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;



/**
 * Represents the HL7 expression linuxforhealthch can generate the extraction string that HAPI
 * terser can evaluate. Supports the following structure <br>
 * 
 *
 * @author pbhallam
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hl7Expression extends AbstractExpression {


  @JsonCreator
  public Hl7Expression(ExpressionAttributes expAttr) {
    super(expAttr);


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
        .append(TemplateFieldNames.VARIABLES, this.getVariables()).toString();
  }
}
