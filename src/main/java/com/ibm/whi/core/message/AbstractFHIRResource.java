package com.ibm.whi.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.whi.core.resource.ResourceModel;


public abstract class AbstractFHIRResource {


  private String resourceName;
  private int order;
  private boolean repeats;
  private String resourcePath;



  @JsonCreator
  public AbstractFHIRResource(@JsonProperty("resourceName") String resourceName,
      @JsonProperty("resourcePath") String resourcePath, @JsonProperty("order") int order,
      @JsonProperty("repeats") boolean repeats) {
    this.resourceName = resourceName;
    this.resourcePath = resourcePath;
    this.order = order;
    this.repeats = repeats;
  }


  public AbstractFHIRResource(String resourceName, String resourcePath, int order) {
    this(resourceName, resourcePath, order, false);
  }

  public String getResourceName() {
    return resourceName;
  }




  public abstract ResourceModel getResource();


  public int getOrder() {
    return order;
  }


  public boolean isRepeats() {
    return repeats;
  }


  public String getResourcePath() {
    return resourcePath;
  }


  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }






}
