/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * 
 * Utility class for converting from HL7V2 codes to FHIR codes
 *
 * @author pbhallam
 */
public class Hl7v2Mapping {
  private Map<String, Map<String, String>> mapping;
  private static Hl7v2Mapping hl7Mapping;

  private Hl7v2Mapping() {
    try {
      mapping = loadV2Mappings();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot initialize mapping", e);
    }
  }

  private static Map<String, Map<String, String>> loadV2Mappings() throws IOException {

    TypeReference<Map<String, Map<String, String>>> typeRef =
        new TypeReference<Map<String, Map<String, String>>>() {};
    String content =
        ResourceReader.getInstance().getResourceInHl7Folder(Constants.V2_TO_FHIR_MAPPING_PATH);
    return ObjectMapperUtil.getYAMLInstance().readValue(content, typeRef);
  }


  protected static Map<String, String> getMapping(String fhirConceptName) {
    if (hl7Mapping == null) {
      hl7Mapping = new Hl7v2Mapping();
    }
    return hl7Mapping.mapping.get(fhirConceptName);
  }


  public static void initMapping() {
    if (hl7Mapping == null) {
      hl7Mapping = new Hl7v2Mapping();
    }

  }

}
