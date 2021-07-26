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
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

public class Hl7TelecomFHIRConversionTest {

  @Test
  public void patient_telcom_test() {

    String patientPhone = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
        // Home has 2 phones and an email, work has one phone and two emails
        + "PID|1||12345678^^^^MR|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||^PRN^PH^^22^555^1111313^^^^^^^^^^^3~^PRN^CP^^22^555^2221313^^^^^^^^^^^1~^NET^X.400^email.test@gmail.com^^^^^^^^^^^^^^2|^PRN^PH^^^555^1111414^889~^^^professional@buisness.com~^^^moose.mickey@buisness.com^^^^^^^^^^^^^^4||||||||||||||||\n";

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientPhone);
    assertThat(patient.hasTelecom()).isTrue();
    List<ContactPoint> contacts = patient.getTelecom();
    assertThat(contacts.size()).isEqualTo(6);

    // First home contact
    ContactPoint contact = contacts.get(0);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.HOME);
    assertThat(contact.getValue()).hasToString("+22 555 111 1313");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("3");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

    // Second home contact is mobile
    contact = contacts.get(1);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.MOBILE);
    assertThat(contact.getValue()).hasToString("+22 555 222 1313");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("1");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

    // Third home contact is an email
    contact = contacts.get(2);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.HOME);
    assertThat(contact.getValue()).hasToString("email.test@gmail.com");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("2");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.EMAIL);

    // First work contact is work phone
    contact = contacts.get(3);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.WORK);
    assertThat(contact.getValue()).hasToString("(555) 111 1414 ext. 889");
    assertThat(contact.hasRank()).isFalse();
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);

    // Second work contact is external work email
    contact = contacts.get(4);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.WORK);
    assertThat(contact.getValue()).hasToString("professional@buisness.com");
    assertThat(contact.hasRank()).isFalse();
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.EMAIL);

    // Third work contact is internal work email and is ranked for contact
    contact = contacts.get(5);
    assertThat(contact.getUse()).isEqualTo(ContactPoint.ContactPointUse.WORK);
    assertThat(contact.getValue()).hasToString("moose.mickey@buisness.com");
    assertThat(contact.hasRank()).isTrue();
    assertThat(contact.getRank()).hasToString("4");
    assertThat(contact.getSystem()).isEqualTo(ContactPoint.ContactPointSystem.EMAIL);

  }

  @Test
  public void patient_no_telcom_test() {

    String patientNoPhone = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
        + "PID|1||12345678^^^^MR|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n";

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientNoPhone);
    assertThat(patient.hasTelecom()).isFalse();

  }

}
