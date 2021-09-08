/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.ServiceRequest;
//import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7IdentifierFHIRConversionTest {

    @Test
    public void patientIdentifiersTest() {
        String patientIdentifiers = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Three ID's for testing, plus a SSN in field 19.
                + "PID|1||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||||||||444556666|D-12445889-Z||||||||||\n";
        Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiers);

        // Expect 5 identifiers
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
    public void patientIdentifiersSpecialCasesTest() {
        String patientIdentifiersSpecialCases = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // First ID has blanks in the authority.  Second ID has no authority provided.
                + "PID|1||MRN12345678^^^Regional Health ID^MR~111223333^^^^SS|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n";
        Patient patient = PatientUtils.createPatientFromHl7Segment(patientIdentifiersSpecialCases);

        // Expect 2 identifiers
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
    public void allergyIdentifierTest() {
        // This identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (AL1.3) the three different messages test the three different outcomes

        // AL1-3.1 and AL1-3.3, concatenate together with a dash
        String Field1andField3 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
        AllergyIntolerance allergy = ResourceUtils.getAllergyResource(Field1andField3);

        // Expect a single identifier
        assertThat(allergy.hasIdentifier()).isTrue();
        assertThat(allergy.getIdentifier()).hasSize(1);

        // Identifier 1: extRef from AL1.3
        Identifier identifier = allergy.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("00000741-LN");
        assertThat(system).isEqualTo("urn:id:extID");

        // AL1-3.1 and AL1-3.2, use AL1-3.1
        String Field1andField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "AL1|1|DA|00000741^OXYCODONE||HYPOTENSION\r";
        allergy = ResourceUtils.getAllergyResource(Field1andField2);
        // Expect a single identifier
        assertThat(allergy.hasIdentifier()).isTrue();
        assertThat(allergy.getIdentifier()).hasSize(1);

        // Identifier 1: extRef from AL1.3
        identifier = allergy.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("00000741");
        assertThat(system).isEqualTo("urn:id:extID");

        // AL1-3.2 only
        String justField2 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "AL1|1|DA|^OXYCODONE||HYPOTENSION\r";
        allergy = ResourceUtils.getAllergyResource(justField2);

        // Expect a single identifier
        assertThat(allergy.hasIdentifier()).isTrue();
        assertThat(allergy.getIdentifier()).hasSize(1);

        // Identifier 1: extRef from AL1.3
        identifier = allergy.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("OXYCODONE");
        assertThat(system).isEqualTo("urn:id:extID");
    }

    @Test
    public void conditionPrbIdentifierTest() {
        // Test with PV1-19.1 for visit number, no PRB-4
        String withoutPRB4 = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S|8846511^^^ACME|A|||||||||||||||||||SF|K||||20170215080000\n"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10||E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";
        Condition condition = ResourceUtils.getCondition(withoutPRB4);

        // Expect a single identifier
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(1);

        // Identifier 1: Visit number
        Identifier identifier = condition.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1.19.1
        assertThat(system).isEqualTo("urn:id:ACME"); // PV1.19.4
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");
        // Test with no PV1-19, but with PID-18 for visit number; PRB-4.1 and PRB-4.3
        String withPRB4 = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
                "PID|1||000054321^^^MRN||||19820512|M||2106-3|||||EN^English|M|CAT|78654||||N\n" +
                "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";
        condition = ResourceUtils.getCondition(withPRB4);

        // Expect 2 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        identifier = condition.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

    }

    @Test
    public void conditionDg1IdentifierTest() {

        String withoutDG120 = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||88654||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\n"
                + "DG1|1|ICD10|C56.9^Ovarian Cancer^I10|Test|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326||S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        Condition condition = ResourceUtils.getCondition(withoutDG120);

        // Expect 2 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        Identifier identifier = condition.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("88654"); // PID.18.1
        assertThat(system).isNull(); // null because PV1.19.4 and PID18.4  are empty
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2:  extID based on DG1-3.1 + DG1-3.3
        identifier = condition.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("C56.9-I10"); // DG1.3.1-DG1.3.3
        assertThat(system).isEqualTo("urn:id:extID");

        // Test PV1-19 for visit number; extId with DG1-3.1.
        // Also test DG1-20 creates additional identifiers.
         String withDG120 = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                        + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                        + "PV1||I||||||||SUR||||||||S|8846511^^^ACME|A|||||||||||||||||||SF|K||||20170215080000\n"
                        + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|one^https://terminology.hl7.org/CodeSystem/two^three^https://terminology.hl7.org/CodeSystem/four|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        condition = ResourceUtils.getCondition(withDG120);

        // Expect 4 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(4);

        // Identifier 1: Visit number
        identifier = condition.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1.19.1
        assertThat(system).isEqualTo("urn:id:ACME"); // PV1.19.4
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: DG1.3.1
        identifier = condition.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("B45678"); // DG1.3.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 3: DG1.20.1 and DG1.20.2
        identifier = condition.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("one"); // DG1.20.1
        assertThat(system).isEqualTo("https://terminology.hl7.org/CodeSystem/two"); // DG1.20.2

        // Identifier 4: DG1.20.3 and DG1.20.4
        identifier = condition.getIdentifier().get(3);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("three"); // DG1.20.3
        assertThat(system).isEqualTo("https://terminology.hl7.org/CodeSystem/four"); // DG1.20.4

        String withDG132 = "MSH|^~\\&|||||201610015080000||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\n"
                + "DG1|1|ICD10|^Ovarian Cancer|Test|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326||S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        condition = ResourceUtils.getCondition(withDG132);

        // Expect 4 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        identifier = condition.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("201610015080000"); // MSH.7
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 3: DG1.3.2
        identifier = condition.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("Ovarian Cancer"); // DG1.3.2
        assertThat(system).isEqualTo("urn:id:extID");
    }

    @Test
    public void observationIdentifierTest() {
        // identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (OBX.3) and joins FILL or PLAC values with it

        // Filler from OBR-3; OBX-3.1/OBX-3.3
        String joinFillPlaAndObx3 = "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r"
                + "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r"
                + "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||||||||||||||\r"
                + "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r"
                + "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r"
                + "OBX|1|ST|DINnumber^^LSFUSERDATAE||N/A||||||R||||||\r";
        Observation observation = ResourceUtils.getObservation(joinFillPlaAndObx3);

        // Expect a single identifier
        assertThat(observation.hasIdentifier()).isTrue();
        assertThat(observation.getIdentifier()).hasSize(1);

        // Identifier 1: extID from OBR.3 plus OBX-3.1/OBX-3.3
        Identifier identifier = observation.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("R-511-DINnumber-LSFUSERDATAE"); // OBR.3.1-OBX.3.1-OBX.3.3
        assertThat(system).isEqualTo("urn:id:extID");

        // Placer from OBR-2; OBX-3.1
        String msg = "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r"
                + "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r"
                + "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||||||||||||||\r"
                + "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r"
                + "OBR|1|ORD448811^NIST EHR|^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r"
                + "OBX|1|ST|DINnumber^^||N/A||||||R||||||\r";
        observation = ResourceUtils.getObservation(msg);

        // Expect a single identifier
        assertThat(observation.hasIdentifier()).isTrue();
        assertThat(observation.getIdentifier()).hasSize(1);

        // Identifier 1: extID from OBR.2 and OBX-3.1
        identifier = observation.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("ORD448811-DINnumber"); // OBR.2.1-OBX.3.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Filler from ORC-3; OBX-3.2
        msg = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
              + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|||^^^^^626^5641111|^^^^^626^5647654||||||||N\r"
              + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
              + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
              + "OBX|1|TX|DINnumber^^LSFUSERDATAE||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
              + "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|";
        observation = ResourceUtils.getObservation(msg);

        // Expect a single identifier
        assertThat(observation.hasIdentifier()).isTrue();
        assertThat(observation.getIdentifier()).hasSize(1);

        // Identifier 1: extID from OBR.3 plus OBX-3.1/OBX-3.3
        identifier = observation.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("FON001-DINnumber-LSFUSERDATAE"); // ORC.3-OBX.3.1-OBX.3.3
        assertThat(system).isEqualTo("urn:id:extID");

        // Placer from ORC-2; OBX-3.1
        msg = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
              + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||||||||||N\r"
              + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A||SF|K||||199501102300\r"
              + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
              + "OBX|1|TX|DINnumber||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
              + "ORC|NW|PON001|||SC|D|1||20170825010500|MS|MS|||||";
        observation = ResourceUtils.getObservation(msg);

        // Expect a single identifier
        assertThat(observation.hasIdentifier()).isTrue();
        assertThat(observation.getIdentifier()).hasSize(1);

        // Identifier 1: extID from OBR.2 and OBX-3.1
        identifier = observation.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("PON001-DINnumber"); // ORC.2-OBX.3.1
        assertThat(system).isEqualTo("urn:id:extID");

    }

    @Test
    public void diagnosticReportIdentifierTest() {
        // Filler and placer from ORC
        String diagnosticReport = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|PON001^LE|FON001^OE PHIMS Stage|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n"
                + "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F\n"
                + "OBX|1|TX|||Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity.||||||F|||20170825010500\n";
        DiagnosticReport report = ResourceUtils.getDiagnosticReport(diagnosticReport);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);

        // Identifier 1: extId with MSH-7
        Identifier identifier = report.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20170825010500"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        identifier = report.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("FON001"); // ORC-3.1
        assertThat(system).isEqualTo("urn:id:OE-PHIMS-Stage"); // ORC-3.2 any whitespace gets replaced with Hyphens
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: Placer
        identifier = report.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();

        assertThat(value).isEqualTo("PON001"); // ORC-2.1
        assertThat(system).isEqualTo("urn:id:LE"); // ORC-2.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");

        // Filler and placer from OBR
        diagnosticReport = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n"
                + "OBR|1|CC_000000^OE PHIMS Stage|CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F\n"
                + "OBX|1|TX|||Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity.||||||F|||20170825010500\n";
        report = ResourceUtils.getDiagnosticReport(diagnosticReport);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);

        // Identifier 1: extId with MSH-7
        identifier = report.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("20170825010500"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        identifier = report.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD_000000"); // OBR-3.1
        assertThat(system).isNull(); // OBR-3.2 is empty
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: Placer
        identifier = report.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();

        assertThat(value).isEqualTo("CC_000000"); // OBR-2
        assertThat(system).isEqualTo("urn:id:OE-PHIMS-Stage"); // OBR-2.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
    }

    @Test
    public void encounterIdentifierTest() {
        // Test: Visit number from PV1-19
        String encounterMsg = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john|||||||||||||112233\r"
                + "PV1||I||||||||SUR||||||||S||A\r";
        Encounter encounter = ResourceUtils.getEncounter(encounterMsg);

        // Expect a single identifier
        assertThat(encounter.hasIdentifier()).isTrue();
        assertThat(encounter.getIdentifier()).hasSize(1);

        // Identifier 1: Visit number
        Identifier identifier = encounter.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("112233"); // PID.18
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Test: Visit number from PV1-19, plus PV1-50
        String encounterW2Identifiers = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john\r"
                + "PV1||I||||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000||||||POL8009|\r";
        encounter = ResourceUtils.getEncounter(encounterW2Identifiers); //the first identifier is the same. the second Identifier comes from PV1.50

        // Expect 2 identifiers
        assertThat(encounter.hasIdentifier()).isTrue();
        assertThat(encounter.getIdentifier()).hasSize(2);

        // Identifier 1: 
        identifier = encounter.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1-19
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: 
        identifier = encounter.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("POL8009"); // PV1.50
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Test: Visit number from MSH-7
        encounterMsg = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john\r"
                + "PV1||I||||||||SUR||||||||S||A\r";
        encounter = ResourceUtils.getEncounter(encounterMsg);

        // Expect a single identifier
        assertThat(encounter.hasIdentifier()).isTrue();
        assertThat(encounter.getIdentifier()).hasSize(1);

        // Identifier 1: Visit number
        identifier = encounter.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("20130531"); // MSH.7
        assertThat(system).isNull(); //PV1.19.4 and PID.18 is empty so system is null
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

    }

    @Test
    public void immunizationIdentifierTest() {
        // RXA-5.1 and RXA-5.3, concatenate together with a dash
        String field1AndField3 = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L\r"
                + "ORC|RE||197027|||||||||M|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX||||00^new immunization record^NIP001|||||||20131210||||CP|A\r";
        Immunization immunization = ResourceUtils.getImmunization(field1AndField3);

        // Expect a single identifier
        assertThat(immunization.hasIdentifier()).isTrue();
        assertThat(immunization.getIdentifier()).hasSize(1);

        // Identifier: extRef from RXA.5
        Identifier identifier = immunization.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("48-CVX");
        assertThat(system).isEqualTo("urn:id:extID");

        // RXA-5.1 and RXA-5.2, use RXA5.1
        String field1AndField2 = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L\r"
                + "ORC|RE||197027||||||||||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T||||00^new immunization record^NIP001|||||||20131210||||CP|A\r";
        immunization = ResourceUtils.getImmunization(field1AndField2);

        // Expect a single identifier
        assertThat(immunization.hasIdentifier()).isTrue();
        assertThat(immunization.getIdentifier()).hasSize(1);

        // Identifier: extRef from RXA.5
        identifier = immunization.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("48");
        assertThat(system).isEqualTo("urn:id:extID");

        // RXA-5.2 only
        String justField2 = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L\r"
                + "ORC|RE||197027||||||||||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|^HIB PRP-T||||00^new immunization record^NIP001|||||||20131210||||CP|A\r";
        immunization = ResourceUtils.getImmunization(justField2);
        // Expect a single identifier
        assertThat(immunization.hasIdentifier()).isTrue();
        assertThat(immunization.getIdentifier()).hasSize(1);

        // Identifier: extRef from RXA.5
        identifier = immunization.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("HIB PRP-T");
        assertThat(system).isEqualTo("urn:id:extID");

    }

    @Test
    public void procedureIdentifierTest() {
        // with PR1 and PID segments used to create identifiers
        String procedureMsg = "MSH|^~\\&|HL7Soup|Instance1|MCM||200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|78654^^^ACME||||N\n"
                + "ROL|5897|UP|AD||20210322133821|20210322133822|10||Hospital|ST||||USA\n"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\n";
        Procedure procedure = ResourceUtils.getProcedure(procedureMsg);

        // Expect 2 identifiers
        assertThat(procedure.hasIdentifier()).isTrue();
        assertThat(procedure.getIdentifier()).hasSize(2);

        // Identifier 1: PR1.19
        Identifier identifier = procedure.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("P98"); // PR1.19.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Visit number from PID-18
        identifier = procedure.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1
        assertThat(system).isEqualTo("urn:id:ACME"); // PID.18.4
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");


        // Test: MSH.7 and PV1.19
        String procedureMSH = "MSH|^~\\&||Instance1|MCM||200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "PV1||I|6N^1234^A^GENERAL HOSPITAL2|||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000\n"
                + "ROL|5897|UP|AD||20210322133821|20210322133822|10||Hospital|ST||||USA\n"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G||X|0|0\n";

        procedure = ResourceUtils.getProcedure(procedureMSH);

        // Expect 2 identifiers
        assertThat(procedure.hasIdentifier()).isTrue();
        assertThat(procedure.getIdentifier()).hasSize(2);

        // Identifier 1: PR1
        identifier = procedure.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("200911021022"); // MSH.7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Visit number from PV1-19
        identifier = procedure.getIdentifier().get(1);
        String valueOBR = identifier.getValue();
        system = identifier.getSystem();
        assertThat(valueOBR).isEqualTo("8846511"); // PV1.19.1
        assertThat(system).isNull(); // No System PV1.19.2 DNE
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");
    }

