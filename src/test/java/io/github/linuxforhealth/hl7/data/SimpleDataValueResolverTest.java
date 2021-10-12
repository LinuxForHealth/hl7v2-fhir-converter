/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.r4.model.codesystems.V3Race;
import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;
import org.junit.jupiter.api.Test;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.XCN;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_VISIT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import io.github.linuxforhealth.core.terminology.SimpleCode;
import io.github.linuxforhealth.hl7.data.date.DateUtil;

public class SimpleDataValueResolverTest {

  private static final String VALID_UUID = "48ed55de-36be-4358-8ab6-4332c4a611ed";

  @Test
  public void get_string_value() throws DataTypeException {
    ORU_R01 message = new ORU_R01();
    TX tx = new TX(message);
    tx.setValue("some value");
    assertThat(SimpleDataValueResolver.STRING.apply(tx)).isEqualTo("some value");
  }

  @Test
  public void get_adm_gender_value() {
    String gen = "F";
    assertThat(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR.apply(gen)).isEqualTo("female");
  }

  @Test
  public void get_adm_gender_value_unknow() {
    String gen = "ABC";
    assertThat(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR.apply(gen)).isEqualTo("unknown");
  }

  @Test
  public void get_boolean_value_non_boolean() {
    String gen = "ABC";
    assertThat(SimpleDataValueResolver.BOOLEAN.apply(gen)).isFalse();
  }

  @Test
  public void get_boolean_value_true() {
    String gen = "True";
    assertThat(SimpleDataValueResolver.BOOLEAN.apply(gen)).isTrue();
  }

  @Test
  public void get_date_value_valid() {
    String gen = "20091130";
    assertThat(SimpleDataValueResolver.DATE.apply(gen)).isEqualTo(DateUtil.formatToDate(gen));
  }

  @Test
  public void get_date_value_null() {

    assertThat(SimpleDataValueResolver.DATE.apply(null)).isNull();
  }

  @Test
  public void get_datetime_value_valid() {
    String gen = "20110613122406";
    assertThat(SimpleDataValueResolver.DATE_TIME.apply(gen)).isEqualTo(DateUtil.formatToDateTimeWithZone(gen));

    // Test DateTime adjusts for milliseconds
    gen = "20110613122406.637";

    System.out.println(SimpleDataValueResolver.DATE_TIME.apply(gen));
    assertThat(SimpleDataValueResolver.DATE_TIME.apply(gen)).isEqualTo(DateUtil.formatToDateTimeWithZone(gen));
  }

  @Test
  public void get_instant_value_null() {
    assertThat(SimpleDataValueResolver.INSTANT.apply(null)).isNull();
  }

  @Test
  public void get_instant_value_valid() {
    String gen = "20091130112038";
    assertThat(SimpleDataValueResolver.INSTANT.apply(gen)).isEqualTo(DateUtil.formatToZonedDateTime(gen));
  }

  @Test
  public void get_datetime_value_null() {
    assertThat(SimpleDataValueResolver.DATE_TIME.apply(null)).isNull();
  }

  @Test
  public void get_float_value_valid() {
    String gen = "123";
    assertThat(SimpleDataValueResolver.FLOAT.apply(gen)).isEqualTo(123.0F);
  }

  @Test
  public void get_float_value_null() {
    assertThat(SimpleDataValueResolver.FLOAT.apply(null)).isNull();
  }

  @Test
  public void get_float_value_invalid() {
    String gen = "abc";
    assertThat(SimpleDataValueResolver.FLOAT.apply(gen)).isNull();
  }

  @Test
  public void get_integer_value_invalid() {
    String gen = "abc";
    assertThat(SimpleDataValueResolver.INTEGER.apply(gen)).isNull();
  }

  @Test
  public void get_integer_value_valid() {
    String gen = "123";
    assertThat(SimpleDataValueResolver.INTEGER.apply(gen)).isEqualTo(123);
  }

  @Test
  public void get_integer_value_null() {
    assertThat(SimpleDataValueResolver.INTEGER.apply(null)).isNull();
  }

