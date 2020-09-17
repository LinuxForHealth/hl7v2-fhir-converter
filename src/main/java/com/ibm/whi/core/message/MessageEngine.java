/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.message;

import java.io.IOException;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import com.ibm.whi.core.expression.GenericResult;

/**
 * Implement this interface for each Data type that needs to be transformed into FHIR resource
 * 
 * 
 */

@FunctionalInterface
public interface MessageEngine {


  /**
   * Transforms source data to a FHIR bundle with the list of resources specified
   * 
   * @param dataExtractor
   * @param resources
   * @param context
   * @return
   * @throws IOException
   */
  Bundle transform(InputData dataSource, Iterable<? extends AbstractFHIRResource> resources,
      Map<String, GenericResult> contextValues) throws IOException;



  }

