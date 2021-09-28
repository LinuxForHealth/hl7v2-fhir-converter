/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class IssueFixTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS =
      new Builder().withValidateResource().withPrettyPrint().build();

  @Test
  public void oru_issue_81() {
    String hl7ORU =
        "MSH|^~\\&|Epic|ATRIUS|||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.3.1|||||||||PHLabReport-Ack^^2.16.840.1.114222.4.10.3^ISO||\n"
            + "SFT|Epic Systems Corporation^L^^^^ANSI&1.2.840&ISO^XX^^^1.2.840.114350|Epic 2015 |Bridges|8.2.2.0||20160605043244\n"
            + "PID|1|788840^^^^HVMA|788840||OLIVER^BETTY^J^^^||19481112|F||BLACK|75 MARK TERRACE^^RANDOLPH^MA^02368^USA^^^NORFOLK||(781)767-1274^^7^^^781^7671274~^NET^Internet^bettyo10@verizon.net|||||||||BLACK||||||||N|||20130320045252|1|\n"
            + "NTE|1|O|BHDNK75 4-14-15.|\n" + "NTE|2|O|BWH# 074-08-20-6       |\n"
            + "PD1|||BRAINTREE-ATRIUS HEALTH^^06|1457352338^TEITLEMAN^CHRISTOPHER^A MD^^^^^^^^^NPISER||||||||||||||\n"
            + "NK1|1|TILLMAN^FRANCES^^|SISTER||(617)436-7319^^7^^^617^4367319|||||||||||||||||||||||||||\n"
            + "NK1|2|EDWARDS^SOYINI^^|DAUGHTER||(213)270-3770^^7^^^213^2703770|||||||||||||||||||||||||||\n"
            + "PV1|1|I||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
            + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker||||||||1457352338^TEITLEMAN^CHRISTOPHER^A MD^^^^^^^^^NPISER||(781)849-2400^^^^^781^8492400|||||||ATRIUS HEALTH, INC^D^^^^POCFULL^XX^^^1020|P.O. BOX 415432^^BOSTON^MA^02241-5432^^B|898-7980^^8^^^800^8987980|111 GROSSMAN DRIVE^^BRAINTREE^MA^02184^USA^C^^NORFOLK|||||||\n"
            + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||20180924152700||||NORM||E11.9^Type 2 diabetes mellitus without complications^ICD-10-CM^^^^^^Type 2 diabetes mellitus without complications|||1457352338^TEITLEMAN^CHRISTOPHER^A MD^^^^^^^^^NPISER|(781)849-2400^^^^^781^8492400|||||20180924152900|||F|||||||&Roache&Gerard&&||||||||||||||||||\n"
            + "TQ1|1||||||20180924152721|20180924235959|R\n"
            + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F|||20180924152700||9548^ROACHE^GERARD^^|||20180924152903||||HVMA DEPARTMENT OF PATHOLOGY AND LAB MEDICINE^D|152 SECOND AVE^^NEEDHAM^MA^02494-2809^^B|\n"
            + "NTE|1|L||\n" + "NTE|2|L|Non-Diabetic Reference range <6.0%|\n" + "NTE|3|L||\n"
            + "NTE|4|L|The American Diabetes Association recommends that the |\n"
            + "NTE|5|L|goal of therapy should be a hemoglobin A1C of <7.0 % |\n"
            + "NTE|6|L|and that physicians should reevaluate the treatment |\n"
            + "NTE|7|L|regimen in patients with hemoglobin A1C      |\n"
            + "NTE|8|L|values consistently >8.0 %|\n"
            + "OBX|2|NM|17853^MEAN BLOOD GLUCOSE^LRR^^^^^^MEAN BLOOD GLUCOSE||114.02|mg/dL|||||F|||20180924152700||9548^ROACHE^GERARD^^|||20180924152903||||HVMA DEPARTMENT OF PATHOLOGY AND LAB MEDICINE^D|152 SECOND AVE^^NEEDHAM^MA^02494-2809^^B|\n"
            + "NTE|1|L||\n" + "NTE|2|L|Estimated Average Glucose |\n" + "NTE|3|L||\n"
            + "NTE|4|L|A1C(%)   mg/dl   (95% CI)|\n" + "NTE|5|L|5         97     (76-120) |\n"
            + "NTE|6|L|6         126    (100-152) |\n" + "NTE|7|L|7         154    (123-185) |\n"
            + "NTE|8|L|8         183    (147-217) |\n" + "NTE|9|L|9         212    (170-249) |\n"
            + "NTE|10|L|10        240    (193-282) |\n" + "NTE|11|L|11        269    (217-314) |\n"
            + "NTE|12|L|12        298    (240-347)  |\n" + "NTE|13|L||\n"
            + "NTE|14|L|Based on the ADAG formula: eAG = (28.7 X A1c) - 46.7|\n"
            + "NTE|15|L|with the 95% confidence interval as reported in:|\n"
            + "NTE|16|L|Nathan DM et al, Diabetes Care, 2008, 31(8);1476|\n"
            + "SPM|1|||^^^^^^^^Blood|||||||||||||20180924152700|20180924152755||||||";


    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7ORU, OPTIONS);

    assertThat(json).isNotBlank();
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;

    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> diagnosticReport =
        e.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReport).hasSize(1);
    DiagnosticReport diag = getDiagnosticReport(diagnosticReport.get(0));
    assertThat(diag.getIssued().toInstant().toString()).contains("2018-09-24T07:29:00Z");
  }


  /**
   * In order to generate messageHeader resource, MSH should have MSH.24.2 as this is required
   * attribute for source attribute, and source is required for MessageHeader resource.
   * 
   * @throws IOException
   */
  @Test
  public void message_header_issue_76() throws IOException {

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
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    Bundle b = (Bundle) bundleResource;

    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> messageHeader =
        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(messageHeader).isEmpty();
  }



  @Test
  public void vxu_issue_75() {
    String hl7vxu =
        "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
            + "PID|1||12345678^^^^MR||Mouse^Mickey^J^III^^^|cat^martha|20060504|M||2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053||^PRN^^^PH^555^5555555|||||||||2186-5^not Hispanic or Latino^CDCREC||N||||||N\n"
            + "PD1|||||||||||02|N|20170513|||A|20170513|20170513\n"
            + "NK1|1|cat^martha|MTH^Mother^HL70063|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|^PRN^PH^^^555^5555555\n"
            + "ORC|RE||IZ-783278^NDA||||||||||||||MIIC^MIIC clinic^HL70362\n"
            + "RXA|0|1|201501011|20150101|141^Influenza^CVX|1|mL||00^NEW IMMUNIZATIONRECORD^NIP001||^^^MIICSHORTCODE||||ABC1234|20211201|SKB^GlaxoSmithKline^MVX|||CP|A\n"
            + "OBX|4|CE|31044-1^reaction^LN|4|VXC12^fever of >40.5C within 48 hrs.^CDCPHINVS||||||F\n"
            + "ORC|RE||IZ-783280^NDA|||||||||||||||MIIC^MIIC clinic^HL70362\n"
            + "RXA|0|1|20170512||998^No vaccine given^CVX|999||||||||||||||CP|\n"
            + "OBX|1|CE|30945-0^contraindication^LN|1|M4^Medical exemption: Influenza^NIP||||||F|||20120916  \n"
    ;


    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7vxu, OPTIONS);

    assertThat(json).isNotBlank();
    FHIRContext context = new FHIRContext();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;

    assertThat(b.getId()).isNotNull();
    assertThat(b.getMeta().getLastUpdated()).isNotNull();

    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> immunization =
        e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Since "RXA|0|1|201501011| " RXA.3 has incorrect date, no immunization resource is generated
    // as occurrenceDateTime is required field and is extracted from RXA.3
    assertThat(immunization).hasSize(1);

  }

  private static DiagnosticReport getDiagnosticReport(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = DiagnosticReport.class;
    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }


  @Test
  public void name_issue_92() {
    String hl7message =
        "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
            + "PID|1||12345678^^^^MR||Mouse^Mickey^J^III^Mr^^|cat^martha|20060504|M||2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053||^PRN^^^PH^555^5555555|||||||||2186-5^not Hispanic or Latino^CDCREC||N||||||N\n"
    ;


    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);

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
    Patient patient = getPatientFromResource(patients.get(0));
    HumanName name = patient.getNameFirstRep();
    
    // Check prefix
    assertThat(name.hasPrefix()).isTrue();
    List<StringType> prefixes = name.getPrefix();
    assertThat(prefixes).hasSize(1);;
    String prefix = prefixes.get(0).getValueNotNull();
    assertThat(prefix).isEqualTo("Mr");

    // Check suffix
    assertThat(name.hasSuffix()).isTrue();
    List<StringType> suffixes = name.getSuffix();
    assertThat(suffixes).hasSize(1);;
    String suffix = suffixes.get(0).getValueNotNull();
    assertThat(suffix).isEqualTo("III");

  }

  private static Patient getPatientFromResource(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Patient.class;
    return (Patient) context.getParser().parseResource(klass, s);
  }

}
