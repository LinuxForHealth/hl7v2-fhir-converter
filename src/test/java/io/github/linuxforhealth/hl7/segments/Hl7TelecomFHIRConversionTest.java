/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ContactPoint;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

public class Hl7TelecomFHIRConversionTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();


  @Test

  public void patient_telcom_test() {

    String patientPhone =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||^PRN^PH^^22^555^1111313^^^^^^^^^^^2~^PRN^CP^^22^555^2221313^^^^^^^^^^^1|^PRN^PH^^^555^1111414^889||||||||||||||||\n"
    ;

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientPhone);
    assertThat(patient.hasTelecom()).isTrue();
    List<ContactPoint> contacts = patient.getTelecom();
    assertThat(contacts.size()).isEqualTo(3);

    // First home contact
    ContactPoint contact = contacts.get(0); 
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.HOME);
    assertThat(contact.getValue()).hasToString("+22 555 111 1313");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("2");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

    // Second home contact
    contact = contacts.get(1); 
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.MOBILE);
    assertThat(contact.getValue()).hasToString("+22 555 222 1313");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("1");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

    // First work contact
    contact = contacts.get(2); 
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.WORK);
    assertThat(contact.getValue()).hasToString("(555) 111 1414 ext. 889");
    assertThat(contact.hasRank()).isFalse();
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

  }

  @Test

  public void patient_no_telcom_test() {

    String patientNoPhone =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n"
    ;
  
    Patient patient = PatientUtils.createPatientFromHl7Segment(patientNoPhone);
    assertThat(patient.hasTelecom()).isFalse();

  }


}