  @Test
  public void get_observation_status_value_valid() {
    String gen = "d";
    assertThat(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR.apply(gen))
        .isEqualTo(ObservationStatus.CANCELLED.toCode());
  }


  @Test
  public void testObservationStatusValueNotValid() {
    String gen = "ZZZ";
    SimpleCode code =
        SimpleDataValueResolver.OBSERVATION_STATUS_FHIR.apply(gen);
    assertThat(code).isNotNull();
    assertThat(code.getCode()).isNull();
    String theSystem = ObservationStatus.CANCELLED.getSystem();
    assertThat(code.getSystem()).isEqualTo(theSystem); 
    assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*"+theSystem);    
  }

  @Test
  public void get_service_request_status_value_valid() {
    String gen = "SC";
    assertThat(SimpleDataValueResolver.SERVICE_REQUEST_STATUS.apply(gen))
		.isEqualTo(ServiceRequestStatus.ACTIVE.toCode());
  }

  
  @Test
  public void get_service_request_status_value_invalid() {
    String gen = "z";
    assertThat(SimpleDataValueResolver.SERVICE_REQUEST_STATUS.apply(gen)).isNull();
  }



  @Test
  public void testReligiousAffiliationValueValid() {
    String gen = "LUT";
    SimpleCode code =
        SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
    assertThat(code.getDisplay()).isEqualTo(V3ReligiousAffiliation._1028.getDisplay());
    assertThat(code.getCode()).isEqualTo(V3ReligiousAffiliation._1028.toCode());
    assertThat(code.getSystem()).isEqualTo(V3ReligiousAffiliation._1028.getSystem());
  }


  @Test
  public void testReligiousAffiliationValueNonvalid() {
    String gen = "ZZZ";
    SimpleCode code =
        SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
    assertThat(code).isNotNull();
    assertThat(code.getCode()).isNull();
    String theSystem = V3ReligiousAffiliation._1029.getSystem();
    assertThat(code.getSystem()).isEqualTo(theSystem); 
    assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*"+theSystem);
  }

  @Test
  public void get_race_value_valid() throws DataTypeException {
    CWE cwe = new CWE(null);
    cwe.getCwe3_NameOfCodingSystem().setValue("HL70005");
    cwe.getCwe1_Identifier().setValue("2028-9");
    cwe.getCwe2_Text().setValue("Asian");

    SimpleCode code = SimpleDataValueResolver.CODING_SYSTEM_V2.apply(cwe);
    assertThat(code.getDisplay()).isEqualTo(V3Race._20289.getDisplay());
    assertThat(code.getCode()).isEqualTo(V3Race._20289.toCode());
    assertThat(code.getSystem()).isEqualTo(V3Race._20289.getSystem());
  }

