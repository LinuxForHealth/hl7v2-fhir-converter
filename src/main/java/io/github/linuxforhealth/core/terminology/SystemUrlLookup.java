/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

/**
 * Utility class for mapping HL7 codes from tables
 *
 * @author pbhallam
 */
public class SystemUrlLookup {
  private final Map<String, CodingSystem> systemUrls;

  private static SystemUrlLookup systemURLLookupInstance;

  private SystemUrlLookup() {
    systemUrls = loadFromFile();
    systemUrls.putAll(loadAdditionalFromFile());

  }

  // ConverterConfiguration
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


  private static Map<String, CodingSystem> loadAdditionalFromFile() {
    TypeReference<List<CodingSystem>> typeRef = new TypeReference<List<CodingSystem>>() {};
    String filePath = ConverterConfiguration.getInstance().getAdditionalConceptmapFile();
    if (StringUtils.isNotBlank(filePath)) {
      try {
        FileInputStream fis = new FileInputStream(filePath);
        List<CodingSystem> systems = ObjectMapperUtil.getYAMLInstance().readValue(fis, typeRef);
      return systems.stream()
          .collect(Collectors.toMap(CodingSystem::getId, codeSystem -> codeSystem));

    } catch (IOException e) {
        throw new IllegalArgumentException(filePath, e);
    }
  }
    return new HashMap<>();
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
  
  /**
   * Reloads the coding system details
   * 
   */
  public static void reinit() {

    systemURLLookupInstance = new SystemUrlLookup();
  }



}
