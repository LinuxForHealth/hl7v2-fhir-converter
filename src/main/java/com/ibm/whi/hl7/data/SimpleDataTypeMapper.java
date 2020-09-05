package com.ibm.whi.hl7.data;

import java.lang.reflect.Type;
import org.apache.commons.lang3.EnumUtils;


public enum SimpleDataTypeMapper {
  
  BOOLEAN(SimpleDataValueResolver.BOOLEAN, Boolean.class), //
  INTEGER(SimpleDataValueResolver.INTEGER, Integer.class), //
  STRING(SimpleDataValueResolver.STRING, String.class), //
  FLOAT(SimpleDataValueResolver.FLOAT, Float.class), //
  
  URI(SimpleDataValueResolver.URI_VAL, java.net.URI.class), //
  URL(SimpleDataValueResolver.STRING, String.class), //
  // BASE64BINARY(SimpleDataValueResolver.STRING),
  INSTANT(SimpleDataValueResolver.INSTANT, java.time.Instant.class), //
  DATE(SimpleDataValueResolver.LOCAL_DATE, java.time.LocalDate.class), //
  DATETIME(SimpleDataValueResolver.LOCAL_DATE_TIME, java.time.LocalDateTime.class), //
  // TIME(SimpleDataValueResolver.STRING),
  ID(SimpleDataValueResolver.STRING, String.class), //
  // MARKDOWN(SimpleDataValueResolver.STRING),
  UNSIGNEDINT(SimpleDataValueResolver.INTEGER, Integer.class), //
  POSITIVEINT(SimpleDataValueResolver.INTEGER, Integer.class), //
  // UUID(SimpleDataValueResolver.UUID_VAL),



  LOCAL_DATE(SimpleDataValueResolver.LOCAL_DATE, java.time.LocalDate.class), //
  LOCAL_DATE_TIME(SimpleDataValueResolver.LOCAL_DATE_TIME, java.time.LocalDateTime.class), //
  ADMINISTRATIVE_GENDER(SimpleDataValueResolver.ADMINISTRATIVE_GENDER, String.class), //
  OBSERVATION_STATUS(SimpleDataValueResolver.OBSERVATION_STATUS, String.class);
 


  private ValueExtractor<Object, ?> valueResolver;
  private Type type;
  SimpleDataTypeMapper(ValueExtractor<Object, ?> valueResolver, Type t) {
    this.valueResolver = valueResolver;
    this.type = t;
  }

  public static ValueExtractor<Object, ?> getValueResolver(String enumName) {
  

    SimpleDataTypeMapper mapper = EnumUtils.getEnumIgnoreCase(SimpleDataTypeMapper.class, enumName);
      if(mapper!=null) {
        return mapper.valueResolver;
      }
    throw new IllegalArgumentException("Cannot find data resolver" + enumName);


  }

  public Type getType() {
    return type;
  }


}
