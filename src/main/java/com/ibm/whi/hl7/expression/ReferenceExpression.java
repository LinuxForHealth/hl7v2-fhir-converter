package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;
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


  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReferenceExpression.class);

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
  public GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.info("Evaluating expression {}", this.reference);
    GenericResult resourceReferenceResult = null;
    // Evaluate the resource first and add it to the list of additional resources generated
    ResourceResult primaryResourceResult =
        evaluateResource(dataSource, contextValues, hl7SpecValues);
    // If the primary resource is generated then create the reference
    if (primaryResourceResult != null && primaryResourceResult.getResources() != null
        && !primaryResourceResult.getResources().isEmpty()) {
      List<ResourceValue> additionalResources = new ArrayList<>();
      additionalResources.addAll(primaryResourceResult.getAdditionalResources());
      additionalResources.add(primaryResourceResult.getResources().get(0));

      Map<String, GenericResult> localContextValues = new HashMap<>(contextValues);
      localContextValues.put("ref-type",
          new GenericResult(primaryResourceResult.getResources().get(0).getResource()));
      GenericResult baseValue = new GenericResult(getSingleValue(hl7SpecValues));

      ResourceResult result =
          this.referenceModel.evaluateSingle(dataSource, ImmutableMap.copyOf(localContextValues),
              baseValue);
      if (result != null && result.getResources() != null && !result.getResources().isEmpty()) {
        List<ResourceValue> resolvedvalues = result.getResources();

      LOGGER.info("Evaluated expression {}, returning {} ", this.reference, resolvedvalues);
      if (resolvedvalues != null && !resolvedvalues.isEmpty()) {
          resourceReferenceResult =
              new GenericResult(resolvedvalues.get(0).getResource(), additionalResources);
      }
      }
    }

    return resourceReferenceResult;

  }



  private ResourceResult evaluateResource(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult hl7SpecValues) {
    // first evaluate the main resource and then evaluate the referenceResource
    GenericResult baseValue = new GenericResult(getSingleValue(hl7SpecValues));

    ResourceResult result =
        this.data.evaluateSingle(dataSource, ImmutableMap.copyOf(contextValues), baseValue);
    List<?> resolvedvalues = result.getResources();

    LOGGER.info("Evaluated expression {}, returning {} ", this.reference, resolvedvalues);
    if (resolvedvalues != null && !resolvedvalues.isEmpty()) {
      return result;

    }
    return null;

  }


  public String getReference() {
    return reference;
  }



}
