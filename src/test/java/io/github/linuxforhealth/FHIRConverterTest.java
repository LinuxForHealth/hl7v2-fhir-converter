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
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverterTest {
  private static final String HL7_FILE_UNIX_NEWLINE = "src/test/resources/sample_unix.hl7";
  private static final String HL7_FILE_WIN_NEWLINE = "src/test/resources/sample_win.hl7";
  private static final String HL7_FILE_WIN_NEWLINE_BATCH =
      "src/test/resources/sample_win_batch.hl7";
  private static final ConverterOptions OPTIONS =
      new Builder().withValidateResource().withPrettyPrint().build();

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();



  @Test
  public void test_patient_encounter() throws IOException {

    String hl7message =
        "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
            + "EVN||201209122222\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
            + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
            + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
            + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE);


  }


  @Test
  public void test_patient_encounter_no_message_header() throws IOException {

    String hl7message =
        "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||\r"
        + "EVN||201209122222\r"
        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE, false);


  }


  @Test
  public void convert_hl7_from_file_to_fhir_unix_line_endings() throws IOException {
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(new File(HL7_FILE_UNIX_NEWLINE));
    verifyResult(json, BundleType.COLLECTION);


  }



  @Test
  public void convert_hl7_from_file_to_fhir_wiin_line_endings() throws IOException {
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    ConverterOptions options =
        new Builder().withBundleType(BundleType.COLLECTION).withValidateResource().build();

    String json = ftv.convert(new File(HL7_FILE_WIN_NEWLINE), options);
    verifyResult(json, BundleType.COLLECTION);


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
  public void test_ORU_r01_without_status() throws IOException {
    // in the following line: "PID|1||PATID5421^^^NIST MPI^MR" we will close the space  between  NIST and MPI until issue #65 (white space bug) is resolved
    String ORU_r01 = "MSH|^~\\&|NIST Test Lab APP|NIST Lab Facility||NIST EHR Facility|20150926140551||ORU^R01|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||\r" +
            "PID|1||PATID5421^^^NISTMPI^MR||Wilson^Patrice^Natasha^^^^L||19820304|F||2106-3^White^HL70005|144 East 12th Street^^Los Angeles^CA^90012^^H||^PRN^PH^^^203^2290210|||||||||N^Not Hispanic or Latino^HL70189\r" +
            "ORC|NW|ORD448811^NIST EHR|R-511^NIST Lab Filler||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r" +
            "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r" +
            "OBX|1|CWE|22314-9^Hepatitis A virus IgM Ab [Presence] in Serum^LN^HAVM^Hepatitis A IgM antibodies (IgM anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r" +
            "OBX|2|CWE|20575-7^Hepatitis A virus Ab [Presence] in Serum^LN^HAVAB^Hepatitis A antibodies (anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r" +
            "OBX|3|NM|22316-4^Hepatitis B virus core Ab [Units/volume] in Serum^LN^HBcAbQ^Hepatitis B core antibodies (anti-HBVc) Quant^L^2.52||0.70|[IU]/mL^international unit per milliliter^UCUM^IU/ml^^L^1.9|<0.50 IU/mL|H|||F|||20150925|||||201509261400";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(ORU_r01, OPTIONS);
    DiagnosticReport expectStatusUnknown = verifyResult(json);
    assertThat(expectStatusUnknown.hasStatusElement());
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

  @Test
  public void test_VXU_V04_message() {
    String hl7VUXmessageRep =
        "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
            + "EVN|A01|20130617154644||01\r"
            + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
            + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
            + "PV1|1|ff|yyy|E|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"

            + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
            + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
            + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
            + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
            + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
            + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
            + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r";


    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    String json = ftv.convert(hl7VUXmessageRep, OPTIONS);

    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);
    List<Resource> messageHeader =
        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(messageHeader).hasSize(1);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(1);

    List<Resource> organizationRes =
        e.stream().filter(v -> ResourceType.Organization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(organizationRes).hasSize(2);
  }

  private void verifyResult(String json, BundleType expectedBundleType) {
    verifyResult(json, expectedBundleType, true);
  }

  private DiagnosticReport verifyResult(String json ){
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> diagnosticReport =
            e.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReport).hasSize(1);
    String s = context.getParser().encodeResourceToString(diagnosticReport.get(0));
    Class<? extends IBaseResource> klass = DiagnosticReport.class;
    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }


  private void verifyResult(String json, BundleType expectedBundleType,
      boolean messageHeaderExpected) {
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(expectedBundleType);
    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

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

    if (messageHeaderExpected) {
    List<Resource> messageHeader =
        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(messageHeader).hasSize(1);
    }
  }

}
