package com.ibm.whi.hl7.expression;

/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author {user}
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
      return "Value : [" + value.toString() + "]";
    } else {
      return "";
    }
  }


}
