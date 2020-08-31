package com.ibm.whi.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class FHIRContext {

  private static FHIRContext fhirContext;
  private IParser parser;
  private FHIRContext() {
    FhirContext ctx = FhirContext.forR4();
    parser = ctx.newJsonParser();
    parser.setSuppressNarratives(true);
  }


  public static IParser getIParserInstance() {
    if (fhirContext == null) {
      fhirContext = new FHIRContext();
    }
    return fhirContext.parser;
  }
}
