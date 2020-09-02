package com.ibm.whi.hl7.expression.model;

import java.util.HashMap;
import java.util.Map;
import org.python.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.expression.GenericResult;

/**
 * Represent a expression that represents resolving a json template
 * 
 *
 * @author {user}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueExtractionGeneralExpression extends AbstractExpression {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValueExtractionGeneralExpression.class);


  private String fetch;



  /**
   * 
   * @param type
   * @param reference
   * @param hl7prefix
   * @param defaultValue
   * @param required
   * @param variables
   */
  @JsonCreator
  public ValueExtractionGeneralExpression(@JsonProperty("type") String type,
      @JsonProperty("fetch") String fetch, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("var") Map<String, String> variables) {
    super(type, defaultValue, required, hl7spec, variables);
    Preconditions.checkArgument(fetch != null && fetch.split(":").length >= 2,
        "value of fetch should include name of the resource and field. ");


    this.fetch = fetch;

  }


  public ValueExtractionGeneralExpression(String type, String fetch, String hl7spec) {
    this(type, fetch, hl7spec, null, false, null);
  }



  /**
   * Any expression that needs to extract value from another object will use fetch field.
   * example:'$ref-type:id' The expression will try and extract the value of the variable $ref-type
   * from the context.
   * 
   * 
   * @see com.ibm.whi.hl7.expression.Expression#execute(java.util.Map)
   */

  @Override
  public GenericResult execute(ImmutableMap<String, ?> executables,
      ImmutableMap<String, GenericResult> variables) {

    Map<String, GenericResult> resolvedVariables = new HashMap<>(variables);
    resolvedVariables.putAll(resolveVariables(this.getVariables(), executables, variables));
    Map<String, Object> localContext = new HashMap<>();
    resolvedVariables
        .forEach((key, value) -> localContext.put(key, value.getValue()));
    String[] token = fetch.split(":");
    if (token.length == 2) {
      Object resource = localContext.get(token[0].replace("$", ""));
    if (resource instanceof Map) {
      Map<String, Object> resourceMap = (Map<String, Object>) resource;
        return new GenericResult(resourceMap.get(token[1]));
      }
    }
    return null;
  }



}
