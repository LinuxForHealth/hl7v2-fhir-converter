package com.ibm.whi.core.expression;

/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author pbhallam
 */
public class GenericResult {


  private Object value;

  public GenericResult(Object value) {
    this.value = value;
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


}
