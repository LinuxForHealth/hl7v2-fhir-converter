package com.ibm.whi.core.resource;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class ResourceResult {

  private List<ResourceValue> resources;
  private List<ResourceValue> additionalResources;

  public ResourceResult(List<ResourceValue> resources) {
    this(resources, new ArrayList<>());
  }

  public ResourceResult(ResourceValue resource) {
    this(Lists.newArrayList(resource));
    Preconditions.checkArgument(resource != null, "resource cannot be null");

  }


  public ResourceResult(ResourceValue resource, ResourceValue additionalResource) {
    this(Lists.newArrayList(resource), Lists.newArrayList(additionalResource));
  }

  public ResourceResult(List<ResourceValue> resources, List<ResourceValue> additionalResources) {
    Preconditions.checkArgument(resources != null && !resources.isEmpty(),
        "resources cannot be null or empty.");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null.");
    this.resources = new ArrayList<>(resources);
    this.additionalResources = new ArrayList<>(additionalResources);
  }

  public List<ResourceValue> getResources() {
    return new ArrayList<>(resources);
  }

  public List<ResourceValue> getAdditionalResources() {
    return new ArrayList<>(additionalResources);
  }

}
