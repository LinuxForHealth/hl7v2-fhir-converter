/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.resource;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.hl7.resource.PendingExpressionState;

public class ResourceResult implements EvaluationResult {

  private String groupId;
  private ResourceValue resource;
  private List<ResourceValue> additionalResources;
  private PendingExpressionState pendingExpressions;

  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources) {
    this(resource, additionalResources, null);
  }


  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources,
      String groupId) {
    this(resource, additionalResources, groupId,
        PendingExpressionState.emptyPendingExpressionState());
  }


  public ResourceResult(ResourceValue resource, List<ResourceValue> additionalResources,
      String groupId, PendingExpressionState pendingExpressions) {
    Preconditions.checkArgument(resource != null, "resources cannot be null.");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null.");
    this.resource = resource;
    this.additionalResources = new ArrayList<>(additionalResources);
    this.groupId = groupId;
    this.pendingExpressions = pendingExpressions;
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


  public PendingExpressionState getPendingExpressions() {
    return pendingExpressions;
  }
}
