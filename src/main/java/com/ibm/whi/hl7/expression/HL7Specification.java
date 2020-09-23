/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.expression;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import com.ibm.whi.core.expression.Specification;
import com.ibm.whi.hl7.message.SupportedSegments;


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

  public HL7Specification(String segment, String field, int component, int subComponent,
      boolean isMultiple) {
    this.segment = segment;
    this.field = field;
    this.component = component;
    this.subComponent = subComponent;
    this.isExtractMultiple = isMultiple;

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


  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }


  public boolean isExtractMultiple() {
    return isExtractMultiple;
  }


}
