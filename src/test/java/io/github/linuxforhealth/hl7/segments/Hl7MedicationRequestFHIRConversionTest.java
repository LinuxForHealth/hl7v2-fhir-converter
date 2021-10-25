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
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
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

        //ORC.5 = A -> Expected medication status = ACTIVE
        String hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC||F800006^OE|P800006^RX||A|E|10^BID^D4^^^R||20180622230000\n"
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

        //ORC.5 = CM -> Expected medication status = COMPLETED
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC||F800006^OE|P800006^RX||CM|E|10^BID^D4^^^R||20180622230000\n"
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
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.COMPLETED);

        //ORC.5 = ER -> Expected medication status = ENTEREDINERROR
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC||F800006^OE|P800006^RX||ER|E|10^BID^D4^^^R||20180622230000\n"
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
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ENTEREDINERROR);

        //ORC.1 = NW -> Expected medication status = ACTIVE
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ACTIVE);

        //ORC.1 = RP -> Expected medication status = UNKNOWN
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|RP|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
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

        //AuthoredOn comes from ORC.9 (the backup value) No RXE.32
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

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
        c.set(2018, 5, 22, 23, 0); // 2018 06 22 23 00 00   -- june is 05 //ORC.9
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

        //AuthoredOn comes from RXE.32
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20190622160000\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|20180622230000||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

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
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

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
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\n"
                + "OBR|1|||555|||20170825010500||||||||||||||||||F\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

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

    // Another test for medication request fields MedicationCodeableConcept and Intent 
    // in a PPR_PCx message. Slightly different message (has a PV segment).
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
    public void testMedicationRequestInPPRWithAPatientVisit() {

        String hl7message = "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PRB|AD|20141015103243|15777000^Prediabetes (disorder)^SNM|654321^^OtherSoftware.ProblemOID|||20120101||\r"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\r"
                + "OBR|1|||555|||20170825010500||||||||||||||||||F\r"
                + "RXO|65862-063-01^METOPROLOL TARTRATE^NDC||||Tablet||||||||2|2|AP1234567||||325|mg\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

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
        assertThat(medCC.getText()).isEqualTo("METOPROLOL TARTRATE");
        assertThat(medCC.getCoding().get(0).getSystem()).isEqualTo("http://hl7.org/fhir/sid/ndc");
        assertThat(medCC.getCoding().get(0).getCode()).isEqualTo("65862-063-01");
        assertThat(medCC.getCoding().get(0).getDisplay()).isEqualTo("METOPROLOL TARTRATE");

    }

    @Test
    public void test_MedicationRequest_ReasonCode(){
        //reason code from RXE.27
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000|||||||4338008^Wheezing^PRN\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC|||||||||134006|||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        assertThat(medicationRequest.getReasonCode()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCoding()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("Wheezing");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("Wheezing");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isEqualTo("urn:id:PRN");

        //reason code from RXO.20
        hl7message = "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I||||||0148^ADDISON^JAMES||SUR||||||||S|1400|A|||||||||||||||||||SF|K||||\r"
                + "PRB|AD|20141015103243|15777000^Prediabetes (disorder)^SNM|654321^^OtherSoftware.ProblemOID|||20120101||\r"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000|||||||4338008^Wheezing^PRN\r"
                + "OBR|1|||555|||20170825010500||||||||||||||||||F\r"
                + "RXO|65862-063-01^METOPROLOL TARTRATE^NDC||||Tablet||||||||2|2|AP1234567||||325|134006\r";

        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        assertThat(medicationRequest.getReasonCode()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCoding()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("134006");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isNull();
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isNull();

        //reason code from ORC.16
        hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000|||||||4338008^Wheezing^PRN\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|20180622230000||||||||\n";

        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        assertThat(medicationRequest.getReasonCode()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCoding()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("4338008");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("Wheezing");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isEqualTo("urn:id:PRN");

    }

    @Test
    public void test_MedicationRequest_category_requester_and_dispenseRequest(){
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000|||3122^PROVIDER^ORDERING^^^DR|||20190606193536||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        // requester comes from ORC.12 which is the back up value for RXE.13
        String requesterRef = medicationRequest.getRequester().getReference();
        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundle(b, requesterRef);

        Identifier practitionerIdentifier = practBundle.getIdentifierFirstRep();
        HumanName practName = practBundle.getNameFirstRep();
        assertThat(practitionerIdentifier.getValue()).isEqualTo("3122"); // ORC.12.1
        assertThat(practitionerIdentifier.getSystem()).isNull(); // ORC.12.9
        assertThat(practName.getFamily()).isEqualTo("PROVIDER"); // ORC.12.2
        assertThat(practName.getGivenAsSingleString()).isEqualTo("ORDERING"); // ORC.12.3
        assertThat(practName.getPrefixAsSingleString()).isEqualTo("DR"); //ORC.12.6
        assertThat(practName.getSuffix()).isEmpty(); // RXE.13.5
        assertThat(practName.getText()).isEqualTo("DR ORDERING PROVIDER"); // ORC.12

        //category comes from  ORC.29
        assertThat(medicationRequest.getCategory()).hasSize(1);
        assertThat(medicationRequest.getCategory().get(0).hasCoding()).isTrue();
        assertThat(medicationRequest.getCategory().get(0).getCodingFirstRep().getCode()).isEqualTo("inpatient");
        assertThat(medicationRequest.getCategory().get(0).getCodingFirstRep().getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/medicationrequest-category");
        assertThat(medicationRequest.getCategory().get(0).getCodingFirstRep().getDisplay()).isEqualTo("Inpatient");

        //DispenseRequest.start comes from ORC.15
        assertThat(medicationRequest.getDispenseRequest().hasValidityPeriod()).isTrue();
        assertThat(medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement().toString()).containsPattern("2019-06-06");

        hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||20190606193536||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||2213^ORDERING^PROVIDER||||||||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        ftv = new HL7ToFHIRConverter();
        json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        b = (Bundle) bundleResource;
        e = b.getEntry();

        medicationRequestList = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0), context);

        // requester comes from RXE.13
        requesterRef = medicationRequest.getRequester().getReference();
        practBundle = ResourceUtils.getSpecificPractitionerFromBundle(b, requesterRef);

        practitionerIdentifier = practBundle.getIdentifierFirstRep();
        practName = practBundle.getNameFirstRep();
        CodeableConcept type = practitionerIdentifier.getType();

        assertThat(type.getCodingFirstRep().getCode().toString()).isEqualTo("DEA");
        assertThat(type.getCodingFirstRep().getDisplay()).isEqualTo("Drug Enforcement Administration registration number");
        assertThat(type.getCodingFirstRep().getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(practitionerIdentifier.getValue()).isEqualTo("2213"); // RXE.13.1
        assertThat(practitionerIdentifier.getSystem()).isNull(); // RXE.13.9
        assertThat(practName.getFamily()).isEqualTo("ORDERING"); // RXE.13.2
        assertThat(practName.getGivenAsSingleString()).isEqualTo("PROVIDER"); // RXE.13.3
        assertThat(practName.getPrefix()).isEmpty(); // RXE.13.6
        assertThat(practName.getSuffix()).isEmpty(); // RXE.13.5
        assertThat(practName.getText()).isEqualTo("PROVIDER ORDERING"); // RXE.13
    }

}