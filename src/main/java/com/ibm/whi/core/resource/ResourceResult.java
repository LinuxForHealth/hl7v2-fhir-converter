/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.resource;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;

public class ResourceResult {

  private ResourceValue resource;
  private List<ResourceValue> additionalResources;


  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources) {
    Preconditions.checkArgument(resource != null, "resources cannot be null.");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null.");
    this.resource = resource;
    this.additionalResources = new ArrayList<>(additionalResources);
  }

  public ResourceValue getResource() {
    return this.resource;
  }

  public List<ResourceValue> getAdditionalResources() {
    return new ArrayList<>(additionalResources);
  }

}
