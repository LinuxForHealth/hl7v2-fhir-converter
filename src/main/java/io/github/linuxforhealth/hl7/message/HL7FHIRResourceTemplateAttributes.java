/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.ResourceCondition;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

@JsonDeserialize(builder = HL7FHIRResourceTemplateAttributes.Builder.class)
public class HL7FHIRResourceTemplateAttributes {

  private String resourceName;
  private boolean repeats;
  private String resourcePath;
  private boolean isReferenced;
  private boolean ignoreEmpty;
  private ResourceCondition condition;
  private String conditionExpression;
  private HL7Segment segment;// primary segment
  private List<HL7Segment> additionalSegments;
  private ResourceModel resource;
  private List<String> group;


  public HL7FHIRResourceTemplateAttributes(Builder builder) {
    Preconditions.checkArgument(StringUtils.isNotBlank(builder.resourceName),
        "resourceName cannot be null");

    this.resourceName = builder.resourceName;
    this.resourcePath = builder.resourcePath;
    this.repeats = builder.repeats;
    this.isReferenced = builder.isReferenced;
    this.ignoreEmpty = builder.ignoreEmpty;
    this.condition = builder.condition;
    this.conditionExpression = builder.conditionExpression;
    additionalSegments = new ArrayList<>();
    builder.rawAdditionalSegments
        .forEach(e -> additionalSegments.add(HL7Segment.parse(e, builder.group)));
    this.segment = HL7Segment.parse(builder.rawSegment, builder.group);
    Preconditions.checkArgument(this.segment != null, "primary segment cannot be null");
    if (builder.resourceModel != null) {
      this.resource = builder.resourceModel;
    } else {
      this.resource = generateResourceModel(resourcePath);
    }
    Preconditions.checkArgument(this.resource != null, "Resource model cannot be null");
    this.group = HL7Segment.parseGroup(builder.group);
  }



  public ResourceModel getResource() {
    return this.resource;
  }

  public HL7Segment getSegment() {
    return segment;
  }


  public List<HL7Segment> getAdditionalSegments() {
    return new ArrayList<>(additionalSegments);
  }

  public List<String> getGroup() {
    return new ArrayList<>(group);
  }


  public String getResourceName() {
    return resourceName;
  }



  public boolean isRepeats() {
    return repeats;
  }



  public boolean isReferenced() {
    return isReferenced;
  }

  public boolean ignoreEmpty() {
    return ignoreEmpty;
  }

  public ResourceCondition condition() {
    return condition;
  }

  public String conditionExpression() {
      return conditionExpression;
  }

  private static ResourceModel generateResourceModel(String resourcePath) {
    return ResourceReader.getInstance().generateResourceModel(resourcePath);
  }



  public static class Builder {


    private String resourceName;
    private String rawSegment;
    private List<String> rawAdditionalSegments;
    private String resourcePath;
    private String group;
    private boolean isReferenced;
    private boolean ignoreEmpty;
    private ResourceCondition condition;
    private String conditionExpression;
    private boolean repeats;
    private ResourceModel resourceModel;

    public Builder() {
      this.rawAdditionalSegments = new ArrayList<>();
    }



    public Builder withResourceName(String resourceName) {
      this.resourceName = resourceName;
      return this;
    }



    public Builder withSegment(String rawSegment) {
      this.rawSegment = rawSegment;
      return this;
    }



    public Builder withAdditionalSegments(List<String> rawAdditionalSegments) {
      if (rawAdditionalSegments != null) {
        this.rawAdditionalSegments = new ArrayList<>(rawAdditionalSegments);
      }
      return this;
    }



    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    public Builder withGroup(String group) {
      this.group = group;
      return this;
    }



    public Builder withRepeats(boolean repeats) {
      this.repeats = repeats;
      return this;
    }

    public Builder withIsReferenced(boolean isReferenced) {
      this.isReferenced = isReferenced;
      return this;
    }

    public Builder withignoreEmpty(boolean ignoreEmpty) {
      this.ignoreEmpty = ignoreEmpty;
      return this;
    }

    public Builder withCondition(String conditionExpr) {
      this.conditionExpression = conditionExpr;
      this.condition = new HL7FHIRResourceCondition(conditionExpr);
      return this;
    }

    public Builder withResourceModel(ResourceModel resourceModel) {
      this.resourceModel = resourceModel;
      return this;
    }

    public HL7FHIRResourceTemplateAttributes build() {
      return new HL7FHIRResourceTemplateAttributes(this);
    }

  }



}
