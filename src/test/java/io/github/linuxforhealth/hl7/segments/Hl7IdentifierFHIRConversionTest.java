/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

import io.github.linuxforhealth.hl7.segments.util.*;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

public class Hl7IdentifierFHIRConversionTest {

   @Test
  public void patient_identifiers_test() {

    String patientIdentifiers =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    // Three ID's for testing, plus a SSN in field 19.
    + "PID|1||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||||||||444556666|D-12445889-Z||||||||||\n"
    ;

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiers);
    assertThat(patient.hasIdentifier()).isTrue();

    List<Identifier> identifiers = patient.getIdentifier();
    assertThat(identifiers.size()).isEqualTo(5);

    // First identifier (Medical Record) deep check
    Identifier identifier = identifiers.get(0);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:ID-XYZ");
    assertThat(identifier.getValue()).hasToString("MRN12345678");
    assertThat(identifier.hasType()).isTrue(); 
    CodeableConcept cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 
    Coding coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("MR");
    assertThat(coding.getDisplay()).hasToString("Medical record number");

    // Second identifier (SSN) medium check
    identifier = identifiers.get(1);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:USA");
    assertThat(identifier.getValue()).hasToString("111223333");
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 

    // Third identifier (Driver's license) medium check
    identifier = identifiers.get(2);
    assertThat(identifier.hasSystem()).isTrue();
    assertThat(identifier.getSystem()).hasToString("urn:id:MNDOT");
    assertThat(identifier.getValue()).hasToString("MN1234567");
    assertThat(identifier.hasType()).isTrue();
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();  

    // Deep check for fourth identifier, which is assembled from PID.19 SSN
    identifier = identifiers.get(3);
    assertThat(identifier.hasSystem()).isFalse();
    // PID.19 SSN has no authority value and therefore no system id
    // Using different SS than ID#2 to confirm coming from PID.19
    assertThat(identifier.getValue()).hasToString("444556666"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("SS");
    assertThat(coding.getDisplay()).hasToString("Social Security number");
    
    // Deep check for fifth identifier, which is assembled from PID.20 DL
    identifier = identifiers.get(4);
    assertThat(identifier.hasSystem()).isFalse();
    // PID.20 has no authority value and therefore no system id
    // Using different DL than ID#2 to confirm coming from PID.20
    assertThat(identifier.getValue()).hasToString("D-12445889-Z"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("DL");
    assertThat(coding.getDisplay()).hasToString("Driver's license number");    

  }

  @Test
  public void patient_identifiers_special_cases_test() {

    String patientIdentifiersSpecialCases =
    "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
    // First ID has blanks in the authority.  Second ID has no authority provided.
    + "PID|1||MRN12345678^^^Regional Health ID^MR~111223333^^^^SS|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n"
    ;

    Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiersSpecialCases);
    assertThat(patient.hasIdentifier()).isTrue();
    List<Identifier> identifiers = patient.getIdentifier();
    assertThat(identifiers.size()).isEqualTo(2);

    // First identifier (Medical Record) deep check
    Identifier identifier = identifiers.get(0);
    assertThat(identifier.hasSystem()).isTrue();
    // Ensure system blanks are filled in with _'s
    assertThat(identifier.getSystem()).hasToString("urn:id:Regional_Health_ID"); 
    assertThat(identifier.getValue()).hasToString("MRN12345678");
    assertThat(identifier.hasType()).isTrue(); 
    CodeableConcept cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue(); 
    Coding coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("MR");
    assertThat(coding.getDisplay()).hasToString("Medical record number");

    // Deep check for second identifier
    identifier = identifiers.get(1);
    // We expect no system because there was no authority.
    assertThat(identifier.hasSystem()).isFalse();
    assertThat(identifier.getValue()).hasToString("111223333"); 
    assertThat(identifier.hasType()).isTrue(); 
    cc = identifier.getType();
    assertThat(cc.hasText()).isFalse();
    assertThat(cc.hasCoding()).isTrue();
    coding = cc.getCodingFirstRep();
    assertThat(coding.getSystem()).hasToString("http://terminology.hl7.org/CodeSystem/v2-0203"); 
    assertThat(coding.getCode()).hasToString("SS");
    assertThat(coding.getDisplay()).hasToString("Social Security number");

  }

  @Test
  public void allergy_identifier_test() {

    String Field1andField3 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
    String Field1andField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE||HYPOTENSION\r";
    String justField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|^OXYCODONE||HYPOTENSION\r";

    AllergyIntolerance joined = AllergyUtils.createAllergyFromHl7Segment(Field1andField3);

    Identifier values = joined.getIdentifier().get(0);
    String joinedValue = values.getValue();
    String system = values.getSystem();

    assertThat(joined.hasIdentifier()).isTrue();
    assertThat(joinedValue).isEqualTo("00000741-LN");
    assertThat(system).isEqualTo("urn:id:extID");

    AllergyIntolerance field1 = AllergyUtils.createAllergyFromHl7Segment(Field1andField2);

    Identifier field1Values = field1.getIdentifier().get(0);
    String field1Value = field1Values.getValue();
    String field1System = field1Values.getSystem();

    assertThat(field1.hasIdentifier()).isTrue();
    assertThat(field1Value).isEqualTo("00000741");
    assertThat(field1System).isEqualTo("urn:id:extID");

    AllergyIntolerance field2 = AllergyUtils.createAllergyFromHl7Segment(justField2);

    Identifier field2Values = field2.getIdentifier().get(0);
    String field2Value = field2Values.getValue();
    String field2System = field2Values.getSystem();

    assertThat(field2.hasIdentifier()).isTrue();
    assertThat(field2Value).isEqualTo("OXYCODONE");
    assertThat(field2System).isEqualTo("urn:id:extID");

  }

