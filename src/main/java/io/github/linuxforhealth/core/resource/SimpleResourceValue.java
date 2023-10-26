/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.resource;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.ResourceValue;

public class SimpleResourceValue implements ResourceValue {

  private Map<String, Object> resource;
  private String resourceClass;

  public SimpleResourceValue(Map<String, Object> resource, String resourceClass) {
    Preconditions.checkArgument(resource != null && !resource.isEmpty(),
        "resource cannot be null or empty.");
    Preconditions.checkArgument(StringUtils.isNotBlank(resourceClass),
        "resourceName cannot be null or blank.");
    this.resource = resource;
    this.resourceClass = resourceClass;
  }

  public Map<String, Object> getResource() {
    return resource;
  }


  public String getFHIRResourceType() {
    return resourceClass;
  }

  @Override
  public boolean isEmpty() {
   return (resource==null || resource.isEmpty());
  }

  @Override
  public String toString() {
    return resourceClass + " " + resource.toString();
  }
}
