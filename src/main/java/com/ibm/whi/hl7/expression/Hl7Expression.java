package com.ibm.whi.hl7.expression;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.data.ValueExtractor;



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
    this(type, hl7spec, null, false, null, null);
  }

  @JsonCreator
  public Hl7Expression(@JsonProperty("type") String type, @JsonProperty("hl7spec") String hl7spec,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition) {
    super(type, defaultValue, required, hl7spec, variables, condition);

  }

  @Override
  public GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");


    Object hl7Value = getSingleValue(hl7SpecValues);
    LOGGER.info("Evaluating expression type {} , hl7spec {} returned hl7 value {} ", this.getType(),
        this.getspecs(), hl7Value);
    Object resolvedValue = null;

    ValueExtractor<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(this.getType());
    if (resolver != null && hl7Value != null) {
      resolvedValue = resolver.apply(hl7Value);
      LOGGER.info("Evaluating expression type {} , hl7spec {} resolved value {} ", this.getType(),
          this.getspecs(), resolvedValue);
    }
    if (resolvedValue != null) {
      return new GenericResult(resolvedValue);
    } else {
      return new GenericResult(this.getDefaultValue());
    }
  }





}
