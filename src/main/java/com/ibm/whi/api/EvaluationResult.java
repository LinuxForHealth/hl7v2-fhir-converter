/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.List;
import com.ibm.whi.core.resource.ResourceValue;

/**
 * Represents value returned after the expression is evaluated.
 * 
 *
 * @author pbhallam
 */
public interface EvaluationResult {


  Object getValue();

  String getName();


  Class<?> getValueType();


  boolean isEmpty();

  List<ResourceValue> getAdditionalResources();

}
