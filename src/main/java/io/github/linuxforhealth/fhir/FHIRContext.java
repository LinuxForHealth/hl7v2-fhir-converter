/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.fhir;

import org.hl7.fhir.r4.model.Bundle;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.github.linuxforhealth.core.Constants;

public class FHIRContext {


  private IParser parser;



  private FhirContext ctx;

  /**
   * Constructor for FHIRContext
   */
  public FHIRContext(boolean isPrettyPrint) {
    ctx = FhirContext.forR4();
    parser = ctx.newJsonParser();
    parser.setPrettyPrint(isPrettyPrint);
  }

  public FHIRContext() {
    this(Constants.DEFAULT_PRETTY_PRINT);
  }

  public IParser getParser() {
    return parser;
  }

  public FhirContext getCtx() {
    return ctx;
  }


  public String encodeResourceToString(Bundle bundle) {
    return this.parser.encodeResourceToString(bundle);
  }
}
