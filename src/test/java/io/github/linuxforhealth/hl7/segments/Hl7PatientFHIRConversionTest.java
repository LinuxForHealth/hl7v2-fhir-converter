/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;

public class Hl7PatientFHIRConversionTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  
  private static FHIRContext context = new FHIRContext(true, false);

  @Test
  public void test_patient_additional_demographics() throws IOException {
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
    assertThat(refs.size() > 0);
        
    List<Resource> practitionerResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practitionerResource).hasSize(1);
    Practitioner doc = getResourcePractitioner(practitionerResource.get(0));
    String lastName = doc.getName().get(0).getFamily();
    assertThat(lastName.equals("LastName"));
  }
  
  
  /**
   * In order to generate messageHeader resource, MSH should have MSH.24.2 as this is required
   * attribute for source attribute, and source is required for MessageHeader resource.
   * 
   * @throws IOException
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
    // A number is ignored if there is no boolean and multiple birth is not even created
    Patient patientObjMultipleNumberOnly = PatientUtils.createPatientFromHl7Segment(patientMsgMultipleNumberOnly);
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirth()).isFalse();   

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
                    "PID|1||PA123456^^^MYEMR^MR||JONES^GEORGE^M^JR^^^B|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r";

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
    List<StringType>  givenName =  name.get(0).getGiven();
    List<StringType> suffix = name.get(0).getSuffix();
    String fullName = name.get(0).getText();
    assertThat(givenName.get(0).toString()).isEqualTo("GEORGE");
    assertThat(givenName.get(1).toString()).isEqualTo("M");
    assertThat(suffix.get(0).toString()).isEqualTo("JR");
    assertThat(fullName).isEqualTo("GEORGE M JONES JR");

  }

  @Test
  public void patient_gender_test() {
    String patientEmptyGenderField =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r";

    String patientWithGenderField =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|M||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r";

    Patient patientObjNoGender = PatientUtils.createPatientFromHl7Segment(patientEmptyGenderField);
    Enumerations.AdministrativeGender gender = patientObjNoGender.getGender();
    assertThat(gender).isNull();

    Patient patientObjGender = PatientUtils.createPatientFromHl7Segment(patientWithGenderField);
    Enumerations.AdministrativeGender gen = patientObjGender.getGender();
    assertThat(gen).isNotNull();
    assertThat(gen).isEqualTo(Enumerations.AdministrativeGender.MALE);
  }
  @Test
  public void patient_marital_status_test(){
    String marriedPatient =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||000054321^^^MRN||COOPER^SHELDON^ANDREW||19820512|M||2106-3|765 SOMESTREET RD UNIT 3A^^PASADENA^LA^558846^United States of America||4652141486^Home^^shelly@gmail.com||EN^English|M^Married|CAT|78654||||N\r";
    String AltTextField =
            "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                    "PID|1||000054321^^^MRN||COOPER^SHELDON^ANDREW||19820512|M||2106-3|765 SOMESTREET RD UNIT 3A^^PASADENA^LA^558846^United States of America||4652141486^Home^^shelly@gmail.com||EN^English|S^^^^^^^^Single|CAT|78654||||N\r";
    Patient patientObjMarried = PatientUtils.createPatientFromHl7Segment(marriedPatient);
    assertThat(patientObjMarried.hasMaritalStatus()).isTrue();
    assertThat(patientObjMarried.getMaritalStatus().getText()).isEqualTo("Married");
    assertThat(patientObjMarried.getMaritalStatus().getCodingFirstRep().getDisplay()).isEqualTo(V3MaritalStatus.M.getDisplay());
    assertThat(patientObjMarried.getMaritalStatus().getCodingFirstRep().getSystem()).isEqualTo(V3MaritalStatus.M.getSystem());

    Patient patientObjMarriedAltText = PatientUtils.createPatientFromHl7Segment(AltTextField);
    assertThat(patientObjMarriedAltText.hasMaritalStatus()).isTrue();
    assertThat(patientObjMarriedAltText.getMaritalStatus().getText()).isEqualTo("Single");
    assertThat(patientObjMarriedAltText.getMaritalStatus().getCodingFirstRep().getDisplay()).isEqualTo(V3MaritalStatus.S.getDisplay());
    assertThat(patientObjMarriedAltText.getMaritalStatus().getCodingFirstRep().getSystem()).isEqualTo(V3MaritalStatus.S.getSystem());

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

}
