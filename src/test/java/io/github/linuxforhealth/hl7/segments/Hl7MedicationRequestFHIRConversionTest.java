/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7MedicationRequestFHIRConversionTest {

    private static FHIRContext context = new FHIRContext(true, false);
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MedicationRequestFHIRConversionTest.class);

    @Test
    public void test_medicationreq_patient() {
        String hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0100^ANDERSON,CARL|S|V446911|A|||||||||||||||||||SF|K||||20180622230000\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientList = e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientList).hasSize(1);

        Patient patient = ResourceUtils.getResourcePatient(patientList.get(0), context);

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getSubject()).isNotNull();
        assertThat(medicationRequest.getSubject().getReference()).isEqualTo(patient.getId());

        // Test that RDE_O11 messages don't create ServiceRequests
        List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();


    }

    @Test
    public void test_medicationreq_status() {

        //ORC.1 = NW -> Expected medication status = ACTIVE
        String hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ACTIVE);

        //ORC.1 = SC -> Expected medication status = UNKNOWN
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|SC|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList.clear();
        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.UNKNOWN);

        //ORC.1 = DC -> Expected medication status = STOPPED
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|DC|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList.clear();
        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.STOPPED);

        // Test that RDE_O11 messages don't create ServiceRequests
        List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();

        //ORC.1 = CA -> Expected medication status = CANCELLED
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0100^ANDERSON,CARL|S|V446911|A|||||||||||||||||||SF|K||||20180622230000\n"
                + "ORC|CA|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList.clear();
        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.CANCELLED);

        // Test that RDE_O11 messages don't create ServiceRequests
        serviceRequestList.clear();  
        serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();
    }

    // Test that OMP_O09 messages don't create ServiceRequests, they create MedicationRequests
    @Test
    public void medicationFromOMPTest() {
        // Minimal valid ORC message.  Requires RXO and RXR segments.
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210101000000|90103687|OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||^Every 6 hours^^20210101||20210101000000|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING|||20210101000000||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||||Take 1 tablet by mouth every 6 (six) hours.||G||4|tablet^tablet|0|222^JONES^JON^E.||||||||||^DECADRON\n"
                + "RXR|PO^Oral\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<BundleEntryComponent> e = bundle.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that one medicationRequest was created. 
        assertThat(medicationRequestList).hasSize(1);

        List<Resource> serviceRequestList = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();
    }
    
    
    // Tests medication request fields MedicationCodeableConcept, Authored On, and Intent.
    // Tests with supported message types RDE-O11, RDE-O25.
    // With both RXO and RXE segments, only RXE will be used.
    @ParameterizedTest
    @ValueSource(strings = 
    { 
    "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\r",
    "MSH|^~\\&||||||S1|RDE^O25||T|2.6|||||||||\r",
    })
    public void test_medicationCodeableConcept_authoredOn_and_intent_in_rde_with_rxO_with_rxe(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|20180622230000||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        
        // Verify Authored On date is correct.
        Date authoredOnDate = medicationRequest.getAuthoredOn();      
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2018, 5, 22, 23, 0); // 2018 06 22 23 00 00   -- june is 05
        ZoneId zone = ConverterConfiguration.getInstance().getZoneId();
        TimeZone timeZone = TimeZone.getTimeZone(zone);
        c.setTimeZone(timeZone);
        Date authoredOnDateTest = c.getTime();
        assertThat(authoredOnDate).isEqualTo(authoredOnDateTest);

        //Verify intent is set correctly
        String intent = medicationRequest.getIntent().toString();
        assertThat(intent).isEqualTo("ORDER");

        //Very medicationCodeableConcept is set correctly
        assertThat(medicationRequest.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medicationRequest.getMedicationCodeableConcept();
        assertThat(medCC.getText()).isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");
        assertThat(medCC.getCoding().get(0).getSystem()).isEqualTo("urn:id:ADS");
        assertThat(medCC.getCoding().get(0).getCode()).isEqualTo("DUONEB3INH");
        assertThat(medCC.getCoding().get(0).getDisplay()).isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");

    }   

    // Tests medication request fields MedicationCodeableConcept, Authored On, and Intent.
    // Tests with supported message types RDE-O11, RDE-O25.
    // With just the RXE segment.
    @ParameterizedTest
    @ValueSource(strings = 
    { 
    "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\r",
    "MSH|^~\\&||||||S1|RDE^O25||T|2.6|||||||||\r",
    })
    public void test_medicationCodeableConcept_authoredOn_and_intent_in_rde_with_just_rxe(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|20180622230000||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        
        // Verify Authored On date is correct.
        Date authoredOnDate = medicationRequest.getAuthoredOn();      
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2018, 5, 22, 23, 0); // 2018 06 22 23 00 00   -- june is 05
        ZoneId zone = ConverterConfiguration.getInstance().getZoneId();
        TimeZone timeZone = TimeZone.getTimeZone(zone);
        c.setTimeZone(timeZone);
        Date authoredOnDateTest = c.getTime();
        assertThat(authoredOnDate).isEqualTo(authoredOnDateTest);

        //Verify intent is set correctly
        String intent = medicationRequest.getIntent().toString();
        assertThat(intent).isEqualTo("ORDER");

        //Very medicationCodeableConcept is set correctly
        assertThat(medicationRequest.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medicationRequest.getMedicationCodeableConcept();
        assertThat(medCC.getText()).isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");
        assertThat(medCC.getCoding().get(0).getSystem()).isEqualTo("urn:id:ADS");
        assertThat(medCC.getCoding().get(0).getCode()).isEqualTo("DUONEB3INH");
        assertThat(medCC.getCoding().get(0).getDisplay()).isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");

    }

    // Tests medication request fields MedicationCodeableConcept and Intent.
    // Tests with supported message types ORM-O01, OMP-O09.
    // With just the RXO segment -- these message types don't support RXE.
    @ParameterizedTest
    @ValueSource(strings = 
    {
    "MSH|^~\\&||||||S1|OMP^O09||T|2.6|||||||||\r",
    // --UNCOMMENT BELOW WHEN CONVERTER SUPPORTS THIS MESSAGE TYPE-- 
    // "MSH|^~\\&||||||S1|ORM^O01||T|2.6|||||||||\r",
    })
    public void test_medicationCodeableConcept_and_intent_in_OMP_and_ORM(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        // Verify authored on is not present
        assertThat(medicationRequest.getAuthoredOn()).isNull();
        
        //Verify intent is set correctly
        String intent = medicationRequest.getIntent().toString();
        assertThat(intent).isEqualTo("ORDER");

        //Very medicationCodeableConcept is set correctly
        assertThat(medicationRequest.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medicationRequest.getMedicationCodeableConcept();
        assertThat(medCC.getText()).isEqualTo("Test15 SODIUM 100 MG CAPSULE");
        assertThat(medCC.getCoding().get(0).getSystem()).isEqualTo("http://hl7.org/fhir/sid/ndc");
        assertThat(medCC.getCoding().get(0).getCode()).isEqualTo("RX800006");
        assertThat(medCC.getCoding().get(0).getDisplay()).isEqualTo("Test15 SODIUM 100 MG CAPSULE");

    }

    // Tests medication request fields MedicationCodeableConcept and Intent.
    // Tests with supported message types PPR-PC1, PPR-PC2, PPR-PC3
    // With just the RXO segment -- these message types don't support RXE.
    @ParameterizedTest
    @ValueSource(strings = 
    { 
    "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r",
    // --UNCOMMENT BELOW WHEN CONVERTER SUPPORTS THIS MESSAGE TYPE--
    // "MSH|^~\\&||||||S1|PPR^PC2||T|2.6|||||||||\r",
    // "MSH|^~\\&||||||S1|PPR^PC3||T|2.6|||||||||\r",
    })
    public void test_medicationCodeableConcept_and_intent_in_PPR(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PRB|AD|20140610234741|^oxygenase|Problem_000054321_20190606193536||20140610234741\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "OBR|1|CD150920001336|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        // Verify authored on is not present
        assertThat(medicationRequest.getAuthoredOn()).isNull();
        
        //Verify intent is set correctly
        String intent = medicationRequest.getIntent().toString();
        assertThat(intent).isEqualTo("ORDER");

        //Very medicationCodeableConcept is set correctly
        assertThat(medicationRequest.hasMedicationCodeableConcept()).isTrue();
        CodeableConcept medCC = medicationRequest.getMedicationCodeableConcept();
        assertThat(medCC.getText()).isEqualTo("Test15 SODIUM 100 MG CAPSULE");
        assertThat(medCC.getCoding().get(0).getSystem()).isEqualTo("http://hl7.org/fhir/sid/ndc");
        assertThat(medCC.getCoding().get(0).getCode()).isEqualTo("RX800006");
        assertThat(medCC.getCoding().get(0).getDisplay()).isEqualTo("Test15 SODIUM 100 MG CAPSULE");

    }

}