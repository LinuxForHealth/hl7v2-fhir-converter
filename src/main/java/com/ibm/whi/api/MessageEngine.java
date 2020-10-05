/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.api;

import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import com.ibm.whi.core.message.AbstractFHIRResource;
import com.ibm.whi.fhir.FHIRContext;

/**
 * Implement this interface for each Data type that needs to be transformed into FHIR resource
 * 
 * 
 */

public interface MessageEngine {


  /**
   * Transforms source data to a FHIR bundle with the list of resources specified
   * 
   * @param dataSource - {@link InputData}
   * @param resources -{@link AbstractFHIRResource
   * @param contextValues - Map of context values
   * @return Fhir {@link Bundle}
   */
  Bundle transform(InputData dataSource, Iterable<? extends AbstractFHIRResource> resources,
      Map<String, EvaluationResult> contextValues);

  /**
   * Return the FHIR context to be used for validating and generating FHIR resources
   * 
   * @return
   */
  FHIRContext getFHIRContext();


  }

