package com.ibm.whi.hl7.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ObjectMapperUtil {

  private static ObjectMapperUtil objectMapperUtil = new ObjectMapperUtil();

  public static ObjectMapper getInstance() {
    return objectMapperUtil.objectMapper;
  }

  private ObjectMapper objectMapper;

  private ObjectMapperUtil() {
    objectMapper = new ObjectMapper(new YAMLFactory());
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }



}
