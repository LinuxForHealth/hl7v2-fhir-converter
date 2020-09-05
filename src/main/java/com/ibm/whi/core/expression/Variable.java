package com.ibm.whi.core.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines Variable object that can be used during the expression evaluation.
 * 
 *
 * @author pbhallam
 */
public class Variable {
  public static final String OBJECT_TYPE = Object.class.getSimpleName();
  private String name;
  private String type;
  private List<String> spec;

  /**
   * Constructor for Variable with default type: Object
   * 
   * @param name
   * @param spec
   */
  public Variable(String name, List<String> spec) {
    this(name, spec, OBJECT_TYPE);
  }

  /**
   * 
   * @param name
   * @param spec
   * @param type
   */
  public Variable(String name, List<String> spec, String type) {
    this.name = name;
    this.spec = new ArrayList<>();
    if (spec != null && !spec.isEmpty()) {
      this.spec.addAll(spec);
    }

    this.type = type;
  }

  public List<String> getSpec() {
    return new ArrayList<>(spec);
  }



  public String getType() {
    return type;
  }


  public String getName() {
    return name;
  }


}
