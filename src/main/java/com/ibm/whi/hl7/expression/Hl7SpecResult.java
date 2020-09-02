package com.ibm.whi.hl7.expression;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import ca.uhn.hl7v2.model.Visitable;

public class Hl7SpecResult {


  private List<Visitable> hl7DatatypeValue;


  private String textValue;

  public Hl7SpecResult(List<Visitable> hl7DatatypeValue) {
    this.hl7DatatypeValue = new ArrayList<>();
    this.hl7DatatypeValue.addAll(hl7DatatypeValue);
    this.textValue = null;
  }

  public Hl7SpecResult(String textValue) {
    hl7DatatypeValue = new ArrayList<>();
    this.textValue = textValue;

  }

  public String getTextValue() {
    return textValue;
  }

  public List<Visitable> getHl7DatatypeValue() {
    return hl7DatatypeValue;
  }

  public boolean isNotEmpty() {
    return (!this.getHl7DatatypeValue().isEmpty() || StringUtils.isNotBlank(this.getTextValue()));
  }

  public boolean isEmpty() {
    return !isNotEmpty();
  }

}
