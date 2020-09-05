package com.ibm.whi.core.resource;

import java.util.Map;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;


public interface ResourceModel {


  Object evaluate(InputData dataExtractor, Map<String, GenericResult> contextValues);

  Map<String, Expression> getExpressions();


  int getOrder();

  String getSpec();


  String getName();




}
