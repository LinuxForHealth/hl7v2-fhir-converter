/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.expression.specification;

import java.util.Map;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.hl7.message.HL7MessageData;


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
  private Class<? extends InputDataExtractor> sourceInputDataClass = HL7MessageData.class;
  
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


  public Class<? extends InputDataExtractor> getSourceInputDataClass() {
    return sourceInputDataClass;
  }


  @Override
  public EvaluationResult extractValueForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues) {
    if (this.isExtractMultiple()) {
      return dataSource.extractMultipleValuesForSpec(this, contextValues);
    } else {
    return dataSource.extractValueForSpec(this, contextValues);
    }
  }


  @Override
  public EvaluationResult extractMultipleValuesForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues) {
    return dataSource.extractMultipleValuesForSpec(this, contextValues);
  }



}
