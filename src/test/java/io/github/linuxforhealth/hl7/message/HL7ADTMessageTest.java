/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class HL7ADTMessageTest {

    @ParameterizedTest
    // ADT_A01, ADT_A04, ADT_A08, ADT_A13 all use the same message structure so we can reuse adt_a01 tests for them.
    @ValueSource(strings = { "ADT^A01"/* , "ADT^A04" */, "ADT^A08"/* , "ADT^A13" */ })
    void testAdtA01MinimumSegments(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);

    }

    @ParameterizedTest
    // ADT_A01, ADT_A04, ADT_A08, ADT_A13 all use the same message structure so we can reuse adt_a01 tests for them.
    @ValueSource(strings = { "ADT^A01"/* , "ADT^A04" */, "ADT^A08"/* , "ADT^A13" */ })
    void testAdtA01MinimumPlusProcedureGroup(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> procedureResource = ResourceUtils.getResourceList(e, ResourceType.Procedure);
        assertThat(procedureResource).hasSize(1); // from PR1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(3);

    }

    @ParameterizedTest
    // ADT_A01, ADT_A04, ADT_A08, ADT_A13 all use the same message structure so we can reuse adt_a01 tests for them.
    @ValueSource(strings = { "ADT^A01"/* , "ADT^A04" */, "ADT^A08"/* , "ADT^A13" */ })
    void testAdtA01FullWithObxTypeTXAndNoGroups(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\n"
                + "OBX|1|TX|1234^some text^SCT||First line||||||F||\n"
                + "OBX|2|TX|1234^some text^SCT||Second line||||||F||\n"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "DG1|1||B45678|||A|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID, PD1

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1, PV2

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).isEmpty(); // from OBX

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2); // from AL1

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1); // from DG1

        List<Resource> documentReferenceResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(documentReferenceResource).isEmpty(); // from OBX of type TX; TODO: this should be 1 when card 855 is implemented

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(5); //TODO: this should be 6 when card 855 is implemented

    }

    @ParameterizedTest
    // ADT_A01, ADT_A04, ADT_A08, ADT_A13 all use the same message structure so we can reuse adt_a01 tests for them.
    @ValueSource(strings = { "ADT^A01"/* , "ADT^A04" */, "ADT^A08"/* , "ADT^A13" */ })
    void testAdtA01FullPlusMultipleProcedureGroup(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\n"
                + "OBX|1|NM|111^TotalProtein||7.5|gm/dl|5.9-8.4||||F\r"
                + "OBX|2|ST|100||Observation content|||||||X\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "DG1|1||B45678|||A|\r"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\r"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\r"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\r"
                + "PR1|1|ICD10|B45678|Fix break|20210322155008|A|75||V46|80|||32|1|D22|G45|1|G|P98|X|0|0\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID, PD1

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1, PV2

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2); // from OBX

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2); // from AL1

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1); // from DG1

        List<Resource> procedureResource = ResourceUtils.getResourceList(e, ResourceType.Procedure);
        assertThat(procedureResource).hasSize(4); //from PROCEDURE.PR1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(11);

    }

    @Test
    @Disabled("adt-a02 not yet supported")
    void testAdtA02PatientEncounterPresent() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A02|controlID|P|2.6\n"
                + "EVN|A01|20150502090000|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expecting 2 total resources
        assertThat(e.size()).isEqualTo(2);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

    }

    @Test
    void testAdtA03AllResourcesPresent() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\n"
                + "EVN|A01|20150502090000|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PID|||1234^^^^MR\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\n"
                + "AL1|1||1605^acetaminophen^L\r"
                + "AL1|1||1605^acetaminophen^L\r"
                + "DG1|1||B45678|||A|\r"
                + "PR1|1||B45678||20210322155008\r"
                + "PR1|1||B45678||20210322155008\r"
                + "OBX||NM|111^TotalProtein\r"
                + "OBX||ST|100\r"
                + "IN1||||Large Blue Organization|456 Ultramarine Lane^^Faketown^CA^ZIP5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expecting 9 total resources
        assertThat(e.size()).isEqualTo(11);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID and PD1

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1, and PV2

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2); // from OBX

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2); // from AL1

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1); // from DG1

        List<Resource> procedureResource = ResourceUtils.getResourceList(e, ResourceType.Procedure);
        assertThat(procedureResource).hasSize(2); //from PROCEDURE.PR1

        List<Resource> insuranceResource = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(insuranceResource).hasSize(1); //from INSURANCE.IN1

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(1); //from INSURANCE.IN1

    }

    @Test
    void testAdtA03AllResourcesNoGroupsPresent() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\n"
                + "EVN|A01|20150502090000|\n"
                + "PID|||1234^^^^MR\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\n"
                + "AL1|1||1605^acetaminophen^L\r"
                + "AL1|1||1605^acetaminophen^L\r"
                + "DG1|1||B45678|||A|\r"
                + "OBX|1|NM|111^TotalProtein\r"
                + "OBX|2|ST|100\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expecting 9 total resources
        assertThat(e.size()).isEqualTo(7);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID and PD1

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1, and PV2

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2); // from OBX

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2); // from AL1

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1); // from DG1

    }

    @Test
    void testAdtA03MininumSegments() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);

    }

    @Test
    void testAdtA03MinimumPlusProcedureGroup() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "PR1|1||B45678||20210322155008\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> procedureResource = ResourceUtils.getResourceList(e, ResourceType.Procedure);
        assertThat(procedureResource).hasSize(1); // from PR1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(3);

    }

    @Test
    void testAdtA03MinimumPlusInsuranceGroup() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR\r"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "IN1||||Large Blue Organization|456 Ultramarine Lane^^Faketown^CA^ZIP5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); // from EVN, PV1

        List<Resource> coverageResource = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverageResource).hasSize(1); // from IN1

        List<Resource> organizationResource = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizationResource).hasSize(1); // Reference from IN1

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(4);

    }

    @Test
    @Disabled("adt-a28 not yet supported")
    //TODO: When this is supported, note that this should be updated to reflect adt_a05 structure
    void testAdtA28PatientEncounterPresent() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A28|controlID|P|2.6\n"
                + "EVN|A01|20150502090000|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expecting 2 total resources
        assertThat(e.size()).isEqualTo(2);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

    }

    @Test
    @Disabled("adt-a31 not yet supported")
    //TODO: When this is supported, note that this should be updated to reflect adt_a05 structure
    void testAdtA31PatientEncounterPresent() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A31|controlID|P|2.6\n"
                + "EVN|A01|20150502090000|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Expecting 2 total resources
        assertThat(e.size()).isEqualTo(2);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

    }

    @ParameterizedTest
    // ADT_A30 structure is also used by ADT_A34, ADT_A35, ADT_A36, ADT_A46, ADT_A47, ADT_A48, ADT_A49.  We can reuse this for those messages if we choose to support them in the future.
    @ValueSource(strings = { /* "ADT^A30", */ "ADT^A34"/*
                                                        * , "ADT^A35", "ADT^A36", "ADT^A46", "ADT^A47", "ADT^A48",
                                                        * "ADT^A49"
                                                        */ })
    void testAdtA30MininumSegments(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "MRG|456||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // There should be two patient resources, the PID patient and the MRG patient.
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(2); // from PID and MRG

        // We currently do not support Encounters for merging, in ADT_A34 merge
        // messages, so no Encounter should be created from EVN.

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);

    }

    @ParameterizedTest
    // ADT_A30 structure is also used by ADT_A34, ADT_A35, ADT_A36, ADT_A46, ADT_A47, ADT_A48, ADT_A49.  We can reuse this for those messages if we choose to support them in the future.
    @ValueSource(strings = { /* "ADT^A30", */ "ADT^A34"/*
                                                        * , "ADT^A35", "ADT^A36", "ADT^A46", "ADT^A47", "ADT^A48",
                                                        * "ADT^A49"
                                                        */ })
    void testAdtA30Full(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PD1|||||||||||01|N||||A\r"
                + "MRG|456||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // There should be two patient resources, the PID patient and the MRG patient.
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(2); // from PID and MRG

        // We currently do not support Encounters for merging, in ADT_A34 merge
        // messages, so no Encounter should be created from EVN.

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);

    }

    @ParameterizedTest
    // ADT_A39 structure is also used by ADT_A40, ADT_A41, ADT_A42.  We can reuse this for those messages if we choose to support them in the future.
    @ValueSource(strings = { /* "ADT^A39", */ "ADT^A40"/* , "ADT^A41", "ADT^A42" */ })
    void testAdtA39MinSegments(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "MRG|456||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // There should be two patient resources, the PID patient and the MRG patient.
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(2); // 1st from PID; 2nd from MRG

        // We currently do not support Encounters for merging,
        // so no Encounter should be created from EVN.

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);

    }

    @ParameterizedTest
    // ADT_A39 structure is also used by ADT_A40, ADT_A41, ADT_A42.  We can reuse this for those messages if we choose to support them in the future.
    @ValueSource(strings = { /* "ADT^A39", */ "ADT^A40"/* , "ADT^A41", "ADT^A42" */ })
    void testAdtA39MinWithMultiplePatientGroups(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1111^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "MRG|123||||||\n"
                + "PID|||2222^^^^MR||DOE^Joe^|||F||||||||||||||||||||||\r"
                + "MRG|456||||||\n"
                + "PID|||3333^^^^MR||DOE^Larry^|||F||||||||||||||||||||||\r"
                + "MRG|789||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // There should be six patient resources from the PID segments and MRG segments.
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(6); // from PIDs and MRGs

        // We currently do not support Encounters for merging,
        // so no Encounter should be created from EVN.

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(6);

    }

    @ParameterizedTest
    // ADT_A39 structure is also used by ADT_A40, ADT_A41, ADT_A42.  We can reuse this for those messages if we choose to support them in the future.
    @ValueSource(strings = { /* "ADT^A39", */ "ADT^A40"/* , "ADT^A41", "ADT^A42" */ })
    void testAdtA39FullWithMultiplePatientGroups(String message) throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1111^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "MRG|123||||||\n"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "PID|||2222^^^^MR||DOE^Joe^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "MRG|456||||||\n"
                + "PID|||3333^^^^MR||DOE^Larry^|||F||||||||||||||||||||||\r"
                + "MRG|789||||||\n"
                + "PV1||I||||||||SUR||||||||S|VisitNumber^^^ACME|A||||||||||||||||||||||||20150502090000|\r"
                + "PID|||4444^^^^MR||DOE^Elizabeth^|||F||||||||||||||||||||||\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // There should be patient resources from the PID and the MRG segments.
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(7); // from PID and MRG

        // We currently do not support Encounters for merging, in ADT_A34 merge
        // messages, so no Encounter should be created from EVN and PV1.

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(7);

    }

}