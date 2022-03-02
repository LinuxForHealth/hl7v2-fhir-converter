/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Duration;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7EncounterFHIRConversionTest {

    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    private static FHIRContext context = new FHIRContext(true, false);
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7EncounterFHIRConversionTest.class);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    @Test
    void test_encounter_visitdescription_present() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av|A|||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309|||<This field> should be found \"Encounter.text\" \\T\\ formatted as xhtml with correct escaped characters.\\R\\HL7 newline should be processed as well|||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||\n";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context); //Encounter.Type comes from  PV1.4
        assertThat(encounter.getTypeFirstRep().getCodingFirstRep().getCode()).isEqualTo("A");
        assertThat(encounter.getTypeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("Accident");
        assertThat(encounter.getTypeFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0007");

        //
        // "text": {
        //   "status": "additional",
        //    "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>&lt;This field&gt; should be found &quot;Encounter.text&quot; &amp; formatted as xhtml with correct escaped characters.<br/>HL7 newline should be processed as well</p></div>"
        //  }
        //

        Narrative encText = encounter.getText();
        assertNotNull(encText);
        assertEquals("additional", encText.getStatusAsString());
        assertEquals(
                "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>&lt;This field&gt; should be found &quot;Encounter.text&quot; &amp; formatted as xhtml with correct escaped characters.<br/>HL7 newline should be processed as well</p></div>",
                encText.getDivAsString());
    }

    @Test
    void test_encounter_visitdescription_missing() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting|S|Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||\n";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        assertThat(encounter.hasType()).isFalse(); // This assertion is to confirm that we do not get a type from PV1.18

        Narrative encText = encounter.getText();
        assertNull(encText.getStatus());
        assertThat(encText.getDiv().getChildNodes()).isEmpty();
    }

    // Test for serviceProvider reference in messages with both PV1 and PV2 segments
    // Part 1: use serviceProvider from PV2.23 subfields
    @ParameterizedTest
    @ValueSource(strings = {
            "ADT^A01", /* "ADT^A02", "ADT^A03", "ADT^A04", */ "ADT^A08", /* "ADT^A28", "ADT^A31", */
            // ADT_A34 and ADT_A40 do not create encounters so they do not need to be tested here
            // MDM messages are not tested here because they do not have PV2 segments
            "OMP^O09",
            "ORU^R01",
            "RDE^O11", "RDE^O25",
            "VXU^V04"
    })
    void test_encounter_with_serviceProvider_from_PV2(String message) {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\r"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\r"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\r"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH_WEYMOUTH|||||||||N||||||\r";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        Reference serviceProvider = encounter.getServiceProvider();
        assertThat(serviceProvider).isNotNull();
        String providerString = serviceProvider.getReference();
        assertThat(providerString).isEqualTo("Organization/ssh-weymouth");

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1);

        Organization orgResource = ResourceUtils.getResourceOrganization(organizations.get(0), context);
        assertThat(orgResource.getId()).isEqualTo(providerString);
        assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
        assertThat(orgResource.getIdentifier()).hasSize(1);
        assertThat(orgResource.getIdentifierFirstRep().getValue()).hasToString("SSH_WEYMOUTH"); // PV2.23.1
        assertThat(orgResource.getIdentifierFirstRep().getSystem()).hasToString("urn:id:extID"); // Because ID is name based
    }

    // Test for serviceProvider reference in messages with both PV1 and PV2 segments
    // Part 2: Field PV2.23 is provided but no PV2.23.8; serviceProvider id should use backup field PV1.3.4.1
    @ParameterizedTest
    @ValueSource(strings = {
            "ADT^A01", /* "ADT^A02", "ADT^A03", "ADT^A04", */ "ADT^A08", /* "ADT^A28", "ADT^A31", */
            // ADT_A34 and ADT_A40 do not create encounters so they do not need to be tested here
            // MDM messages are not tested here because they do not have PV2 segments
            "OMP^O09",
            "ORU^R01",
            "RDE^O11", "RDE^O25",
            "VXU^V04"
    })
    void test_encounter_PV1_serviceProvider(String message) {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\r"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\r"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\r"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth|||||||||N||||||\r";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        Reference serviceProvider = encounter.getServiceProvider();
        assertThat(serviceProvider).isNotNull();
        String providerString = serviceProvider.getReference();
        assertThat(providerString).isEqualTo("Organization/toronto");

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1);

        Organization orgResource = ResourceUtils.getResourceOrganization(organizations.get(0), context);
        assertThat(orgResource.getId()).isEqualTo(providerString);
        assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
        assertThat(orgResource.getIdentifier()).hasSize(1);
        assertThat(orgResource.getIdentifierFirstRep().getValue()).hasToString("Toronto"); // PV1.3.4.1
        assertThat(orgResource.getIdentifierFirstRep().getSystem()).hasToString("urn:id:extID"); // Because ID is name based
    }

    // Test for serviceProvider reference in messages with PV1 segment and no PV2 segment
    // Use serviceProvider from PV1-3.4.1
    @ParameterizedTest
    @ValueSource(strings = {
            "ADT^A01", /* "ADT^A02", "ADT^A03", "ADT^A04", */ "ADT^A08", /* "ADT^A28", "ADT^A31", */
            // ADT_A34 and ADT_A40 do not create encounters so they do not need to be tested here
            // MDM messages are not tested here because they do not have PV2 segments
            "OMP^O09",
            "ORU^R01",
            "RDE^O11", "RDE^O25",
            "VXU^V04"
    })
    void test_encounter_with_serviceProvider_from_PV1_3_4(String message) {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||" + message
                + "|controlID|P|2.6\r"
                + "EVN|A01|20150502090000|\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                // PV1-3.4 used for serviceProvider reference; used for both id and name
                + "PV1||I|INT^0001^02^Toronto East|||||||SUR||||||||S|VisitNumber^^^Toronto North|A|||||||||||||||||||Toronto West||||||\r";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        Reference serviceProvider = encounter.getServiceProvider();
        assertThat(serviceProvider).isNotNull();
        String providerString = serviceProvider.getReference();
        assertThat(providerString).isEqualTo("Organization/toronto-east"); // Also verify underscore replacement for VALID_ID

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1);

        Organization orgResource = ResourceUtils.getResourceOrganization(organizations.get(0), context);
        assertThat(orgResource.getId()).isEqualTo(providerString);
        assertThat(orgResource.getName()).isEqualTo("Toronto East"); // PV1.3.4.1
        assertThat(orgResource.getIdentifier()).hasSize(1);
        assertThat(orgResource.getIdentifierFirstRep().getValue()).hasToString("Toronto East"); // PV1.3.4.1
        assertThat(orgResource.getIdentifierFirstRep().getSystem()).hasToString("urn:id:extID"); // Because ID is name based
    }

    @Test
    void test_encounter_PV2_serviceProvider_idfix() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH*WEYMOUTH WEST_BUILD-7.F|||||||||N||||||\n";

        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        Reference serviceProvider = encounter.getServiceProvider();
        assertThat(serviceProvider).isNotNull();
        String providerString = serviceProvider.getReference();
        assertThat(providerString).isEqualTo("Organization/ssh-weymouth-west-build-7.f");

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1);

        Organization orgResource = ResourceUtils.getResourceOrganization(organizations.get(0), context);
        assertThat(orgResource.getId()).isEqualTo(providerString);
        assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
        assertThat(orgResource.getIdentifier()).hasSize(1);
        assertThat(orgResource.getIdentifierFirstRep().getValue()).hasToString("SSH*WEYMOUTH WEST_BUILD-7.F"); // PV2.23.1
        assertThat(orgResource.getIdentifierFirstRep().getSystem()).hasToString("urn:id:extID"); // Because ID is name based
    }

    @Test
    void test_encounter_class() {
        // PV1.2 has mapped value and should returned fhir value
        String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);

        assertThat(encounter.hasClass_()).isTrue();
        Coding encounterClass = encounter.getClass_();
        assertThat(encounterClass.getCode()).isEqualTo("EMER");
        assertThat(encounterClass.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        assertThat(encounterClass.getDisplay()).isEqualTo("emergency");
        assertThat(encounterClass.getVersion()).isNull();

        // Should return hl7Code if not a mapped value
        hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|L|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);

        assertThat(encounter.hasClass_()).isTrue();
        encounterClass = encounter.getClass_();
        assertThat(encounterClass.getCode()).isEqualTo("L");
        assertThat(encounterClass.getSystem()).isNull();
        assertThat(encounterClass.getDisplay()).isNull();
        assertThat(encounterClass.getVersion()).isNull();
    }

    @Test
    void testEncounterReasonCode() {
        // EVN.4 and PV2.3 for reasonCode; both with known codes
        String hl7message = "MSH|^~\\&|||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200||O|||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||||||||||\n"
                // PV2.3 with known coding
                + "PV2|||01.4^Fatigue^CCC||Diamond ring|||||||||||||||||||||||||||||||\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);

        assertThat(encounter.hasReasonCode()).isTrue();
        List<CodeableConcept> reasonCodes = encounter.getReasonCode();
        assertThat(reasonCodes).hasSize(2);

        CodeableConcept encounterReasonEVN = reasonCodes.get(0);
        CodeableConcept encounterReasonPV2 = reasonCodes.get(1);
        if (encounterReasonPV2.getTextElement().toString() != "Fatigue") {
            encounterReasonEVN = reasonCodes.get(1);
            encounterReasonPV2 = reasonCodes.get(0);
        }
        DatatypeUtils.checkCommonCodeableConceptAssertions(encounterReasonPV2, "01.4", "Fatigue",
                "http://terminology.hl7.org/CodeSystem/CCC", "Fatigue");
        DatatypeUtils.checkCommonCodeableConceptAssertions(encounterReasonEVN, "O", "Other",
                "http://terminology.hl7.org/CodeSystem/v2-0062", null);

        // Using EVN-4 and PV2.3 for reasonCode BOTH with with unknown codes
        hl7message = "MSH|^~\\&|||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200||REG_UPDATE|||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||||||||||\n"
                + "PV2|||vomits|||||||||||||||||||||||||||||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);

        assertThat(encounter.hasReasonCode()).isTrue();
        reasonCodes = encounter.getReasonCode();
        assertThat(reasonCodes).hasSize(2);

        encounterReasonEVN = reasonCodes.get(0);
        encounterReasonPV2 = reasonCodes.get(1);
        if (encounterReasonPV2.getCodingFirstRep().getCode() != "vomits") {
            encounterReasonEVN = reasonCodes.get(1);
            encounterReasonPV2 = reasonCodes.get(0);
        }
        DatatypeUtils.checkCommonCodeableConceptAssertions(encounterReasonPV2, "vomits", null, null, null);
        DatatypeUtils.checkCommonCodeableConceptAssertions(encounterReasonEVN, "REG_UPDATE", null, null, null);
    }

    @Test
    void testEncounterLength() {

        // Both start PV1.44 and end PV1.45 must be present to use either value as part of length
        // When both are present, the calculation value is provided in "Minutes"
        String hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 present; PV1.45 present
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||20161013154626|20161014154634|||||||\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasLength()).isTrue();
        Duration encounterLength = encounter.getLength();
        assertThat(encounterLength.getValue()).isEqualTo((BigDecimal.valueOf(1440)));
        assertThat(encounterLength.getUnit()).isEqualTo("minutes");
        assertThat(encounterLength.getCode()).isEqualTo("min");
        assertThat(encounterLength.getSystem()).isEqualTo("http://unitsofmeasure.org");

        // If PV1.44 or PV1.45 are missing, PV2.11 is used as back-up length; assumes "Days" for unit
        hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 empty; PV1.45 present
                + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||20171018154634|||||||\n"
                // PV2.11 present
                + "PV2|||||||||||3|||||||||||||||||||||||||||||||||||||||||||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasLength()).isTrue();
        encounterLength = encounter.getLength();
        assertThat(encounterLength.getValue()).isEqualTo((BigDecimal.valueOf(3)));
        assertThat(encounterLength.getUnit()).isEqualTo("days");
        assertThat(encounterLength.getCode()).isEqualTo("d");
        assertThat(encounterLength.getSystem()).isEqualTo("http://unitsofmeasure.org");

        // If PV1.44 or PV1.45 are missing, and there is no PV2.11 for back-up, no length is created
        hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 empty; PV1.45 present
                + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||20171018154634|||||||\n"
                // PV2.11 empty
                + "PV2|||vomits|||||||||||||||||||||||||||||||||||||||||||||||||||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasLength()).isFalse();

        // If PV1.44 and PV1.45 are present with a date only (no minutes time), and there is no PV2.11 then nothing  
        // Because dates can't be evaluated to minutes
        hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 present; PV1.45 present; but they have no minutes, so they will not be used
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||20161014|20161015|||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasLength()).isFalse();
    }

    @Test
    void testEncounterPeriod() {

        // Both start PV1.44 (EVN.6 and EVN.2 are here to PV1.44 takes precedence) and end PV1.45 are present
        String hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||20151008111200\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 present; PV1.45 present
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||20161013154626|20161014154634|||||||\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasPeriod()).isTrue();
        Period encounterPeriod = encounter.getPeriod();
        assertThat(encounterPeriod.hasStart()).isTrue();
        assertThat(encounterPeriod.hasEnd()).isTrue();
        assertThat(encounterPeriod.getStartElement().toString()).contains("2016-10-13");
        assertThat(encounterPeriod.getEndElement().toString()).contains("2016-10-14");

        // If PV1.44(Start) is missing we fall back to either EVN.6 or EVN.2 in this case EVN.6 is present and takes precedence over EVN.2
        hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20201008111211||||20151008111200|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 empty; PV1.45 present
                + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||20171018154634|||||||\n"
                // PV2.11 present
                + "PV2|||||||||||3|||||||||||||||||||||||||||||||||||||||||||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasPeriod()).isTrue();
        encounterPeriod = encounter.getPeriod();
        assertThat(encounterPeriod.hasStart()).isTrue();
        assertThat(encounterPeriod.hasEnd()).isTrue();
        assertThat(encounterPeriod.getStartElement().toString()).contains("2015-10-08");
        assertThat(encounterPeriod.getEndElement().toString()).contains("2017-10-18");

        // If PV1.44(Start) AND EVN.6(Start) is missing we fall back to EVN.2 for start no value present for period.End
        hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 empty; PV1.45 empty
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||||||||||\n"
                + "PV2|||vomits|||||||||||||||||||||||||||||||||||||||||||||||||||||||\n";
        encounter = ResourceUtils.getEncounter(ftv, hl7message);
        assertThat(encounter.hasPeriod()).isTrue();
        encounterPeriod = encounter.getPeriod();
        assertThat(encounterPeriod.hasStart()).isTrue();
        assertThat(encounterPeriod.hasEnd()).isFalse();
        assertThat(encounterPeriod.getStartElement().toString()).contains("2015-10-08");
        assertThat(encounterPeriod.getEnd()).isNull();
    }

    @Test
    void testEncounterModeOfArrival() {
        String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"
                + "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|C^Car^HL70430\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);

        List<Extension> extensionList = encounter.getExtension();
        assertNotNull(extensionList);
        assertThat(extensionList).hasSize(1);
        boolean extFound = false;
        for (Extension ext : extensionList) {
            if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival")) {
                extFound = true;
                assertTrue(ext.getValue() instanceof Coding);
                Coding valueCoding = (Coding) ext.getValue();
                assertThat(valueCoding.getCode()).isEqualTo("C");
                assertThat(valueCoding.getDisplay()).isEqualTo("Car");
                assertThat(valueCoding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0430");
                break;
            }
        }
        assertTrue(extFound, "modeOfArrival extension not found");
    }

    @Test
    void test_encounter_modeOfarrival_invalid_singlevalue() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMBULATORY\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        List<Extension> extensionList = encounter.getExtension();
        assertNotNull(extensionList);
        assertThat(extensionList).isNotEmpty();

        boolean extFound = false;
        for (Extension ext : extensionList) {
            if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival")) {
                extFound = true;
                assertTrue(ext.getValue() instanceof Coding);
                Coding valueCoding = (Coding) ext.getValue();
                assertThat(valueCoding.getCode()).isEqualTo("AMBULATORY");
                assertThat(valueCoding.getDisplay()).isNull();
                assertThat(valueCoding.getSystem()).isNull();
                break;
            }
        }
        assertTrue(extFound, "modeOfArrival extension not found");
    }

    @Test
    void test_encounter_modeOfarrival_invalid_with_codeAndDisplay() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMB^AMBULATORY\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        List<Extension> extensionList = encounter.getExtension();
        assertNotNull(extensionList);
        assertThat(extensionList).isNotEmpty();
        boolean extFound = false;
        for (Extension ext : extensionList) {
            if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival")) {
                extFound = true;
                assertTrue(ext.getValue() instanceof Coding);
                Coding valueCoding = (Coding) ext.getValue();
                assertThat(valueCoding.getCode()).isEqualTo("AMB");
                assertThat(valueCoding.getDisplay()).isEqualTo("AMBULATORY");
                assertThat(valueCoding.getSystem()).isNull();
                break;
            }
        }
        assertTrue(extFound, "modeOfArrival extension not found");
    }

    @Test
    void test_encounter_modeOfarrival_invalid_with_system() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
                + "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMB^AMBULATORY^FUNKY\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        List<Extension> extensionList = encounter.getExtension();
        assertNotNull(extensionList);
        assertThat(extensionList).isNotEmpty();
        boolean extFound = false;
        for (Extension ext : extensionList) {
            if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival")) {
                extFound = true;
                assertTrue(ext.getValue() instanceof Coding);
                Coding valueCoding = (Coding) ext.getValue();
                assertThat(valueCoding.getCode()).isEqualTo("AMB");
                assertThat(valueCoding.getDisplay()).isEqualTo("AMBULATORY");
                assertThat(valueCoding.getSystem()).isEqualTo("urn:id:FUNKY");
                break;
            }
        }
        assertTrue(extFound, "modeOfArrival extension not found");
    }

    // Test messages with PV1 segment and no PV2 segment, and no serviceProvider provided
    // Extension list should be empty and serviceProvider should be null
    @ParameterizedTest
    @ValueSource(strings = {
            "ADT^A01", /* "ADT^A02", "ADT^A03", "ADT^A04", */ "ADT^A08", /* "ADT^A28", "ADT^A31", */
            // ADT_A34 and ADT_A40 do not create encounters so they do not need to be tested here
            // MDM messages are not tested here because they do not have PV2 segments
            "OMP^O09",
            "ORU^R01",
            "RDE^O11", "RDE^O25",
            "VXU^V04"
    })
    void test_encounter_PV2segment_missing(String message) {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|" + message
                + "|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
                + "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
                + "PV1||I|^^^^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";
        Encounter encounter = ResourceUtils.getEncounter(ftv, hl7message);
        Narrative encText = encounter.getText();
        assertNull(encText.getStatus());
        assertThat(encText.getDiv().getChildNodes()).isEmpty();
        List<Extension> extensionList = encounter.getExtension();
        assertNotNull(extensionList);
        assertThat(extensionList).isEmpty();
        Reference serviceProvider = encounter.getServiceProvider();
        assertThat(serviceProvider).isNotNull();
        assertThat(serviceProvider.getReference()).isNull();
    }

    @Test
    void testEncounterParticipantList() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR||||20210330144208||ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||||\n"
                + "PID|1||ABC12345^^^MRN||DOE^JANE|||||||||||||||\n"
                // Key fields are PV1.7, PV1.8, PV1.9, and PV1.17
                // These fields each have multiple XCNs to test they work with repeating values
                + "PV1||I|||||2905^DoctorA^Attending^M^IV^^MD~2905-2^DoctorA2^Attending2^M2|5755^DoctorB^Referring^^Sr~5755-2^DoctorB2^Referring2^^Sr2|770542^DoctorC^Consulting^^Jr~770542-2^DoctorC2^Consulting2^^Sr||||||||59367^DoctorD^Admitting~59367-2^DoctorD2^Admitting2|||||||||||||||||||||||||||\n";
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);

        List<EncounterParticipantComponent> encParticipantList = encounter.getParticipant();
        assertThat(encParticipantList).hasSize(8);

        List<Resource> practioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practioners).hasSize(8);

        HashMap<String, List<String>> practionerMap = new HashMap<String, List<String>>();
        //Make sure that practitioners found are matching the HL7 by using known ID as check
        List<String> practionerIds = Arrays.asList("2905", "2905-2", "5755", "5755-2", "770542", "770542-2", "59367",
                "59367-2");
        for (Resource r : practioners) {
            Practitioner p = ResourceUtils.getResourcePractitioner(r, context);
            assertThat(p.getIdentifier()).hasSize(1);
            String value = p.getIdentifier().get(0).getValue();
            assertThat(practionerIds).contains(value);
            // Make a map where key is the Participant ID <GUID>, first value is Participant name, second is Participant Code
            List<String> values = new ArrayList<String>();
            switch (value) {
                case "2905":
                    values.add("Attending M DoctorA IV");
                    values.add("ATND");
                    break;
                case "2905-2":
                    values.add("Attending2 M2 DoctorA2");
                    values.add("ATND");
                    break;
                case "5755":
                    values.add("Referring DoctorB Sr");
                    values.add("REF");
                    break;
                case "5755-2":
                    values.add("Referring2 DoctorB2 Sr2");
                    values.add("REF");
                    break;
                case "770542":
                    values.add("Consulting DoctorC Jr");
                    values.add("CON");
                    break;
                case "770542-2":
                    values.add("Consulting2 DoctorC2 Sr");
                    values.add("CON");
                    break;
                case "59367":
                    values.add("Admitting DoctorD");
                    values.add("ADM");
                    break;
                case "59367-2":
                    values.add("Admitting2 DoctorD2");
                    values.add("ADM");
                    break;
            }
            practionerMap.put(p.getId(), values);
        }

        //Make sure that each practitioner is correctly mapped within the Encounter
        for (EncounterParticipantComponent participantComponent : encParticipantList) {
            String id = participantComponent.getIndividual().getReference();
            // Use the Id to look up the expected Participant name and Participant code
            // In map, first value is Participant name , second is Participant code
            assertEquals(practionerMap.get(id).get(0), participantComponent.getIndividual().getDisplay());
            assertEquals(practionerMap.get(id).get(1),
                    participantComponent.getType().get(0).getCoding().get(0).getCode());
        }
    }

    /**
     * Test Encounter correctly creates and references Practitioners as Participants.
     * Sparse data test. Only one participant is created.
     */
    @Test
    void testEncounterParticipantMissing() {
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR||||20210330144208||ADT^A01|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
                + "EVN||20210330144208||||\n"
                + "PID|1||ABC12345^^^MRN||DOE^JANE|||||||||||||||\n"
                // Key field is PV1.17; note that PV1.7, PV1.8, PV1.9 are purposely empty.  See companion test testEncounterParticipantList
                + "PV1||I|||||||||||||||59367^Doctor^Admitting|||||||||||||||||||||||||||\n";
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter encounter = ResourceUtils.getResourceEncounter(encounterResource.get(0), context);
        List<EncounterParticipantComponent> encParticipantList = encounter.getParticipant();
        assertThat(encParticipantList).hasSize(1);
        EncounterParticipantComponent participantComponent = encParticipantList.get(0);

        List<Resource> practioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practioners).hasSize(1);
        Practitioner practitioner = ResourceUtils.getResourcePractitioner(practioners.get(0), context);

        // With one practitioner and one participant, confirm the ID's match, the code and name are expected.
        assertThat(participantComponent.getIndividual().getReference()).isEqualTo(practitioner.getId());
        assertThat(participantComponent.getType().get(0).getCoding().get(0).getCode()).isEqualTo("ADM");
        assertThat(participantComponent.getIndividual().getDisplay()).isEqualTo("Admitting Doctor");

    }

    /**
     * Testing Encounter correctly references Observation
     */
    @Test
    void testEncounterReferencesObservation() throws IOException {
        String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
                + "PV1|1|O|Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\n"
                + "OBX|1|SN|24467-3^CD3+CD4+ (T4 helper) cells [#/volume] in Blood^LN||=^440|{Cells}/uL^cells per microliter^UCUM|649-1346 cells/mcL|L|||F\r";

        String json = ftv.convert(hl7message, OPTIONS);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter enc = (Encounter) encounterResource.get(0);
        List<Reference> reasonRefs = enc.getReasonReference();
        assertEquals(1, reasonRefs.size());
        assertTrue(reasonRefs.get(0).getReference().contains("Observation"));
    }

    /**
     * Testing Encounter correctly references Observation AND Diagnosis when both are present.
     */
    @Test
    void testEncounterReferencesObservationAndDiagnosis() throws IOException {
        String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
                + "PV1|1|O|Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\n"
                + "OBX|1|SN|24467-3^CD3+CD4+ (T4 helper) cells [#/volume] in Blood^LN||=^440|{Cells}/uL^cells per microliter^UCUM|649-1346 cells/mcL|L|||F\r"
                + "DG1|1|ICD10|^Ovarian Cancer|||||||||||||||||||||\r";
        String json = ftv.convert(hl7message, OPTIONS);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> obsResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(obsResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter enc = (Encounter) encounterResource.get(0);
        List<Reference> reasonRefs = enc.getReasonReference();
        assertEquals(2, reasonRefs.size());
        // Guess at the order of the references
        Reference refObservation = reasonRefs.get(0);
        Reference refCondition = reasonRefs.get(1);
        // If guessed wrong, reverse them
        if (!refObservation.getReference().contains("Observation")) {
            refObservation = reasonRefs.get(1);
            refCondition = reasonRefs.get(0);
        }
        assertTrue(refObservation.getReference().contains("Observation"));
        assertTrue(refCondition.getReference().contains("Condition"));
    }

}
