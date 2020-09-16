package com.ibm.whi.core.expression.condition;

import java.util.Map;
import com.ibm.whi.core.expression.GenericResult;

@FunctionalInterface
public interface Condition {

  boolean test(Map<String, GenericResult> contextVariables);
}
