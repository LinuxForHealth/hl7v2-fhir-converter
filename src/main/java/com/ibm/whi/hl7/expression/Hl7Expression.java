package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.hl7.data.DataEvaluator;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import ca.uhn.hl7v2.model.Visitable;


/**
 * Represents the HL7 expression which can generate the extraction string that HAPI terser can
 * evaluate. Supports the following structure <br>
 * 
 *
 * @author pbhallam@us.ibm.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hl7Expression extends AbstractExpression {


  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7Expression.class);


  public Hl7Expression(@JsonProperty("type") String type, @JsonProperty("hl7spec") String hl7spec) {
    this(type, hl7spec, null, false);
  }

  @JsonCreator
  public Hl7Expression(@JsonProperty("type") String type, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required) {
    super(type, defaultValue, required, hl7spec, new HashMap<>());

  }

  @Override
  public Object execute(Map<String, Object> context) {
    LOGGER.info("Evaluating expression type {} , hl7spec {}", this.getType(), this.getHl7specs());
  
    Visitable hl7Value = getValueFromSpecs(this.getHl7specs(), context);
    LOGGER.info("Evaluating expression type {} , hl7spec {} returned hl7 value {} ", this.getType(),
        this.getHl7specs(), hl7Value);
    Object resolvedValue = null;

    DataEvaluator<Object, ?> resolver =
        SimpleDataTypeMapper.getValueResolver(this.getType());
    if (resolver != null) {
      resolvedValue = resolver.apply(hl7Value);
      LOGGER.info("Evaluating expression type {} , hl7spec {} resolved value {} ", this.getType(),
          this.getHl7specs(), resolvedValue);
    }



    if (resolvedValue != null) {
      return resolvedValue;
    } else {
      return this.getDefaultValue();
    }
  }



}
