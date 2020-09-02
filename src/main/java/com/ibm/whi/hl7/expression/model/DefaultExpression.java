package com.ibm.whi.hl7.expression.model;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.hl7.data.DataEvaluator;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExpression.class);

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
    LOGGER.info("Evaluating {}", this.value);
    if (isVar(value)) {
      Object obj = getVariableValue(value, context);
      LOGGER.info("Evaluated value {} to {} ", this.value, obj);
      if (obj != null) {
        LOGGER.info("Evaluated value {} to {} type {} ", this.value, obj, obj.getClass());
        DataEvaluator<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
        return resolver.apply(obj);
      }
      return obj;
    } else {
    return this.value;
    }
  }




}
