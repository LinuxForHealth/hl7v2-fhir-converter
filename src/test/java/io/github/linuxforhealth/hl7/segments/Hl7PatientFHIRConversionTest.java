/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class Hl7PatientFHIRConversionTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS =
      new Builder().withValidateResource().withPrettyPrint().build();

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();


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

    Patient patientObjDeceasedEmpty = createPatientFromHl7Segment(patientMsgDeceasedEmpty);
    assertThat(patientObjDeceasedEmpty.hasDeceased()).isFalse();   
    assertThat(patientObjDeceasedEmpty.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedEmpty.hasDeceasedDateTimeType()).isFalse(); 

    Patient patientObjNotDeadBooleanN = createPatientFromHl7Segment(patientMsgNotDeadBooleanN);
    assertThat(patientObjNotDeadBooleanN.hasDeceased()).isTrue();   
    assertThat(patientObjNotDeadBooleanN.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientObjNotDeadBooleanN.getDeceasedBooleanType().booleanValue()).isFalse();   

    Patient patientObjDeceasedBooleanYOnly = createPatientFromHl7Segment(patientMsgDeceasedBooleanYOnly);
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedDateTimeType()).isFalse(); 
    assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientObjDeceasedBooleanYOnly.getDeceasedBooleanType().booleanValue()).isTrue();  

    Patient patientObjDeceasedDateOnlyYYYY = createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYY);
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006"); 
    
    Patient patientObjDeceasedDateOnlyYYYYMM = createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYYMM);
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMM.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11");  

    Patient patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ = createPatientFromHl7Segment(patientMsgDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ);
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20T11:59:30+01:00");

    Patient patientObjDeceasedDateAndBooleanY = createPatientFromHl7Segment(patientMsgDeceasedDateAndBooleanY);
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceased()).isTrue();   
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientObjDeceasedDateAndBooleanY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20");  //DateUtil.formatToDate
  }

  @Test
  public void patient_multiple_birth_conversion_test() {

    String patientMsgEmptyMultiple =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|||USA||||\n"
    ;
    String patientMsgMultipleN =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|N||USA||||\n"
    ;
    String patientMsgMultipleNumberOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA||2|USA||||\n"
    ;
    String patientMsgMultipleBooleanYOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y||USA||||\n"
    ;
    String patientMsgMultipleNumberAndBooleanY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|3|USA||||\n"
    ;

    Patient patientObjEmptyMultiple = createPatientFromHl7Segment(patientMsgEmptyMultiple);
    assertThat(patientObjEmptyMultiple.hasMultipleBirth()).isFalse();   
    assertThat(patientObjEmptyMultiple.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjEmptyMultiple.hasMultipleBirthBooleanType()).isFalse(); 

    Patient patientObjMultipleN = createPatientFromHl7Segment(patientMsgMultipleN);
    assertThat(patientObjMultipleN.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleN.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjMultipleN.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientObjMultipleN.getMultipleBirthBooleanType().booleanValue()).isFalse();   

    Patient patientObjMultipleBooleanYOnly = createPatientFromHl7Segment(patientMsgMultipleBooleanYOnly);
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientObjMultipleBooleanYOnly.getMultipleBirthBooleanType().booleanValue()).isTrue();  

    // A number supercedes any boolean value, and multiple births are assumed true 
    Patient patientObjMultipleNumberOnly = createPatientFromHl7Segment(patientMsgMultipleNumberOnly);
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientObjMultipleNumberOnly.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientObjMultipleNumberOnly.getMultipleBirthIntegerType().asStringValue()).isEqualTo("2"); 

    Patient patientObjMultipleNumberAndBooleanY = createPatientFromHl7Segment(patientMsgMultipleNumberAndBooleanY);
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirth()).isTrue();   
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientObjMultipleNumberAndBooleanY.getMultipleBirthIntegerType().asStringValue()).isEqualTo("3");  //DateUtil.formatToDate
  }

  private static Patient createPatientFromHl7Segment(String inputSegment){
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(inputSegment, OPTIONS);
    System.out.println(json.toString());

    assertThat(json).isNotBlank();
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patients =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patients).hasSize(1);
    return getPatientFromResource(patients.get(0));
  }  

  private static Patient getPatientFromResource(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Patient.class;
    return (Patient) context.getParser().parseResource(klass, s);
  }


}
