/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.resource.ResourceResult;
import io.github.linuxforhealth.hl7.resource.HL7DataBasedResourceModel;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceExpression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceExpression.class);

  private HL7DataBasedResourceModel data;
  private String resourceToGenerate;



  @JsonCreator
  public ResourceExpression(ExpressionAttributes expAttr) {
    super(expAttr);

    this.resourceToGenerate = expAttr.getValueOf();
    this.data = (HL7DataBasedResourceModel) ResourceReader.getInstance()
        .generateResourceModel(this.resourceToGenerate);
    Preconditions.checkState(this.data != null, "Resource model cannot be null");
  }



  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.debug("Evaluating expression {}", this.resourceToGenerate);
    EvaluationResult evaluationResult = null;

    ResourceResult result =
        this.data.evaluate(dataSource, ImmutableMap.copyOf(contextValues), baseValue);
    if (result != null && result.getValue() != null) {
      ResourceValue resolvedvalues = result.getValue();

      LOGGER.debug("Evaluated expression {}, returning {} ", this.resourceToGenerate,
          resolvedvalues);
      if (resolvedvalues != null) {
        evaluationResult = EvaluationResultFactory.getEvaluationResult(resolvedvalues.getResource(),
            result.getAdditionalResources());
      }
    }


    return evaluationResult;

  }



  public String getResource() {
    return this.resourceToGenerate;
  }



  HL7DataBasedResourceModel getData() {
    return this.data;
  }



}
