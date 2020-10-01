/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

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
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.InputData;
import com.ibm.whi.core.expression.EvaluationResultFactory;
import com.ibm.whi.core.resource.ResourceResult;
import com.ibm.whi.core.resource.ResourceValue;
import com.ibm.whi.hl7.resource.HL7DataBasedResourceModel;
import com.ibm.whi.hl7.resource.ResourceModelReader;

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



  /**
   * 
   * @param type
   * @param reference
   * @param hl7spec
   * @param defaultValue
   * @param required
   * @param variables
   * @param referencesResources
   */
  @JsonCreator
  public ReferenceExpression(@JsonProperty("type") String type,
      @JsonProperty("reference") String reference, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition) {
    super(type, defaultValue, required, hl7spec, variables, condition);

    Preconditions.checkArgument(StringUtils.isNotBlank(reference), "reference cannot be blank");
    if (reference.endsWith("*")) {
      this.setMultiple();
      reference = StringUtils.removeEnd(reference, "*");
    }
    this.reference = StringUtils.strip(reference);
    this.data = (HL7DataBasedResourceModel) ResourceModelReader.getInstance()
        .generateResourceModel(this.reference);
    Preconditions.checkState(this.data != null, "Resource reference model cannot be null");


  }


  public ReferenceExpression(@JsonProperty("type") String type,
      @JsonProperty("reference") String reference, @JsonProperty("hl7spec") String hl7spec) {
    this(type, reference, hl7spec, null, false, null, null);
  }



  public HL7DataBasedResourceModel getData() {
    return data;
  }



  @Override
  public EvaluationResult evaluateExpression(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.info("Evaluating expression {}", this.reference);
    EvaluationResult resourceReferenceResult = null;
    // Evaluate the resource first and add it to the list of additional resources generated
    ResourceResult primaryResourceResult =
        evaluateResource(dataSource, contextValues, hl7SpecValues);
    // If the primary resource is generated then create the reference
    if (primaryResourceResult != null && primaryResourceResult.getResource() != null) {
      List<ResourceValue> additionalResources = new ArrayList<>();
      additionalResources.addAll(primaryResourceResult.getAdditionalResources());
      additionalResources.add(primaryResourceResult.getResource());

      Map<String, EvaluationResult> localContextValues = new HashMap<>(contextValues);
      localContextValues.put("ref-type",
          EvaluationResultFactory
              .getEvaluationResult(primaryResourceResult.getResource().getResource()));


      ResourceResult result = this.referenceModel.evaluate(dataSource,
          ImmutableMap.copyOf(localContextValues), hl7SpecValues);
      if (result != null && result.getResource() != null) {
        ResourceValue resolvedvalues = result.getResource();

        LOGGER.info("Evaluated expression {}, returning {} ", this.reference, resolvedvalues);
        if (resolvedvalues != null) {
          resourceReferenceResult =
              EvaluationResultFactory.getEvaluationResult(resolvedvalues.getResource(),
                  additionalResources);
        }
      }
    }

    return resourceReferenceResult;

  }



  private ResourceResult evaluateResource(InputData dataSource,
      Map<String, EvaluationResult> contextValues, EvaluationResult hl7SpecValue) {
    ResourceResult result =
        this.data.evaluate(dataSource, ImmutableMap.copyOf(contextValues), hl7SpecValue);
    if (result != null && result.getResource() != null) {
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
    return new ToStringBuilder(this.getClass().getSimpleName()).append("hl7spec", this.getspecs())
        .append("isMultiple", this.isMultiple()).append("variables", this.getVariables())
        .append("reference", this.reference).build();
  }

}
