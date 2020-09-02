package com.ibm.whi.hl7.expression.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.GenericResult;
import com.ibm.whi.hl7.expression.Variable;
import com.ibm.whi.hl7.resource.ResourceModel;

/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceExpression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExpression.class);

  private ResourceModel data;
  private String reference;


  private List<String> referencesResources;




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
      @JsonProperty("reference-resources") String referencesResources) {
    super(type, defaultValue, required, hl7spec, variables);



    this.reference = reference;
    this.data = ResourceModel.generateResourceModel(this.reference);
    Preconditions.checkState(this.data != null, "Resource reference model cannot be null");
    this.referencesResources = new ArrayList<>();
    if (referencesResources != null) {
      this.referencesResources.add(referencesResources);
    }

  }


  public ReferenceExpression(@JsonProperty("type") String type,
      @JsonProperty("reference") String reference, @JsonProperty("hl7spec") String hl7spec) {
    this(type, reference, hl7spec, null, false, null, null);
  }



  public ResourceModel getData() {
    return data;
  }




  @Override
  public GenericResult execute(ImmutableMap<String, ?> executables,
      ImmutableMap<String, GenericResult> variables) {

    if ("ARRAY".equalsIgnoreCase(this.getType())) {

      List<?> dataValues = getRepts(executables, variables);
      List<Object> resolvedvalues = new ArrayList<>();
      for (Object o : dataValues) {
        Map<String, GenericResult> localContext =
            getLocalVariables(o, this.getVariables(), executables, variables);

        resolvedvalues.add(this.data.evaluate(executables, ImmutableMap.copyOf(localContext)));
      }
      resolvedvalues.removeIf(Objects::isNull);
      if (resolvedvalues.isEmpty()) {
        return null;
      } else {
        return new GenericResult(resolvedvalues);
      }



    } else {
      GenericResult hl7Value = getValueFromSpecs(this.getHl7specs(), executables, variables);
      Object val = null;
      if (hl7Value != null) {
        val = hl7Value.getValue();
      }
      Map<String, GenericResult> localContext =
          getLocalVariables(val, this.getVariables(), executables, variables);
      return new GenericResult(
          this.data.evaluate(executables, ImmutableMap.copyOf(localContext)));

    }



  }


  private List<?> getRepts(ImmutableMap<String, ?> executables,
      ImmutableMap<String, GenericResult> variables) {
    GenericResult result = getValuesFromSpecs(this.getHl7specs(), executables, variables);
    if (result != null && result.getValue() instanceof List) {
      return (List) result.getValue();
    }
    return new ArrayList<>();
  }


  private static Map<String, GenericResult> getLocalVariables(Object hl7Value,
      List<Variable> variableNames,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> variables) {
    Map<String, GenericResult> localVariables = new HashMap<>(variables);

    if (hl7Value != null) {
      String type = getDataType(hl7Value);

      LOGGER.info(type);
      localVariables.put(type, new GenericResult(hl7Value));
    }

    localVariables
        .putAll(resolveVariables(variableNames, executables, ImmutableMap.copyOf(localVariables)));

    return ImmutableMap.copyOf(localVariables);
  }




  public String getReference() {
    return reference;
  }


}
