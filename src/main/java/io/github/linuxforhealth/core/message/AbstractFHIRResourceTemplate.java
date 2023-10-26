/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linuxforhealth.api.FHIRResourceTemplate;


public abstract class AbstractFHIRResourceTemplate implements FHIRResourceTemplate {


  private String resourceName;
  private boolean repeats;
  private String resourcePath;
  private boolean isReferenced;
  private boolean ignoreEmpty;
  private String condition;
  private String resourceProfile;

  @JsonCreator
  public AbstractFHIRResourceTemplate(@JsonProperty("resourceName") String resourceName,
      @JsonProperty("resourcePath") String resourcePath,
      @JsonProperty("isReferenced") boolean isReferenced,
      @JsonProperty("repeats") boolean repeats,
      @JsonProperty("ignoreEmpty") boolean ignoreEmpty,
      @JsonProperty("condition") String conditionExpression,
      @JsonProperty("profile") String resourceProfile) {
    this.resourceName = resourceName;
    this.resourcePath = resourcePath;
    this.repeats = repeats;
    this.isReferenced = isReferenced;
    this.ignoreEmpty = ignoreEmpty;
    this.condition = conditionExpression;
    this.resourceProfile = resourceProfile;
  }


  public AbstractFHIRResourceTemplate(String resourceName, String resourcePath) {
    this(resourceName, resourcePath, false, false, false, null, null);
  }

  @Override
  public String getResourceName() {
    return resourceName;
  }




  @Override
  public boolean isGenerateMultiple() {
    return repeats;
  }


  public String getResourcePath() {
    return resourcePath;
  }


  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }


  @Override
  public boolean isReferenced() {
    return isReferenced;
  }

  @Override
  public boolean ignoreEmpty() {
    return ignoreEmpty;
  }

  @Override
  public String conditionExpression() {
    return condition;
  }

  @Override
  public String getResourceProfile() {
    return resourceProfile;
  }
}
