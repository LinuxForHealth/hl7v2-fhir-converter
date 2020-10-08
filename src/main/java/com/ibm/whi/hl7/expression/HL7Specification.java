/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import com.ibm.whi.api.Specification;
import com.ibm.whi.hl7.message.util.SupportedSegments;


/**
 * Represents HL7 data specification. It defines segment, field, component and subcomponent
 * names/identifiers that can be used for extracting data.
 * 
 *
 * @author pbhallam
 */

public class HL7Specification implements Specification {

  private String segment;
  private String field;
  private int component;
  private int subComponent;
  private boolean isExtractMultiple;
  private String stringRep;
  
  public HL7Specification(String segment, String field, int component, int subComponent,
      boolean isMultiple) {
    this.segment = segment;
    this.field = field;
    this.component = component;
    this.subComponent = subComponent;
    this.isExtractMultiple = isMultiple;
    this.stringRep = getToStringRep();
  }


  public HL7Specification(String segment, String field, int component, int subComponent) {
    this(segment, field, component, subComponent, false);

  }

  public String getSegment() {
    return segment;
  }

  public String getField() {
    return field;
  }

  public int getComponent() {
    return component;
  }

  public int getSubComponent() {
    return subComponent;
  }


  public static Specification parse(String rawSpec, boolean extractMultiple) {
    StringTokenizer stk = new StringTokenizer(rawSpec, ".");
    String segment = null;
    String field = null;
    int component = -1;
    int subComponent = -1;
    if (stk.hasNext()) {
      String tok = stk.next();
      if (EnumUtils.isValidEnumIgnoreCase(SupportedSegments.class, tok)) {
        segment = tok;
        if (stk.hasNext()) {
          field = stk.nextToken();
        }
        if (stk.hasNext()) {
          component = NumberUtils.toInt(stk.nextToken());
        }


      } else {
        field = tok;
        if (stk.hasNext()) {
          component = NumberUtils.toInt(stk.nextToken());
        }
        if (stk.hasNext()) {
          subComponent = NumberUtils.toInt(stk.nextToken());
        }
      }
    }


    return new HL7Specification(segment, field, component, subComponent, extractMultiple);

  }

  @Override
  public String toString() {
    return this.stringRep;


  }

  private String getToStringRep() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    if (segment != null) {
      sb.append(segment);
    }

    if (field != null) {
      sb.append(".").append(field);
    }
    if (component > -1) {
      sb.append(".").append(component);
    }
    if (subComponent > -1) {
      sb.append(".").append(subComponent);
    }

    return sb.append("]").toString();


  }


  public boolean isExtractMultiple() {
    return isExtractMultiple;
  }


}
