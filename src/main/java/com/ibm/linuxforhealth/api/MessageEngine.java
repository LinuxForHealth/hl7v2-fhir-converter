/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.api;

import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import com.ibm.linuxforhealth.fhir.FHIRContext;

/**
 * Implement this interface for each Data type that needs to be transformed into FHIR resource
 * 
 * 
 */

public interface MessageEngine {


  /**
   * Transforms source data to a FHIR bundle with the list of resources specified
   * 
   * @param dataSource - {@link InputDataExtractor}
   * @param resources -{@link FHIRResourceTemplate}
   * @param contextValues - Map of context values
   * @return FHIR {@link Bundle}
   */
  Bundle transform(InputDataExtractor dataSource, Iterable<FHIRResourceTemplate> resources,
      Map<String, EvaluationResult> contextValues);

  /**
   * Return the FHIR context to be used for validating and generating FHIR resources
   * 
   * @return {@link FHIRContext}
   */
  FHIRContext getFHIRContext();


  }

