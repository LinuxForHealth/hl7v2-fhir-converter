/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverterTest {
  private static final String HL7_FILE_UNIX_NEWLINE = "src/test/resources/sample_unix.hl7";
  private static final String HL7_FILE_WIN_NEWLINE = "src/test/resources/sample_win.hl7";
  private static final String HL7_FILE_WIN_NEWLINE_BATCH = "src/test/resources/sample_win_batch.hl7";
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

  @Test
  public void test_patient_encounter() throws IOException {

    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN||201209122222\r"
        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r" + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE);

  }

  @Test
  public void test_patient_encounter_no_message_header() throws IOException {

    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||\r"
        + "EVN||201209122222\r"
        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r" + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
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
    ConverterOptions options = new Builder().withBundleType(BundleType.COLLECTION).withValidateResource().build();

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

    Assertions.assertThrows(UnsupportedOperationException.class, () -> {
        ftv.convert(hl7message);
    });
  }

  @Test
  public void test_ORU_r01_without_status() throws IOException {
    String ORU_r01 = "MSH|^~\\&|NIST Test Lab APP|NIST Lab Facility||NIST EHR Facility|20150926140551||ORU^R01|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||\r"
        + "PID|1||PATID5421^^^NISTMPI^MR||Wilson^Patrice^Natasha^^^^L||19820304|F||2106-3^White^HL70005|144 East 12th Street^^Los Angeles^CA^90012^^H||^PRN^PH^^^203^2290210|||||||||N^Not Hispanic or Latino^HL70189\r"
        + "ORC|NW|ORD448811^NIST EHR|R-511^NIST Lab Filler||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
        + "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
        + "OBX|1|CWE|22314-9^Hepatitis A virus IgM Ab [Presence] in Serum^LN^HAVM^Hepatitis A IgM antibodies (IgM anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r"
        + "OBX|2|CWE|20575-7^Hepatitis A virus Ab [Presence] in Serum^LN^HAVAB^Hepatitis A antibodies (anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r"
        + "OBX|3|NM|22316-4^Hepatitis B virus core Ab [Units/volume] in Serum^LN^HBcAbQ^Hepatitis B core antibodies (anti-HBVc) Quant^L^2.52||0.70|[IU]/mL^international unit per milliliter^UCUM^IU/ml^^L^1.9|<0.50 IU/mL|H|||F|||20150925|||||201509261400";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(ORU_r01, OPTIONS);

    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> diagnosticReport = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReport).hasSize(1);

    String s = context.getParser().encodeResourceToString(diagnosticReport.get(0));
    Class<? extends IBaseResource> klass = DiagnosticReport.class;
    DiagnosticReport expectStatusUnknown = (DiagnosticReport) context.getParser().parseResource(klass, s);
    DiagnosticReport.DiagnosticReportStatus status = expectStatusUnknown.getStatus();

    assertThat(expectStatusUnknown.hasStatus()).isTrue();
    assertThat(status).isEqualTo(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
  }

  @Test
  public void test_dosage_output() throws  IOException {
String hl7message =
                "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r" +
                "PID|1||PA123456^^^MYEMR^MR||JONES^GEORGE^M^JR^^^L|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r" +
                "ORC|RE||197023^CMC|||||||^Clark^Dave||1234567890^Smith^Janet^^^^^^NPPES^L^^^NPI^^^^^^^^MD\r" +
                "RXA|0|1|20140730||08^HEPB-PEDIATRIC/ADOLESCENT^CVX|.5|mL^mL^UCUM||00^NEW IMMUNIZATION RECORD^NIP001|1234567890^Smith^Janet^^^^^^NPPES^^^^NPI^^^^^^^^MD |^^^DE-000001||||0039F|20200531|MSD^MERCK^MVX|||CP|A";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);

    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> immunization =
            e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(immunization).hasSize(1);

    String s = context.getParser().encodeResourceToString(immunization.get(0));
    Class<? extends IBaseResource> klass = Immunization.class;
    Immunization expectDoseQuantity = (Immunization) context.getParser().parseResource(klass, s);
    assertThat(expectDoseQuantity.hasDoseQuantity()).isTrue();
    Quantity dosage = expectDoseQuantity.getDoseQuantity();
    BigDecimal value = dosage.getValue();
    String  unit = dosage.getUnit();
    assertThat(value).isEqualTo(BigDecimal.valueOf(.5));
    assertThat(unit).isEqualTo("mL");
  }

  @Test
  public void test_invalid_message_throws_error() throws IOException {
    String hl7message = "some text";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
   
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
        ftv.convert(hl7message);
    });
  }

  @Test
  public void test_blank_message_throws_error() throws IOException {
    String hl7message = "";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
        ftv.convert(hl7message);
    });
  }
