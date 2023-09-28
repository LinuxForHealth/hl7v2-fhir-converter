/*
 * (C) Copyright IBM Corp. 2020, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import org.apache.commons.lang3.EnumUtils;


public enum SimpleDataTypeMapper {

  BOOLEAN(SimpleDataValueResolver.BOOLEAN), 
  INTEGER(SimpleDataValueResolver.INTEGER),
  STRING(SimpleDataValueResolver.STRING),
  STRING_ALL(SimpleDataValueResolver.STRING_ALL),
  VALID_ID(SimpleDataValueResolver.VALID_ID),
  FLOAT(SimpleDataValueResolver.FLOAT),
  BIG_DECIMAL(SimpleDataValueResolver.BIG_DECIMAL),
  BASE64_BINARY(SimpleDataValueResolver.BASE64_BINARY),

  URI(SimpleDataValueResolver.URI_VAL),
  URL(SimpleDataValueResolver.STRING),

  DATE(SimpleDataValueResolver.DATE),

  ID(SimpleDataValueResolver.STRING),
  UNSIGNEDINT(SimpleDataValueResolver.INTEGER),
  POSITIVEINT(SimpleDataValueResolver.INTEGER),
  UUID(SimpleDataValueResolver.UUID_VAL),
  NAMED_UUID(SimpleDataValueResolver.NAMED_UUID),
  OBJECT(SimpleDataValueResolver.OBJECT),
  CODING_SYSTEM_V2(SimpleDataValueResolver.CODING_SYSTEM_V2),
  CODING_SYSTEM_V2_ALTERNATE(SimpleDataValueResolver.CODING_SYSTEM_V2_ALTERNATE),
  CODING_SYSTEM_V2_IDENTIFIER(SimpleDataValueResolver.CODING_SYSTEM_V2_IDENTIFIER),
  CODING_SYSTEM_V2_IS_USER_DEFINED_TABLE(SimpleDataValueResolver.CODING_SYSTEM_V2_IS_USER_DEFINED_TABLE),
  SYSTEM_URL(SimpleDataValueResolver.SYSTEM_URL),
  SYSTEM_ID(SimpleDataValueResolver.SYSTEM_ID),
  DOSE_SYSTEM(SimpleDataValueResolver.DOSE_SYSTEM),
  DOSE_VALUE(SimpleDataValueResolver.DOSE_VALUE),

  CONTACT_POINT_SYSTEM_FHIR(SimpleDataValueResolver.CONTACT_POINT_SYSTEM_FHIR),

  ALLERGY_INTOLERANCE_CATEGORY(SimpleDataValueResolver.ALLERGY_INTOLERANCE_CATEGORY_CODE_FHIR),
  ALLERGY_INTOLERANCE_CRITICALITY(
      SimpleDataValueResolver.ALLERGY_INTOLERANCE_CRITICALITY_CODE_FHIR),
  ADMINISTRATIVE_GENDER(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR),
  CONDITION_CATEGORY_CODES(SimpleDataValueResolver.CONDITION_CATEGORY_CODES),
  DIAGNOSTIC_REPORT_STATUS(SimpleDataValueResolver.DIAGNOSTIC_REPORT_STATUS_CODES),
  ARRAY(SimpleDataValueResolver.ARRAY),
  OBSERVATION_STATUS(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR),
  SERVICE_REQUEST_STATUS(SimpleDataValueResolver.SERVICE_REQUEST_STATUS), 
  RELATIVE_REFERENCE(SimpleDataValueResolver.RELATIVE_REFERENCE),
  RELATIVE_REFERENCE_IDENTIFIER(SimpleDataValueResolver.RELATIVE_REFERENCE_IDENTIIFER),
  MESSAGE_REASON_ENCOUNTER(SimpleDataValueResolver.MESSAGE_REASON_ENCOUNTER),
  SPECIMEN_STATUS(SimpleDataValueResolver.SPECIMEN_STATUS_CODE_FHIR),
  NAME_USE(SimpleDataValueResolver.NAME_USE_CODE_FHIR),
  CONDITION_CLINICAL_STATUS_FHIR(SimpleDataValueResolver.CONDITION_CLINICAL_STATUS_FHIR),
  CONDITION_VERIFICATION_STATUS_FHIR(SimpleDataValueResolver.CONDITION_VERIFICATION_STATUS_FHIR),
  RELIGIOUS_AFFILIATION_CC(SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC),
  MARITAL_STATUS(SimpleDataValueResolver.MARITAL_STATUS),
  DIAGNOSIS_USE(SimpleDataValueResolver.DIAGNOSIS_USE),
  BUILD_IDENTIFIER_FROM_CWE(SimpleDataValueResolver.BUILD_IDENTIFIER_FROM_CWE),
  MEDREQ_STATUS(SimpleDataValueResolver.MEDREQ_STATUS_CODE_FHIR),
  MEDREQ_CATEGORY(SimpleDataValueResolver.MEDREQ_CATEGORY_CODE_FHIR),
  ACT_ENCOUNTER(SimpleDataValueResolver.ACT_ENCOUNTER_CODE_FHIR),
  PERSON_DISPLAY_NAME(SimpleDataValueResolver.PERSON_DISPLAY_NAME),
  PATIENT_INSTRUCTION(SimpleDataValueResolver.PATIENT_INSTRUCTION),
  DOC_REF_DOC_STATUS(SimpleDataValueResolver.DOC_REF_DOC_STATUS_CODE_FHIR),
  POLICYHOLDER_RELATIONSHIP_IN117(SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN117),
  POLICYHOLDER_RELATIONSHIP_IN272(SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN272),
  SUBSCRIBER_RELATIONSHIP_IN117(SimpleDataValueResolver.SUBSCRIBER_RELATIONSHIP_IN117),
  SUBSCRIBER_RELATIONSHIP_IN272(SimpleDataValueResolver.SUBSCRIBER_RELATIONSHIP_IN272),
  RELATED_PERSON_NEEDED_IN117(SimpleDataValueResolver.RELATED_PERSON_NEEDED_IN117),
  RELATED_PERSON_NEEDED_IN272(SimpleDataValueResolver.RELATED_PERSON_NEEDED_IN272),
  UNIT_SYSTEM(SimpleDataValueResolver.UNIT_SYSTEM),
  FIND_EDUCATION_PUBLICATION_DATE(SimpleDataValueResolver.FIND_EDUCATION_PUBLICATION_DATE),
  FIND_EDUCATION_PRESENTATION_DATE(SimpleDataValueResolver.FIND_EDUCATION_PRESENTATION_DATE),
  FIND_EDUCATION_DOCUMENT_TYPE_TEXT(SimpleDataValueResolver.FIND_EDUCATION_DOCUMENT_TYPE_TEXT),
  FIND_EDUCATION_REFERENCE_TEXT(SimpleDataValueResolver.FIND_EDUCATION_REFERENCE_TEXT),
  CLEAN_SSN(SimpleDataValueResolver.CLEAN_SSN),
  ENCOUNTER_MODE_ARRIVAL_DISPLAY(SimpleDataValueResolver.ENCOUNTER_MODE_ARRIVAL_DISPLAY);

  private ValueExtractor<Object, ?> valueResolver;

  SimpleDataTypeMapper(ValueExtractor<Object, ?> valueResolver) {
    this.valueResolver = valueResolver;

  }

  public static ValueExtractor<Object, ?> getValueResolver(String enumName) {


    SimpleDataTypeMapper mapper = EnumUtils.getEnumIgnoreCase(SimpleDataTypeMapper.class, enumName);
    if (mapper != null) {
      return mapper.valueResolver;
    }
    throw new IllegalArgumentException("Cannot find data resolver" + enumName);


  }



}
