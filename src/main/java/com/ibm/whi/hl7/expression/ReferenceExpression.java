package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import com.ibm.whi.hl7.resource.ResourceModel;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Visitable;

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

      List<Visitable> dataValues = getRepts(this.getHl7specs(), context);
      List<Object> resolvedvalues = new ArrayList<>();

      for (Visitable o : dataValues) {
        Map<String, Object> localContext = getLocalContext(o, this.getVariables(), context);

        resolvedvalues.add(this.data.evaluate(localContext));
      }
      resolvedvalues.removeIf(Objects::isNull);
      if (resolvedvalues.isEmpty()) {
        return null;
      } else {
        return resolvedvalues;
      }



    } else {
      Visitable hl7Value = getValueFromSpecs(this.getHl7specs(), context);
      return this.data.evaluate(getLocalContext(hl7Value, this.getVariables(), context));

    }



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


  /**
   * Gets the repetition based on hl7spec for the reference resource.
   * 
   * @param hl7specs
   * @param context
   * @return
   */
  static List<Visitable> getRepts(List<String> hl7specs, Map<String, Object> context) {

    List<String> specs = new ArrayList<>(hl7specs);
    specs.removeIf(String::isEmpty);
    for (String hl7specValue : specs) {
      List<Visitable> fetchedValues = new ArrayList<>();
        if (isVar(hl7specValue)) {

        Object o = context.get(hl7specValue.replace("$", ""));
        if (o instanceof List) {
          fetchedValues.addAll((Collection<? extends Visitable>) o);
        } else if (o instanceof Visitable) {
          fetchedValues.add((Visitable) o);
        }

        } else {
          String[] tokens = StringUtils.split(hl7specValue, HL7_SPEC_SPLITTER.pattern());
          Object obj = context.get(tokens[0]);
          Hl7DataExtractor hde = (Hl7DataExtractor) context.get("hde");
          if (obj instanceof Segment) {
            int field = NumberUtils.toInt(tokens[1]);
          fetchedValues.addAll((List<Visitable>) hde.getTypes((Segment) obj, field));
          } else if (obj instanceof Type) {
            int component = NumberUtils.toInt(tokens[1]);
          fetchedValues.add(hde.getComponent((Type) obj, component));
          } else {
          fetchedValues.addAll(hde.getAllStructures(tokens[0]));
          }

        }

      if (!fetchedValues.isEmpty()) {
          return fetchedValues;
        }

    }
    return new ArrayList<>();
  }


  public List<String> getReferencesResources() {
    return referencesResources;
  }



}
