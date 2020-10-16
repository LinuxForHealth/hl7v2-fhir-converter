/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.core.terminology;

import com.ibm.fhir.model.resource.CodeSystem;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.registry.FHIRRegistry;
import com.ibm.fhir.term.service.FHIRTermService;
import com.ibm.fhir.term.spi.LookupOutcome;

public class TerminologyLookup {

  private static final FHIRRegistry REGISTRY = FHIRRegistry.getInstance();
  private static final FHIRTermService TERMINOLOGY_SEVICE = FHIRTermService.getInstance();

  private TerminologyLookup() {}


  public static SimpleCode lookup(String system, String value) {
    Uri url = getSystemUrl(system);
    if (url != null) {
      Code c = Code.of(value);

      LookupOutcome outcome = TERMINOLOGY_SEVICE.lookup(url, null, c);
      if (outcome != null && outcome.getDisplay() != null) {
        return new SimpleCode(value, url.getValue(), outcome.getDisplay().getValue());
      }
    }
      return null;

  }

  private static Uri getSystemUrl(String value) {

    String hl7v2 = SystemUrlLookup.getSystemV2Url(value);
    CodeSystem s = null;

    s = REGISTRY.getResource(hl7v2, CodeSystem.class);
    if (s != null && s.getUrl() != null) {
      return s.getUrl();
    }
    return null;
  }




}
