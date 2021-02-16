/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.util.Map;
import com.google.common.base.Preconditions;
import com.ibm.fhir.model.resource.CodeSystem;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.registry.FHIRRegistry;
import com.ibm.fhir.term.service.FHIRTermService;
import com.ibm.fhir.term.spi.LookupOutcome;

public class TerminologyLookup {

  private FHIRRegistry fhirRegistry;
  private FHIRTermService fhirTerminologyService;

  private static TerminologyLookup lookup;

  private TerminologyLookup() {
    Hl7v2Mapping.initMapping();
    SystemUrlLookup.initSystemUrlLookup();
    fhirRegistry = FHIRRegistry.getInstance();
    fhirTerminologyService = FHIRTermService.getInstance();

  }



  SimpleCode lookupTerminology(String system, String value) {
    Uri url = getSystemUrlFromFhirRegistry(system);
    if (url != null) {
      Code c = Code.of(value);

      LookupOutcome outcome = fhirTerminologyService.lookup(url, null, c);
      if (outcome != null && outcome.getDisplay() != null) {
        String ver = null;
        if (outcome.getVersion() != null) {
          ver = outcome.getVersion().getValue();
        }

        return new SimpleCode(value, url.getValue(), outcome.getDisplay().getValue(), ver);
      }
    }
    return null;

  }

  Uri getSystemUrlFromFhirRegistry(String value) {

    String hl7v2 = SystemUrlLookup.getSystemV2Url(value);
    if (hl7v2 != null) {
      CodeSystem s = fhirRegistry.getResource(hl7v2, CodeSystem.class);
      if (s != null && s.getUrl() != null) {
        return s.getUrl();
      }
    }
    return null;
  }

  public static void init() {
    if (lookup == null) {
      lookup = new TerminologyLookup();
    }
  }



  public static SimpleCode lookup(String system, String value) {
    Preconditions.checkArgument(lookup != null, "TerminologyLookup lookup not initialized");
    return lookup.lookupTerminology(system, value);

  }

  public static Uri getSystemUrl(String value) {
    Preconditions.checkArgument(lookup != null, "TerminologyLookup lookup not initialized");
    return lookup.getSystemUrlFromFhirRegistry(value);

  }

  public static Map<String, String> getMapping(String fhirConceptName) {
    Preconditions.checkArgument(lookup != null, "TerminologyLookup lookup not initialized");
    return Hl7v2Mapping.getMapping(fhirConceptName);
  }

}
