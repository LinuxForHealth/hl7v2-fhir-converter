/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.Map;

@FunctionalInterface
public interface Condition {

  boolean test(Map<String, EvaluationResult> contextVariables);
}
