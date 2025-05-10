/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.FHIRResourceTemplate;
import io.github.linuxforhealth.api.ResourceCondition;
import io.github.linuxforhealth.api.ResourceModel;



public class HL7FHIRResourceTemplate implements FHIRResourceTemplate {
  private HL7FHIRResourceTemplateAttributes attributes;// primary segment


  public HL7FHIRResourceTemplate(HL7FHIRResourceTemplateAttributes attributes) {
    Preconditions.checkArgument(attributes != null,
        "HL7FHIRResourceTemplateAttributes cannot be null");
    this.attributes = attributes;

  }


  @Override
  public ResourceModel getResource() {
    return attributes.getResource();
  }



  @Override
  public String getResourceName() {
    return this.attributes.getResourceName();
  }


  public HL7FHIRResourceTemplateAttributes getAttributes() {
    return attributes;
  }


  @Override
  public boolean isGenerateMultiple() {
    return this.attributes.isRepeats();
  }


  @Override
  public boolean isReferenced() {
    return this.attributes.isReferenced();
  }

  @Override
  public boolean ignoreEmpty() {
    return this.attributes.ignoreEmpty();
  }

  @Override
  public String conditionExpression() {
    return this.attributes.conditionExpression();
  }

  // The conditionExpression above gets parsed into a ResourceCondition
  public ResourceCondition condition() {
    return this.attributes.condition();
 }
}
