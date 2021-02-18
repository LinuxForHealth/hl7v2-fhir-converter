/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Utility class for mapping HL7 codes from table 0396 ( https://www.hl7.org/fhir/v2/0396/index.html
 * ) to respective coding system urls.
 * 
 *
 * @author pbhallam
 */
public class SystemUrlLookup {
  private final Map<String, CodingSystem> systemUrls;

  private static SystemUrlLookup systemURLLookupInstance;

  private SystemUrlLookup() {
    systemUrls = loadFromFile();

  }

  private static Map<String, CodingSystem> loadFromFile() {
    TypeReference<List<CodingSystem>> typeRef = new TypeReference<List<CodingSystem>>() {};
    try {
      String content =
          ResourceReader.getInstance().getResourceInHl7Folder(Constants.CODING_SYSTEM_MAPPING_PATH);
      List<CodingSystem> systems = ObjectMapperUtil.getYAMLInstance().readValue(content, typeRef);
      return systems.stream()
          .collect(Collectors.toMap(CodingSystem::getId, codeSystem -> codeSystem));

    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Cannot read codesystem/CodingSystemMapping.yml", e);
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
    if (StringUtils.startsWith(value, "http://") || StringUtils.startsWith(value, "https://")) {
      return value;
    } else if (value != null) {
      CodingSystem system = systemURLLookupInstance.systemUrls.get(StringUtils.upperCase(value));
      if (system != null) {
        return system.getUrl();
      }
    }
      return null;

  }


  /**
   * Read the coding system details from the file and loads it in memory
   * 
   * 
   * 
   */
  public static void init() {
    if (systemURLLookupInstance == null) {
      systemURLLookupInstance = new SystemUrlLookup();
    }


  }



}
