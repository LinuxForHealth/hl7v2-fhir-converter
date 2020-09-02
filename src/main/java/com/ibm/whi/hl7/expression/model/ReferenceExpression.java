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
import com.google.common.collect.Lists;
import com.ibm.whi.hl7.expression.Hl7SpecResult;
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
  public Object execute(Map<String, Object> context) {

    if ("ARRAY".equalsIgnoreCase(this.getType())) {

      List<Object> dataValues = (List<Object>) getRepts(context);
      List<Object> resolvedvalues = new ArrayList<>();
      for (Object o : dataValues) {
        Map<String, Object> localContext =
            getLocalContext(o, this.getVariables(), context);

        resolvedvalues.add(this.data.evaluate(localContext));
      }
      resolvedvalues.removeIf(Objects::isNull);
      if (resolvedvalues.isEmpty()) {
        return null;
      } else {
        return resolvedvalues;
      }



    } else {
      Object hl7Value = getValueFromSpecs(this.getHl7specs(), context);
      return this.data.evaluate(getLocalContext(hl7Value, this.getVariables(), context));

    }



  }


  private List<?> getRepts(Map<String, Object> context) {
    Hl7SpecResult result = getValuesFromSpecs(this.getHl7specs(), context);
    if (result != null && result.isNotEmpty()) {
      if (!result.getHl7DatatypeValue().isEmpty()) {
        return result.getHl7DatatypeValue();
      } else {
        return Lists.newArrayList(result.getTextValue());
      }
    }
    return new ArrayList<>();
  }


  private static Map<String, Object> getLocalContext(Object hl7Value,
      List<Variable> vars, Map<String, Object> context) {
    Map<String, Object> localContext = new HashMap<>(context);

    if (hl7Value != null) {
      String type = getDataType(hl7Value);

      LOGGER.info(type);
      localContext.put(type, hl7Value);
    }

    localContext.putAll(resolveVariables(vars, localContext));

    return localContext;
  }




  public String getReference() {
    return reference;
  }


}
