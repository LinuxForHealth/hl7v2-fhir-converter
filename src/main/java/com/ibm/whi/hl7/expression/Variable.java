package com.ibm.whi.hl7.expression;

import java.util.List;

public class Variable {
  public static final String OBJECT_TYPE = "Object";
  private String name;
  private String type;
  private List<String> spec;


  public Variable(String name, List<String> spec) {
    this(name, spec, OBJECT_TYPE);
  }

  public Variable(String name, List<String> spec, String type) {
    this.name = name;
    this.spec = spec;
    this.type = type;
  }

  public List<String> getSpec() {
    return spec;
  }



  public String getType() {
    return type;
  }


  public String getName() {
    return name;
  }


}
