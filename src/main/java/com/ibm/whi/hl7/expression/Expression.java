package com.ibm.whi.hl7.expression;

import java.util.List;
import com.google.common.collect.ImmutableMap;



public interface Expression {
  String getType();
  Object getDefaultValue();

  List<String> getHl7specs();

  GenericResult execute(ImmutableMap<String, ?> executables,
      ImmutableMap<String, GenericResult> varables);


}
