/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

import io.github.linuxforhealth.hl7.segments.util.AllergyUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

public class Hl7IdentifierFHIRConversionTest {

   @Test
  public void patient_identifiers_test() {

    String patientIdentifiers =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    // Three ID's for testing, plus a SSN in field 19.
    + "PID|1||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||||||||444556666|D-12445889-Z||||||||||\n"
    ;

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiers);
    assertThat(patient.hasIdentifier()).isTrue();

    List<Identifier> identifiers = patient.getIdentifier();
    assertThat(identifiers.size()).isEqualTo(5);

    // First identifier (Medical Record) deep check
    Identifier identifier = identifiers.get(0);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:ID-XYZ");
    assertThat(identifier.getValue()).hasToString("MRN12345678");
    assertThat(identifier.hasType()).isTrue(); 
    CodeableConcept cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 
    Coding coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("MR");
    assertThat(coding.getDisplay()).hasToString("Medical record number");

    // Second identifier (SSN) medium check
    identifier = identifiers.get(1);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:USA");
    assertThat(identifier.getValue()).hasToString("111223333");
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 

    // Third identifier (Driver's license) medium check
    identifier = identifiers.get(2);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:MNDOT");
    assertThat(identifier.getValue()).hasToString("MN1234567");
    assertThat(identifier.hasType()).isTrue();
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();  

    // Deep check for fourth identifier, which is assembled from PID.19 SSN
    identifier = identifiers.get(3);
    assertThat(identifier.hasSystem()).isFalse();
    // PID.19 SSN has no authority value and therefore no system id
    // Using different SS than ID#2 to confirm coming from PID.19
    assertThat(identifier.getValue()).hasToString("444556666"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("SS");
    assertThat(coding.getDisplay()).hasToString("Social Security number");
    
    // Deep check for fifth identifier, which is assembled from PID.20 DL
    identifier = identifiers.get(4);
    assertThat(identifier.hasSystem()).isFalse();
    // PID.20 has no authority value and therefore no system id
    // Using different DL than ID#2 to confirm coming from PID.20
    assertThat(identifier.getValue()).hasToString("D-12445889-Z"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("DL");
    assertThat(coding.getDisplay()).hasToString("Driver's license number");    

  }

  @Test
  public void patient_identifiers_special_cases_test() {

    String patientIdentifiersSpecialCases =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    // First ID has blanks in the authority.  Second ID has no authority provided.
    + "PID|1||MRN12345678^^^Regional Health ID^MR~111223333^^^^SS|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n"
    ;

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiersSpecialCases);
    assertThat(patient.hasIdentifier()).isTrue();
    List<Identifier> identifiers = patient.getIdentifier();
    assertThat(identifiers.size()).isEqualTo(2);

    // First identifier (Medical Record) deep check
    Identifier identifier = identifiers.get(0);
    assertThat(identifier.hasSystem()).isTrue();
    // Ensure system blanks are filled in with _'s
    assertThat(identifier.getSystem()).hasToString("urn:id:Regional_Health_ID"); 
    assertThat(identifier.getValue()).hasToString("MRN12345678");
    assertThat(identifier.hasType()).isTrue(); 
    CodeableConcept cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 
    Coding coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("MR");
    assertThat(coding.getDisplay()).hasToString("Medical record number");

    // Deep check for second identifier
    identifier = identifiers.get(1);
    // We expect no system because there was no authority.
    assertThat(identifier.hasSystem()).isFalse();
    assertThat(identifier.getValue()).hasToString("111223333"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("SS");
    assertThat(coding.getDisplay()).hasToString("Social Security number");

  }

  @Test
  public void allergy_identifier_test() {

    String joinedByHyphen = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
    String justField1 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE||HYPOTENSION\r";
    String justField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|OXYCODONE||HYPOTENSION\r";

    AllergyIntolerance joined = AllergyUtils.createAllergyFromHl7Segment(joinedByHyphen);

    Identifier values = joined.getIdentifier().get(0);
    String joinedValue = values.getValue();
    String system = values.getSystem();

    assertThat(joined.hasIdentifier()).isTrue();
    assertThat(joinedValue).isEqualTo("00000741-LN");
    assertThat(system).isEqualTo("urn:id:extID");

    AllergyIntolerance field1 = AllergyUtils.createAllergyFromHl7Segment(justField1);

    Identifier field1Values = field1.getIdentifier().get(0);
    String field1Value = field1Values.getValue();
    String field1System = field1Values.getSystem();

    assertThat(field1.hasIdentifier()).isTrue();
    assertThat(field1Value).isEqualTo("00000741");
    assertThat(field1System).isEqualTo("urn:id:extID");

    AllergyIntolerance field2 = AllergyUtils.createAllergyFromHl7Segment(justField2);

    Identifier field2Values = field2.getIdentifier().get(0);
    String field2Value = field2Values.getValue();
    String field2System = field2Values.getSystem();

    assertThat(field2.hasIdentifier()).isTrue();
    assertThat(field2Value).isEqualTo("OXYCODONE");
    assertThat(field2System).isEqualTo("urn:id:extID");

  }
}