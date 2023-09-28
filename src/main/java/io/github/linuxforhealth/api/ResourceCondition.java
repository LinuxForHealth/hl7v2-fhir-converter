/*
 * (c) Copyright Te Whatu Ora, Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.api;

/**
 * Resource Condition allows for conditional application of ResourceTemplate to a particular message
 * 
 * @author Stuart McGrigor
 */
public interface ResourceCondition {


    /**
     * Evaluates the condition against the HL7 Message
     * 
     * @param ide input data extractor
     * @param context {@link EvaluationResult} representing the HL7 Segment being evaluated
     * @return true if condition is satisfied by the HL7 Segment being evaluated, otherwise returns false;
     */
    default boolean isConditionSatisfied(InputDataExtractor ide, EvaluationResult context) {
      return true;
    }
}
