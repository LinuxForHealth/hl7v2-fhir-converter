package com.ibm.whi.hl7.expression;

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
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
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
    this.data =
        (HL7DataBasedResourceModel) ResourceModelReader.getInstance()
            .generateResourceModel(this.reference);
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



  public HL7DataBasedResourceModel getData() {
    return data;
  }




  @Override
  public GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");

    if ("ARRAY".equalsIgnoreCase(this.getType())) {

      List<?> dataValues =
          getRepts(dataSource, ImmutableMap.copyOf(contextValues));
      List<Object> resolvedvalues = new ArrayList<>();
      for (Object o : dataValues) {
        Map<String, GenericResult> localContext =
            getLocalVariables(dataSource, o, this.getVariables(),
                ImmutableMap.copyOf(contextValues));

        resolvedvalues.add(this.data.evaluate(dataSource,
            ImmutableMap.copyOf(localContext)));
      }
      resolvedvalues.removeIf(Objects::isNull);
      if (resolvedvalues.isEmpty()) {
        return null;
      } else {
        return new GenericResult(resolvedvalues);
      }



    } else {
      GenericResult hl7Value =
          dataSource.extractSingleValueForSpec(this.getspecs(),
              ImmutableMap.copyOf(contextValues));
      Object val = null;
      if (hl7Value != null) {
        val = hl7Value.getValue();
      }
      Map<String, GenericResult> localContext =
          getLocalVariables(dataSource, val, this.getVariables(),
              ImmutableMap.copyOf(contextValues));
      return new GenericResult(
          this.data.evaluate(dataSource, ImmutableMap.copyOf(localContext)));

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


  private static Map<String, GenericResult> getLocalVariables(InputData dataExtractor,
      Object hl7Value,
      List<Variable> variableNames,
      ImmutableMap<String, GenericResult> contextValues) {
    Map<String, GenericResult> localVariables = new HashMap<>(contextValues);

    if (hl7Value != null) {
      String type = getDataType(hl7Value);

      LOGGER.info(type);
      localVariables.put(type, new GenericResult(hl7Value));
    }

    localVariables
        .putAll(dataExtractor.resolveVariables(variableNames, ImmutableMap.copyOf(localVariables)));

    return ImmutableMap.copyOf(localVariables);
  }




  public String getReference() {
    return reference;
  }


}
