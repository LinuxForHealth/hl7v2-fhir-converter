/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.ibm.fhir.model.resource.CodeSystem;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.registry.FHIRRegistry;
import com.ibm.fhir.term.service.FHIRTermService;
import com.ibm.fhir.term.service.LookupOutcome;

public class TerminologyLookup {

    private static final FHIRRegistry REGISTRY = FHIRRegistry.getInstance();
    private static final FHIRTermService TERMINOLOGY_SEVICE = FHIRTermService.getInstance();
    private static TerminologyLookup termInstance;
    static Map<String, String> alternativeCodingSystemMapping =
        java.util.Map.of("v2-0005", "v3-Race", "CDCREC", "v3-Race");
     private TerminologyLookup() {
    }

    public static SimpleCode lookup(String system, String value) {
      String codingSystemName = system;
      if (alternativeCodingSystemMapping.containsKey(system)) {
        codingSystemName = alternativeCodingSystemMapping.get(system);
      }
      Uri url = getSystemUrl(codingSystemName);

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
        String sys = UrlLookup.getSystemUrl(value);
        CodeSystem s = null;
        if (sys != null) {
            s = REGISTRY.getResource(sys, CodeSystem.class);
            if (s != null && s.getUrl() != null) {
                return s.getUrl();
            }
        }
        return null;
    }




    public static void init() {
        if (termInstance == null) {
            termInstance = new TerminologyLookup();
        }
    }

}
