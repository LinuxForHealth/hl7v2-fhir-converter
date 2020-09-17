/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression;

import java.util.ArrayList;
import java.util.List;
import com.ibm.whi.core.expression.util.GeneralUtil;
import com.ibm.whi.core.resource.ResourceValue;

/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author pbhallam
 */
public class GenericResult {


  private Object value;
  private Class<?> klass;
  private String klassName;
  private List<ResourceValue> additionalResources;


  public GenericResult(Object value) {
    this(value, new ArrayList<>());
  }



  public GenericResult(Object value, List<ResourceValue> additionalResources) {

    this.value = value;
    if (value != null) {
      this.klass = value.getClass();
      this.klassName = GeneralUtil.getDataType(value);
    }
    this.additionalResources = additionalResources;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    if (value != null) {
      return "Type: [" + value.getClass().getName() + "] Value : [" + value.toString() + "]";
    } else {
      return "";
    }
  }


  public Class<?> getKlass() {
    return klass;
  }





  public String getKlassName() {
    return klassName;
  }

  public boolean isEmpty() {
    if (this.value == null) {
      return true;
    }
    return false;
  }




  public List<ResourceValue> getAdditionalResources() {
    return additionalResources;
  }



}
