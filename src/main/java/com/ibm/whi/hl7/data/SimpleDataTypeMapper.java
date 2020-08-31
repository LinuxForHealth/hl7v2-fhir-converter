package com.ibm.whi.hl7.data;

import org.apache.commons.lang3.EnumUtils;


public enum SimpleDataTypeMapper {
  
  BOOLEAN(SimpleDataValueResolver.BOOLEAN), 
  INTEGER(SimpleDataValueResolver.INTEGER),
  STRING(SimpleDataValueResolver.STRING), 
  DECIMAL(SimpleDataValueResolver.FLOAT),
  
  URI(SimpleDataValueResolver.URI_VAL),
  URL(SimpleDataValueResolver.STRING),
  // BASE64BINARY(SimpleDataValueResolver.STRING),
  INSTANT(SimpleDataValueResolver.INSTANT),
  DATE(SimpleDataValueResolver.LOCAL_DATE), //
  DATETIME(SimpleDataValueResolver.LOCAL_DATE_TIME),
  // TIME(SimpleDataValueResolver.STRING),
  ID(SimpleDataValueResolver.STRING),
  // MARKDOWN(SimpleDataValueResolver.STRING),
  UNSIGNEDINT(SimpleDataValueResolver.INTEGER), POSITIVEINT(SimpleDataValueResolver.INTEGER),
  // UUID(SimpleDataValueResolver.UUID_VAL),



  LOCAL_DATE(SimpleDataValueResolver.LOCAL_DATE), //
  LOCAL_DATE_TIME(SimpleDataValueResolver.LOCAL_DATE_TIME), //
  ADMINISTRATIVE_GENDER(SimpleDataValueResolver.ADMINISTRATIVE_GENDER), //
  OBSERVATION_STATUS(SimpleDataValueResolver.OBSERVATION_STATUS);
 


  private DataEvaluator<Object, ?> valueResolver;

  SimpleDataTypeMapper(DataEvaluator<Object, ?> valueResolver) {
    this.valueResolver = valueResolver;
  }

  public static DataEvaluator<Object, ?> getValueResolver(String enumName) {
  

    SimpleDataTypeMapper mapper = EnumUtils.getEnumIgnoreCase(SimpleDataTypeMapper.class, enumName);
      if(mapper!=null) {
        return mapper.valueResolver;
      }
    throw new IllegalArgumentException("Cannot find data resolver" + enumName);


  }


}
