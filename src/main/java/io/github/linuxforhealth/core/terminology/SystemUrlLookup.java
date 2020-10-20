/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linuxforhealth.core.ObjectMapperUtil;

/**
 * Utility class for mapping HL7 codes from table 0396 ( https://www.hl7.org/fhir/v2/0396/index.html
 * ) to respective coding system urls.
 * 
 *
 * @author pbhallam
 */
public class SystemUrlLookup {
  private final Map<String, String> systemUrls;

  private static final String HL7V2_SYSTEM_PREFIX = "http://terminology.hl7.org/CodeSystem/v2-";
  private static SystemUrlLookup systemURLLookupInstance;

  private SystemUrlLookup() {
    systemUrls = loadFromFile();

  }

  private static Map<String, String> loadFromFile() {
    TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
    try {
      return ObjectMapperUtil.getYAMLInstance().readValue(
          new File("src/main/resources/hl7/codesystem/CodingSystemMapping.yml"), typeRef);

    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Cannot read \"src/main/resources/hl7/codesystem/CodingSystemMapping.yml\"", e);
    }
  }


  /**
   * Get the system associated with the value
   * 
   * @param value -String
   * @return String
   * 
   */
  public static String getSystemUrl(String value) {
    if (systemURLLookupInstance == null) {
      systemURLLookupInstance = new SystemUrlLookup();
    }
    if (value != null) {
      return systemURLLookupInstance.systemUrls.get(StringUtils.upperCase(value));
    } else {
      return null;
    }
  }




  public static String getSystemV2Url(String value) {
    if (value != null) {
      return HL7V2_SYSTEM_PREFIX + value;
    } else {
      return null;
    }
  }
}
