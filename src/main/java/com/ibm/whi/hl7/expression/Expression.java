package com.ibm.whi.hl7.expression;

import java.util.List;
import java.util.Map;

// @JsonDeserialize(using = ExpressionlDeserializer.class)

public interface Expression {
  String getType();
  Object getDefaultValue();

  List<String> getHl7specs();

  Object execute(Map<String, Object> context);


}
