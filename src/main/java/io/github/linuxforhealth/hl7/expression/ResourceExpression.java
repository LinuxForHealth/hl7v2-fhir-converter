/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.resource.ResourceResult;
import io.github.linuxforhealth.hl7.resource.HL7DataBasedResourceModel;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;

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
  private boolean isGenerateMultipleResource;


  /**
   * 
   * @param type
   * @param resourceToGenerate
   * @param specs
   * @param defaultValue
   * @param required
   * @param variables
   * @param condition
   * @param constants
   * @param useGroup
   */
  @JsonCreator
  public ResourceExpression(@JsonProperty(TemplateFieldNames.TYPE) String type,
      @JsonProperty(TemplateFieldNames.RESOURCE) String resourceToGenerate,
      @JsonProperty(TemplateFieldNames.SPEC) String specs,
      @JsonProperty(TemplateFieldNames.DEFAULT_VALUE) String defaultValue,
      @JsonProperty(TemplateFieldNames.REQUIRED) boolean required,
      @JsonProperty(TemplateFieldNames.VARIABLES) Map<String, String> variables,
      @JsonProperty(TemplateFieldNames.CONDITION) String condition,
      @JsonProperty(TemplateFieldNames.CONSTANTS) Map<String, String> constants,
      @JsonProperty(TemplateFieldNames.USE_GROUP) boolean useGroup) {
    super(type, defaultValue, required, specs, variables, condition, constants, useGroup);

    Preconditions.checkArgument(StringUtils.isNotBlank(resourceToGenerate),
        "reference cannot be blank");
    if (resourceToGenerate.endsWith("*")) {
      this.isGenerateMultipleResource = true;
      resourceToGenerate = StringUtils.removeEnd(resourceToGenerate, "*");
    }
    this.resourceToGenerate = StringUtils.strip(resourceToGenerate);
    this.data = (HL7DataBasedResourceModel) ResourceReader.getInstance()
        .generateResourceModel(this.resourceToGenerate);
    Preconditions.checkState(this.data != null, "Resource reference model cannot be null");


  }


  public ResourceExpression(String type, String resourceToGenerate, String hl7spec) {
    this(type, resourceToGenerate, hl7spec, null, false, null, null, null, false);
  }



  public HL7DataBasedResourceModel getData() {
    return data;
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
        evaluationResult =
            EvaluationResultFactory.getEvaluationResult(resolvedvalues.getResource(),
                result.getAdditionalResources());
      }
    }


    return evaluationResult;

  }



  public String getResourceName() {
    return resourceToGenerate;
  }


  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .append(TemplateFieldNames.USE_GROUP, this.isUseGroup())
        .append(TemplateFieldNames.RESOURCE, this.resourceToGenerate).build();
  }

  @Override
  public boolean isMultiple() {
    return this.isGenerateMultipleResource;
  }
}