@Test
public void test_adt_full(){
    String hl7Message = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n" +
            "SFT|Orion Health|2.4.3.52854|Rhapsody|2.4.3.52854|Software identification information|20210322132143\n" +
            "UAC|1235|Password\n" +
            "EVN|A01|200911021022|200911021022|1|74357|20210319134735|MAYO^ST^DNS\n" +
            "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n" +
            "ARV|1|A34|77|Action restriction reason|Any special instructions for restriction|20210322^20210324\n" +
            "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|(555)222-3333|(555)444-5555|USA\n" +
            "NK1|1|Stanley^Jane^S^Jr^Mrs^BS^P|SPO^Text^CodeSystem|19 Raymond St^Route 3^Albany^NY^56321^USA^H|(002)912-8668^WPN^CP|(555)777-8888^WPN^PH|C^Text^SNM3|19980708||Nurse|C34|D345678|Ward|M|F|19790612153405|S|A0|USA|EN|F|F|N|N|COC|Flick|GERMAN|N|EMERGANCY|Jane Flick|1-888-777-9999|20 Golden Drive^Rochester^NY|Flick|O|2131-1|NO|111224444|Denver^CO\n" +
            "PV1|1|I|CARE POINT^5^1^Instate^3^C|R|0123||DR DANCE|DR MICKEY|DR DUCK|SUR|OR|777|R|1|A0|N|DR WHITE|111|8573245|2|3|4|1|7|20210322|30.00|8|9|10|20210322|11|200.00|100.00|12||01|HOME|13|MAYO|H|GOOD|ALBANY|ROCHESTER|200911011122|200911022114|12354.00|23456.00|123.00|156.00|D2345|A|GENERAL METHODIST\n" +
            "PV2|ROCHESTER|99|Surgery|Refferal|Car Keys|Locker 19|HO|20210318142726|20210323142728|5|6|Surgery|123|202001221|N|I|20210330|ES|Y|3|F|Y|Mayo|AI|2|20210322|G|20210322|20180322|77|88|Y|20210318143005|Y|Y|N|N|C|T|AC|O|S|F|F|DNR|20210322|20210330143113|20210315143120|O|20210322\n" +
            "ARV|2|A34|77|Action restriction reason|Any special instructions for restriction|20210322^20210324\n" +
            "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
            "DB1|1|IN|12345^text|Y|20110322|20210324|20210325|20210329\n" +
            "OBX|1|CWE|DQW^Text^SNM3|100|2|5^Text^SNM3|56-98|IND|25|A|F|20210322153839|LKJ|20210320153850|N56|Dr Watson|Manual^Text^SNM3|F84|20210322153925|Observation Site^Text^SNM3|Instance Identifier|M56|Radiology|467 Albany Hospital|Cardiology\n" +
            "AL1|1|DA|LUKE|MO|H34|20210322\n" +
            "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|Dr Orange|C|Y|20210322154326|V45|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\n" +
            "DRG|1|20210322154831|Y|DRG67|C|12|1478.96|C|987.32|Y|E|Coder^Name|Grouper^Status|PCCL^Status^Code|202|358.21|Status|Software^Name|2.3.1|Financial|surcharge|Basic|Total|Discount|12|M|43|5|N|Mode|7.21|54|Admission\n" +
            "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75|DR FISH|V46|80|DR WHITE|DR RED|32|1|D22|G45|1|G|P98|X|0|0\n" +
            "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
            "GT1|1|987654|Flat Stanley|Rebecca|19 Raymond ST|1-555-444-7777|1-555-999-8888|19700322143823|M|BANK|FTH|999557777|20210322143902|20210415143914|1|IBM|123 IBM Way|1-555-888-4444|456789|1|WH|N|7||N|654123|123987|4|456789|M|20210322144103|20210416144111|U|A0|USA|EN|F|O|Y|N|COC|Stanley|DUTCH|N|Spock Stanley|1-777-888-4444|EXPENSE|BRO|CEO|33|CDP|N|T|RICH|2106-3|London|YES\n" +
            "IN1|1|GLOBAL|7776664|Blue Cross Blue Shield|456 Blue Cross Lane|Maria Kirk|1-222-555-6666|UA34567|Blue|987123|IBM|20210101145034|20211231145045|Verbal|DELUXE|Jim Stanley|NON|19780429145224|19 Raymond St|M|CO|3|Y|20210101145300|N|20210102145320|N|Certificate here|20210322145350|Dr Disney|S|GOOD|200|12|B6543|H789456|1000.00|5000.00|17|210.00|520.00|1|M|123 IBM way|True|NONE|B|NO|J321456|M|20210322145605|London|YES\n" +
            "IN2|A23456|222001111|IBM 1345|EMPLOYEED|E|N23497234|R3294809|S234234|Army|U439823|SGT SCHULTZ|Army|Fort Wayne|USA|E1... E9|ACT|20300402145954|N|N|N|Yes|Grey Duck|Goose|T34941341232|D123123123|C435435345|2|SPR|2ANC|1234.00|O|A0|USA|EN|F|F|Y|N|COG|Stanley|DUTCH|N|M|20170322150208||Software Engineer|8|P|Mr Blue|1-555-333-4444|SURGERY|Jim Stanley|1-555-222-3333|FIRST|20170202150409|20210322150400|3|1-222-333-4444|GLOBAL|GROUP|B14456789|OTH|1-444-777-8888|1-444-555-3333|NONE|N|Y|N|GVH 123456|CDP 98765|2106-3|9\n" +
            "IN3|1|A249034203|John Doe|Y|PC|20210322151047|20210323151051|John Wayne|20210322151057|20210405151104|AP|CONCUR|20210322151126|DR GOOD|Dr Teeth|1-999-888-7777|NONE|GOOD VIEW HEALTH|1-777-888-9999|ER|Mrs Watson|20210318151253|DOUBLE|NO DOCUMENTATION RECEIVED|DR GREEN\n" +
            "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
            "ACC|20210310151401|F56|Albany|NY|N|N|Red Panda|Car|Wife|Y|19 Justice St\n" +
            "UB1|1|43|40|41|2|25|37|23|24|123.45 G65|90|44|87|20210322151657|20210330151702|28|33|20210322151710|20210407151719|Loc2|Loc9|Loc27|Loc45\n" +
            "UB2|1|9|24|7|8|4567.12 K90|F87 2021-03-22|X34|Locator2|Locator11|Locator31|A9384234H|Locator49|Locator56|Locator57|Locator78|4\n" +
            "PDA|None|NONE|NONE||None|N|NA|Dr Nobody|N\n";

  HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

  String json = ftv.convert(hl7Message, OPTIONS);
  System.out.println(json);
}
  @Test
  public void test_VXU_V04_message() {
    String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN|A01|20130617154644||01\r"
        + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
        + "PV1|1|B|yyy|E|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"

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
    List<Resource> patientResource = e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);
    List<Resource> messageHeader = e.stream()
        .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(messageHeader).hasSize(1);

    List<Resource> encounterResource = e.stream()
        .filter(v -> ResourceType.Encounter == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    List<Resource> obsResource = e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource = e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(1);

    List<Resource> organizationRes = e.stream()
        .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Patient organizations use a system urn:id reference for the organization, 
    // so we only expect the manufacturer to have an organization.    
    assertThat(organizationRes).hasSize(1);
  }

  @Test

  /*
   * This tests some of coding systems of interest or potential problems
   */
  public void testCodingSystems() throws FHIRException {
    String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|201305330||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN|A01|20130617154644||01\r"
        + "PID|1||12345678^^^MYEMR^MR||TestPatient^John|||M|\r"
        + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
        // Test MVX
        + "RXA|0|1|20130528|20130529|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
        // Test HL70162 & HL70163
        + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    String json = ftv.convert(hl7VUXmessageRep, OPTIONS);

    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
    assertThat(b.getId()).isNotNull();
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> obsResource = e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);

    Immunization immunization = (Immunization) obsResource.get(0);

    // Check that organization identifier (MVX) has a system
    Organization org = (Organization) immunization.getManufacturer().getResource();
    List<Identifier> li = org.getIdentifier();
    Identifier ident = li.get(0);
    assertThat(ident.hasSystem()).isTrue();
    assertThat(ident.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/MVX");
    assertThat(ident.hasValue()).isTrue();
    assertThat(ident.getValue()).isEqualTo("PMC");

    // Check that route (HL70162) has a system
    CodeableConcept route = immunization.getRoute();
    assertThat(route.hasCoding()).isTrue();
    List<Coding> codings = route.getCoding();
    assertThat(codings.size()).isEqualTo(2);
    Coding coding = codings.get(0);
    // If the first one is not the one we want look at the second one.
    if (coding.getCode().contains("C28161")){
      coding = codings.get(1);
    }
    assertThat(coding.hasSystem()).isTrue();
    assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0162");

    // Check that site (HL70163) has a system
    CodeableConcept site = immunization.getSite();
    coding = site.getCodingFirstRep();
    assertThat(coding.hasSystem()).isTrue();
    assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0163");
  }

  private void verifyResult(String json, BundleType expectedBundleType) {
    verifyResult(json, expectedBundleType, true);
  }

  private void verifyResult(String json, BundleType expectedBundleType, boolean messageHeaderExpected) {
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(expectedBundleType);
    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
        .filter(v -> ResourceType.Encounter == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    List<Resource> obsResource = e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource = e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(4);

    List<Resource> allergyResources = e.stream()
        .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(allergyResources).hasSize(2);

    if (messageHeaderExpected) {
      List<Resource> messageHeader = e.stream()
          .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
          .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(messageHeader).hasSize(1);
    }
  }

}
