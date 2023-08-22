/*
 * (C) Copyright IBM Corp. 2020, 2022
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
import ca.uhn.hl7v2.model.v26.datatype.TX;
import ca.uhn.hl7v2.model.v26.datatype.XCN;

import ca.uhn.hl7v2.model.v26.message.ORU_R01;

import io.github.linuxforhealth.core.terminology.SimpleCode;
import io.github.linuxforhealth.hl7.data.date.DateUtil;

class SimpleDataValueResolverTest {

    private static final String VALID_UUID = "48ed55de-36be-4358-8ab6-4332c4a611ed";

    @Test
    void get_string_value() throws DataTypeException {
        ORU_R01 message = new ORU_R01();
        TX tx = new TX(message);
        tx.setValue("some value");
        assertThat(SimpleDataValueResolver.STRING.apply(tx)).isEqualTo("some value");
    }

    @Test
    void get_adm_gender_value() {
        String gen = "F";
        assertThat(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR.apply(gen)).isEqualTo("female");
    }

    @Test
    void get_adm_gender_value_unknown() {
        String gen = "ABC";
        assertThat(SimpleDataValueResolver.ADMINISTRATIVE_GENDER_CODE_FHIR.apply(gen)).isEqualTo("unknown");
    }

    @Test
    void get_contact_point_system_value_phone() {
        String gen = "PH";
        assertThat(SimpleDataValueResolver.CONTACT_POINT_SYSTEM_FHIR.apply(gen)).isEqualTo("phone");
    }
    @Test
    void get_contact_point_system_value_other() {
        String gen = "XX";
        assertThat(SimpleDataValueResolver.CONTACT_POINT_SYSTEM_FHIR.apply(gen)).isEqualTo("other");
    }

    @Test
    void get_boolean_value_non_boolean() {
        String gen = "ABC";
        assertThat(SimpleDataValueResolver.BOOLEAN.apply(gen)).isFalse();
    }

    @Test
    void get_clean_ssn() {
        // Check dashes are removed
        String ssn = "777-88-9999";
        assertThat(SimpleDataValueResolver.CLEAN_SSN.apply(ssn)).isEqualTo("777889999");

        // Check no dashes remains the same
        ssn = "777889999";
        assertThat(SimpleDataValueResolver.CLEAN_SSN.apply(ssn)).isEqualTo("777889999");

        // Check that empty input returns empty (and doesn't crash)
        ssn = "";
        assertThat(SimpleDataValueResolver.CLEAN_SSN.apply(ssn)).isEqualTo("");

        // Check that null input returns null (and doesn't crash)
        ssn = null;
        assertThat(SimpleDataValueResolver.CLEAN_SSN.apply(ssn)).isEqualTo(null);
    }

    @Test
    void get_boolean_value_true() {
        String gen = "True";
        assertThat(SimpleDataValueResolver.BOOLEAN.apply(gen)).isTrue();
    }

    @Test
    void get_date_value_valid() {
        String gen = "20091130";
        assertThat(SimpleDataValueResolver.DATE.apply(gen)).isNotNull();
        assertThat(SimpleDataValueResolver.DATE.apply(gen)).isEqualTo(DateUtil.formatToDate(gen));
    }

    @Test
    void get_date_value_null() {

        assertThat(SimpleDataValueResolver.DATE.apply(null)).isNull();
    }

    @Test
    void get_float_value_valid() {
        String gen = "123";
        assertThat(SimpleDataValueResolver.FLOAT.apply(gen)).isEqualTo(123.0F);
    }

    @Test
    void get_float_value_null() {
        assertThat(SimpleDataValueResolver.FLOAT.apply(null)).isNull();
    }

    @Test
    void get_float_value_invalid() {
        String gen = "abc";
        assertThat(SimpleDataValueResolver.FLOAT.apply(gen)).isNull();
    }

    @Test
    void get_integer_value_invalid() {
        String gen = "abc";
        assertThat(SimpleDataValueResolver.INTEGER.apply(gen)).isNull();
    }

    @Test
    void get_integer_value_valid() {
        String gen = "123";
        assertThat(SimpleDataValueResolver.INTEGER.apply(gen)).isEqualTo(123);
    }

    @Test
    void get_integer_value_null() {
        assertThat(SimpleDataValueResolver.INTEGER.apply(null)).isNull();
    }

    @Test
    void get_observation_status_value_valid() {
        String gen = "d";
        assertThat(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR.apply(gen))
                .isEqualTo(ObservationStatus.CANCELLED.toCode());
    }

    @Test
    void testObservationStatusValueNotValid() {
        String gen = "ZZZ";
        SimpleCode code = SimpleDataValueResolver.OBSERVATION_STATUS_FHIR.apply(gen);
        assertThat(code).isNotNull();
        assertThat(code.getCode()).isNull();
        String theSystem = ObservationStatus.CANCELLED.getSystem();
        assertThat(code.getSystem()).isEqualTo(theSystem);
        assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*" + theSystem);
    }

    @Test
    void get_service_request_status_value_valid() {
        String gen = "SC";
        assertThat(SimpleDataValueResolver.SERVICE_REQUEST_STATUS.apply(gen))
                .isEqualTo(ServiceRequestStatus.ACTIVE.toCode());
    }

    @Test
    void get_service_request_status_value_invalid() {
        String gen = "z";
        assertThat(SimpleDataValueResolver.SERVICE_REQUEST_STATUS.apply(gen)).isNull();
    }

    @Test
    void testReligiousAffiliationValueValid() {
        String gen = "LUT";
        SimpleCode code = SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
        assertThat(code.getDisplay()).isEqualTo(V3ReligiousAffiliation._1028.getDisplay());
        assertThat(code.getCode()).isEqualTo(V3ReligiousAffiliation._1028.toCode());
        assertThat(code.getSystem()).isEqualTo(V3ReligiousAffiliation._1028.getSystem());
    }

    @Test
    void testReligiousAffiliationValueNonvalid() {
        String gen = "ZZZ";
        SimpleCode code = SimpleDataValueResolver.RELIGIOUS_AFFILIATION_FHIR_CC.apply(gen);
        assertThat(code).isNotNull();
        assertThat(code.getCode()).isNull();
        String theSystem = V3ReligiousAffiliation._1029.getSystem();
        assertThat(code.getSystem()).isEqualTo(theSystem);
        assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*" + theSystem);
    }

    @Test
    // Tests one leg of CODING_SYSTEM_V2_IS_USER_DEFINED_TABLE that is not covered in other tests
    void testCodingSystemV2ISUserDefinedTable() {
        String code = "ZZZ";
        SimpleCode coding = SimpleDataValueResolver.CODING_SYSTEM_V2_IS_USER_DEFINED_TABLE.apply(code);
        assertThat(coding).isNotNull();
        assertThat(coding.getCode()).isEqualTo(code);
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();
    }

    @Test
    // Tests VALID_ID
    void testConvertToValidId() {
        assertThat(SimpleDataValueResolver.VALID_ID.apply("A B C")).isEqualTo("a-b-c");
        assertThat(SimpleDataValueResolver.VALID_ID.apply("A-B-C")).isEqualTo("a-b-c");
        assertThat(SimpleDataValueResolver.VALID_ID.apply("A/B/C")).isEqualTo("a-b-c");
        assertThat(SimpleDataValueResolver.VALID_ID.apply("A_B,C")).isEqualTo("a-b-c");
        assertThat(SimpleDataValueResolver.VALID_ID.apply("A.B.C")).isEqualTo("a.b.c");
    }

    @Test
    void get_race_value_valid() throws DataTypeException {
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
    void testMaritalStatusValueValid() {
        String gen = "A";
        SimpleCode coding = (SimpleCode) SimpleDataValueResolver.MARITAL_STATUS.apply(gen);
        assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.A.getDisplay());
        assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.A.getSystem());
    }

    @Test
    void testMaritalStatusValueNonValid() {
        String gen = "ZZZ";
        SimpleCode code = SimpleDataValueResolver.MARITAL_STATUS.apply(gen);
        assertThat(code).isNotNull();
        assertThat(code.getCode()).isNull();
        String theSystem = V3MaritalStatus.M.getSystem();
        assertThat(code.getSystem()).isEqualTo(theSystem);
        assertThat(code.getDisplay()).containsPattern("Invalid.*ZZZ.*" + theSystem);
    }

    @Test
    void get_observation_status_value_invalid() {
        String gen = "ddx";
        assertThat(SimpleDataValueResolver.OBSERVATION_STATUS_CODE_FHIR.apply(gen)).isNull();
    }

    @Test
    void get_specimen_status_value_valid() {
        String gen = "Y";
        assertThat(SimpleDataValueResolver.SPECIMEN_STATUS_CODE_FHIR.apply(gen))
                .isEqualTo(SpecimenStatus.AVAILABLE.toCode());
    }

    @Test
    void get_specimen_status_value_invalid() {
        String gen = "x";
        assertThat(SimpleDataValueResolver.SPECIMEN_STATUS_CODE_FHIR.apply(gen)).isNull();
    }

    @Test
    void get_URI_value_valid() throws URISyntaxException {
        String gen = VALID_UUID;
        assertThat(SimpleDataValueResolver.URI_VAL.apply(gen)).isEqualTo(new URI("urn", "uuid", VALID_UUID));
    }

    @Test
    void get_URI_value_invalid() {
        String gen = "ddx";
        assertThat(SimpleDataValueResolver.URI_VAL.apply(gen)).isNull();

    }

    @Test
    void get_UUID_value_invalid() {
        String gen = "ddx";
        assertThat(SimpleDataValueResolver.UUID_VAL.apply(gen)).isNull();

    }

    @Test
    void get_UUID_value_valid() {
        String gen = VALID_UUID;
        assertThat(SimpleDataValueResolver.UUID_VAL.apply(gen)).isEqualTo(UUID.fromString(VALID_UUID));
    }

    @Test
    void get_system_id_value_valid() {
        assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("ABC")).isEqualTo("urn:id:ABC");
        assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("A B C")).isEqualTo("urn:id:A_B_C");
        assertThat(SimpleDataValueResolver.SYSTEM_ID.apply("")).isNull();
        assertThat(SimpleDataValueResolver.SYSTEM_ID.apply(null)).isNull();
    }

    @Test
    void getDisplayNameValid() throws DataTypeException {
        XCN xcn = new XCN(null);
        xcn.getPrefixEgDR().setValue("Dr");
        xcn.getGivenName().setValue("Joe");
        xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue("Q");
        xcn.getFamilyName().getSurname().setValue("Johnson");
        xcn.getSuffixEgJRorIII().setValue("III");

        assertThat(SimpleDataValueResolver.PERSON_DISPLAY_NAME.apply(xcn)).isEqualTo("Dr Joe Q Johnson III");
    }

    @Test
    void getDisplayNameNotValid() throws DataTypeException {
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
    void testPolicyholderRelationship() {

        // Check supported known input code (from table 0063)
        SimpleCode coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN117.apply("PAR");
        assertThat(coding).isNotNull();
        assertThat(coding.getCode()).isEqualTo("PRN");
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v3-RoleCode");
        assertThat(coding.getDisplay()).isEqualTo("parent");

        // Check supported known input code (from table 0344) REVERSES the relationship.  See notes in v2ToFhirMapping.
        coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN272.apply("18"); // 18 is parent
        assertThat(coding).isNotNull();
        assertThat(coding.getCode()).isEqualTo("CHILD");
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v3-RoleCode");
        assertThat(coding.getDisplay()).isEqualTo("child");

        // Check unsupported unknown input codes
        // Because PET has no mapping, we pass it without a system.
        coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN117.apply("PET");
        assertThat(coding.getCode()).isEqualTo("PET");
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();

        // Because PET has no mapping, we pass it without a system.
        coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN272.apply("PET");
        assertThat(coding.getCode()).isEqualTo("PET");
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();
    }

    @Test
    void testSubscriberRelationship() {

        // Check supported known input code (from table 0063) REVERSES the relationship.  See notes in v2ToFhirMapping.
        SimpleCode coding = SimpleDataValueResolver.SUBSCRIBER_RELATIONSHIP_IN117.apply("CHD");
        assertThat(coding).isNotNull();
        assertThat(coding.getCode()).isEqualTo("parent");
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/subscriber-relationship");
        assertThat(coding.getDisplay()).isEqualTo("Parent");

        // Check supported known input code (from table 0344)
        coding = SimpleDataValueResolver.SUBSCRIBER_RELATIONSHIP_IN272.apply("04"); // is child
        assertThat(coding).isNotNull();
        assertThat(coding.getCode()).isEqualTo("child");
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/subscriber-relationship");
        assertThat(coding.getDisplay()).isEqualTo("Child");

        // Check unsupported unknown input codes
        // Because GOAT has no mapping, we pass it without a system.
        coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN117.apply("GOAT");
        assertThat(coding.getCode()).isEqualTo("GOAT");
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();

        // Because GOAT has no mapping, we pass it without a system.
        coding = SimpleDataValueResolver.POLICYHOLDER_RELATIONSHIP_IN272.apply("GOAT");
        assertThat(coding.getCode()).isEqualTo("GOAT");
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();
    }

}