  @Test
  public void testMaritalStatusValueValid() {
    String gen = "A";
    SimpleCode coding = (SimpleCode) SimpleDataValueResolver.MARITAL_STATUS.apply(gen);
    assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.A.getDisplay());
    assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.A.getSystem());
  }

  @Test
  public void testMaritalStatusValueNonValid() {
    String gen = "ZZZ";
    SimpleCode code =
        SimpleDataValueResolver.MARITAL_STATUS.apply(gen);
    assertThat(code).isNotNull();
    assertThat(code.getCode()).isNull();
    String theSystem = V3MaritalStatus.M.getSystem();
    assertThat(code.getSystem()).isEqualTo(theSystem); 
    assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*"+theSystem);
  }

  @Test
  public void get_observation_status_value_invalid() {
    String gen = "ddx";
    assertThat(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR.apply(gen)).isNull();
  }

  @Test
  public void get_specimen_status_value_valid() {
    String gen = "Y";
    assertThat(SimpleDataValueResolver.SPECIMEN_STATUS_CODE_FHIR.apply(gen))
        .isEqualTo(SpecimenStatus.AVAILABLE.toCode());
  }

  @Test
  public void get_specimen_status_value_invalid() {
    String gen = "x";
    assertThat(SimpleDataValueResolver.SPECIMEN_STATUS_CODE_FHIR.apply(gen)).isNull();
  }

  @Test
  public void get_URI_value_valid() throws URISyntaxException {
    String gen = VALID_UUID;
    assertThat(SimpleDataValueResolver.URI_VAL.apply(gen)).isEqualTo(new URI("urn", "uuid", VALID_UUID));
  }

  @Test
  public void get_URI_value_invalid() {
    String gen = "ddx";
    assertThat(SimpleDataValueResolver.URI_VAL.apply(gen)).isNull();

  }

  @Test
  public void get_UUID_value_invalid() {
    String gen = "ddx";
    assertThat(SimpleDataValueResolver.UUID_VAL.apply(gen)).isNull();

  }

  @Test
  public void get_UUID_value_valid() {
    String gen = VALID_UUID;
    assertThat(SimpleDataValueResolver.UUID_VAL.apply(gen)).isEqualTo(UUID.fromString(VALID_UUID));
  }

  @Test
  public void get_system_id_value_valid() {
    assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("ABC")).isEqualTo("urn:id:ABC");
    assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("A B C")).isEqualTo("urn:id:A_B_C");
    assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("")).isNull();
    assertThat(SimpleDataValueResolver.SYSTEM_ID.apply(null)).isNull();
  }

  @Test
  public void getDisplayNameValid() throws DataTypeException {
    XCN xcn = new XCN(null);
    xcn.getPrefixEgDR().setValue("Dr");
    xcn.getGivenName().setValue("Joe");
    xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue("Q");
    xcn.getFamilyName().getSurname().setValue("Johnson");
    xcn.getSuffixEgJRorIII().setValue("III");
 
    assertThat(SimpleDataValueResolver.PERSON_DISPLAY_NAME.apply(xcn)).isEqualTo("Dr Joe Q Johnson III");
  }

  @Test
  public void getDisplayNameNotValid() throws DataTypeException {
    CWE cwe = new CWE(null);
    cwe.getCwe3_NameOfCodingSystem().setValue("HL70005");
    cwe.getCwe1_Identifier().setValue("2028-9");
    cwe.getCwe2_Text().setValue("Asian");
  
    // CWE is not a valid input and should return null
    assertThat(SimpleDataValueResolver.PERSON_DISPLAY_NAME.apply(cwe)).isNull();
    // String is not a valid input and should return null
    assertThat(SimpleDataValueResolver.PERSON_DISPLAY_NAME.apply("Bogus String")).isNull();
  }  

  @Test
  public void getPV1DurationLength() throws DataTypeException  {
    // Get a PV1
    ORU_R01 message = new ORU_R01();
    ORU_R01_PATIENT_RESULT patientResult = message.getPATIENT_RESULT();
    ORU_R01_PATIENT patient = patientResult.getPATIENT();
    ORU_R01_VISIT visit = patient.getVISIT();
    PV1 pv1 =  visit.getPV1();

    // Admit and Discharge are not yet set; they are still empty
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply(pv1)).isNull();

    // Admit set, but Discharge not yet set
    pv1.getAdmitDateTime().setValue("20161013154626"); 
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply(pv1)).isNull();

    // Admit and Discharge set to valid values
    pv1.getAdmitDateTime().setValue("20161013154626"); 
    pv1.getDischargeDateTime().setValue("20161013164626"); 
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply(pv1)).isEqualTo("60");

    // Admit and Discharge set to valid values less that one minute apart
    pv1.getAdmitDateTime().setValue("20161013154626"); 
    pv1.getDischargeDateTime().setValue("20161013154628"); 
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply(pv1)).isEqualTo("0");

    // Admit and Discharge set to insufficient detail values (have no minutes) return null
    pv1.getAdmitDateTime().setValue("20161013"); 
    pv1.getDischargeDateTime().setValue("20161013"); 
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply(pv1)).isNull();

    // Other input types, such as a string, are not valid and null is returned
    assertThat(SimpleDataValueResolver.PV1_DURATION_LENGTH.apply("A string")).isNull();
  }

}
