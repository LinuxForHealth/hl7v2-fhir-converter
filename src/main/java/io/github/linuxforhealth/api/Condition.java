/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.api;

import java.util.Map;

/**
 * 
 * Interface that represents a condition. The implementing class should define how to resolve the
 * condition using the context values.
 *
 * @author pbhallam
 */
@FunctionalInterface
public interface Condition {
  /**
   * Returns True if the condition is satisfied.
   * 
   * @param contextValues - Map of String, {@link EvaluationResult}
   * @return boolean
   */
  boolean test(Map<String, EvaluationResult> contextValues);
}
