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
import io.github.linuxforhealth.hl7.resource.ResourceModelReader;
import io.github.linuxforhealth.hl7.resource.deserializer.TemplateFieldNames;

/**
 * Represent a expression that represents resolving a json template and creating a reference data
 * type.
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceExpression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExpression.class);

  private HL7DataBasedResourceModel data;
  private HL7DataBasedResourceModel referenceModel = (HL7DataBasedResourceModel) ResourceModelReader
      .getInstance().generateResourceModel("datatype/Reference");
  private String reference;
  private boolean isGenerateMultipleResource;

  /**
   * 
   * @param type
   * @param reference
   * @param specs
   * @param defaultValue
   * @param required
   * @param variables
   * @param condition
   * @param constants
   * @param useGroup
   */
  @JsonCreator
  public ReferenceExpression(@JsonProperty(TemplateFieldNames.TYPE) String type,
      @JsonProperty(TemplateFieldNames.REFERENCE) String reference,
      @JsonProperty(TemplateFieldNames.SPEC) String specs,
      @JsonProperty(TemplateFieldNames.DEFAULT_VALUE) String defaultValue,
      @JsonProperty(TemplateFieldNames.REQUIRED) boolean required,
      @JsonProperty(TemplateFieldNames.VARIABLES) Map<String, String> variables,
      @JsonProperty(TemplateFieldNames.CONDITION) String condition,
      @JsonProperty(TemplateFieldNames.CONSTANTS) Map<String, String> constants,
      @JsonProperty(TemplateFieldNames.USE_GROUP) boolean useGroup) {
    super(type, defaultValue, required, specs, variables, condition, constants, useGroup);

    Preconditions.checkArgument(StringUtils.isNotBlank(reference), "reference cannot be blank");

    if (reference.endsWith("*")) {
      isGenerateMultipleResource = true;
      reference = StringUtils.removeEnd(reference, "*");
    }
    this.reference = StringUtils.strip(reference);
    this.data = (HL7DataBasedResourceModel) ResourceModelReader.getInstance()
        .generateResourceModel(this.reference);
    Preconditions.checkState(this.data != null, "Resource reference model cannot be null");


  }


  public ReferenceExpression(String type, String reference, String hl7spec) {
    this(type, reference, hl7spec, null, false, null, null, null, false);
  }



  public HL7DataBasedResourceModel getData() {
    return data;
  }



  @Override
  public EvaluationResult evaluateExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult baseValue) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.debug("Evaluating expression {}", this.reference);
    EvaluationResult resourceReferenceResult = null;
    // Evaluate the resource first and add it to the list of additional resources generated
    ResourceResult primaryResourceResult =
        evaluateResource(dataSource, contextValues, baseValue);
    // If the primary resource is generated then create the reference
    if (primaryResourceResult != null && primaryResourceResult.getValue() != null) {
      List<ResourceValue> additionalResources = new ArrayList<>();
      additionalResources.addAll(primaryResourceResult.getAdditionalResources());
      additionalResources.add(primaryResourceResult.getValue());


      EvaluationResult genBaseValue = EvaluationResultFactory
          .getEvaluationResult(primaryResourceResult.getValue().getResource());

      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);

      ResourceResult result = this.referenceModel.evaluate(dataSource,
          ImmutableMap.copyOf(localContextValues), genBaseValue);
      if (result != null && result.getValue() != null) {
        ResourceValue resolvedvalues = result.getValue();

        LOGGER.debug("Evaluated expression {}, returning {} ", this.reference, resolvedvalues);
        if (resolvedvalues != null) {
          resourceReferenceResult =
              EvaluationResultFactory.getEvaluationResult(resolvedvalues.getResource(),
                  additionalResources);
        }
      }
    }

    return resourceReferenceResult;

  }



  private ResourceResult evaluateResource(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValue) {
    ResourceResult result =
        this.data.evaluate(dataSource, ImmutableMap.copyOf(contextValues), hl7SpecValue);
    if (result != null && result.getValue() != null) {
      return result;
    }
    return null;

  }


  public String getReference() {
    return reference;
  }


  @Override
  public String toString() {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
    return new ToStringBuilder(this)
        .append(TemplateFieldNames.TYPE, this.getClass().getSimpleName())
        .append(TemplateFieldNames.SPEC, this.getspecs()).append("isMultiple", this.isMultiple())
        .append(TemplateFieldNames.VARIABLES, this.getVariables())
        .append(TemplateFieldNames.REFERENCE, this.reference).build();
  }

  @Override
  public boolean isMultiple() {
    return this.isGenerateMultipleResource;
  }
}
