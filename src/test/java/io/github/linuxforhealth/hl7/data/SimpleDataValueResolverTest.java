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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.junit.Test;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import io.github.linuxforhealth.core.terminology.SimpleCode;
import io.github.linuxforhealth.hl7.data.date.DateUtil;

import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;

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
    String gen = "20091130112038";
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
  public void get_religious_affiliation_value_valid() {
    String gen = "LUT";
    CodeableConcept codeableConcept = SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
    Coding coding = codeableConcept.getCodingFirstRep();
    assertThat(codeableConcept.getText()).isEqualTo(V3ReligiousAffiliation._1028.getDisplay());
    assertThat(codeableConcept.hasCoding()).isTrue();
    assertThat(codeableConcept.getText()).isEqualTo(V3ReligiousAffiliation._1028.getDisplay());
    assertThat(coding.getDisplay()).isEqualTo(V3ReligiousAffiliation._1028.getDisplay());
    assertThat(coding.getSystem()).isEqualTo(V3ReligiousAffiliation._1028.getSystem());
  }

  @Test
  public void get_religious_affiliation_value_nonvalid() {
    String gen = "ZZZ";
    CodeableConcept codeableConcept = SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
    assertThat(codeableConcept).isNull();
  }

  @Test
  public void get_marital_status_value_valid() {
    String gen = "A";
    SimpleCode coding = (SimpleCode) SimpleDataValueResolver.MARITAL_STATUS.apply(gen);
    assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.A.getDisplay());
    assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.A.getSystem());
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

}
