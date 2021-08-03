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

    String joinedByHyphen = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
    String justField1 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|00000741^OXYCODONE||HYPOTENSION\r";
    String justField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "AL1|1|DA|OXYCODONE||HYPOTENSION\r";

    AllergyIntolerance joined = AllergyUtils.createAllergyFromHl7Segment(joinedByHyphen);

    Identifier values = joined.getIdentifier().get(0);
    String joinedValue = values.getValue();
    String system = values.getSystem();

    assertThat(joined.hasIdentifier()).isTrue();
    assertThat(joinedValue).isEqualTo("00000741-LN");
    assertThat(system).isEqualTo("urn:id:extID");

    AllergyIntolerance field1 = AllergyUtils.createAllergyFromHl7Segment(justField1);

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
    String system = values.getSystem();

    assertThat(noPrb4.hasIdentifier()).isTrue();
    assertThat(noPrb4Value).isEqualTo("331");
    assertThat(system).isNull();

    Condition prb4 = ConditionUtils.createConditionFromHl7Segment(withPRB4);

    Identifier identifier1 = prb4.getIdentifier().get(0);
    String identifier1Value = identifier1.getValue();
    String identifier1System = identifier1.getSystem();

    assertThat(noPrb4.hasIdentifier()).isTrue();
    assertThat(identifier1Value).isEqualTo("331");
    assertThat(identifier1System).isNull();

    Identifier identifier2 = prb4.getIdentifier().get(1);
    String identifier2Value = identifier2.getValue();
    String identifier2System = identifier2.getSystem();

    assertThat(prb4.hasIdentifier()).isTrue();
    assertThat(identifier2Value).isEqualTo("26744");
    assertThat(identifier2System).isEqualTo("urn:id:extID");

  }

  @Test
  public void observation_identifier_test() {

    String joinObx1AndObx3 =
                    "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|1||8503720^^^Assigning Authority||TESTTHREEM^TESTFOUR^^^^^L^^^20151218||19841106|M||R5|876 TESTING LANE^^ALBANY^NY^12209^USA^M^^01^^^20151218||^PRN^PH^^1^518^5555555^^PREF||ENG|S|NOV|500084595||||E2||N|0|||||N|N\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective|||1265664825^DURHAM FOWLER^JENNIFER^A^^^PSYD^^900004^L^^^NPI^^^^20121012~09314^^^^^^^^11^^^^PRN~019837^^^^^^^^13^^^^LN~11744^^^^^^^^900000^^^^DN~PSY000^^^^^^^^900002^^^^UPIN|||PSY|||N|9|||00602^CARAMORE^MARILYN^W^^^MD^^11^L^^^PRN^^^^20000101~1285694620^^^^^^^^900004^^^^NPI~145381^^^^^^^^13^^^^LN~188^^^^^^^^900000^^^^DN~B82457^^^^^^^^900002^^^^UPIN|IP|4388720^^^14^VN^^20151220|Self-pay|||||||||||||||||||1488|||||201512201007|||||||V\r" +
                    "PV2|||^TESTING STILL||||~~~~false||||||None||N||||||N||INPATIENT PSYCH E2 000783^L^28^^^900000^XX^^ ~^^1659307817^^^900004^NPI^^ ~^^000783^^^15^HPOGLMK^^ ~^^141338307^^^900001^TX^^ |Checked in|||||||N|||||N||AM||Private|||||||\r" +
                    "OBX|1|ST|DINnumber^^LSFUSERDATAE||N/A||||||R||||||\r";
    String joinObx1 =
            "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r" +
                    "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r" +
                    "PID|1||8503720^^^Assigning Authority||TESTTHREEM^TESTFOUR^^^^^L^^^20151218||19841106|M||R5|876 TESTING LANE^^ALBANY^NY^12209^USA^M^^01^^^20151218||^PRN^PH^^1^518^5555555^^PREF||ENG|S|NOV|500084595||||E2||N|0|||||N|N\r" +
                    "PV1|1|I|E2^E211^E211B^1488|Elective|||1265664825^DURHAM FOWLER^JENNIFER^A^^^PSYD^^900004^L^^^NPI^^^^20121012~09314^^^^^^^^11^^^^PRN~019837^^^^^^^^13^^^^LN~11744^^^^^^^^900000^^^^DN~PSY000^^^^^^^^900002^^^^UPIN|||PSY|||N|9|||00602^CARAMORE^MARILYN^W^^^MD^^11^L^^^PRN^^^^20000101~1285694620^^^^^^^^900004^^^^NPI~145381^^^^^^^^13^^^^LN~188^^^^^^^^900000^^^^DN~B82457^^^^^^^^900002^^^^UPIN|IP|4388720^^^14^VN^^20151220|Self-pay|||||||||||||||||||1488|||||201512201007|||||||V\r" +
                    "PV2|||^TESTING STILL||||~~~~false||||||None||N||||||N||INPATIENT PSYCH E2 000783^L^28^^^900000^XX^^ ~^^1659307817^^^900004^NPI^^ ~^^000783^^^15^HPOGLMK^^ ~^^141338307^^^900001^TX^^ |Checked in|||||||N|||||N||AM||Private|||||||\r" +
                    "OBX|1|ST|DINnumber^^||N/A||||||R||||||\r";



    Observation Obx1AndObx3 = ObservationUtils.createObservationFromHl7Segment(joinObx1AndObx3);

    Identifier values = Obx1AndObx3.getIdentifier().get(0);
    String Obx1AndObx3Value = values.getValue();
    String system = values.getSystem();

    assertThat(Obx1AndObx3.hasIdentifier()).isTrue();
    assertThat(Obx1AndObx3Value).isEqualTo("201512201012-DINnumber-LSFUSERDATAE");
    assertThat(system).isNull();

    Observation Obx1 = ObservationUtils.createObservationFromHl7Segment(joinObx1);

    Identifier identifier = Obx1.getIdentifier().get(0);
    String Obx1Value = identifier.getValue();
    String Obx1System = identifier.getSystem();

    assertThat(Obx1.hasIdentifier()).isTrue();
    assertThat(Obx1Value).isEqualTo("201512201012-DINnumber");
    assertThat(Obx1System).isEqualTo("urn:id:extID");

  }

  @Test
  public void diagnostic_report_identifier_test() {

    String diagnosticReport =
                    "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n" +
                    "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n" +
                    "PV1|1|O||R|||550542^Tsadok550542^Janetary|660542^Merrit660542^Darren^F|770542^Das770542^Surjya^P||||||A4||880542^Winter880542^Oscar^||66100542\n" +
                    "PV2|||Hold for Observation||Wedding Ring|X-5546||20170822051700|20170824051700|2 days|2 days|\n" +
                    "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                    "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n" +
                    "OBX|1|TX|||--------------------------------------------------Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity. Degree of uptake is most suggestive of recurrent tumor.   2.  No finding for additional hypermetabolic metastatic disease in the head/neck.   3.  There is moderately increased metabolic activity localizing to the post treatment area of the right upper lung including the second and third intercostal muscles and third rib. The degree of uptake is nonspecific and may be compatible with post radiation treatment changes. Also, there may be healing fractures of these ribs. Close attention on follow up CT chest is however recommended to assess for stability/resolution, and to exclude possible residual tumor.   4.  No finding for FDG metastasis in the remainder of the chest, abdomen, or pelvis.      --------------------------------------------------  Diagnostic  Interpretation:      POSITRON EMISSION TOMOGRAPHY (WITH NONCONTRAST CT), SKULL BASE TO MID THIGHS      CPT  CODE: [ALPHANUMERICID]       CLINICAL  INDICATION: Head and neck cancer and history of recurrence treated with radiosurgery. Also history of right lung cancer, treated with radiation. Last received [DATE]. Abnormality on CT scan.Subsequent treatment strategy.      RADIOPHARMACEUTICAL:  13.3 mCi 18F-fluorodeoxyglucose (FDG)      SERUM  GLUCOSE: 97 mg/dl      COMPARISON  PET/CT: [DATE]      CORRELATIVE  ANATOMIC IMAGING: CT [DATE]      TECHNIQUE:  Following i.v. administration of 18F-fluorodeoxyglucose (FDG), PET was performed spanning from the skull base to the mid thighs. CT was performed just prior to PET, after administration of oral contrast only, to provide attenuation correction and anatomic localization for the PET. CT images acquired with PET are not standard diagnostic CT, limited by lack of i.v. contrast, reduced mAs (x-rays), and slower imaging during  respiration (to improve fusion with PET images).      FINDINGS  -   PET/CT  HEAD/NECK: Physiologic activity is seen in the visualized portions of the brain.      Post  operative changes consistent with glossectomy, pharyngectomy and laryngectomy with bilateral neck dissection noted. In the area of enhancing nodule described on previous CT neck in the left oral cavity, there is markedly intense metabolic activity, head and neck series image 30. Maximum standard uptake value is 14.2. Finding is most compatible with tumor recurrence. There is increased metabolic activity anterolaterally along the adjacent mucosa.       There  is no hypermetabolic node metastasis seen in the neck. There is increased metabolic activity seen in the left masticator space, image 27. It is asymmetric but most likely muscle related.      PET/CT  CHEST: There is low to intermediate grade metabolic activity localizing to the irregular opacity seen at the right upper lung in the area of radiation treatment. Maximum standard uptake value is 2.9. No additional hypermetabolic lung nodules or masses identified. No FDG avid mediastinal or hilar lymph nodes. No effusions.      PET/CT  ABDOMEN/PELVIS: No abnormal FDG localization seen in the liver, spleen, pancreas, gallbladder, adrenal glands, or kidneys. Sutures seen along the stomach and upper abdominal wall. Physiologic activity is seen throughout the stomach, small bowel, and colon. No hypermetabolic abdominal or pelvic lymphadenopathy. Severe atherosclerotic disease of the aorta and iliac vessels, no gross aneurysmal dilatation. The bladder is moderately distended without gross abnormality. Prostate gland is not enlarged. No ascites.      PET/CT  OSSEOUS STRUCTURES: Increased metabolic activity localizing to the lateral right second/third intercostal space and third rib and adjacent pleural thickening. Maximum standard uptake value is 3.4. Subtle changes  suggestive of rib fractures seen on CT. The remainder of the visualized osseous structures do not show any convincing finding for metastatic bone disease.      ADDITIONAL  CT FINDINGS: Right maxillary sinus disease. Right infraorbital postsurgical change.      _      Dictated by: [PERSONALNAME], M.D., [PERSONALNAME]  on: [ALPHANUMERICID] 15:55  Finalized  on: [ALPHANUMERICID] [DATE]:[DATE]  Finalized  by: [PERSONALNAME], M.D., [PERSONALNAME]       ** [PERSONALNAME] **      ||||||F|||20170825010500\n" +
                    "NTE|1||Original Report Text Filename %%filename%%";



    DiagnosticReport report = DiagnosticReportUtils.createDiagnosticReportFromHl7Segment(diagnosticReport);

    Identifier identifier1 = report.getIdentifier().get(0);
    String value = identifier1.getValue();
    String system = identifier1.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(value).isEqualTo("FON001");
    assertThat(system).isNull();

    Identifier identifier2 = report.getIdentifier().get(1);
    String identifier2Value = identifier2.getValue();
    String identifier2System = identifier2.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(identifier2Value).isEqualTo("PON001");
    assertThat(identifier2System).isNull();

    Identifier identifier3 = report.getIdentifier().get(2);
    String identifier3Value = identifier3.getValue();
    String identifier3System = identifier3.getSystem();

    assertThat(report.hasIdentifier()).isTrue();
    assertThat(identifier3Value).isEqualTo("20170825010500"); //the MSH fix will require this to change. 20170825010500 is not MSH.7
    assertThat(identifier3System).isEqualTo("urn:id:extID");

  }
}