//      @Test //Test works, but message type is not configured yet
//      public void documentReferenceIdentifierTest() {
//          // Filler and placer from ORC, extId from MSH-7
//          String documentReference =
//                  "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
//                          "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
//                          "TXA|1||B45678||||||\n" +
//                          "ORC|NW|PON001^LE|FON001^OE|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
//                          "OBR|1||CD_000000^IE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n";
//          DocumentReference report = ResourceUtils.getDocumentReference(documentReference);
//
//          // Expect 3 identifiers
//          assertThat(report.hasIdentifier()).isTrue();
//          assertThat(report.getIdentifier()).hasSize(3);
//
//          // Identifier 1: extID from MSH-7
//          Identifier identifier = report.getIdentifier().get(0);
//          String value = identifier.getValue();
//          String system = identifier.getSystem();
//          assertThat(value).isEqualTo("200911021022");  // MSH-7
//          assertThat(system).isEqualTo("urn:id:extID");
//
//          // Identifier 2: Filler
//          identifier = report.getIdentifier().get(1);
//          value = identifier.getValue();
//          system = identifier.getSystem();
//          assertThat(value).isEqualTo("FON001"); //ORC.3.1
//          assertThat(system).isEqualTo("urn:id:OE"); // ORC.3.2
//          CodeableConcept type = identifier.getType();
//          Coding coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("FILL");
//          assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");
//
//          // Identifier 3: Placer
//          Identifier identifier3 = report.getIdentifier().get(2);
//          value = identifier3.getValue();
//          system = identifier3.getSystem();
//          assertThat(value).isEqualTo("PON001"); //ORC.2.1
//          assertThat(system).isEqualTo("urn:id:LE"); // ORC.2.2
//          type = identifier3.getType();
//          coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("PLAC");
//          assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
//
//          // Filler from OBR, placer from TXA-14
//          documentReference =
//                  "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
//                          "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
//                          "TXA|1||B45678|||||||||||PON001^IE||\n" +
//                          "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
//                          "OBR|1||CD_000000^OE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|";
//          report = ResourceUtils.getDocumentReference(documentReference);
//
//          // Expect 3 identifiers
//          assertThat(report.hasIdentifier()).isTrue();
//          assertThat(report.getIdentifier()).hasSize(3);
//
//          // Identifier 1: extID from MSH-7
//          identifier = report.getIdentifier().get(0);
//          value = identifier.getValue();
//          system = identifier.getSystem();
//          assertThat(value).isEqualTo("200911021022");  // MSH-7
//          assertThat(system).isEqualTo("urn:id:extID");
//
//          // Identifier 2: Filler
//          identifier = report.getIdentifier().get(1);
//          value = identifier.getValue();
//          system = identifier.getSystem();
//          assertThat(value).isEqualTo("CD_000000"); //OBR.3.1
//          assertThat(system).isEqualTo("urn:id:OE"); // OBR.3.2
//          type = identifier.getType();
//          coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("FILL");
//          assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");
//
//          // Identifier 3: Placer
//          identifier3 = report.getIdentifier().get(2);
//          value = identifier3.getValue();
//          system = identifier3.getSystem();
//          assertThat(value).isEqualTo("PON001"); // TXA-14.1
//          assertThat(system).isEqualTo("urn:id:IE"); // TXA.14.2
//          type = identifier3.getType();
//          coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("PLAC");
//          assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
//
//          // Placer from OBR, Filler from TXA-15
//          documentReference =
//                  "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
//                          "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
//                          "TXA|1||B45678||||||||||||PON001^IE\n" +
//                          "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
//                          "OBR|1|CD_000000^OE||2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n";
//          report = ResourceUtils.getDocumentReference(documentReference);
//
//          // Expect 3 identifiers
//          assertThat(report.hasIdentifier()).isTrue();
//          assertThat(report.getIdentifier()).hasSize(3);
//          // Identifier 1: extID from MSH-7
//          identifier = report.getIdentifier().get(0);
//          value = identifier.getValue();
//          system = identifier.getSystem();
//          assertThat(value).isEqualTo("200911021022");  // MSH-7
//          assertThat(system).isEqualTo("urn:id:extID");
//
//          // Identifier 2: Filler
//          identifier3 = report.getIdentifier().get(1);
//          value = identifier3.getValue();
//          system = identifier3.getSystem();
//          assertThat(value).isEqualTo("PON001"); // TXA-15.1
//          assertThat(system).isEqualTo("urn:id:IE"); // TXA.15.2
//          type = identifier3.getType();
//          coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("FILL");
//          assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");
//
//          // Identifier 3: Placer
//          identifier = report.getIdentifier().get(2);
//          value = identifier.getValue();
//          system = identifier.getSystem();
//          assertThat(value).isEqualTo("CD_000000"); //OBR.2.1
//          assertThat(system).isEqualTo("urn:id:OE"); // OBR.2.2
//          type = identifier.getType();
//          coding = type.getCoding().get(0);
//          assertThat(type.getText()).isNull();
//          assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
//          assertThat(coding.getCode()).isEqualTo("PLAC");
//          assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
//      }

    @Test
    public void serviceRequestIdentifierTest1() {
        //  - Visit number with MSH-7
        //  - filler and placer from ORC
        String serviceRequest = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|OMP^O09^OMP_O09|1|P^I|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|||12345^^^^MR||smith^john\r"
                + "ORC|NW|1000^OE|9999999^RX\r"
                + "OBR|1|2233|4455\r"
                + "OBX|1|TX|^hunchback|1|Increasing||||||S\r";
        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        // Identifier 1: visit number
        Identifier identifier = serviceReq.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("200603081747"); // MSH.7
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("9999999"); // ORC.3.1
        assertThat(system).isEqualTo("urn:id:RX"); // ORC.3.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("1000"); // ORC.2.1
        assertThat(system).isEqualTo("urn:id:OE"); // ORC.2.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");

        // Test 2: 
        //  - Visit number with PID-18
        //  - filler and placer from OBR
        serviceRequest = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n" +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|78654||||N\n" +
                "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n" +
                "ORC||||||E|^Q6H^D10^^^R\n" +
                "OBR|1|CD150920001336^OE|CD150920001336^IE|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||";

        serviceReq = ResourceUtils.getServiceRequest(serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        // Identifier 1: visit number
        identifier = serviceReq.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.3.1
        assertThat(system).isEqualTo("urn:id:IE"); // OBR.3.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.2.1
        assertThat(system).isEqualTo("urn:id:OE"); // OBR.2.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");

        // Test 3: 
        //  - Visit number with PV1-19
        //  - filler from OBR
        //  - placer from ORC
        serviceRequest = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n" +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                "PV1||I|6N^1234^A^GENERAL HOSPITAL2|||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000\n" +
                "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n" +
                "ORC||PON001||||E|^Q6H^D10^^^R\n" +
                "OBR|1||CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||";

        serviceReq = ResourceUtils.getServiceRequest(serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        // Identifier 1: visit number
        identifier = serviceReq.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1.19
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.3.1
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(2);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("PON001"); // ORC.2.1
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");

    }

    @Test
    public void medicationRequestIdentifierTest() {
        // Visit number from PID-18, extID from RXO-1.1
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN|||||||||||||||78654\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(medicationRequest);

        // Expect 2 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1, since no PV1.19.1 in this message
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: extID based on RXO-1.1
        identifier = medReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("RX700001"); // RXO-1.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Test: Visit number from PV1-19, extID from RXO-1.1 and RXO-1.3
        medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN|||||||||||||||78654\r"
                + "PV1||I|||||||||||||||||789789\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE^ABC|100||mg|||||G||10||5|\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        medReq = ResourceUtils.getMedicationRequest(medicationRequest);

        // Expect 2 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        identifier = medReq.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("789789"); // PV1.19
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: extID based on RXO-1.1 and RX-O1.3
        identifier = medReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("RX700001-ABC"); // RXO-1.1 and RXO-1.3
        assertThat(system).isEqualTo("urn:id:extID");

        // Test: Visit number from MSH-7, extID from RXO-2
        medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        medReq = ResourceUtils.getMedicationRequest(medicationRequest);

        // Expect 2 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        // Identifier 1: Visit number
        identifier = medReq.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("20170215080000"); // MSH-7
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2: extID based on RXO-1.2 
        identifier = medReq.getIdentifier().get(1);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("DOCUSATE SODIUM 100 MG CAPSULE"); // RXO1.2
        assertThat(system).isEqualTo("urn:id:extID");

        // Test: Visit number from MSH-7, no RXO-1
        medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|||||||||G||10||5|\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        medReq = ResourceUtils.getMedicationRequest(medicationRequest);

        // Expect 1 identifier
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(1);

        // Identifier 1: Visit number
        identifier = medReq.getIdentifier().get(0);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("20170215080000"); // MSH-7
        assertThat(system).isNull();
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");
    }

    @Test
    public void medicationAdministration_identifier_test() {
        // TODO
        // Currently no messages generate MedicationAdministration resources
    }

}