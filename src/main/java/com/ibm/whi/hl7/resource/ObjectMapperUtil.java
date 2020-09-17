/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ObjectMapperUtil {

  private static ObjectMapperUtil objectMapperUtilYAML = new ObjectMapperUtil(true);
  private static ObjectMapperUtil objectMapperUtilJSON = new ObjectMapperUtil(false);

  private ObjectMapper objectMapper;

  private ObjectMapperUtil(boolean isyaml) {
    if (isyaml) {
      objectMapper = new ObjectMapper(new YAMLFactory());
    } else {
      objectMapper = new ObjectMapper();
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      // StdDateFormat is ISO8601 since jackson 2.9
      objectMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
    }
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static ObjectMapper getYAMLInstance() {
    return objectMapperUtilYAML.objectMapper;
  }

  public static ObjectMapper getJSONInstance() {
    return objectMapperUtilJSON.objectMapper;
  }





}
