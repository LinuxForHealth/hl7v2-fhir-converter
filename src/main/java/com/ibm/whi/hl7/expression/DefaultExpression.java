package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.data.ValueExtractor;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExpression.class);

  private String value;


  @JsonCreator
  public DefaultExpression(String var) {
    this("String", var);

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
  public GenericResult evaluate(InputData dataSource, Map<String, GenericResult> contextValues) {

    return convert(contextValues);
  }

  private GenericResult convert(Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.info("Evaluating {}", this.value);
    if (isVar(value)) {
      GenericResult obj =
          getVariableValueFromVariableContextMap(value, ImmutableMap.copyOf(contextValues));
      LOGGER.info("Evaluated value {} to {} ", this.value, obj);
      if (obj != null) {
        LOGGER.info("Evaluated value {} to {} type {} ", this.value, obj, obj.getClass());
        ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
        return new GenericResult(resolver.apply(obj.getValue()));
      }
      LOGGER.info("Evaluated {} returning null", this.value);
      return null;
    } else {
      LOGGER.info("Evaluated {} returning value enclosed as GenericResult.", this.value);
      return new GenericResult(this.value);
    }
  }





}
