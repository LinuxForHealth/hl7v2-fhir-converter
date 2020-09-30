/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.data;

import org.apache.commons.lang3.EnumUtils;


public enum SimpleDataTypeMapper {
  
  BOOLEAN(SimpleDataValueResolver.BOOLEAN), //
  INTEGER(SimpleDataValueResolver.INTEGER), //
  STRING(SimpleDataValueResolver.STRING), //
  STRING_ALL(SimpleDataValueResolver.STRING_ALL), //
  FLOAT(SimpleDataValueResolver.FLOAT), //
  
  URI(SimpleDataValueResolver.URI_VAL), //
  URL(SimpleDataValueResolver.STRING), //
  INSTANT(SimpleDataValueResolver.INSTANT), //

  DATE(SimpleDataValueResolver.DATE), //

  DATE_TIME(SimpleDataValueResolver.DATE_TIME), //
  // TIME(SimpleDataValueResolver.TIME_TYPE),
  ID(SimpleDataValueResolver.STRING), //
  // MARKDOWN(SimpleDataValueResolver.STRING),
  UNSIGNEDINT(SimpleDataValueResolver.INTEGER), //
  POSITIVEINT(SimpleDataValueResolver.INTEGER), //
  UUID(SimpleDataValueResolver.UUID_VAL),
  OBJECT(SimpleDataValueResolver.OBJECT),
  CODING_SYSTEM_V2(SimpleDataValueResolver.CODING_SYSTEM_V2), //
  SYSTEM_URL(SimpleDataValueResolver.SYSTEM_URL), //

  ALLERGY_INTOLERANCE_CATEGORY(SimpleDataValueResolver.ALLERGY_INTOLERANCE_CATEGORY_CODE_FHIR), //
  ALLERGY_INTOLERANCE_CRITICALITY(
      SimpleDataValueResolver.ALLERGY_INTOLERANCE_CRITICALITY_CODE_FHIR), //
  ADMINISTRATIVE_GENDER(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR), //
  OBSERVATION_STATUS(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR);
 


  private ValueExtractor<Object, ?> valueResolver;

  SimpleDataTypeMapper(ValueExtractor<Object, ?> valueResolver) {
    this.valueResolver = valueResolver;

  }

  public static ValueExtractor<Object, ?> getValueResolver(String enumName) {
  

    SimpleDataTypeMapper mapper = EnumUtils.getEnumIgnoreCase(SimpleDataTypeMapper.class, enumName);
      if(mapper!=null) {
        return mapper.valueResolver;
      }
    throw new IllegalArgumentException("Cannot find data resolver" + enumName);


  }



}