  @Test
  public void condition_identifier_test() {
    //NOTE: the DG1 segment is still being worked on so we do not have a test case for this one but it  is similar to PRB4
    String withoutPRB4 =
            "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
            "PID|||10290^^^WEST^MR||KARLS^TOM^ANDREW^^MR.^||20040530|M|||||||||||398-44-5555|||||||||||N\n" +
            "PRB|AD|2004062916460000|596.5^BLADDER DYSFUNCTION^I9||||20040629||||||ACTIVE|||20040629";
    String withPRB4 =
            "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
            "PID|||10290^^^WEST^MR||KARLS^TOM^ANDREW^^MR.^||20040530|M|||||||||||398-44-5555|||||||||||N\n" +
            "PRB|AD|2004062916460000|596.5^BLADDER DYSFUNCTION^I9|26744|||20040629||||||ACTIVE|||20040629";

    Condition noPrb4 = ConditionUtils.createConditionFromHl7Segment(withoutPRB4);

    Identifier values = noPrb4.getIdentifier().get(0);
    String noPrb4Value = values.getValue();

    assertThat(noPrb4.hasIdentifier()).isTrue();
    assertThat(noPrb4.getIdentifier()).hasSize(1);
    assertThat(noPrb4Value).isEqualTo("331");
    CodeableConcept noPrb4Type = values.getType();
    Coding noPrb4TypeValues = noPrb4Type.getCoding().get(0);
    assertThat(noPrb4TypeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(noPrb4TypeValues.getCode()).isEqualTo("VN");
    assertThat(noPrb4TypeValues.getDisplay()).isEqualTo("Visit Number");

    Condition prb4 = ConditionUtils.createConditionFromHl7Segment(withPRB4);

    Identifier identifier1 = prb4.getIdentifier().get(0);
    String identifier1Value = identifier1.getValue();
    String identifier1System = identifier1.getSystem();

    assertThat(prb4.hasIdentifier()).isTrue();
    assertThat(prb4.getIdentifier()).hasSize(2);
    assertThat(identifier1Value).isEqualTo("331");
    assertThat(identifier1System).isNull();
    CodeableConcept type = identifier1.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit Number");

    Identifier identifier3 = prb4.getIdentifier().get(1);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(prb4.hasIdentifier()).isTrue();
    assertThat(identifier3Value).isEqualTo("26744");
    assertThat(identifier3System).isEqualTo("urn:id:extID");

  }

  @Test
  public void observation_identifier_test() {
    String joinFillPlaAndObx3 =
                    "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||Moose^Mickey^J^III^^^||||||||||||\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r" +
                    "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r" +
                    "OBX|1|ST|DINnumber^^LSFUSERDATAE||N/A||||||R||||||\r";
    String joinObx1 =
            "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||Moose^Mickey^J^III^^^||||||||||||\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r" +
                    "OBR|1|ORD448811^NIST EHR|^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r" +
                    "OBX|1|ST|DINnumber^^||N/A||||||R||||||\r";



    Observation Obx1AndObx3 = ObservationUtils.createObservationFromHl7Segment(joinFillPlaAndObx3);

    Identifier values = Obx1AndObx3.getIdentifier().get(0);
    String Obx1AndObx3Value = values.getValue();
    String system = values.getSystem();

    assertThat(Obx1AndObx3.hasIdentifier()).isTrue();
    assertThat(Obx1AndObx3Value).isEqualTo("R-511-DINnumber-LSFUSERDATAE");
    assertThat(system).isEqualTo("urn:id:extID");

    Observation Obx1 = ObservationUtils.createObservationFromHl7Segment(joinObx1);

    Identifier identifier = Obx1.getIdentifier().get(0);
    String Obx1Value = identifier.getValue();
    String Obx1System = identifier.getSystem();

    assertThat(Obx1.hasIdentifier()).isTrue();
    assertThat(Obx1Value).isEqualTo("ORD448811-DINnumber");
    assertThat(Obx1System).isEqualTo("urn:id:extID");

  }

  @Test
  public void diagnostic_report_identifier_test() {

    String diagnosticReport =
                    "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n" +
                    "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n" +
                    "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                    "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n" +
                    "OBX|1|TX|||--------------------------------------------------Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity. Degree of uptake is most suggestive of recurrent tumor.||||||F|||20170825010500\n";
    DiagnosticReport report = DiagnosticReportUtils.createDiagnosticReportFromHl7Segment(diagnosticReport);

    assertThat(report.getIdentifier()).hasSize(3);

    Identifier identifier1 = report.getIdentifier().get(0);
    String value = identifier1.getValue();
    String system = identifier1.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("20170825010500");
    assertThat(system).isEqualTo("urn:id:extID");

    Identifier identifier2 = report.getIdentifier().get(1);
    String identifier2Value = identifier2.getValue();
    String identifier2System = identifier2.getSystem();

    assertThat(identifier2Value).isEqualTo("FON001");
    assertThat(identifier2System).isNull();
    CodeableConcept type = identifier2.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("FILL");
    assertThat(typeValues.getDisplay()).isEqualTo("Filler Identifier");

    Identifier identifier3 = report.getIdentifier().get(2);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(identifier3Value).isEqualTo("PON001");
    assertThat(identifier3System).isNull();
    CodeableConcept Type = identifier3.getType();
    Coding typeValue = Type.getCoding().get(0);
    assertThat(typeValue.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValue.getCode()).isEqualTo("PLAC");
    assertThat(typeValue.getDisplay()).isEqualTo("Placer Identifier");

  }

  @Test
  public void encounter_identifier_test() {
    String encounter =
            "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
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


    Encounter encounterResource = EncounterUtils.createEncounterFromHl7Segment(encounter);

    Identifier values = encounterResource.getIdentifier().get(0);
    String value = values.getValue();

    assertThat(encounterResource.hasIdentifier()).isTrue();
    assertThat(encounterResource.getIdentifier()).hasSize(1);
    assertThat(value).isEqualTo("48390");
    CodeableConcept type = values.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit Number");
  }

  @Test
  public void Immunization_identifier_test() {
    String field1AndField3 =
            "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                    + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                    + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                    + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r";
    String field1AndField2 =
            "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                    + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                    + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                    + "RXA|0|1|20130531|20130531|48^HIB PRP-T|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r";
    String justField2 =
            "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                    + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                    + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                    + "RXA|0|1|20130531|20130531|^HIB PRP-T|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r";

    Immunization immunizationF1F3 = ImmunizationUtils.createImmunizationFromHl7Segment(field1AndField3);

    Identifier values = immunizationF1F3.getIdentifier().get(0);
    String value = values.getValue();
    String system = values.getSystem();

    assertThat(immunizationF1F3.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("48-CVX");
    assertThat(system).isEqualTo("urn:id:extID");

    Immunization immunizationF1F2 = ImmunizationUtils.createImmunizationFromHl7Segment(field1AndField2);

    Identifier field1Values = immunizationF1F2.getIdentifier().get(0);
    String field1Value = field1Values.getValue();
    String field1System = field1Values.getSystem();

    assertThat(immunizationF1F2.hasIdentifier()).isTrue();
    assertThat(field1Value).isEqualTo("48");
    assertThat(field1System).isEqualTo("urn:id:extID");

    Immunization immunizationF2 = ImmunizationUtils.createImmunizationFromHl7Segment(justField2);

    Identifier field2Values = immunizationF2.getIdentifier().get(0);
    String field2Value = field2Values.getValue();
    String field2System = field2Values.getSystem();

    assertThat(immunizationF2.hasIdentifier()).isTrue();
    assertThat(field2Value).isEqualTo("HIB PRP-T");
    assertThat(field2System).isEqualTo("urn:id:extID");
  }

  @Test
  public void Procedure_identifier_test() {

    String procedure =
            "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n" +
             "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n" +
             "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
             "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n" +
             "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
             "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75|DR FISH|V46|80|DR WHITE|DR RED|32|1|D22|G45|1|G|P98|X|0|0\n";
    Procedure report = ProcedureUtils.createProcedureFromHl7Segment(procedure);

    assertThat(report.getIdentifier()).hasSize(4);

    Identifier identifier1 = report.getIdentifier().get(0);
    String val = identifier1.getValue();
    String sys = identifier1.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(val).isEqualTo("200911021022");
    assertThat(sys).isEqualTo("urn:id:extID");

    Identifier identifier2 = report.getIdentifier().get(1);
    String value = identifier2.getValue();
    String system = identifier2.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("200911021022");
    assertThat(system).isNull();
    CodeableConcept type = identifier2.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit Number");

    Identifier identifier3 = report.getIdentifier().get(2);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(identifier3Value).isEqualTo("FON001");
    assertThat(identifier3System).isNull();
    CodeableConcept types = identifier3.getType();
    Coding typevalues = types.getCoding().get(0);
    assertThat(typevalues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typevalues.getCode()).isEqualTo("FILL");
    assertThat(typevalues.getDisplay()).isEqualTo("Filler Identifier");

    Identifier identifier4 = report.getIdentifier().get(3);
    String identifier4Value = identifier4.getValue();
    String identifier4System = identifier4.getSystem();

    assertThat(identifier4Value).isEqualTo("PON001");
    assertThat(identifier4System).isNull();
    CodeableConcept Type = identifier4.getType();
    Coding typeValue = Type.getCoding().get(0);
    assertThat(typeValue.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValue.getCode()).isEqualTo("PLAC");
    assertThat(typeValue.getDisplay()).isEqualTo("Placer Identifier");

  }

}