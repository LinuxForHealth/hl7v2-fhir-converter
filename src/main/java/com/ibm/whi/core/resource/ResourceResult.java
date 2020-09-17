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

  private List<ResourceValue> resources;
  private List<ResourceValue> additionalResources;


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
