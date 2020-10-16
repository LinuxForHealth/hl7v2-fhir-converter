/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.resource;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import com.ibm.linuxforhealth.api.EvaluationResult;
import com.ibm.linuxforhealth.api.ResourceValue;

public class ResourceResult implements EvaluationResult {

  private String groupId;
  private ResourceValue resource;
  private List<ResourceValue> additionalResources;


  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources) {
    Preconditions.checkArgument(resource != null, "resources cannot be null.");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null.");
    this.resource = resource;
    this.additionalResources = new ArrayList<>(additionalResources);
  }


  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources,
      String groupId) {
    Preconditions.checkArgument(resource != null, "resources cannot be null.");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null.");
    this.resource = resource;
    this.additionalResources = new ArrayList<>(additionalResources);
    this.groupId = groupId;
  }


  @Override
  public List<ResourceValue> getAdditionalResources() {
    return new ArrayList<>(additionalResources);
  }

  public String getGroupId() {
    return groupId;
  }


  @Override
  public ResourceValue getValue() {
    return this.resource;
  }


  @Override
  public String getIdentifier() {
    return null;
  }


  @Override
  public Class<?> getValueType() {
    return ResourceValue.class;
  }


  @Override
  public boolean isEmpty() {
    return (this.resource == null || this.resource.isEmpty());
  }
}
