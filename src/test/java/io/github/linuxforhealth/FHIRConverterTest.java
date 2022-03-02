/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;

class FHIRConverterTest {
    private static final String HL7_FILE_UNIX_NEWLINE = "src/test/resources/sample_unix.hl7";
    private static final String HL7_FILE_WIN_NEWLINE = "src/test/resources/sample_win.hl7";
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(FHIRConverterTest.class);
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    @Test
    void test_patient_encounter_bundle_return() throws IOException {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\n" +
                "PID|||MR1^^^XYZ||MAIDENNAME^EVE\n" +
                "MRG|MR2^^^XYZ\n";

        Bundle b = ftv.convertToBundle(hl7message, OPTIONS, null);

        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);

        List<Resource> patientResource = b.getEntry().stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(2);

    }

    @Test
    void test_patient_encounter() throws IOException {

        String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN||201209122222\r"
                + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
                + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
                + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
                + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r" + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
                + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

        String json = ftv.convert(hl7message, OPTIONS);
        verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE);

    }

    @Test
    void test_patient_encounter_no_message_header() throws IOException {

        String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||\r"
                + "EVN||201209122222\r"
                + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
                + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
                + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
                + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r" + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
                + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

        String json = ftv.convert(hl7message, OPTIONS);
        LOGGER.debug("FHIR json result:\n" + json);
        verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE, false);

    }

    @Test
    void convert_hl7_from_file_to_fhir_unix_line_endings() throws IOException {
        String json = ftv.convert(new File(HL7_FILE_UNIX_NEWLINE));
        verifyResult(json, BundleType.COLLECTION);

    }

    @Test
    void convert_hl7_from_file_to_fhir_wiin_line_endings() throws IOException {
        ConverterOptions options = new Builder().withBundleType(BundleType.COLLECTION).withValidateResource().build();

        String json = ftv.convert(new File(HL7_FILE_WIN_NEWLINE), options);
        verifyResult(json, BundleType.COLLECTION);

    }

    @Test
    void test_valid_message_but_unsupported_message_throws_exception() throws IOException {
        String hl7message = "MSH|^~\\&|MESA_ADT|XYZ_ADMITTING|MESA_IS|XYZ_HOSPITAL|201612291501||ADT^A18^ADT_A18|101166|P|2.3.1\n"
                + "EVN|A18|201604211000||||201604210950\n"
                + "PID|1||000010004^^^ST01A^MR~000010014^^^ST01B^MR~000010024^^^ST01^MR~000029970^^^EHIS^PI~999999999^^^SSA^SS||SENTARA10004^PAT^L||19251008|F||Caucasian||||||Married|Protestant|1002523||||||||||||PV1|1|O|||||2740^Tsadok^Janetary|2913^Merrit^Darren^F|3065^Mahoney^Paul^J||||||||9052^Winter^Oscar^||1001918\n"
                + "PV1|1|O|||||2741^Yung^Den|2914^Smith^John^F|3066^Mahr^Paul^J||||||||9053^Summer^Oscar^||1001200\n"
                + "MRG|000010510^^^def^MR~000010765^^^ST01B^MR|||000010510^^^def|||WHITE^CHARLES";

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ftv.convert(hl7message);
        });
    }

    @Test
    void test_dosage_output() throws IOException {
        String hl7message = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|1||PA123456^^^MYEMR^MR||JONES^GEORGE^M^JR^^^L|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r"
                +
                "ORC|RE||197023^CMC|||||||^Clark^Dave||1234567890^Smith^Janet^^^^^^NPPES^L^^^NPI^^^^^^^^MD\r" +
                "RXA|0|1|20140730||08^HEPB-PEDIATRIC/ADOLESCENT^CVX|.5|mL^mL^UCUM||00^NEW IMMUNIZATION RECORD^NIP001|1234567890^Smith^Janet^^^^^^NPPES^^^^NPI^^^^^^^^MD |^^^DE-000001||||0039F|20200531|MSD^MERCK^MVX|||CP|A";

        String json = ftv.convert(hl7message, OPTIONS);

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> immunization = e.stream()
                .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunization).hasSize(1);

        String s = context.getParser().encodeResourceToString(immunization.get(0));
        Class<? extends IBaseResource> klass = Immunization.class;
        Immunization expectDoseQuantity = (Immunization) context.getParser().parseResource(klass, s);
        assertThat(expectDoseQuantity.hasDoseQuantity()).isTrue();
        Quantity dosage = expectDoseQuantity.getDoseQuantity();
        BigDecimal value = dosage.getValue();
        String unit = dosage.getUnit();
        assertThat(value).isEqualTo(BigDecimal.valueOf(.5));
        assertThat(unit).isEqualTo("mL");
    }

    @Test
    void test_invalid_message_throws_error() throws IOException {
        String hl7message = "some text";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ftv.convert(hl7message);
        });
    }

    @Test
    void test_blank_message_throws_error() throws IOException {
        String hl7message = "";

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ftv.convert(hl7message);
        });
    }

    @Test
    // Test an example of a message with message structure specified
    void test_adt_40_message_with_adt_a39_structure_specified() throws Exception {
        Message hl7message = null;
        // Test that an ADT A40 message with MSH-9.3 of 'ADT_A39' is successfully parsed and converted as an ADT A40 message.
        // Note that ADT_A39 is the expected structure of an ADT_A40 message.
        String hl7messageString = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\n" +
                "PID|||MR1^^^XYZ||MAIDENNAME^EVE\n" +
                "MRG|MR2^^^XYZ\n";

        InputStream ins = IOUtils.toInputStream(hl7messageString, StandardCharsets.UTF_8);
        Hl7InputStreamMessageStringIterator iterator = new Hl7InputStreamMessageStringIterator(ins);

        if (iterator.hasNext()) {
            HL7HapiParser hparser = new HL7HapiParser();
            hl7message = hparser.getParser().parse(iterator.next());
        }

        String messageType = HL7DataExtractor.getMessageType(hl7message);

        assertThat(messageType).isEqualTo("ADT_A40");

        // Convert and check for a patient resource
        String json = ftv.convert(hl7messageString, ConverterOptions.SIMPLE_OPTIONS);

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();

        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        assertThat(b.getId()).isNotNull();
        assertThat(b.getMeta().getLastUpdated()).isNotNull();

        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(2);

    }

    @Test
    // Test an example of a message with no message structure specifed
    void test_adt_40_message() throws Exception {
        Message hl7message = null;
        // Test that an ADT A40 message with no MSH-9.3 is successfully parsed and converted.
        String hl7messageString = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40|00000003|P|2.6\n" +
                "PID|||MR1^^^XYZ||MAIDENNAME^EVE\n" +
                "MRG|MR2^^^XYZ\n";

        InputStream ins = IOUtils.toInputStream(hl7messageString, StandardCharsets.UTF_8);
        Hl7InputStreamMessageStringIterator iterator = new Hl7InputStreamMessageStringIterator(ins);

        if (iterator.hasNext()) {
            HL7HapiParser hparser = new HL7HapiParser();
            hl7message = hparser.getParser().parse(iterator.next());
        }

        String messageType = HL7DataExtractor.getMessageType(hl7message);

        assertThat(messageType).isEqualTo("ADT_A40");

        // Convert and check for a patient resource
        String json = ftv.convert(hl7messageString, ConverterOptions.SIMPLE_OPTIONS);

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();

        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        assertThat(b.getId()).isNotNull();
        assertThat(b.getMeta().getLastUpdated()).isNotNull();

        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(2);

    }

    @Test
    /*
     * This tests some of coding systems of interest or potential problems
     */
    void testCodingSystems() throws FHIRException {
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|201305330||VXU^V04^VXU_V04|20130531RI881401010105|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "EVN|A01|20130617154644||01\r"
                + "PID|1||12345678^^^MYEMR^MR||TestPatient^John|||M|\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                // Test MVX
                + "RXA|0|1|20130528|20130529|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                // Test HL70162 & HL70163
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r";

        String json = ftv.convert(hl7VUXmessageRep, OPTIONS);

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        assertThat(b.getId()).isNotNull();
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
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
        if (coding.getCode().contains("C28161")) {
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
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource)
                .collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // No Observation resource because OBX.2 is type TX        
        assertThat(obsResource).isEmpty();

        List<Resource> pracResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
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
