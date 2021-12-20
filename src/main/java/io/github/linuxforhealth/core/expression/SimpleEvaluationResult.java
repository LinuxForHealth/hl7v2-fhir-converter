/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.google.common.base.Preconditions;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.data.DataTypeUtil;


/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author pbhallam
 * @param <V> The value to be evaluated
 * 
 */
public class SimpleEvaluationResult<V> implements EvaluationResult {

  private UUID groupId;
  private V value;
  private Class<?> klass;
  private String klassName;
  private List<ResourceValue> additionalResources;


  public SimpleEvaluationResult(V value) {
    this(value, new ArrayList<>());
  }



  public SimpleEvaluationResult(V value, List<ResourceValue> additionalResources,
      UUID groupId) {
    Preconditions.checkArgument(value != null, "value cannot be null");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null");
    this.value = value;

    this.klass = value.getClass();
    this.klassName = DataTypeUtil.getDataType(value);
    this.additionalResources = new ArrayList<>();
    this.additionalResources.addAll(additionalResources);
    this.groupId = groupId;

  }


  public SimpleEvaluationResult(V value, List<ResourceValue> additionalResources) {
    Preconditions.checkArgument(value != null, "value cannot be null");
    Preconditions.checkArgument(additionalResources != null, "additionalResources cannot be null");
    this.value = value;

    this.klass = value.getClass();
    this.klassName = DataTypeUtil.getDataType(value);
    this.additionalResources = new ArrayList<>();
    this.additionalResources.addAll(additionalResources);

  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public String toString() {
    if (value != null) {
      return "Type: [" + this.klassName + "] Value : [" + value.toString() + "]";
    } else {
      return "";
    }
  }

  @Override
  public Class<?> getValueType() {
    return klass;
  }


  @Override
  public String getIdentifier() {
    return klassName;
  }

  @Override
  public boolean isEmpty() {
    return this.value == null;
  }

  public List<ResourceValue> getAdditionalResources() {
    return new ArrayList<>(additionalResources);
  }

  public UUID getGroupId() {
    return groupId;
  }



}
