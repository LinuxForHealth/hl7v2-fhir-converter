package com.ibm.whi.core.resource;

import java.util.List;
import java.util.Map;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.message.InputData;


public interface ResourceModel {

  Object evaluateSingle(InputData dataExtractor, Map<String, GenericResult> contextValues,
      GenericResult baseValue);

  List<?> evaluateMultiple(InputData dataExtractor, Map<String, GenericResult> contextValues,
      List<GenericResult> baseValues, List<Variable> variables);

  Map<String, Expression> getExpressions();


  int getOrder();

  String getSpec();


  String getName();




}
