/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.core.message.AbstractFHIRResourceTemplate;
import io.github.linuxforhealth.hl7.resource.ResourceModelReader;


@JsonIgnoreProperties(ignoreUnknown = true)
public class HL7FHIRResourceTemplate extends AbstractFHIRResourceTemplate {
  private HL7Segment segment;// primary segment
  private List<HL7Segment> additionalSegments;
  private ResourceModel resource;
  private String group;


  public HL7FHIRResourceTemplate(String resourceName, String segment, ResourceModel resource,
      boolean isReferenced, boolean repeats, List<String> additionalSegments, String group) {
    super(resourceName, "", isReferenced, repeats);
    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      additionalSegments.forEach(e -> this.additionalSegments.add(HL7Segment.parse(e)));
    }

    this.segment = HL7Segment.parse(segment);
    this.resource = resource;
    this.group = group;
  }

  public HL7FHIRResourceTemplate(String resourceName, String segment, ResourceModel resource,
      boolean isReferenced, boolean repeats, List<String> additionalSegments) {
    this(resourceName, segment, resource, isReferenced, repeats, additionalSegments, null);
  }

  @JsonCreator
  public HL7FHIRResourceTemplate(@JsonProperty("resourceName") String resourceName,
      @JsonProperty("segment") String segment, @JsonProperty("resourcePath") String resourcePath,
      @JsonProperty("isReferenced") boolean isReferenced, @JsonProperty("repeats") boolean repeats,
      @JsonProperty("additionalSegments") List<String> additionalSegments,
      @JsonProperty("group") String group) {
    super(resourceName, resourcePath, isReferenced, repeats);

    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      additionalSegments.forEach(e -> this.additionalSegments.add(HL7Segment.parse(e)));
    }

    this.segment = HL7Segment.parse(segment);
    this.resource = generateResourceModel(resourcePath);
    this.group = group;
  }






  public static ResourceModel generateResourceModel(String resourcePath) {
    return ResourceModelReader.getInstance().generateResourceModel(resourcePath);
  }

  @Override
  public ResourceModel getResource() {
    return this.resource;
  }

  public HL7Segment getSegment() {
    return segment;
  }


  public List<HL7Segment> getAdditionalSegments() {
    return new ArrayList<>(additionalSegments);
  }

  public String getGroup() {
    return group;
  }



}
