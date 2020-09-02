package com.ibm.whi.hl7.message;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.hl7.resource.ResourceModel;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FHIRResource {



  private String resourceName;
  private String segment;// primary segment
  private List<String> additionalSegments;
  private ResourceModel resource;
  private int order;
  private boolean repeates;

  public FHIRResource(String resourceName, String segment, ResourceModel resource, int order,
      boolean repeates, List<String> additionalSegments) {
    this.resourceName = resourceName;
    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      this.additionalSegments.addAll(additionalSegments);
    }
    this.segment = segment;
    this.resource = resource;
    this.order = order;
    this.repeates = repeates;
  }

  @JsonCreator
  public FHIRResource(@JsonProperty("resourceName") String resourceName,
      @JsonProperty("segment") String segment, @JsonProperty("resourcePath") String resourcePath,
      @JsonProperty("order") int order, @JsonProperty("repeates") boolean repeates,
      @JsonProperty("additionalSegments") List<String> additionalSegments) {
    this.resourceName = resourceName;
    this.additionalSegments = new ArrayList<>();
    if (additionalSegments != null) {
      this.additionalSegments.addAll(additionalSegments);
    }
    this.segment = segment;
    this.resource = ResourceModel.generateResourceModel(resourcePath);
    this.order = order;
    this.repeates = repeates;
  }


  public FHIRResource(String resourceName, String segment, ResourceModel resource,
      int order) {
    this(resourceName, segment, resource, order, false, new ArrayList<>());
  }

  public String getResourceName() {
    return resourceName;
  }


  public List<String> getAdditionalSegments() {
    return additionalSegments;
  }


  public ResourceModel getResource() {
    return resource;
  }

  public int getOrder() {
    return order;
  }


  public boolean isRepeates() {
    return repeates;
  }


  public String getSegment() {
    return segment;
  }




}
