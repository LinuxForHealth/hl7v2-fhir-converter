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
//This identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (AL1.3) the three different messages test the three different outcomes
    String Field1andField3 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
    String Field1andField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "AL1|1|DA|00000741^OXYCODONE||HYPOTENSION\r";
    String justField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "AL1|1|DA|^OXYCODONE||HYPOTENSION\r";

   AllergyIntolerance joined = ResourceUtils.getAllergyResource(Field1andField3);

    Identifier values = joined.getIdentifier().get(0);
    assertThat(joined.getIdentifier()).hasSize(1);
    String joinedValue = values.getValue();
    String system = values.getSystem();

    assertThat(joined.hasIdentifier()).isTrue();
    assertThat(joinedValue).isEqualTo("00000741-LN");
    assertThat(system).isEqualTo("urn:id:extID");

    AllergyIntolerance field1 = ResourceUtils.getAllergyResource(Field1andField2);

    Identifier field1Values = field1.getIdentifier().get(0);
    assertThat(field1.getIdentifier()).hasSize(1);
    String field1Value = field1Values.getValue();
    String field1System = field1Values.getSystem();

    assertThat(field1.hasIdentifier()).isTrue();
    assertThat(field1Value).isEqualTo("00000741");
    assertThat(field1System).isEqualTo("urn:id:extID");

    AllergyIntolerance field2 = ResourceUtils.getAllergyResource(justField2);

    Identifier field2Values = field2.getIdentifier().get(0);
    assertThat(field2.getIdentifier()).hasSize(1);
    String field2Value = field2Values.getValue();
    String field2System = field2Values.getSystem();

    assertThat(field2.hasIdentifier()).isTrue();
    assertThat(field2Value).isEqualTo("OXYCODONE");
    assertThat(field2System).isEqualTo("urn:id:extID");

  }

  @Test
  public void condition_identifier_test() {
    //NOTE: the DG3 segment is still being worked on so we do not have a test case for this one but it  is similar to PRB4
    String withoutPRB4 =
            "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
            "PID|||10290^^^WEST^MR||KARLS^TOM^ANDREW^^MR.^||20040530|M|||||||||||398-44-5555|||||||||||N\n" +
            "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511^^^ACME|A|||||||||||||||||||SF|K||||20170215080000\n" +
            "PRB|AD|2004062916460000|596.5^BLADDER DYSFUNCTION^I9||||20040629||||||ACTIVE|||20040629";
    String withPRB4 =
            "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
            "PID|1||000054321^^^MRN||COOPER^SHELDON^ANDREW||19820512|M||2106-3|||||EN^English|M|CAT|78654||||N\n" +
            "PRB|AD|2004062916460000|596.5^BLADDER DYSFUNCTION^I9|26744|||20040629||||||ACTIVE|||20040629";

    Condition noPrb4 = ResourceUtils.getCondition(withoutPRB4);

    Identifier values = noPrb4.getIdentifier().get(0);
    String noPrb4Value = values.getValue();
    String noPrb4System= values.getSystem();

    assertThat(noPrb4.hasIdentifier()).isTrue();
    assertThat(noPrb4.getIdentifier()).hasSize(1);
    assertThat(noPrb4Value).isEqualTo("8846511"); //PV1.19.1
    assertThat(noPrb4System).isEqualTo("urn:id:ACME"); //PV1.19.4
    CodeableConcept noPrb4Type = values.getType();
    Coding noPrb4TypeValues = noPrb4Type.getCoding().get(0);
    assertThat(noPrb4TypeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(noPrb4TypeValues.getCode()).isEqualTo("VN");
    assertThat(noPrb4TypeValues.getDisplay()).isEqualTo("Visit number");

    Condition prb4 = ResourceUtils.getCondition(withPRB4);

    Identifier identifier1 = prb4.getIdentifier().get(0);
    String identifier1Value = identifier1.getValue();
    String identifier1System = identifier1.getSystem();

    assertThat(prb4.hasIdentifier()).isTrue();
    assertThat(prb4.getIdentifier()).hasSize(2);
    assertThat(identifier1Value).isEqualTo("78654"); //PID.18.1
    assertThat(identifier1System).isNull();
    CodeableConcept type = identifier1.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit number");

    Identifier identifier3 = prb4.getIdentifier().get(1);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(prb4.hasIdentifier()).isTrue();
    assertThat(identifier3Value).isEqualTo("26744");
    assertThat(identifier3System).isEqualTo("urn:id:extID");

  }

  @Test
  public void observation_identifier_test() {
    String joinFillPlaAndObx3 = // identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (OBX.3) and joins FILL or PLAC values with it
                    "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||Moose^Mickey^J^III^^^||||||||||||\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r" +
                    "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r" +
                    "OBX|1|ST|DINnumber^^LSFUSERDATAE||N/A||||||R||||||\r";
    String joinObx1AndObr2 = //This identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (OBX.3)
            "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||Moose^Mickey^J^III^^^||||||||||||\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r" +
                    "OBR|1|ORD448811^NIST EHR|^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r" +
                    "OBX|1|ST|DINnumber^^||N/A||||||R||||||\r";



    Observation Obx1AndObx3 = ResourceUtils.getObservation(joinFillPlaAndObx3);

    Identifier values = Obx1AndObx3.getIdentifier().get(0);
    String Obx1AndObx3Value = values.getValue();
    String system = values.getSystem();

    assertThat(Obx1AndObx3.hasIdentifier()).isTrue();
    assertThat(Obx1AndObx3Value).isEqualTo("R-511-DINnumber-LSFUSERDATAE"); //OBR.3.1-OBX.3.1-OBX.3.3
    assertThat(system).isEqualTo("urn:id:extID");

    Observation Obx1 = ResourceUtils.getObservation(joinObx1AndObr2);

    Identifier identifier = Obx1.getIdentifier().get(0);
    String Obx1Value = identifier.getValue();
    String Obx1System = identifier.getSystem();

    assertThat(Obx1.hasIdentifier()).isTrue();
    assertThat(Obx1Value).isEqualTo("ORD448811-DINnumber");//OBR.2.1-OBX.3.1
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
    DiagnosticReport report = ResourceUtils.getDiagnosticReport(diagnosticReport);

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
                    + "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000\r" ;

    String encounterW2Identifiers =
            "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                    + "EVN|A01|20130617154644||01\r"
                    + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                    + "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000||||||POL8009|\r" ;

    Encounter encounterResource = ResourceUtils.getEncounter(encounter);

    Identifier values = encounterResource.getIdentifier().get(0);
    String value = values.getValue();
    String system = values.getSystem();

    //Gets value from PV1.19.1
    assertThat(encounterResource.hasIdentifier()).isTrue();
    assertThat(encounterResource.getIdentifier()).hasSize(1);
    assertThat(value).isEqualTo("8846511");
    assertThat(system).isNull(); //PV1.19.4 and PID.18 is empty so system is null
    CodeableConcept type = values.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit number");

    Encounter encounters = ResourceUtils.getEncounter(encounterW2Identifiers); //the first identifier is the same. the second Identifier comes from PV1.50

    Identifier encounter1values = encounters.getIdentifier().get(0);
    String encounter1value = encounter1values.getValue();
    Identifier encounter2values = encounters.getIdentifier().get(1);
    String encounter2value = encounter2values.getValue();

    assertThat(encounters.hasIdentifier()).isTrue();
    assertThat(encounters.getIdentifier()).hasSize(2);

    assertThat(encounter1value).isEqualTo("8846511");
    CodeableConcept encounter1type = encounter1values.getType();
    Coding encounter1typeValues = encounter1type.getCoding().get(0);
    assertThat(encounter1typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(encounter1typeValues.getCode()).isEqualTo("VN");
    assertThat(encounter1typeValues.getDisplay()).isEqualTo("Visit number");

    assertThat(encounter2value).isEqualTo("POL8009"); //PV1.50
    CodeableConcept encounter2type = encounter2values.getType();
    Coding encounter2typeValues = encounter2type.getCoding().get(0);
    assertThat(encounter2typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(encounter2typeValues.getCode()).isEqualTo("VN");
    assertThat(encounter2typeValues.getDisplay()).isEqualTo("Visit number");
  }

  @Test
  public void Immunization_identifier_test() { //This identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (RXA.5) the three different messages test the three different outcomes
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

    Immunization immunizationF1F3 = ResourceUtils.getImmunization(field1AndField3);

    Identifier values = immunizationF1F3.getIdentifier().get(0);
    String value = values.getValue();
    String system = values.getSystem();

    assertThat(immunizationF1F3.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("48-CVX");
    assertThat(system).isEqualTo("urn:id:extID");

    Immunization immunizationF1F2 = ResourceUtils.getImmunization(field1AndField2);

    Identifier field1Values = immunizationF1F2.getIdentifier().get(0);
    String field1Value = field1Values.getValue();
    String field1System = field1Values.getSystem();

    assertThat(immunizationF1F2.hasIdentifier()).isTrue();
    assertThat(field1Value).isEqualTo("48");
    assertThat(field1System).isEqualTo("urn:id:extID");

    Immunization immunizationF2 = ResourceUtils.getImmunization(justField2);

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
             "PID|1||000054321^^^MRN||COOPER^SHELDON^ANDREW||19820512|M||2106-3|||||EN^English|M|CAT|78654^^^ACME||||N\n" +
             "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
             "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A||||||||||||78654||\n" +
             "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
             "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75|DR FISH|V46|80|DR WHITE|DR RED|32|1|D22|G45|1|G|P98|X|0|0\n";

    String procedureOBR =
            "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n" +
            "PID|1||000054321^^^MRN||COOPER^SHELDON^ANDREW||19820512|M||2106-3|||||EN^English|M|CAT|78654||||N\n" +
            "OBR|1|PON001|CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|||||||||||||7865||\n" +
            "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000\n" +
            "ROL|5897|UP|AD|Dr Disney|20210322133821|20210322133822|10||Hospital|ST|19 Raymond St^Route 3^Albany^NY|1-555-222-3333|1-555-444-5555|USA\n" +
            "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75|DR FISH|V46|80|DR WHITE|DR RED|32|1|D22|G45|1|G|P98|X|0|0\n";
    Procedure report = ResourceUtils.getProcedure(procedure);

    assertThat(report.getIdentifier()).hasSize(4);

    Identifier identifier1 = report.getIdentifier().get(0);
    String val = identifier1.getValue();
    String sys = identifier1.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(val).isEqualTo("78654"); //OBR.44.1
    assertThat(sys).isEqualTo("urn:id:extID");

    Identifier identifier2 = report.getIdentifier().get(1);
    String value = identifier2.getValue();
    String system = identifier2.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("78654"); //PID.18.1
    assertThat(system).isEqualTo("urn:id:ACME"); //PID.18.4
    CodeableConcept type = identifier2.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit number");

    Identifier identifier3 = report.getIdentifier().get(2);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(identifier3Value).isEqualTo("FON001"); //ORC.3.1
    assertThat(identifier3System).isNull();
    CodeableConcept types = identifier3.getType();
    Coding typevalues = types.getCoding().get(0);
    assertThat(typevalues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typevalues.getCode()).isEqualTo("FILL");
    assertThat(typevalues.getDisplay()).isEqualTo("Filler Identifier");

    Identifier identifier4 = report.getIdentifier().get(3);
    String identifier4Value = identifier4.getValue();
    String identifier4System = identifier4.getSystem();

    assertThat(identifier4Value).isEqualTo("PON001"); //ORC.2.1
    assertThat(identifier4System).isNull();
    CodeableConcept Type = identifier4.getType();
    Coding typeValue = Type.getCoding().get(0);
    assertThat(typeValue.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValue.getCode()).isEqualTo("PLAC");
    assertThat(typeValue.getDisplay()).isEqualTo("Placer Identifier");

    Procedure reportOBR = ResourceUtils.getProcedure(procedureOBR); // Instead of ORC values we use OBR

    assertThat(reportOBR.getIdentifier()).hasSize(4);

    Identifier identifier1OBR = reportOBR.getIdentifier().get(0);
    String valOBR = identifier1OBR.getValue();
    String sysOBR = identifier1OBR.getSystem();

    assertThat(reportOBR.hasIdentifier()).isTrue();
    assertThat(valOBR).isEqualTo("7865"); //OBR.45.1
    assertThat(sysOBR).isEqualTo("urn:id:extID");

    Identifier identifier2OBR = reportOBR.getIdentifier().get(1);
    String valueOBR = identifier2OBR.getValue();
    String systemOBR = identifier2OBR.getSystem();

    assertThat(reportOBR.hasIdentifier()).isTrue();
    assertThat(valueOBR).isEqualTo("8846511"); //PV1.19.1
    assertThat(systemOBR).isNull();
    CodeableConcept typeOBR = identifier2OBR.getType();
    Coding typeValuesOBR = typeOBR.getCoding().get(0);
    assertThat(typeValuesOBR.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValuesOBR.getCode()).isEqualTo("VN");
    assertThat(typeValuesOBR.getDisplay()).isEqualTo("Visit number");

    Identifier identifier3OBR = reportOBR.getIdentifier().get(2);
    String identifier3ValueOBR = identifier3OBR.getValue();
    String identifier3SystemOBR = identifier3OBR.getSystem();

    assertThat(identifier3ValueOBR).isEqualTo("CD_000000"); //OBR.3.1
    assertThat(identifier3SystemOBR).isNull();
    CodeableConcept typesOBR = identifier3OBR.getType();
    Coding typevaluesOBR = typesOBR.getCoding().get(0);
    assertThat(typevaluesOBR.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typevaluesOBR.getCode()).isEqualTo("FILL");
    assertThat(typevaluesOBR.getDisplay()).isEqualTo("Filler Identifier");

    Identifier identifier4OBR = reportOBR.getIdentifier().get(3);
    String identifier4ValueOBR = identifier4OBR.getValue();
    String identifier4SystemOBR = identifier4OBR.getSystem();

    assertThat(identifier4ValueOBR).isEqualTo("PON001"); //OBR.2.1
    assertThat(identifier4SystemOBR).isNull();
    CodeableConcept TypeOBR = identifier4OBR.getType();
    Coding typeValueOBR = TypeOBR.getCoding().get(0);
    assertThat(typeValueOBR.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValueOBR.getCode()).isEqualTo("PLAC");
    assertThat(typeValueOBR.getDisplay()).isEqualTo("Placer Identifier");

  }

//  @Test Test works, but message type is not configured yet
//  public void document_reference_identifier_test() {
//
//    String documentReference =
//            "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n" +
//            "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n" +
//            "TXA|1||B45678||||||\n" +
//            "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
//            "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n";
//
//    DocumentReference report = getResourceUtils.getDocumentReference(documentReference);
//
//    assertThat(report.getIdentifier()).hasSize(3);
//
//    Identifier identifier1 = report.getIdentifier().get(0);
//    String value = identifier1.getValue();
//    String system = identifier1.getSystem();
//
//    assertThat(report.hasIdentifier()).isTrue();
//    assertThat(value).isEqualTo("200911021022");
//    assertThat(system).isEqualTo("urn:id:extID");
//
//    Identifier identifier2 = report.getIdentifier().get(1);
//    String identifier2Value = identifier2.getValue();
//    String identifier2System = identifier2.getSystem();
//
//    assertThat(identifier2Value).isEqualTo("FON001");
//    assertThat(identifier2System).isNull();
//    CodeableConcept type = identifier2.getType();
//    Coding typeValues = type.getCoding().get(0);
//    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//    assertThat(typeValues.getCode()).isEqualTo("FILL");
//    assertThat(typeValues.getDisplay()).isEqualTo("Filler Identifier");
//
//    Identifier identifier3 = report.getIdentifier().get(2);
//    String identifier3Value = identifier3.getValue();
//    String identifier3System = identifier3.getSystem();
//
//    assertThat(identifier3Value).isEqualTo("PON001");
//    assertThat(identifier3System).isNull();
//    CodeableConcept Type = identifier3.getType();
//    Coding typeValue = Type.getCoding().get(0);
//    assertThat(typeValue.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//    assertThat(typeValue.getCode()).isEqualTo("PLAC");
//    assertThat(typeValue.getDisplay()).isEqualTo("Placer Identifier");
//
//  }

  @Test
  public void service_request_identifier_test() {
/// This test is an OMP message which does not have OBR segments Testing for MSH.7 as value
    String serviceRequest =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|OMP^O09^OMP_O09|1|P^I|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r" +
                    "PID|||555444222111^^^MPI&GenHosp^MR||smith^john||19600614|M||C|99 Oakland #106^^Toronto^ON^44889||||||||343132266|||N\r" +
                    "ORC|NW|1000^OE|9999999^RX|||E|40^QID^D10^^^R\r" +
                    "OBX|1|TX|^hunchback|1|Increasing||||||S\r" +
                    "NTE|1|P|comment after OBX\r";

    ServiceRequest report = ResourceUtils.getServiceRequest(serviceRequest);

    assertThat(report.getIdentifier()).hasSize(3);

    Identifier identifier1 = report.getIdentifier().get(0);
    String value = identifier1.getValue();
    String system = identifier1.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("200603081747"); //MSH.7
    assertThat(system).isNull();
    CodeableConcept type = identifier1.getType();
    Coding typeValues = type.getCoding().get(0);
    assertThat(typeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValues.getCode()).isEqualTo("VN");
    assertThat(typeValues.getDisplay()).isEqualTo("Visit number");

    Identifier identifier2 = report.getIdentifier().get(1);
    String identifier2Value = identifier2.getValue();
    String identifier2System = identifier2.getSystem();

    assertThat(identifier2Value).isEqualTo("9999999"); //ORC.3.1
    assertThat(identifier2System).isNull();
    CodeableConcept types = identifier2.getType();
    Coding typevalues = types.getCoding().get(0);
    assertThat(typevalues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typevalues.getCode()).isEqualTo("FILL");
    assertThat(typevalues.getDisplay()).isEqualTo("Filler Identifier");

    Identifier identifier3 = report.getIdentifier().get(2);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(identifier3Value).isEqualTo("1000"); //ORC.2.1
    assertThat(identifier3System).isNull();
    CodeableConcept Type = identifier3.getType();
    Coding typeValue = Type.getCoding().get(0);
    assertThat(typeValue.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(typeValue.getCode()).isEqualTo("PLAC");
    assertThat(typeValue.getDisplay()).isEqualTo("Placer Identifier");

  }

  @Test
  public void medicationRequest_identifier_test() {
    String medicationRequest =
            "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r" +
                    "PID|1||000054321^^^MRN||COOPER^SHELDON^||19820512|M|||765 SOMESTREET RD UNIT 3A^^PASADENA^LA^|||||S||78654|\r" +
                    "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S||A|||||||||||||||||||SF|K||||20170215080000\r" +
                    "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r" +
                    "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r" +
                    "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";

    MedicationRequest medReq = ResourceUtils.getMedicationRequest(medicationRequest);

    assertThat(medReq.hasIdentifier()).isTrue();
    assertThat(medReq.getIdentifier()).hasSize(2);

    Identifier identifier1 = medReq.getIdentifier().get(0);
    String medReqValue = identifier1.getValue();

    assertThat(medReq.hasIdentifier()).isTrue();
    assertThat(medReq.getIdentifier()).hasSize(2);
    assertThat(medReqValue).isEqualTo("78654"); //PID.18.1 no PV1.19.1 in this instance
    CodeableConcept medReqType = identifier1.getType();
    Coding medReqTypeValues = medReqType.getCoding().get(0);
    assertThat(medReqTypeValues.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
    assertThat(medReqTypeValues.getCode()).isEqualTo("VN");
    assertThat(medReqTypeValues.getDisplay()).isEqualTo("Visit number");

    //uses logic from BUILD_IDENTIFIER_FROM_CWE (RXO.1)
    Identifier identifier2 = medReq.getIdentifier().get(1);
    String medReqVal = identifier2.getValue();
    String medReqSystem = identifier2.getSystem();

    assertThat(medReqVal).isEqualTo("RX700001");
    assertThat(medReqSystem).isEqualTo("urn:id:extID");
  }

}