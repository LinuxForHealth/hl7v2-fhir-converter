package com.ibm.whi.core.resource;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Preconditions;

public class ResourceValue {

  private Map<String, Object> resource;
  private String resourceClass;

  public ResourceValue(Map<String, Object> resource, String resourceClass) {
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


  public String getResourceClass() {
    return resourceClass;
  }


}
