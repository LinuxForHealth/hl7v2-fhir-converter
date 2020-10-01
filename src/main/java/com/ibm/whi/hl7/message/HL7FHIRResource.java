/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.api.ResourceModel;
import com.ibm.whi.core.message.AbstractFHIRResource;
import com.ibm.whi.hl7.resource.ResourceModelReader;


@JsonIgnoreProperties(ignoreUnknown = true)
public class HL7FHIRResource extends AbstractFHIRResource {
  private String segment;// primary segment
  private List<String> additionalSegments;
  private ResourceModel resource;

  public HL7FHIRResource(String resourceName, String segment, ResourceModel resource, int order,
      boolean repeats, List<String> additionalSegments) {
    super(resourceName, "", order, repeats);
    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      this.additionalSegments.addAll(additionalSegments);
    }
    this.segment = segment;
    this.resource = resource;
  }

  public HL7FHIRResource(String resourceName, String segment, ResourceModel resource, int order,
      boolean repeats) {
    this(resourceName, segment, resource, order, repeats, new ArrayList<>());
  }

  @JsonCreator
  public HL7FHIRResource(@JsonProperty("resourceName") String resourceName,
      @JsonProperty("segment") String segment, @JsonProperty("resourcePath") String resourcePath,
      @JsonProperty("order") int order, @JsonProperty("repeats") boolean repeats,
      @JsonProperty("additionalSegments") List<String> additionalSegments) {
    super(resourceName, resourcePath, order, repeats);

    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      this.additionalSegments.addAll(additionalSegments);
    }
    this.segment = segment;
    this.resource = generateResourceModel(resourcePath);
  }


  public static ResourceModel generateResourceModel(String resourcePath) {
    return ResourceModelReader.getInstance().generateResourceModel(resourcePath);
  }

  @Override
  public ResourceModel getResource() {
    return this.resource;
  }

  public String getSegment() {
    return segment;
  }



}
