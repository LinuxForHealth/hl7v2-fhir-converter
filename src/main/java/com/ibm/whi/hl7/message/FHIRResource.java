package com.ibm.whi.hl7.message;

import java.util.ArrayList;
import java.util.List;
import com.ibm.whi.hl7.resource.ResourceModel;

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
    this.additionalSegments.addAll(additionalSegments);
    this.segment = segment;
    this.resource = resource;
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
