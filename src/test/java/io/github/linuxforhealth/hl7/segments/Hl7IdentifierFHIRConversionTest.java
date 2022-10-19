/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7IdentifierFHIRConversionTest {
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    // Suppress warnings about too many assertions in a test.  Justification: creating a FHIR message is costly; we need to check many asserts per creation for efficiency.  
    @java.lang.SuppressWarnings("squid:S5961")
    @Test
    void patientIdentifiersTest() {
        String patientIdentifiers = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Three ID's for testing, plus a SSN in field 19.  Both SSNs have dashes that should be removed
                + "PID|1||MRN12345678^^^ID-XYZ^MR~111-22-3333^^^USA^SS~MN34567^^^MNDOT^DL|ALTID|Moose^Mickey^J^III^^^||20060504|M|||||||||||444-55-6666|D-12445889-Z||||||||||\n";
        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientIdentifiers);

        // Expect 5 identifiers
        assertThat(patient.hasIdentifier()).isTrue();
        List<Identifier> identifiers = patient.getIdentifier();
        assertThat(identifiers.size()).isEqualTo(5);

        // Match the id's to position; we can't depend on an order.
        int posMR = getIdentifierPositionByValue("MRN12345678", identifiers);
        assertThat(posMR).isNotSameAs(-1);
        int posSSN = getIdentifierPositionByValue("111223333", identifiers);
        assertThat(posSSN).isNotSameAs(-1);
        int posDL = getIdentifierPositionByValue("MN34567", identifiers);
        assertThat(posDL).isNotSameAs(-1);
        int posSSNPID19 = getIdentifierPositionByValue("444556666", identifiers);
        assertThat(posSSNPID19).isNotSameAs(-1);
        int posDLPID20 = getIdentifierPositionByValue("D-12445889-Z", identifiers);
        assertThat(posDLPID20).isNotSameAs(-1);

        // First identifier (Medical Record) deep check
        Identifier identifier = identifiers.get(posMR);
        assertThat(identifier.hasSystem()).isTrue();
        assertThat(identifier.getSystem()).hasToString("urn:id:ID-XYZ");
        assertThat(identifier.getValue()).hasToString("MRN12345678");
        assertThat(identifier.hasType()).isTrue();
        CodeableConcept cc = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(cc, "MR", "Medical record number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Second identifier (SSN) medium check
        identifier = identifiers.get(posSSN);
        assertThat(identifier.hasSystem()).isTrue();
        assertThat(identifier.getSystem()).hasToString("urn:id:USA");
        assertThat(identifier.getValue()).hasToString("111223333");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        assertThat(cc.hasText()).isFalse();
        assertThat(cc.hasCoding()).isTrue();

        // Third identifier (Driver's license) medium check
        identifier = identifiers.get(posDL);
        assertThat(identifier.hasSystem()).isTrue();
        assertThat(identifier.getSystem()).hasToString("urn:id:MNDOT");
        assertThat(identifier.getValue()).hasToString("MN34567");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        assertThat(cc.hasText()).isFalse();
        assertThat(cc.hasCoding()).isTrue();

        // Deep check for fourth identifier, which is assembled from PID.19 SSN
        identifier = identifiers.get(posSSNPID19);
        assertThat(identifier.hasSystem()).isFalse();
        // PID.19 SSN has no authority value and therefore no system id
        // Using different SS than ID#2 to confirm coming from PID.19
        // Also tests that dashes are removed from SSN
        assertThat(identifier.getValue()).hasToString("444556666");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(cc, "SS", "Social Security number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Deep check for fifth identifier, which is assembled from PID.20 DL
        identifier = identifiers.get(posDLPID20);
        assertThat(identifier.hasSystem()).isFalse();
        // PID.20 has no authority value and therefore no system id
        // Using different DL than ID#2 to confirm coming from PID.20
        assertThat(identifier.getValue()).hasToString("D-12445889-Z");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(cc, "DL", "Driver's license number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

    }

    @Test
    void patientIdentifiersSpecialCasesTest() {
        String patientIdentifiersSpecialCases = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // First ID has blanks in the authority.  Second ID has no authority provided.  Third ID has an unknown CODE in the standard v2 table.
                // SSN has dashes which should be removed during processing.
                + "PID|1||MRN12345678^^^Regional Health ID^MR~111-22-3333^^^^SS~A100071402^^^^AnUnknownCode|ALTID|Moose^Mickey^J^III^^^||20060504|M||||||||||||||||||||||\n";
        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientIdentifiersSpecialCases);

        // Expect 2 identifiers
        assertThat(patient.hasIdentifier()).isTrue();
        List<Identifier> identifiers = patient.getIdentifier();
                assertThat(identifiers.size()).isEqualTo(3);

        // Match the id's to position; we can't depend on an order.
        int posMR = getIdentifierPositionByValue("MRN12345678", identifiers);
        assertThat(posMR).isNotSameAs(-1);
        int posSSN = getIdentifierPositionByValue("111223333", identifiers);
        assertThat(posSSN).isNotSameAs(-1);
        int posUNKNOWN = getIdentifierPositionByValue("A100071402", identifiers);
        assertThat(posUNKNOWN).isNotSameAs(-1);

        // First identifier (Medical Record) deep check
        Identifier identifier = identifiers.get(posMR);
        assertThat(identifier.hasSystem()).isTrue();
        // Ensure system blanks are filled in with _'s
        assertThat(identifier.getSystem()).hasToString("urn:id:Regional_Health_ID");
        assertThat(identifier.getValue()).hasToString("MRN12345678");
        assertThat(identifier.hasType()).isTrue();
        CodeableConcept cc = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(cc, "MR", "Medical record number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Second identifier
        identifier = identifiers.get(posSSN);
        // We expect no system because there was no authority.
        assertThat(identifier.hasSystem()).isFalse();
        assertThat(identifier.getValue()).hasToString("111223333");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(cc, "SS", "Social Security number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Third identifier - handles an unknown code.  Create the following:
        // "identifier": [ {
        //         "type": {
        //           "text": "AnUnknownCode"
        //         },
        //         "value": "A100071402"
        //       },
        identifier = identifiers.get(posUNKNOWN);
        assertThat(identifier.hasSystem()).isFalse();
        assertThat(identifier.getValue()).hasToString("A100071402");
        assertThat(identifier.hasType()).isTrue();
        cc = identifier.getType();
        assertThat(cc.hasText()).isTrue();
        assertThat(cc.getText()).hasToString("AnUnknownCode");
        assertThat(cc.hasCoding()).isFalse();
    }

    @Test
    void allergyIdentifierTest() {
        // This identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (AL1.3) the three different messages test the three different outcomes

        // AL1-3.1 and AL1-3.3, concatenate together with a dash
        String Field1andField3 = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "AL1|1|DA|00000741^OXYCODONE^LN||HYPOTENSION\r";
        AllergyIntolerance allergy = ResourceUtils.getAllergyResource(ftv, Field1andField3);

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
        allergy = ResourceUtils.getAllergyResource(ftv, Field1andField2);
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
        allergy = ResourceUtils.getAllergyResource(ftv, justField2);

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
    void conditionPrbIdentifierTest() {
        // Test with PV1-19.1 for visit number, no PRB-4
        String withoutPRB4 = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S|8846511^^^ACME|A|||||||||||||||||||SF|K||||20170215080000\n"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10||E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";
        Condition condition = ResourceUtils.getCondition(ftv, withoutPRB4);

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
        // Coding coding = type.getCoding().get(0);
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        String withPRB4 = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\n" +
                "PID|1||000054321^^^MRN||||19820512|M||2106-3|||||EN^English|M|CAT|78654||||N\n" +
                "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|remission^Remission^http://terminology.hl7.org/CodeSystem/condition-clinical|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";
        condition = ResourceUtils.getCondition(ftv, withPRB4);

        // Expect 2 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);

        List<Identifier> identifiers = condition.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("78654", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("53956", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        identifier = condition.getIdentifier().get(posVN);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2:  extID based on PRB.4
        identifier = condition.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("53956"); // DG1.3.1-DG1.3.3
        assertThat(system).isEqualTo("urn:id:extID");

    }

    @Test
    void conditionDg1IdentifierTest() {

        String withoutDG120 = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||88654||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\n"
                + "DG1|1|ICD10|C56.9^Ovarian Cancer^I10|Test|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326||S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        Condition condition = ResourceUtils.getCondition(ftv, withoutDG120);

        // Expect 2 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);

        List<Identifier> identifiers = condition.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("88654", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("C56.9-I10", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = condition.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("88654"); // PID.18.1
        assertThat(system).isNull(); // null because PV1.19.4 and PID18.4  are empty
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2:  extID based on DG1-3.1 + DG1-3.3
        identifier = condition.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("C56.9-I10"); // DG1.3.1-DG1.3.3
        assertThat(system).isEqualTo("urn:id:extID");
    }

    @Test
    void conditionDg1IdentifierTest2() {
        // Test PV1-19 for visit number; extID with DG1-3.1.
        // Also test DG1-20 creates additional identifiers.
        String withDG120 = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S|8846511^^^ACME|A|||||||||||||||||||SF|K||||20170215080000\n"
                + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|one^https://terminology.hl7.org/CodeSystem/two^three^https://terminology.hl7.org/CodeSystem/four|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        Condition condition = ResourceUtils.getCondition(ftv, withDG120);

        // Expect 4 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(4);

        List<Identifier> identifiers = condition.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("8846511", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("B45678", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posExtDG20a = getIdentifierPositionByValue("one", identifiers);
        assertThat(posExtDG20a).isNotSameAs(-1);
        int posExtDG20b = getIdentifierPositionByValue("three", identifiers);
        assertThat(posExtDG20b).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = condition.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1.19.1
        assertThat(system).isEqualTo("urn:id:ACME"); // PV1.19.4
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: DG1.3.1
        identifier = condition.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("B45678"); // DG1.3.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 3: DG1.20.1 and DG1.20.2
        identifier = condition.getIdentifier().get(posExtDG20a);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("one"); // DG1.20.1
        assertThat(system).isEqualTo("https://terminology.hl7.org/CodeSystem/two"); // DG1.20.2

        // Identifier 4: DG1.20.3 and DG1.20.4
        identifier = condition.getIdentifier().get(posExtDG20b);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("three"); // DG1.20.3
        assertThat(system).isEqualTo("https://terminology.hl7.org/CodeSystem/four"); // DG1.20.4
    }

    @Test
    void conditionDg1IdentifierTest3() {
        String withDG132 = "MSH|^~\\&|||||201610015080000||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|||10290^^^WEST^MR||||20040530|M||||||||||||||||||||||N\n"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\n"
                + "DG1|1|ICD10|^Ovarian Cancer|Test|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326||S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";
        Condition condition = ResourceUtils.getCondition(ftv, withDG132);

        // Expect 2 identifiers
        assertThat(condition.hasIdentifier()).isTrue();
        assertThat(condition.getIdentifier()).hasSize(2);
        List<Identifier> identifiers = condition.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("201610015080000", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("Ovarian Cancer", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = condition.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("201610015080000"); // MSH.7
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 3: DG1.3.2
        identifier = condition.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("Ovarian Cancer"); // DG1.3.2
        assertThat(system).isEqualTo("urn:id:extID");
    }

    @Test
    void observationIdentifierTest() {
        // identifier uses the logic from BUILD_IDENTIFIER_FROM_CWE (OBX.3) and joins FILL or PLAC values with it

        // Filler from OBR-3; OBX-3.1/OBX-3.3
        String joinFillPlaAndObx3 = "MSH|^~\\&|SOARF|E_AMC|||201512201012||ADT^A01|MSG$1|P|2.7||1|||||||\r"
                + "EVN|A01|201512201012||CI|wiestc|201512201007|1488\r"
                + "PID|||MRN12345678^^^ID-XYZ^MR~111223333^^^USA^SS~MN1234567^^^MNDOT^DL||||||||||||||\r"
                + "PV1|1|I|E2^E211^E211B^1488|Elective||||||||||V\r"
                + "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||\r"
                + "OBX|1|ST|DINnumber^^LSFUSERDATAE||N/A||||||R||||||\r";
        Observation observation = ResourceUtils.getObservation(ftv, joinFillPlaAndObx3);

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
        observation = ResourceUtils.getObservation(ftv, msg);

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
                + "OBX|1|ST|DINnumber^^LSFUSERDATAE||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
                + "ORC|NW|PON001|FON001|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|";
        observation = ResourceUtils.getObservation(ftv, msg);

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
                + "OBX|1|ST|DINnumber||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
                + "ORC|NW|PON001|||SC|D|1||20170825010500|MS|MS|||||";
        observation = ResourceUtils.getObservation(ftv, msg);

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
    void diagnosticReportIdentifierTest() {
        // Filler and placer from ORC
        String diagnosticReport = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|PON001^LE|FON001^OE PHIMS Stage|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n"
                + "OBR|1||CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F\n"
                + "OBX|1|TX|||Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity.||||||F|||20170825010500\n";
        DiagnosticReport report = ResourceUtils.getDiagnosticReport(ftv, diagnosticReport);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = report.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("20170825010500", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("FON001", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("PON001", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: extID with MSH-7
        Identifier identifier = report.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20170825010500"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        identifier = report.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("FON001"); // ORC-3.1
        assertThat(system).isEqualTo("urn:id:OE_PHIMS_Stage"); // ORC-3.2 any whitespace gets replaced with underscores
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 3: Placer
        identifier = report.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("PON001"); // ORC-2.1
        assertThat(system).isEqualTo("urn:id:LE"); // ORC-2.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

    }

    @Test
    void diagnosticReportIdentifierTestFillerPlacerFromOBR() {
        // Filler and placer from OBR
        String diagnosticReport = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170825010500||ORU^R01|MSGID22102712|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n"
                + "OBR|1|CC_000000^OE PHIMS Stage|CD_000000|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F\n"
                + "OBX|1|TX|||Impression: 1. Markedly intense metabolic activity corresponding with the area of nodular enhancement in the left oral cavity.||||||F|||20170825010500\n";
        DiagnosticReport report = ResourceUtils.getDiagnosticReport(ftv, diagnosticReport);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = report.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("20170825010500", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD_000000", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("CC_000000", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);
        // Identifier 1: extID with MSH-7
        Identifier identifier = report.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20170825010500"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        identifier = report.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD_000000"); // OBR-3.1
        assertThat(system).isNull(); // OBR-3.2 is empty
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 3: Placer
        identifier = report.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();

        assertThat(value).isEqualTo("CC_000000"); // OBR-2
        assertThat(system).isEqualTo("urn:id:OE_PHIMS_Stage"); // OBR-2.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    @Test
    void encounterIdentifierTest() {
        // Test: Visit number from PV1-19
        String encounterMsg = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john|||||||||||||112233\r"
                + "PV1||I||||||||SUR||||||||S||A\r";
        Encounter encounter = ResourceUtils.getEncounter(ftv, encounterMsg);

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
    }

    @Test
    void encounterIdentifierVisitNumberFromPV1() {

        // Test: Visit number from PV1-19, plus PV1-50
        String encounterW2Identifiers = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john\r"
                + "PV1||I||||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000||||||POL8009|\r";
        Encounter encounter = ResourceUtils.getEncounter(ftv, encounterW2Identifiers); //the first identifier is the same. the second Identifier comes from PV1.50

        // Expect 2 identifiers
        assertThat(encounter.hasIdentifier()).isTrue();
        assertThat(encounter.getIdentifier()).hasSize(2);
        List<Identifier> identifiers = encounter.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posVN1 = getIdentifierPositionByValue("8846511", identifiers);
        assertThat(posVN1).isNotSameAs(-1);
        int posVN2 = getIdentifierPositionByValue("POL8009", identifiers);
        assertThat(posVN2).isNotSameAs(-1);

        // Identifier 1:
        Identifier identifier = encounter.getIdentifier().get(posVN1);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1-19
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

        // Identifier 2:
        identifier = encounter.getIdentifier().get(posVN2);
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
    }

    @Test
    void encounterIdentifierVisitNumberFromMSH7() {
        // Test: Visit number from MSH-7
        String encounterMsg = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|||12345^^^^MR||smith^john\r"
                + "PV1||I||||||||SUR||||||||S||A\r";
        Encounter encounter = ResourceUtils.getEncounter(ftv, encounterMsg);

        // Expect a single identifier
        assertThat(encounter.hasIdentifier()).isTrue();
        assertThat(encounter.getIdentifier()).hasSize(1);

        // Identifier 1: Visit number
        Identifier identifier = encounter.getIdentifier().get(0);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20130531"); // MSH.7
        assertThat(system).isNull(); //PV1.19.4 and PID.18 is empty so system is null
        CodeableConcept type = identifier.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("VN");
        assertThat(coding.getDisplay()).isEqualTo("Visit number");

    }

    @Test
    void immunizationIdentifierTest() {
        // RXA-5.1 and RXA-5.3, concatenate together with a dash
        String field1AndField3 = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||432155^^^ANF^MR||Patient^Johnny^New^^^^L\r"
                + "ORC|RE||197027|||||||||M|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX||||00^new immunization record^NIP001|||||||20131210||||CP|A\r";
        Immunization immunization = ResourceUtils.getImmunization(ftv, field1AndField3);

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
        immunization = ResourceUtils.getImmunization(ftv, field1AndField2);

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
        immunization = ResourceUtils.getImmunization(ftv, justField2);
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
    void procedureIdentifierTest() {
        // with PR1 and PID segments used to create identifiers
        String procedureMsg = "MSH|^~\\&|HL7Soup|Instance1|MCM||200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|78654^^^ACME||||N\n"
                + "ROL|5897|UP|AD||20210322133821|20210322133822|10||Hospital|ST||||USA\n"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\n";
        Procedure procedure = ResourceUtils.getProcedure(ftv, procedureMsg);

        // Expect 2 identifiers
        assertThat(procedure.hasIdentifier()).isTrue();
        assertThat(procedure.getIdentifier()).hasSize(2);
        List<Identifier> identifiers = procedure.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("P98", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posACME = getIdentifierPositionByValue("78654", identifiers);
        assertThat(posACME).isNotSameAs(-1);

        // Identifier 1: PR1.19
        Identifier identifier = procedure.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("P98"); // PR1.19.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Visit number from PID-18
        identifier = procedure.getIdentifier().get(posACME);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1
        assertThat(system).isEqualTo("urn:id:ACME"); // PID.18.4
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Test: MSH.7 and PV1.19
        String procedureMSH = "MSH|^~\\&||Instance1|MCM||200911021022|Security|ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM|CDP|^4086::132:2A57:3C28^IPV6|^4086::132:2A57:3C25^IPV6|\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "PV1||I|6N^1234^A^GENERAL HOSPITAL2|||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000\n"
                + "ROL|5897|UP|AD||20210322133821|20210322133822|10||Hospital|ST||||USA\n"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G||X|0|0\n";

        procedure = ResourceUtils.getProcedure(ftv, procedureMSH);

        // Expect 2 identifiers
        assertThat(procedure.hasIdentifier()).isTrue();
        assertThat(procedure.getIdentifier()).hasSize(2);
        identifiers = procedure.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        posExtId = getIdentifierPositionByValue("200911021022", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posVN = getIdentifierPositionByValue("8846511", identifiers);
        assertThat(posVN).isNotSameAs(-1);

        // Identifier 1: PR1
        identifier = procedure.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("200911021022"); // MSH.7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Visit number from PV1-19
        identifier = procedure.getIdentifier().get(posVN);
        String valueOBR = identifier.getValue();
        system = identifier.getSystem();
        assertThat(valueOBR).isEqualTo("8846511"); // PV1.19.1
        assertThat(system).isNull(); // No System PV1.19.2 DNE
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    @Test
    void documentReferenceIdentifierTest() {
        // Filler and placer from ORC, extID from MSH-7
        String documentReference = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                "PV1|1|I|||||||||||||||||||||||||||||||||||||||||||\n" +
                "ORC|NW|PON001^LE|FON001^OE|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                "OBR|1||CD_000000^IE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n"
                +
                "TXA|1||B45678||||||\n";

        DocumentReference report = ResourceUtils.getDocumentReference(ftv, documentReference);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);

        List<Identifier> identifiers = report.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("200911021022", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("FON001", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("PON001", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: extID from MSH-7
        Identifier identifier = report.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("200911021022"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        Identifier identifier2 = report.getIdentifier().get(posFILLER);
        value = identifier2.getValue();
        system = identifier2.getSystem();
        assertThat(value).isEqualTo("FON001"); //ORC.3.1
        assertThat(system).isEqualTo("urn:id:OE"); // ORC.3.2
        CodeableConcept type = identifier2.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: Placer
        Identifier identifier3 = report.getIdentifier().get(posPLACER);
        value = identifier3.getValue();
        system = identifier3.getSystem();
        assertThat(value).isEqualTo("PON001"); //ORC.2.1
        assertThat(system).isEqualTo("urn:id:LE"); // ORC.2.2
        type = identifier3.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
    }

    @Test
    void documentReferenceIdentifierTest2() {
        // Filler from OBR, placer from TXA-14
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                "PV1|1|I|||||||||||||||||||||||||||||||||||||||||||\n" +
                "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                "OBR|1||CD_000000^OE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n"
                +
                "TXA|1||B45678|||||||||||PON001^IE||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(ftv, documentReferenceMessage);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);

        List<Identifier> identifiers = report.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("200911021022", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD_000000", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("PON001", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: extID from MSH-7
        Identifier identifier = report.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("200911021022"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        Identifier identifier2 = report.getIdentifier().get(posFILLER);
        value = identifier2.getValue();
        system = identifier2.getSystem();
        assertThat(value).isEqualTo("CD_000000"); //OBR.3.1
        assertThat(system).isEqualTo("urn:id:OE"); // OBR.3.2
        CodeableConcept type = identifier2.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: Placer
        Identifier identifier3 = report.getIdentifier().get(posPLACER);
        value = identifier3.getValue();
        system = identifier3.getSystem();
        assertThat(value).isEqualTo("PON001"); // TXA-14.1
        assertThat(system).isEqualTo("urn:id:IE"); // TXA.14.2
        type = identifier3.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");

    }

    @Test
    void documentReferenceIdentifierTest3() {
        // Placer from OBR, Filler from TXA-15
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                "PV1|1|I|||||||||||||||||||||||||||||||||||||||||||\n" +
                "ORC|NW|||PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                "OBR|1|CD_000000^OE||2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n"
                +
                "TXA|1||B45678||||||||||||FON001^IE\n";
        DocumentReference report = ResourceUtils.getDocumentReference(ftv, documentReferenceMessage);

        // Expect 3 identifiers
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.getIdentifier()).hasSize(3);

        List<Identifier> identifiers = report.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posExtId = getIdentifierPositionByValue("200911021022", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("FON001", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("CD_000000", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: extID from MSH-7
        Identifier identifier = report.getIdentifier().get(posExtId);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("200911021022"); // MSH-7
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 2: Filler
        Identifier identifier3 = report.getIdentifier().get(posFILLER);
        value = identifier3.getValue();
        system = identifier3.getSystem();
        assertThat(value).isEqualTo("FON001"); // TXA-15.1
        assertThat(system).isEqualTo("urn:id:IE"); // TXA.15.2
        CodeableConcept type = identifier3.getType();
        Coding coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("FILL");
        assertThat(coding.getDisplay()).isEqualTo("Filler Identifier");

        // Identifier 3: Placer
        identifier = report.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD_000000"); //OBR.2.1
        assertThat(system).isEqualTo("urn:id:OE"); // OBR.2.2
        type = identifier.getType();
        coding = type.getCoding().get(0);
        assertThat(type.getText()).isNull();
        assertThat(coding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(coding.getCode()).isEqualTo("PLAC");
        assertThat(coding.getDisplay()).isEqualTo("Placer Identifier");
    }

    @Test
    void serviceRequestIdentifierTest1() {
        // Test 1 removed:  OMP_O09 messages do not create a service request

        // Test 2:
        //  - Visit number with PID-18
        //  - filler and placer from OBR
        String serviceRequest = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
                +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|78654||||N\n" +
                "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n" +
                // ORC.4 is not used as an identifier
                "ORC||||PG1234567^MYPG||E|^Q6H^D10^^^R\n" +
                "OBR|1|CD150920001336^OE|CD150920001337^IE|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||";

        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(ftv, serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        List<Identifier> identifiers = serviceReq.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("78654", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD150920001337", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("CD150920001336", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: visit number
        Identifier identifier = serviceReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001337"); // OBR.3.1
        assertThat(system).isEqualTo("urn:id:IE"); // OBR.3.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.2.1
        assertThat(system).isEqualTo("urn:id:OE"); // OBR.2.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    @Test
    void serviceRequestIdentifierTest2() {
        // Test 3:
        //  - Visit number with PV1-19
        //  - filler from OBR
        //  - placer from ORC
        String serviceRequest = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
                +
                "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                "PV1||I|6N^1234^A^GENERAL HOSPITAL2|||||||SUR||||||||S|8846511|A|||||||||||||||||||SF|K||||20170215080000\n"
                +
                "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n" +
                "ORC||PON001||||E|^Q6H^D10^^^R\n" +
                "OBR|1||CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||";

        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(ftv, serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = serviceReq.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("8846511", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD150920001336", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("PON001", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: visit number
        Identifier identifier = serviceReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("8846511"); // PV1.19
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.3.1
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("PON001"); // ORC.2.1
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

    }

    // NOTE: ORU_RO1 records do not create the ServiceRequest directly.  They create a DiagnosticReport and it creates the ServiceRequest.
    // This test makes sure the specification for ORU_RO1.DiagnosticReport is specifying PID and PV1 correctly in AdditionalSegments.
    @Test
    void serviceRequestIdentifierTest3() {
        // Test 1:
        //  - Visit number with PV1.19
        //  - filler and placer from OBR
        String serviceRequest = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
                // PID.18 is ignored as visit number identifier because PV1.19 is present
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.19 is used as the identifier visit number
                + "PV1|1|E|||||||||||||||||78654||||||||||||||||||||||||||\n"
                //  1. ORC.2 empty so OBR.2 is used
                //  2. ORC.3 empty so OBR.3 is used
                + "ORC|RE|||ML18267-C00001^Beaker||||||||||||||||||||||||||||||||||\n"
                //  10. OBR.3 used for Filler 
                //  11. OBR.2 used for Placer
                + "OBR|1|CD150920001336^OE|CD150920001337^IE|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||||||||||||||||||||||||||||||||||||||\n";

        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(ftv, serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        List<Identifier> identifiers = serviceReq.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("78654", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD150920001337", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("CD150920001336", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: visit number should be set by PV1.19
        Identifier identifier = serviceReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PV1.19
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001337"); // OBR.3.1
        assertThat(system).isEqualTo("urn:id:IE"); // OBR.3.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD150920001336"); // OBR.2.1
        assertThat(system).isEqualTo("urn:id:OE"); // OBR.2.2
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    // NOTE: ORU_RO1 records do not create the ServiceRequest directly.  They create a DiagnosticReport and it creates the ServiceRequest.
    // This test makes sure the specification for ORU_RO1.DiagnosticReport is specifying PID and PV1 correctly in AdditionalSegments.
    @Test
    void serviceRequestIdentifierTest4() {
        // Test 2
        //  - Visit number with PID.18
        //  - filler from ORC
        //  - placer from ORC
        String serviceRequest = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
                // PID.18 is used as backup identifier visit number because PV1.19 is empty
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||665544||||||||||||\n"
                // PV1.19 is empty and not used as visit number identifier 
                + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
                //  1. ORC.2 is used as Placer because it has priority over OBR.2
                //  1. ORC.3 is used as Filler because it has priority over OBR.3
                + "ORC|RE|248648498|248648499|ML18267-C00001^Beaker||||||||||||||||||||||||||||||||||||\n"
                //  10. OBR.2 ignored as Placer
                //  11. OBR.3 ignored as Filler
                + "OBR|1|CD150920001336|CD150920001336|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||||||||||||||||||||||||||||||||||||||||||||\n";

        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(ftv, serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = serviceReq.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("665544", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("248648499", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("248648498", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: visit number should be set by PID.18
        Identifier identifier = serviceReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("665544"); // PID.18
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("248648499"); // ORC.3
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("248648498"); // ORC.2
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    // NOTE: ORU_RO1 records do not create the ServiceRequest directly.  They create a DiagnosticReport and it creates the ServiceRequest.
    // This test makes sure the specification for ORU_RO1.DiagnosticReport is specifying PID and PV1 correctly in AdditionalSegments.
    @Test
    void serviceRequestIdentifierTest5() {
        // Test 3:
        //  - MSH.7 as the visit number
        //  - filler from ORC
        //  - placer from ORC
        String serviceRequest = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
                // PID.18 is empty so MSH.7 with be used as backup identifier visit number 
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.19 is empty so MSH.7 with be used as backup identifier visit number 
                + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
                //  1. ORC.2 is used as Placer because it has priority over OBR.2
                //  1. ORC.3 is used as Filler because it has priority over OBR.3
                + "ORC|RE|222298|222299|ML18267-C00001^Beaker||||||||||||||||||||||||||||\n"
                //  10. OBR.2 ignored as Placer
                //  11. OBR.3 ignored as Filler
                + "OBR|1|||83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||||||||||||||||||||||||||||||||||||||\n";

        ServiceRequest serviceReq = ResourceUtils.getServiceRequest(ftv, serviceRequest);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);

        // Expect 3 identifiers
        assertThat(serviceReq.hasIdentifier()).isTrue();
        assertThat(serviceReq.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = serviceReq.getIdentifier();
        // Match the three id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("20180924152907", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("222299", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("222298", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: visit number should be set by MSH.7
        Identifier identifier = serviceReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20180924152907"); // MSH.7
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: filler
        identifier = serviceReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("222299"); // ORC.3
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        //Identifier 3: placer
        identifier = serviceReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("222298"); // ORC.2
        assertThat(system).isNull();
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

    }

    @Test
    void medicationRequestIdentifierTest() {
        // Visit number from PID-18, extID from RXO-1.1
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||OMP^O09|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN|||||||||||||||78654\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequest);

        // Expect 4 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(4);
        List<Identifier> identifiers = medReq.getIdentifier();
        // Match the four id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("78654", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("RX700001", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posFILLER = getIdentifierPositionByValue("CD2017071101", identifiers);
        assertThat(posFILLER).isNotSameAs(-1);
        int posPLACER = getIdentifierPositionByValue("PON001", identifiers);
        assertThat(posPLACER).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("78654"); // PID.18.1, since no PV1.19.1 in this message
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: extID based on RXO-1.1
        identifier = medReq.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("RX700001"); // RXO-1.1
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 3: Filler
        identifier = medReq.getIdentifier().get(posFILLER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD2017071101"); // ORC-3.1
        assertThat(system).isEqualTo("urn:id:RX"); // ORC-3.2 any whitespace gets replaced with underscores
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 4: Placer
        identifier = medReq.getIdentifier().get(posPLACER);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("PON001"); // ORC-3.1
        assertThat(system).isEqualTo("urn:id:OE"); // ORC-3.2 any whitespace gets replaced with underscores
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "PLAC", "Placer Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);
    }

    @Test
    void medicationRequestIdentifierTest2() throws IOException {

        // Test: Visit number from PV1-19, extID from RXO-1.1 and RXO-1.3
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||OMP^O09|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN|||||||||||||||78654\r"
                + "PV1||I|||||||||||||||||789789\r"
                + "ORC|NW|||||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE^ABC|100||mg|||||G||10||5|\r";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequest);

        // Expect 2 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        List<Identifier> identifiers = medReq.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("789789", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("RX700001-ABC", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("789789"); // PV1.19
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: extID based on RXO-1.1 and RX-O1.3
        identifier = medReq.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("RX700001-ABC"); // RXO-1.1 and RXO-1.3
        assertThat(system).isEqualTo("urn:id:extID");

    }

    @Test
    void medicationRequestIdentifierTest3() throws IOException {

        // Test: Visit number from MSH-7, extID from RXE-2
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||OMP^O09|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW||CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequest);

        // Expect 3 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(3);
        List<Identifier> identifiers = medReq.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("20170215080000", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("DOCUSATE SODIUM 100 MG CAPSULE", identifiers);
        assertThat(posExtId).isNotSameAs(-1);
        int posRX = getIdentifierPositionByValue("CD2017071101", identifiers);
        assertThat(posRX).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20170215080000"); // MSH-7
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: extID based on RXO-1.2
        identifier = medReq.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("DOCUSATE SODIUM 100 MG CAPSULE"); // RXO-1.2
        assertThat(system).isEqualTo("urn:id:extID");

        // Identifier 3: RX
        identifier = medReq.getIdentifier().get(posRX);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("CD2017071101"); // ORC.3.1
        assertThat(system).isEqualTo("urn:id:RX"); // ORC-3.2 any whitespace gets replaced with underscores
        type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "FILL", "Filler Identifier",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

    }

    @Test
    void medicationRequestIdentifierTest4() throws IOException {

        // Test: Visit number from MSH-7, no RXO-1
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN\r"
                + "PV1||I||||||||SUR||||||||S||A|||||||||||||||||||SF|K||||20170215080000\r"
                + "ORC|NW|||||E|10^BID^D4^^^R||20170215080000\r"
                + "RXO|||||||||G||10||5|\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequest);

        // Expect 2 identifier
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        List<Identifier> identifiers = medReq.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("20170215080000", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("999-NDC", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("20170215080000"); // MSH-7
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: extID based on RXE-2.1 and RXE-2.3
        identifier = medReq.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("999-NDC"); // RXE-2.1 and RXE-2.3
        assertThat(system).isEqualTo("urn:id:extID");

    }

    @Test
    void medicationRequestIdentifierTest5() throws IOException {

        // Test: Visit number from PV1-19, extID from RXE-2.1 and RXE-2.3
        String medicationRequest = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN|||||||||||||||78654\r"
                + "PV1||I|||||||||||||||||789789\r"
                + "ORC|NW|||||E|10^BID^D4^^^R||20170215080000\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|";
        MedicationRequest medReq = ResourceUtils.getMedicationRequest(ftv, medicationRequest);

        // Expect 2 identifiers
        assertThat(medReq.hasIdentifier()).isTrue();
        assertThat(medReq.getIdentifier()).hasSize(2);

        List<Identifier> identifiers = medReq.getIdentifier();
        // Match the id's to position; we can't depend on an order.
        int posVN = getIdentifierPositionByValue("789789", identifiers);
        assertThat(posVN).isNotSameAs(-1);
        int posExtId = getIdentifierPositionByValue("999-NDC", identifiers);
        assertThat(posExtId).isNotSameAs(-1);

        // Identifier 1: Visit number
        Identifier identifier = medReq.getIdentifier().get(posVN);
        String value = identifier.getValue();
        String system = identifier.getSystem();
        assertThat(value).isEqualTo("789789"); // PV1.19
        assertThat(system).isNull();
        CodeableConcept type = identifier.getType();
        DatatypeUtils.checkCommonCodeableConceptAssertions(type, "VN", "Visit number",
                "http://terminology.hl7.org/CodeSystem/v2-0203", null);

        // Identifier 2: extID based on RXE-2.1 and RXE-2.3
        identifier = medReq.getIdentifier().get(posExtId);
        value = identifier.getValue();
        system = identifier.getSystem();
        assertThat(value).isEqualTo("999-NDC"); // RXE-2.1 and RXE-2.3
        assertThat(system).isEqualTo("urn:id:extID");

    }

    // TODO Add this when MedicationAdministration is supported
    // @Test
    // @Disabled("Currently no messages generate MedicationAdministration resources")
    // void medicationAdministration_identifier_test() {
    //     
    //     // Currently no messages generate MedicationAdministration resources
    // }

    private static int getIdentifierPositionByValue(String value, List<Identifier> identifiers) {
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i).hasValue() && value.contains(identifiers.get(i).getValue())) {
                return i;
            }
        }
        return -1;

    }

}