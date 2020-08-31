package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.hl7.data.DataEvaluator;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultExpression extends AbstractExpression {

  private String value;


  @JsonCreator
  public DefaultExpression(String var) {
    super("CONSTANT", null, false, "", new HashMap<>());
    this.value = var;
  }

  @JsonCreator
  public DefaultExpression(@JsonProperty("type") String type, @JsonProperty("value") String var) {
    super(type, null, false, "", new HashMap<>());
    this.value = var;
  }


  public String getValue() {
    return value;
  }


  @Override
  public Object execute(Map<String, Object> context) {
    if (isVar(value)) {
      Object obj = getVariableValue(value, context);
      if (obj != null) {
        DataEvaluator<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
      return resolver.apply(obj.toString());
      }
      return obj;
    } else {
    return this.value;
    }
  }




}
