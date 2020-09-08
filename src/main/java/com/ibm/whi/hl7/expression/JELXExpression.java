package com.ibm.whi.hl7.expression;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;


@JsonIgnoreProperties(ignoreUnknown = true)
public class JELXExpression extends AbstractExpression {
  private static final Logger LOGGER = LoggerFactory.getLogger(JELXExpression.class);
  private static final WHIAJexlEngine JEXL = new WHIAJexlEngine();
  private String evaluate;




  @JsonCreator
  public JELXExpression(@JsonProperty("type") String type,
      @JsonProperty("default") Object defaultValue, @JsonProperty("required") boolean required,
      @JsonProperty("hl7spec") String hl7spec, @JsonProperty("evaluate") String evaluate,
      @JsonProperty("var") Map<String, String> variables,
      @JsonProperty("condition") String condition) {
    super(type, defaultValue, required, hl7spec, variables, condition);
    Preconditions.checkArgument(StringUtils.isNotBlank(evaluate), "evaluate file cannot be blank");


    this.evaluate = evaluate;


  }


  public JELXExpression(String evaluate, Map<String, String> variables) {
    this("String", null, false, null, evaluate, variables, null);


  }


  @Override
  public GenericResult evaluateExpression(InputData dataSource,
      Map<String, GenericResult> contextValues, GenericResult hl7SpecValues) {
    Preconditions.checkArgument(dataSource != null, "dataSource cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    LOGGER.info("Evaluating expression {}", this.evaluate);
    Map<String, Object> localContext = new HashMap<>();

    Map<String, GenericResult> resolvedVariables = new HashMap<>(contextValues);
    resolvedVariables
        .forEach((key, value) -> localContext.put(key, value.getValue()));
    Object obj = JEXL.evaluate(this.evaluate, localContext);
    LOGGER.info("Evaluated expression {}, returning {}", this.evaluate, obj);
    if (obj != null) {
      return new GenericResult(obj);
    } else {
      return new GenericResult(this.getDefaultValue());
    }
  }




}
