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

    String patientDeceasedEmpty =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||\n"
    ;
    String patientNotDeadBooleanN =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||N\n"
    ;
    String patientDeceasedDateOnlyYYYY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||2006|\n"
    ;
    String patientDeceasedDateOnlyYYYYMM =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||200611|\n"
    ;
    String patientDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120115930+0100|\n"
    ;

    String patientDeceasedBooleanYOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||Y\n"
    ;
    String patientDeceasedDateAndBooleanY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120|Y\n"
    ;

    Patient patientAliveUnknown = createPatientFromHl7Segment(patientDeceasedEmpty);
    assertThat(patientAliveUnknown.hasDeceased()).isFalse();   
    assertThat(patientAliveUnknown.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientAliveUnknown.hasDeceasedDateTimeType()).isFalse(); 

    Patient patientNotDead = createPatientFromHl7Segment(patientNotDeadBooleanN);
    assertThat(patientNotDead.hasDeceased()).isTrue();   
    assertThat(patientNotDead.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientNotDead.getDeceasedBooleanType().booleanValue()).isFalse();   

    Patient patientBool = createPatientFromHl7Segment(patientDeceasedBooleanYOnly);
    assertThat(patientBool.hasDeceased()).isTrue();   
    assertThat(patientBool.hasDeceasedDateTimeType()).isFalse(); 
    assertThat(patientBool.hasDeceasedBooleanType()).isTrue(); 
    assertThat(patientBool.getDeceasedBooleanType().booleanValue()).isTrue();  

    Patient patientDateYYYY = createPatientFromHl7Segment(patientDeceasedDateOnlyYYYY);
    assertThat(patientDateYYYY.hasDeceased()).isTrue();   
    assertThat(patientDateYYYY.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientDateYYYY.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientDateYYYY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006"); 
    
    Patient patientDateYYYYMM = createPatientFromHl7Segment(patientDeceasedDateOnlyYYYYMM);
    assertThat(patientDateYYYYMM.hasDeceased()).isTrue();   
    assertThat(patientDateYYYYMM.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientDateYYYYMM.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientDateYYYYMM.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11");  

    Patient patientDateYYYYMMDDHHMMSSZZZZ = createPatientFromHl7Segment(patientDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ);
    assertThat(patientDateYYYYMMDDHHMMSSZZZZ.hasDeceased()).isTrue();   
    assertThat(patientDateYYYYMMDDHHMMSSZZZZ.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientDateYYYYMMDDHHMMSSZZZZ.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientDateYYYYMMDDHHMMSSZZZZ.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20T11:59:30+01:00");

    Patient patientDateBool = createPatientFromHl7Segment(patientDeceasedDateAndBooleanY);
    assertThat(patientDateBool.hasDeceased()).isTrue();   
    assertThat(patientDateBool.hasDeceasedDateTimeType()).isTrue(); 
    assertThat(patientDateBool.hasDeceasedBooleanType()).isFalse(); 
    assertThat(patientDateBool.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20");  //DateUtil.formatToDate
  }

  @Test
  public void patient_multiple_conversion_test() {

    String patientEmptyMultiple =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|||USA||||\n"
    ;
    String patientMultipleN =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|N||USA||||\n"
    ;
    String patientMultipleNumberOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA||2|USA||||\n"
    ;
    String patientMultipleBooleanYOnly =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y||USA||||\n"
    ;
    String patientMultipleNumberAndBooleanY =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|3|USA||||\n"
    ;

    Patient patientBirthMultEmpty = createPatientFromHl7Segment(patientEmptyMultiple);
    assertThat(patientBirthMultEmpty.hasMultipleBirth()).isFalse();   
    assertThat(patientBirthMultEmpty.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientBirthMultEmpty.hasMultipleBirthBooleanType()).isFalse(); 

    Patient patientBirthMultN= createPatientFromHl7Segment(patientMultipleN);
    assertThat(patientBirthMultN.hasMultipleBirth()).isTrue();   
    assertThat(patientBirthMultN.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientBirthMultN.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientBirthMultN.getMultipleBirthBooleanType().booleanValue()).isFalse();   

    Patient patientBirthMultY = createPatientFromHl7Segment(patientMultipleBooleanYOnly);
    assertThat(patientBirthMultY.hasMultipleBirth()).isTrue();   
    assertThat(patientBirthMultY.hasMultipleBirthIntegerType()).isFalse(); 
    assertThat(patientBirthMultY.hasMultipleBirthBooleanType()).isTrue(); 
    assertThat(patientBirthMultY.getMultipleBirthBooleanType().booleanValue()).isTrue();  

    // A number supercedes any boolean value, and multiple births are assumed true 
    Patient patientBirthNumber = createPatientFromHl7Segment(patientMultipleNumberOnly);
    assertThat(patientBirthNumber.hasMultipleBirth()).isTrue();   
    assertThat(patientBirthNumber.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientBirthNumber.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientBirthNumber.getMultipleBirthIntegerType().asStringValue()).isEqualTo("2"); 

    Patient patientBirthNumberBool = createPatientFromHl7Segment(patientMultipleNumberAndBooleanY);
    assertThat(patientBirthNumberBool.hasMultipleBirth()).isTrue();   
    assertThat(patientBirthNumberBool.hasMultipleBirthIntegerType()).isTrue(); 
    assertThat(patientBirthNumberBool.hasMultipleBirthBooleanType()).isFalse(); 
    assertThat(patientBirthNumberBool.getMultipleBirthIntegerType().asStringValue()).isEqualTo("3");  //DateUtil.formatToDate
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
