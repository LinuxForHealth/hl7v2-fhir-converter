/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.hl7.data.SimpleDataTypeMapper;
import io.github.linuxforhealth.hl7.data.ValueExtractor;



/**
 * Represents the HL7 expression linuxforhealthch can generate the extraction string that HAPI
 * terser can evaluate. Supports the following structure <br>
 * 
 *
 * @author pbhallam
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hl7Expression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7Expression.class);


  private List<Specification> valueof;

  @JsonCreator
  public Hl7Expression(ExpressionAttributes expAttr) {
    super(expAttr);
    this.valueof = ExpressionAttributes.getSpecList(expAttr.getValueOf(), expAttr.isUseGroup(),
        expAttr.isGenerateMultiple());

  }

  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");


    List<Object> baseSpecvalues = getSpecValues(dataSource, contextValues, baseValue, this.valueof);
    LOGGER.debug("Base values evaluated {} values {} ", this, baseSpecvalues);

    List<Object> resolvedValues = generateValue(baseSpecvalues);

    if (!resolvedValues.isEmpty() && this.getExpressionAttr().isGenerateMultiple()) {
      return EvaluationResultFactory.getEvaluationResult(resolvedValues);
    } else if (!resolvedValues.isEmpty()) {
      return EvaluationResultFactory.getEvaluationResult(resolvedValues.get(0));
    } else {
      return null;
    }
  }

  private List<Object> generateValue(List<Object> baseSpecvalues) {
    List<Object> resolvedValues = new ArrayList<>();
    if (baseSpecvalues != null && !baseSpecvalues.isEmpty()) {
      ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
      if (resolver != null && StringUtils.equalsIgnoreCase("STRING_ALL", this.getType())) {
        resolvedValues.add(resolver.apply(baseSpecvalues));
      } else if (resolver != null) {

        for (Object hl7Value : baseSpecvalues) {
          Object data = resolver.apply(hl7Value);
          if (data != null) {
            resolvedValues.add(data);
          }

          if (!this.getExpressionAttr().isGenerateMultiple() && !resolvedValues.isEmpty()) {
            break;
          }
        }
      }



    }
    return resolvedValues;
  }


}
