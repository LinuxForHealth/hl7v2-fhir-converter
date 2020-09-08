package com.ibm.whi.core.expression;

import com.ibm.whi.core.expression.util.GeneralUtil;

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

  public GenericResult(Object value) {

    this.value = value;
    if (value != null) {
    this.klass = value.getClass();
    this.klassName = GeneralUtil.getDataType(value);
    }
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


}
