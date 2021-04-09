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

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
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
  public void test_omp() throws IOException, HL7Exception {
    String vmsg = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|OMP^O09^OMP_O09|1|P^I|2.6|\r" +
            "PID|||555444222111^^^MPI&GenHosp^MR||smith^john||19600614|M||C|99 Oakland #106^^Toronto^ON^44889||||||||343132266|||N\r" +
            "PD1|S|A|patientpfacisXON|primarycareXCN||||F|Y|duppatient|pubcode|Y|20060316|placeofworshipXON|DNR|A|20060101|20050101|USA||ACT\r" +
            "NTE|1|P|comment after PD1\r" +
            "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r" +
            "PV2|priorpendingpoint|accomcode|admitreason|transferreason|patientvalu|patientvaluloc|PH|20060315|20060316|2|4|visit description|referral1~referral2|20050411|Y|P|20060415|FP|Y|1|F|Y|clinicorgname|AI|2|20060316|05|20060318|20060318|chargecode|RC|Y|20060315|Y|Y|Y|Y|P|K|AC|A|A|N|Y|DNR|20060101|20060401|200602140900|O\r" +
            "IN1|1|insuranceplanA|insurancecoB\r" +
            "IN2|Insuredempid|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||Y|N|Y\r" +
            "GT1|6357||gname|gspousename|gaddress|||20060308|F|GT||111-22-3456|20060101|20060101|23|gempname|||||||||N||||||||M\r" +
            "AL1|1|DA|allergydescription|SV|allergyreasonxx\r" +
            "ORC|NW|1000^OE|9999999^RX|||E|40^QID^D10^^^R\r" +
            "TQ1||||||30^M|199401060930|199401061000\r" +
            "TQ2|1|S|relatedplacer|relatedfiller|placergroupnumber|EE|*|10^min|4|N\r" +
            "RXO|RX2111^Polycillin 500 mg TAB|500||MG|reqdosgform|providerspharm~prpharm2|providersadmin1~providersadmin2||G||40|dispenseunits|2|orderDEAisXCN|pharmverifidXCN|Y|day|3|units|indication|rate|rateunits\r" +
            "NTE|1|P|comment after RXO\r" +
            "RXR|^PO|\r" +
            "RXC|B|D5/.45NACL|1000|ML\r" +
            "NTE|1|P|comment after RXC\r" +
            "OBX|1|TX|^hunchback|1|Increasing||||||S\r" +
            "NTE|1|P|comment after OBX\r" +
            "FT1|1|||199805291115||CG|00378112001^VerapamilHydrochloride 120 mgTAB^NDC|||1|5&USD^TP\r" +
            "BLG|D|CH|accountid|01";

//    HapiContext ctx = new DefaultHapiContext();
//    Message msg = ctx.getGenericParser().parse(vmsg);

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    ConverterOptions OPTIONS2 =
            new Builder().withPrettyPrint().build();
    String json = ftv.convert(vmsg, OPTIONS2);

    System.out.println(json);

  }

  @Test
  public void test_med() throws IOException {
    String hl7message = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6\r" +
            "PID|1||000054321^^^MRN||COOPER^SHELDON^||19820512|M|||765 SOMESTREET RD UNIT 3A^^PASADENA^LA^|||||S||78654|\r" +
            "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000\r" +
            "PV2|priorpendingpoint|accomcode|admitreason^Prescribed Medications|transferreason|patientvalu|patientvaluloc|PH|20170315|20060316|2|4|visit description|referral1~referral2|20170411|Y|P|20170415|FP|Y|1|F|Y|General Hospital^^^^^^^GH|AI|2|20170316|05|20170318|20170318|chargecode|RC|Y|20170315|Y|Y|Y|Y|P|K|AC|A|A|N|Y|DNR|20170101|20170401|201702140900|O\r" +
            "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r" +
            "TQ1|||BID|||10^MG|20170215080000|20170228080000\r" +
            "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r" +
            "RXR|^MTH\r" +
            "RXC|B|Ampicillin 250|250|MG\r" +
            "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|" +
            "TQ1|||BID|||10^MG|20170215080000|20170228080000\r" +
            "RXR|^MTH\r" +
            "ORC|NW|F700002^OE|P100002^RX|||E|40^QID^D10^^^R||20170215080000\r" +
            "TQ1|||QID|||40^MG|20170215080000|20170222080000\r" +
            "RXO|RX700002^colchicine 0.6 mg capsule|0.6||mg|||||G||40||2|||Y|day|3|units|indication|rate|rateunits\r" +
            "RXR|^NS\r" +
            "RXC|B|D5/.45NACL|1000|ML\r" +
            "RXE|^^^20170923230000^^R|999^D5/.45NACL|1000||ml|||||40||2|\r" +
            "TQ1|||QID|||40^MG|20170215080000|20170222080000\r" +
            "RXR|^NS\r" +
            "ORC|NW|F700003^OE|P100003^RX|||E|20^QSHIFT^D10^^^R||20170202080000\r" +
            "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r" +
            "RXO|RX700003^thyroxine 0.05 MG|0.05||mg|||||G||20||2|||Y|day|3|units|indication|rate|rateunits\r" +
            "RXR|^PR\r" +
            "RXE|^^^20170923230000^^R|999^thyroxine 0.05 MG|0.05||mg|||||20||2|\r" +
            "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r" +
            "RXR|^PR\r" +
            "ORC|NW|F700004^OE|P100004^RX|||E|5^QHS^D4^^^R||20170125080000\r" +
            "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r" +
            "RXO|RX700004^metformin 850 ml orally|850||ml|||||G||5||5|\r" +
            "RXR|^PO\r" +
            "RXE|^^^20170923230000^^R|999^metformin 850 ml|850||ml|||||3||2|\r" +
            "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r" +
            "RXR|^PO\r" +
            "ORC|NW|F700005^OE|P100005^RX|||E|8^TID^D3^^^R||20170125080000\r" +
            "TQ1|||TID|||8^MG|20170125080000|20171125230000\r" +
            "RXO|RX700005^OxyContin 20 MG CAPSULE|20||mg|||||G||8||3|\r" +
            "RXR|^EP\r" +
            "RXE|^^^20170923230000^^R|999^OxyContin 20 MG CAPSULE|20||mg|||||5||3|\r" +
            "TQ1|||TID|||8^MG|20170125080000|20171125230000\r" +
            "RXR|^EP";


    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    ConverterOptions OPTIONS2 =
            new Builder().withPrettyPrint().build();
    String json = ftv.convert(hl7message, OPTIONS2);

    System.out.println(json);

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

    System.out.println(json);

//    FHIRContext context = new FHIRContext();
//    IBaseResource bundleResource = context.getParser().parseResource(json);
//    assertThat(bundleResource).isNotNull();
//    Bundle b = (Bundle) bundleResource;
//    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
//    assertThat(b.getId()).isNotNull();
//    assertThat(b.getMeta().getLastUpdated()).isNotNull();
//
//    List<BundleEntryComponent> e = b.getEntry();
//    List<Resource> patientResource =
//        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(patientResource).hasSize(1);
//    List<Resource> messageHeader =
//        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(messageHeader).hasSize(1);
//
//    List<Resource> encounterResource =
//        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(encounterResource).hasSize(1);
//    List<Resource> obsResource =
//        e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(obsResource).hasSize(1);
//    List<Resource> pracResource =
//        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(pracResource).hasSize(1);
//
//    List<Resource> organizationRes =
//        e.stream().filter(v -> ResourceType.Organization == v.getResource().getResourceType())
//            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
//    assertThat(organizationRes).hasSize(2);


  }
  private void verifyResult(String json, BundleType expectedBundleType) {
    verifyResult(json, expectedBundleType, true);
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
