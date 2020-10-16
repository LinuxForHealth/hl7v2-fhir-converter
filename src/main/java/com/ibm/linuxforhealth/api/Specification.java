/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.api;

import java.util.Map;

/**
 * Defines Data source specific data extraction String Example: for HL7 data, specification defines
 * segment, field, component and subcomponent names/identifiers that can be used for extracting
 * data.
 * 
 *
 * @author pbhallam
 */

public interface Specification {

  /**
   * Extract the single value for the specifications.
   * 
   * 
   * @param dataSource {@link InputDataExtractor}
   * @param contextValues {@link Map} of String and value {@link EvaluationResult }
   * @return {@link EvaluationResult}
   */
  EvaluationResult extractValueForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues);

  /**
   * Extract the multiple values for the specifications.
   * 
   *
   * @param dataSource {@link InputDataExtractor}
   * @param contextValues {@link Map} of String and value {@link EvaluationResult }
   * @return {@link EvaluationResult}
   */
  EvaluationResult extractMultipleValuesForSpec(InputDataExtractor dataSource,
      Map<String, EvaluationResult> contextValues);

}
