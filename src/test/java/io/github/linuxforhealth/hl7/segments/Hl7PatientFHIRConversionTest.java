/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7PatientFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7PatientFHIRConversionTest.class);

  @Test
  public void test_patient_additional_demographics() {
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\n"
    		+ "PID|1||1234^^^AssigningAuthority^MR||TEST^PATIENT|\n"
    		+ "PD1|||Sample Family Practice^^2222|1111^LastName^ClinicianFirstName^^^^Title||||||||||||A|";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
	String json = ftv.convert(hl7message , PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);
    Patient patient = getResourcePatient(patientResource.get(0));
    List<Reference> refs = patient.getGeneralPractitioner();
    assertThat(refs.size()).isGreaterThan(0);

    List<Resource> practitionerResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practitionerResource).hasSize(1);
    Practitioner doc = getResourcePractitioner(practitionerResource.get(0));
    String lastName = doc.getName().get(0).getFamily();
    assertThat(lastName).isEqualTo("LastName");
  }


  /**
   * In order to generate messageHeader resource, MSH should have MSH.24.2 as this is required
   * attribute for source attribute, and source is required for MessageHeader resource.
   * 
   * 
   */

  @Test
  public void patient_deceased_conversion_test() {

    String patientMsgDeceasedEmpty =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||\n"
    ;
    String patientMsgNotDeadBooleanN =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||N\n"
    ;
    String patientMsgDeceasedDateOnlyYYYY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||2006|\n"
    ;
    String patientMsgDeceasedDateOnlyYYYYMM =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||200611|\n"
    ;
    String patientMsgDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120115930+0100|\n"
    ;

    String patientMsgDeceasedBooleanYOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||Y\n"
    ;
    String patientMsgDeceasedDateAndBooleanY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120|Y\n"
    ;

    Patient patientObjDeceasedEmpty = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedEmpty);
    assertThat(patientObjDeceasedEmpty.hasDeceased()).isFalse();   
    assertThat(patientObjDeceasedEmpty.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedEmpty.hasDeceasedDateTimeType()).isFalse(); 

    Patient patientObjNotDeadBooleanN = PatientUtils.createPatientFromHl7Segment(patientMsgNotDeadBooleanN);
    assertThat(patientObjNotDeadBooleanN.hasDeceased()).isTrue();   
    assertThat(patientObjNotDeadBooleanN.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientObjNotDeadBooleanN.getDeceasedBooleanType().booleanValue()).isFalse();   

    Patient patientObjDeceasedBooleanYOnly = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedBooleanYOnly);
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedDateTimeType()).isFalse(); 
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientObjDeceasedBooleanYOnly.getDeceasedBooleanType().booleanValue()).isTrue();  

    Patient patientObjDeceasedDateOnlyYYYY = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYY);
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006"); 
    
    Patient patientObjDeceasedDateOnlyYYYYMM = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYYMM);
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMM.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11");  

    Patient patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ);
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20T11:59:30+01:00");

    Patient patientObjDeceasedDateAndBooleanY = PatientUtils.createPatientFromHl7Segment(patientMsgDeceasedDateAndBooleanY);
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateAndBooleanY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20");  //DateUtil.formatToDate
  }

  @Test
  public void patient_multiple_birth_conversion_test() {

    /** 
     * Simplified logic for multiple birth  
     * 
     * Y + number = number
     * N + number = N
     * Y + blank = Y
     * N + blank = N
     * blank + number = number
     * blank + blank = nothing. 
    * 
     */

    String patientMsgEmptyMultiple =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|||USA||||\n"
    ;
    Patient patientObjEmptyMultiple = PatientUtils.createPatientFromHl7Segment(patientMsgEmptyMultiple);
    assertThat(patientObjEmptyMultiple.hasMultipleBirth()).isFalse();   
    assertThat(patientObjEmptyMultiple.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjEmptyMultiple.hasMultipleBirthBooleanType()).isFalse(); 

    String patientMsgMultipleN =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|N||USA||||\n"
    ;
    Patient patientObjMultipleN = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleN);
    assertThat(patientObjMultipleN.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleN.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjMultipleN.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientObjMultipleN.getMultipleBirthBooleanType().booleanValue()).isFalse(); 

    String patientMsgMultipleNumberOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA||2|USA||||\n"
    ;
    // A number when the boolean is missing presumes the number has meaning.  An integer is created.
    Patient patientObjMultipleNumberOnly = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleNumberOnly);
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientObjMultipleNumberOnly.getMultipleBirthIntegerType().asStringValue()).isEqualTo("2"); 

    String patientMsgMultipleBooleanYOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y||USA||||\n"
    ;
    Patient patientObjMultipleBooleanYOnly = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleBooleanYOnly);
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientObjMultipleBooleanYOnly.getMultipleBirthBooleanType().booleanValue()).isTrue(); 
 
    String patientMsgMultipleNumberAndBooleanY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y|3|USA||||\n"
    ;
    Patient patientObjMultipleNumberAndBooleanY = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleNumberAndBooleanY);
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientObjMultipleNumberAndBooleanY.getMultipleBirthIntegerType().asStringValue()).isEqualTo("3");  //DateUtil.formatToDate

    String patientMsgMultipleN16 =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|N|16|USA||||\n"
    ;
    Patient patientObjMultipleN16 = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleN16);
    assertThat(patientObjMultipleN16.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleN16.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjMultipleN16.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientObjMultipleN16.getMultipleBirthBooleanType().booleanValue()).isFalse();  
  }

  @Test
  public void patient_use_name_conversion_test() {
    String patientUseName =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||PA123456^^^MYEMR^MR||TestPatient^John^M^^^^B|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r";

    Patient patientObjUsualName = PatientUtils.createPatientFromHl7Segment(patientUseName);

    java.util.List<org.hl7.fhir.r4.model.HumanName> name = patientObjUsualName.getName();
    HumanName.NameUse useName =  name.get(0).getUse();
    assertThat(useName).isEqualTo(HumanName.NameUse.OFFICIAL);

  }

  @Test
  public void patient_name_test() {
    String patientHasMiddleName =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||PA123456^^^MYEMR^MR||JONES^GEORGE^M^JR^^^B|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r";

    Patient patientObjUsualName = PatientUtils.createPatientFromHl7Segment(patientHasMiddleName);

    java.util.List<org.hl7.fhir.r4.model.HumanName> name = patientObjUsualName.getName();
    List  givenName =  name.get(0).getGiven();
    List<StringType> suffix = name.get(0).getSuffix();
    String fullName = name.get(0).getText();
    assertThat(givenName.get(0).toString()).isEqualTo("GEORGE");
    assertThat(givenName.get(1).toString()).isEqualTo("M");
    assertThat(suffix.get(0).toString()).isEqualTo("JR");
    assertThat(fullName).isEqualTo("GEORGE M JONES JR");

  }

  @Test
  public void patientGenderTest() {
    String patientEmptyGenderField =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|0010||||DOE^JOHN^A^|||||||\r";

    String patientWithGenderField =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|0010||||DOE^JOHN^A^|||M|||||||\r";

    Patient patientObjNoGender = PatientUtils.createPatientFromHl7Segment(patientEmptyGenderField);
    Enumerations.AdministrativeGender gender = patientObjNoGender.getGender();
    assertThat(gender).isNull();

    Patient patientObjGender = PatientUtils.createPatientFromHl7Segment(patientWithGenderField);
    Enumerations.AdministrativeGender gen = patientObjGender.getGender();
    assertThat(gen).isNotNull();
    assertThat(gen).isEqualTo(Enumerations.AdministrativeGender.MALE);
  }

  @Test
  public void patientMaritalStatusTest(){
    String marriedPatientWithVersion =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||12345678^^^MRN||TestPatient^Jane|||||||||||M^^^^^^47||||||\r";

    String singlePatientWithVersion =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||12345678^^^MRN||TestPatient^Jane|||||||||||S^^^^^^1.1||||||\r";

    Patient patientObjMarried = PatientUtils.createPatientFromHl7Segment(marriedPatientWithVersion);
    assertThat(patientObjMarried.hasMaritalStatus()).isTrue();
    assertThat(patientObjMarried.getMaritalStatus().getText()).isEqualTo("Married");
    assertThat(patientObjMarried.getMaritalStatus().getCoding()).hasSize(1);
    Coding coding = patientObjMarried.getMaritalStatus().getCodingFirstRep();
    assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.M.getDisplay());
    assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.M.getSystem());
    assertThat(coding.getVersion()).isEqualTo("47");

    Patient patientObjMarriedAltText = PatientUtils.createPatientFromHl7Segment(singlePatientWithVersion);
    assertThat(patientObjMarriedAltText.hasMaritalStatus()).isTrue();
    assertThat(patientObjMarriedAltText.getMaritalStatus().getText()).isEqualTo("Never Married");
    assertThat(patientObjMarriedAltText.getMaritalStatus().getCoding()).hasSize(1);
    coding = patientObjMarriedAltText.getMaritalStatus().getCodingFirstRep();
    assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.S.getDisplay());
    assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.S.getSystem());
    assertThat(coding.getVersion()).isEqualTo("1.1");

  }

  @Test
  public void patientCommunicationLanguage(){

    String patientSpeaksEnglishWithSystem =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||PA123456^^^MYEMR^MR||DOE^JOHN|||M|||||||ENG^English^HL70296|||||||||Y|2\r";

    String patientEnglishNoSystem = //NO coding system given in the CWE
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||PA123456^^^MYEMR^MR||DOE^JANE|||M|||||||ENG^English|||||||||Y|2\r";

    String patientEnglishCodeOnly = //NO coding system given in the CWE
    "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
            "PID|1||PA123456^^^MYEMR^MR||DOE^JANE|||M|||||||ENG|||||||||Y|2\r";                

    Patient patientObjEnglish = PatientUtils.createPatientFromHl7Segment(patientSpeaksEnglishWithSystem);
    assertThat(patientObjEnglish.hasCommunication()).isTrue();
    assertThat(patientObjEnglish.getCommunication().get(0).getPreferred()).isTrue();
    assertThat(patientObjEnglish.getCommunication()).hasSize(1);
    Patient.PatientCommunicationComponent cc = patientObjEnglish.getCommunication().get(0);
    assertThat(cc.getPreferred()).isTrue();
    assertThat(cc.getLanguage().getText()).isEqualTo("English");
    Coding code = cc.getLanguage().getCodingFirstRep();
    assertThat(code.getCode()).isEqualTo("ENG");
    assertThat(code.getSystem()).isEqualTo("urn:id:v2-0296");
    assertThat(code.getDisplay()).isEqualTo("English");

    Patient patientObjNoSystem = PatientUtils.createPatientFromHl7Segment(patientEnglishNoSystem);
    assertThat(patientObjNoSystem.hasCommunication()).isTrue();
    assertThat(patientObjNoSystem.getCommunication().get(0).getPreferred()).isTrue();
    assertThat(patientObjNoSystem.getCommunication()).hasSize(1);
    Patient.PatientCommunicationComponent ccNoCode = patientObjNoSystem.getCommunication().get(0);
    assertThat(ccNoCode.getPreferred()).isTrue();
    assertThat(ccNoCode.getLanguage().getText()).isEqualTo("English");
    Coding codeNo = ccNoCode.getLanguage().getCodingFirstRep();
    assertThat(codeNo.getCode()).isEqualTo("ENG");
    assertThat(codeNo.getSystem()).isNull();
    assertThat(codeNo.hasDisplay()).isFalse();

    Patient patientObjCodeOnly = PatientUtils.createPatientFromHl7Segment(patientEnglishCodeOnly);
    assertThat(patientObjCodeOnly.hasCommunication()).isTrue();
    assertThat(patientObjCodeOnly.getCommunication().get(0).getPreferred()).isTrue();
    assertThat(patientObjCodeOnly.getCommunication()).hasSize(1);
    Patient.PatientCommunicationComponent ccCodeOnly = patientObjCodeOnly.getCommunication().get(0);
    assertThat(ccCodeOnly.getPreferred()).isTrue();
    assertThat(ccCodeOnly.getLanguage().hasText()).isFalse();
    Coding coding = ccCodeOnly.getLanguage().getCodingFirstRep();
    assertThat(coding.getCode()).isEqualTo("ENG");
    assertThat(coding.getSystem()).isNull();
    assertThat(codeNo.hasDisplay()).isFalse();

  }

  private Patient getResourcePatient(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Patient.class;
    return (Patient) context.getParser().parseResource(klass, s);
  }

  private static Practitioner getResourcePractitioner(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Practitioner.class;
    return (Practitioner) context.getParser().parseResource(klass, s);
  }

  private void validate_lineage_json(List<Extension>  extensions, String messageType) {

    assertThat(extensions.size()).isEqualTo(6);

    for(Extension extension: extensions) {

        // Get the URL
        String url = extension.getUrl();
        LOGGER.debug("URL:" + url);

        // Get the value
        String value = extension.getValue().toString();

        //If the value is a codeable concept and not a simple value - parse the value out of the codeable concept.
        if(value.indexOf("CodeableConcept") >= 0) {

          String codeableConceptValue = extension.getValue().getChildByName("coding").getValues().get(0).getNamedProperty("code").getValues().get(0).toString();
          LOGGER.debug("CodeableConceptValue:" + codeableConceptValue.toString());
          value = codeableConceptValue.toString();

        }
        //Get the Name from the URL
        String name = url.substring(url.lastIndexOf("/")+1,url.length());
        LOGGER.debug("Name:" + name);
        LOGGER.debug("Value:" + value);

        LOGGER.debug("Message Type:" + messageType);

        String[] messageParts = messageType.split("\\^");

        // test value based off the name.
        switch(name) {
          case "source-event-timestamp":
            assertThat(value).isEqualTo("DateTimeType[2006-09-15T21:00:00+08:00]");
            break;
          case "source-record-id":
            assertThat(value).isEqualTo("1473973200100600");
            break;
          case "source-data-model-version":
            assertThat(value).isEqualTo("2.3");
            break;
          case "process-client-id":
            assertThat(value).isEqualTo("SendingApplication");
            break;
          case "source-event-trigger":
            assertThat(value).isEqualTo(messageParts[1]);
            break;
          case "source-record-type":
            assertThat(value).isEqualTo(messageParts[0]);
            break;
          default:
            // this shouldn't happen
            LOGGER.debug("Not found");
            Assertions.fail();
            break;
        }

    }

  }

  private void validate_data_lineage(String hl7message, String messageType) {

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message , PatientUtils.OPTIONS);
    System.out.println(json);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Get bundle meta extensions *not using these currently*
    // Meta bundleMeta = b.getMeta();
    // List<Extension> bundleMetaExtensions = bundleMeta.getExtension();

    LOGGER.debug("Found {} resources.", e.stream().count());

    e.stream().forEach(bec -> { 
      LOGGER.debug("Validating "+bec.getResource().getResourceType());
      Meta meta = bec.getResource().getMeta();
      List<Extension> extensions =  meta.getExtension();
      LOGGER.debug("Found "+extensions.size()+" meta extensions");
      validate_lineage_json(extensions, messageType); 
    });

  }

  // Tests Data lineage for a ORU^R01 message
  @Test
  public void verify_data_lineage_ORU() {

    String hl7message = 
        "MSH|^~\\&|SendingApplication|Sending^Facility|Receiving-Application|ReceivingFacility|20060915210000||ORU^R01|1473973200100600|P|2.3|||NE|NE\n" +
        "PID|1||1234^^^AssigningAuthority^MR||TEST^PATIENT|\n" +
        "PD1|||Sample Family Practice^^2222|1111^LastName^ClinicianFirstName^^^^Title||||||||||||A|";

    validate_data_lineage(hl7message, "ORU^R01");  

  }

  // Tests Data lineage for a ADT^A01 message
  @Test
  public void verify_data_lineage_ADT() {

    String hl7message = "MSH|^~\\&|SendingApplication|hl7Integration|||20060915210000||ADT^A01|1473973200100600||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r";

    validate_data_lineage(hl7message, "ADT^A01");

  }


  // Tests Data lineage for a VXU^V04 message
  @Test
  public void verify_data_lineage_VXU() {

    String hl7message = 
    "MSH|^~\\&|SendingApplication|RI88140101|KIDSNET_IFL|RIHEALTH|20060915210000||VXU^V04|1473973200100600|P|2.3|||NE|AL||||||RI543763\r"
    + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
    + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
    + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
    + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
    + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
    + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
    + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
    + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
    + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r"
    + "OBX|5|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r";

    validate_data_lineage(hl7message, "VXU^V04");

  }


}
