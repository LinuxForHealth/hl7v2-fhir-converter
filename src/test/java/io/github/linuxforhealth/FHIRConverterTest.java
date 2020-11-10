/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverterTest {
  private static final String HL7_FILE_UNIX_NEWLINE = "src/test/resources/sample_unix.hl7";
  private static final String HL7_FILE_WIN_NEWLINE = "src/test/resources/sample_win.hl7";
  private static final String HL7_FILE_WIN_NEWLINE_BATCH =
      "src/test/resources/sample_win_batch.hl7";

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void test_patient_encounter() throws IOException {

    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, true, BundleType.TRANSACTION);

    System.out.println(json);
    verifyResult(json);


  }



  @Test
  public void convert_hl7_from_file_to_fhir_unix_line_endings() throws IOException {
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter(true, BundleType.COLLECTION);
    String json = ftv.convert(new File(HL7_FILE_UNIX_NEWLINE));
    verifyResult(json);


  }



  @Test
  public void convert_hl7_from_file_to_fhir_wiin_line_endings() throws IOException {
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter(true, BundleType.COLLECTION);
    String json = ftv.convert(new File(HL7_FILE_WIN_NEWLINE));
    verifyResult(json);


  }

  @Test
  public void test_valid_message_but_unsupported_message_throws_exception() throws IOException {
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A02|102|T|2.6|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    exceptionRule.expect(UnsupportedOperationException.class);
    ftv.convert(hl7message);

  }


  @Test
  public void test_invalid_message_throws_error() throws IOException {
    String hl7message = "some text";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    exceptionRule.expect(IllegalArgumentException.class);
    ftv.convert(hl7message);

  }



  @Test
  public void test_blank_message_throws_error() throws IOException {
    String hl7message = "";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    exceptionRule.expect(IllegalArgumentException.class);
    ftv.convert(hl7message);

  }

  private void verifyResult(String json) {
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.TRANSACTION);
    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();
    assertThat(b.getMeta().getSource()).contains("Message: ADT_A01, Message Control Id: 102");
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(4);

    List<Resource> allergyResources =
        e.stream().filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(allergyResources).hasSize(2);
  }


}
