/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.core.data.DataTypeUtil;
import com.ibm.whi.core.resource.ResourceValue;

/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author pbhallam
 */
public class SimpleEvaluationResult implements EvaluationResult {


  private Object value;
  private Class<?> klass;
  private String klassName;
  private List<ResourceValue> additionalResources;


  public SimpleEvaluationResult(Object value) {
    this(value, new ArrayList<>());
  }



  public SimpleEvaluationResult(Object value, List<ResourceValue> additionalResources) {
    Preconditions.checkArgument(value != null, "value cannot be null");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null");
    this.value = value;

    this.klass = value.getClass();
    this.klassName = DataTypeUtil.getDataType(value);

    this.additionalResources = new ArrayList<>();

    this.additionalResources.addAll(additionalResources);



  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    if (value != null) {
      return "Type: [" + this.klassName + "] Value : [" + value.toString() + "]";
    } else {
      return "";
    }
  }

  @Override
  public Class<?> getValueType() {
    return klass;
  }


  @Override
  public String getName() {
    return klassName;
  }

  @Override
  public boolean isEmpty() {
    if (this.value == null) {
      return true;
    }
    return false;
  }



  public List<ResourceValue> getAdditionalResources() {
    return new ArrayList<>(additionalResources);
  }



}
