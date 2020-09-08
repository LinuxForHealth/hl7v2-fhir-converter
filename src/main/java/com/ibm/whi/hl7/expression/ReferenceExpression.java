package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.ibm.whi.hl7.resource.HL7DataBasedResourceModel;
import com.ibm.whi.hl7.resource.ResourceModelReader;

/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceExpression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExpression.class);

  private HL7DataBasedResourceModel data;
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
      this.setMultiple(true);
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
    if (this.isMultiple() && hl7SpecValues != null) {
      List<?> dataValues = (List<?>) hl7SpecValues.getValue();
      List<GenericResult> baseValues = new ArrayList<>();

      dataValues.removeIf(Objects::isNull);
      dataValues.forEach(d -> baseValues.add(new GenericResult(d)));
      List<?> resolvedvalues = this.data.evaluateMultiple(dataSource,
          ImmutableMap.copyOf(contextValues), baseValues, this.getVariables());
      LOGGER.info("Evaluated expression {}, returning {} ", this.reference, resolvedvalues);
      if (resolvedvalues == null || resolvedvalues.isEmpty()) {
        return null;
      } else {
        return new GenericResult(resolvedvalues);
      }
    } else {
      GenericResult baseValue = new GenericResult(getSingleValue(hl7SpecValues));

      Object obj =
          this.data.evaluateSingle(dataSource, ImmutableMap.copyOf(contextValues), baseValue);
      LOGGER.info("Evaluated expression {}, returning {} ", this.reference, obj);
      if (obj != null) {
        return new GenericResult(obj);
      } else {
        return null;
      }

    }



  }


  private List<?> getRepts(InputData dataExtractor,
      ImmutableMap<String, GenericResult> contextValues) {
    GenericResult result =
        dataExtractor.extractMultipleValuesForSpec(this.getspecs(), contextValues);
    if (result != null && result.getValue() instanceof List) {
      return (List) result.getValue();
    }
    return new ArrayList<>();
  }



  public String getReference() {
    return reference;
  }



}
