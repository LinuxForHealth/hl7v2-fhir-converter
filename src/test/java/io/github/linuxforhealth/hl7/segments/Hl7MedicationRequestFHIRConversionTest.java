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

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Ratio;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7MedicationRequestFHIRConversionTest {

    // private static FHIRContext context = new FHIRContext(true, false);
    //     private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MedicationRequestFHIRConversionTest.class);

    @Test
    void test_medicationreq_patient() {
        String hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0100^ANDERSON,CARL|S|V446911|A|||||||||||||||||||SF|K||||20180622230000\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> patientList = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientList).hasSize(1);

        Patient patient = ResourceUtils.getResourcePatient(patientList.get(0), ResourceUtils.context);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getSubject()).isNotNull();
        assertThat(medicationRequest.getSubject().getReference()).isEqualTo(patient.getId());

        // Test that RDE_O11 messages don't create ServiceRequests
        List<Resource> serviceRequestList = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();

    }

    @Test
    void test_medicationreq_status() {

        //ORC.5 = A -> Expected medication status = (ACTIVE ORC.1 is present but ORC.5 takes precedence)
        String hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX||A|E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ACTIVE);

        //ORC.5 = CM -> Expected medication status = COMPLETED (ORC.1 is present but ORC.5 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX||CM|E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.COMPLETED);

        //ORC.5 = ER -> Expected medication status = ENTEREDINERROR (ORC.1 is present but ORC.5 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX||ER|E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ENTEREDINERROR);

        //ORC.1 = NW -> Expected medication status = ACTIVE (Missing ORC.5 so ORC.1 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";

        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.ACTIVE);

        //ORC.1 = RP -> Expected medication status = UNKNOWN (Missing ORC.5 so ORC.1 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|RP|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.UNKNOWN);

        //ORC.1 = DC -> Expected medication status = STOPPED (Missing ORC.5 so ORC.1 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|DC|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
        assertThat(medicationRequest.getStatus()).isEqualTo(MedicationRequestStatus.STOPPED);

        // Test that RDE_O11 messages don't create ServiceRequests
        List<Resource> serviceRequestList = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();

        //ORC.1 = CA -> Expected medication status = CANCELLED (Missing ORC.5 so ORC.1 takes precedence)
        hl7message = "MSH|^~\\&|APP|FAC|WHIA|IBM|20180622230000||RDE^O11^RDE_O11|MSGID221xx0xcnvMed31|T|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0100^ANDERSON,CARL|S|V446911|A|||||||||||||||||||SF|K||||20180622230000\n"
                + "ORC|CA|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\n"
                + "RXE|^^^20180622230000^^R|62756-017^Testosterone Cypionate^NDC|100||mg|||||10||5\n";
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList.clear();
        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);
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
    void medicationFromOMPTest() {
        // Minimal valid ORC message.  Requires RXO and RXR segments.
        String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|MEDORDER|IBM|20210101000000|90103687|OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||^Every 6 hours^^20210101||20210101000000|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING|||20210101000000||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||||Take 1 tablet by mouth every 6 (six) hours.||G||4|tablet^tablet|0|222^JONES^JON^E.||||||||||^DECADRON\n"
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created. 
        assertThat(medicationRequestList).hasSize(1);

        List<Resource> serviceRequestList = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        // Confirm that a serviceRequest was not created.      
        assertThat(serviceRequestList).isEmpty();
    }

    // Tests medication request fields MedicationCodeableConcept, Authored On, and Intent.
    // Tests with supported message types RDE-O11, RDE-O25.
    // With both RXO and RXE segments, only RXE will be used.
    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\r",
            "MSH|^~\\&||||||S1|RDE^O25||T|2.6|||||||||\r",
    })
    void test_medicationCodeableConcept_authoredOn_and_intent_in_rde_with_rxO_with_rxe(String msh) {

        //AuthoredOn comes from ORC.9 (the backup value) No RXE.32
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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
        assertThat(medCC.getCoding().get(0).getDisplay())
                .isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");

    }

    // Tests medication request fields MedicationCodeableConcept, Authored On, and Intent.
    // Tests with supported message types RDE-O11, RDE-O25.
    // With just the RXE segment.
    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\r",
            "MSH|^~\\&||||||S1|RDE^O25||T|2.6|||||||||\r",
    })
    void test_medicationCodeableConcept_authoredOn_and_intent_in_rde_with_just_rxe(String msh) {

        //AuthoredOn comes from RXE.32
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20190622160000\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||||||^DUONEB|20180622230000||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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
        assertThat(medCC.getCoding().get(0).getDisplay())
                .isEqualTo("3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN");

    }

    // Tests medication request fields MedicationCodeableConcept and Intent.
    // Tests with supported message types ORM-O01, OMP-O09.
    // With just the RXO segment -- these message types don't support RXE.
    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&||||||S1|OMP^O09||T|2.6|||||||||\r",
            // --UNCOMMENT BELOW WHEN CONVERTER SUPPORTS THIS MESSAGE TYPE-- 
            "MSH|^~\\&||||||S1|ORM^O01||T|2.6|||||||||\r",
    })
    void test_medicationCodeableConcept_and_intent_in_OMP_and_ORM(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\n"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);

        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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
    @ValueSource(strings = {
            "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r",
    // --UNCOMMENT BELOW WHEN CONVERTER SUPPORTS THIS MESSAGE TYPE--
    // "MSH|^~\\&||||||S1|PPR^PC2||T|2.6|||||||||\r",
    // "MSH|^~\\&||||||S1|PPR^PC3||T|2.6|||||||||\r",
    })
    void test_medicationCodeableConcept_and_intent_in_PPR(String msh) {

        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PRB|AD|20140610234741|^oxygenase|Problem_000054321_20190606193536||20140610234741\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\n"
                + "OBR|1|||555|||20170825010500||||||||||||||||||F\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE^NDC||100||mg|||||G||10||5\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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
    @ValueSource(strings = {
            "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r",
    // --UNCOMMENT BELOW WHEN CONVERTER SUPPORTS THIS MESSAGE TYPE--
    // "MSH|^~\\&||||||S1|PPR^PC2||T|2.6|||||||||\r",
    // "MSH|^~\\&||||||S1|PPR^PC3||T|2.6|||||||||\r",
    })
    void testMedicationRequestInPPRWithAPatientVisit() {

        String hl7message = "MSH|^~\\&||||||S1|PPR^PC1||T|2.6|||||||||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PRB|AD|20141015103243|15777000^Prediabetes (disorder)^SNM|654321^^OtherSoftware.ProblemOID|||20120101||\r"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||\r"
                + "OBR|1|||555|||20170825010500||||||||||||||||||F\r"
                + "RXO|65862-063-01^METOPROLOL TARTRATE^NDC||||Tablet||||||||2|2|AP1234567||||325|mg\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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
    void test_MedicationRequest_ReasonCode() {
        //reason code from RXE.27
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|F800006^OE|P800006^RX|||E|10^BID^D4^^^R||20180622230000|||||||4338008^Wheezing^PRN\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC|||||||||134006|||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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

        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

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

        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        assertThat(medicationRequest.getReasonCode()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCoding()).hasSize(1);
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("4338008");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("Wheezing");
        assertThat(medicationRequest.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isEqualTo("urn:id:PRN");

    }

    @Test
    void testMedicationRequestCategoryRequesterAndDispenseRequest() {
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000|||3122^PROVIDER^ORDERING^^^DR|||20190606193536||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||||||||||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        // requester comes from ORC.12 which is the back up value for RXE.13
        String requesterRef = medicationRequest.getRequester().getReference();
        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundleEntriesList(e, requesterRef);

        Identifier practitionerIdentifier = practBundle.getIdentifierFirstRep();
        HumanName practName = practBundle.getNameFirstRep();
        assertThat(practitionerIdentifier.getValue()).isEqualTo("3122"); // ORC.12.1
        assertThat(practitionerIdentifier.getSystem()).isNull(); // ORC.12.9
        assertThat(practName.getFamily()).isEqualTo("PROVIDER"); // ORC.12.2
        assertThat(practName.getGivenAsSingleString()).isEqualTo("ORDERING"); // ORC.12.3
        assertThat(practName.getPrefixAsSingleString()).isEqualTo("DR"); //ORC.12.6
        assertThat(practName.getSuffix()).isEmpty(); //ORC.12.5
        assertThat(practName.getText()).isEqualTo("DR ORDERING PROVIDER"); // ORC.12

        //category comes from  ORC.29
        assertThat(medicationRequest.getCategory()).hasSize(1);
        DatatypeUtils.checkCommonCodeableConceptAssertions(medicationRequest.getCategory().get(0), "inpatient",
                "Inpatient", "http://terminology.hl7.org/CodeSystem/medicationrequest-category",
                null);

        //DispenseRequest.start comes from ORC.15
        assertThat(medicationRequest.getDispenseRequest().hasValidityPeriod()).isTrue();
        assertThat(medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement().toString())
                .containsPattern("2019-06-06");
    }

    @Test
    void testMedicationRequestCategoryRequesterAndDispenseRequest2() {
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||20190606193536||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47||||1|PC||2213^ORDERING^PROVIDER||||||||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||||\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        // requester comes from RXE.13
        String requesterRef = medicationRequest.getRequester().getReference();
        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundleEntriesList(e, requesterRef);

        Identifier practitionerIdentifier = practBundle.getIdentifierFirstRep();
        HumanName practName = practBundle.getNameFirstRep();
        CodeableConcept practitionerIdentifierType = practitionerIdentifier.getType();

        //Check meta extension.display is null
        Extension ext = practBundle.getMeta().getExtension().get(0);
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();

        assertThat(cc.hasCoding()).isTrue();
        assertThat(cc.getCoding().get(0).getDisplay()).isNull();
        DatatypeUtils.checkCommonCodeableConceptAssertions(practitionerIdentifierType, "DEA",
                "Drug Enforcement Administration registration number", "http://terminology.hl7.org/CodeSystem/v2-0203",
                null);
        assertThat(practitionerIdentifier.getValue()).isEqualTo("2213"); // RXE.13.1
        assertThat(practitionerIdentifier.getSystem()).isNull(); // RXE.13.9
        assertThat(practName.getFamily()).isEqualTo("ORDERING"); // RXE.13.2
        assertThat(practName.getGivenAsSingleString()).isEqualTo("PROVIDER"); // RXE.13.3
        assertThat(practName.getPrefix()).isEmpty(); // RXE.13.6
        assertThat(practName.getSuffix()).isEmpty(); // RXE.13.5
        assertThat(practName.getText()).isEqualTo("PROVIDER ORDERING"); // RXE.13
    }

    @Test
    void dispenseRequestTestRXO() {
        // Get DispenseRequest from RXO segment(RXO.11, RXO.12.1 and default system)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS|||||"
                // split and concatenate RXO for easier understanding
                + "|Take 1 tablet by mouth every 6 (six) hours.||G||4|tablet^tablet|0|222^JONES^JON^E.|||||||||7^PC|^DECADRON\n" // RXO 11 & 12 are on this line
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        MedicationRequest.MedicationRequestDispenseRequestComponent disReq = medicationRequest.getDispenseRequest();

        // dispenseRequest.Quantity comes from RXO.11, RXO.12.1 and default system
        assertThat(disReq.getQuantity().getValue().toString()).isEqualTo("4.0");
        assertThat(disReq.getQuantity().getUnit()).isEqualTo("tablet");
        assertThat(disReq.getQuantity().getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

    @Test
    void dispenseRequestTestRXE() {
        // Get DispenseRequest from RXE segment (RXE.10, RXE.11.1 and RXE.11.3)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                    + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                    + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47|||"
                    // split and concatenate RXE for easier understanding
                    + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER||||||||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n"; // RXE 10 & 11 are on this line

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
            // Confirm that one medicationRequest was created.
            assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                    ResourceUtils.context);

        MedicationRequest.MedicationRequestDispenseRequestComponent disReq = medicationRequest.getDispenseRequest();

            // dispenseRequest.Quantity comes from RXE.10, RXE.11.1 and RXE.11.3
            assertThat(disReq.getQuantity().getValue().toString()).isEqualTo("1.0");
            assertThat(disReq.getQuantity().getUnit()).isEqualTo("PC");
            assertThat(disReq.getQuantity().getSystem()).isEqualTo("urn:id:measureofunits");

            // dispenseRequest.InitialFIll.Quantity comes from RXE.39
            assertThat(disReq.getInitialFill().getQuantity().getValue().toString()).isEqualTo("7.0");
    }

    @Test
    void dosageInstructionTestMaxDosePerPeriodRXO() {
        // Test dosageInstruction.maxDosePerPeriod from RXO.23
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS|||||"
                // split and concatenate RXO for easier understanding
                + "|Take 1 tablet by mouth every 6 (six) hours.||G||4|tablet^tablet|0|222^JONES^JON^E.|||||||||7^PC|^DECADRON\n" // RXO 23 is on this line
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Ratio maxDose = medicationRequest.getDosageInstructionFirstRep().getMaxDosePerPeriod();

        // dosageInstruction.maxDosePerPeriod.numerator(RXO.23)
        assertThat(maxDose.getNumerator().getValue().toString()).isEqualTo("7.0");
        assertThat(maxDose.getNumerator().getUnit()).isEqualTo("PC");
        assertThat(maxDose.getNumerator().getSystem()).isEqualTo("http://unitsofmeasure.org");

        // dosageInstruction.maxDosePerPeriod.denominator
        assertThat(maxDose.getDenominator().getValue().toString()).isEqualTo("1.0");
        assertThat(maxDose.getDenominator().getUnit()).isEqualTo("day");
        assertThat(maxDose.getDenominator().getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

    @Test
    void dosageInstructionTestMaxDosePerPeriodRXE() {
        // Test dosageInstruction.maxDosePerPeriod
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47|||"
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER||||||5^PC||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n"; // RXE.19 is on this line

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Ratio maxDose = medicationRequest.getDosageInstructionFirstRep().getMaxDosePerPeriod();

        // dosageInstruction.maxDosePerPeriod.numerator(RXE.19)
        assertThat(maxDose.getNumerator().getValue().toString()).isEqualTo("5.0");
        assertThat(maxDose.getNumerator().getUnit()).isEqualTo("PC");
        assertThat(maxDose.getNumerator().getSystem()).isEqualTo("http://unitsofmeasure.org");

        // dosageInstruction.maxDosePerPeriod.denominator
        assertThat(maxDose.getDenominator().getValue().toString()).isEqualTo("1.0");
        assertThat(maxDose.getDenominator().getUnit()).isEqualTo("day");
        assertThat(maxDose.getDenominator().getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

    @Test
    void dosageInstructionTestPatientInstructionRXO() {
        // Test dosageInstruction.patietInstruction (RXO.7)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS|||||"
                // split and concatenate RXO for easier understanding
                + "|^Take 1 tablet by mouth every 6 (six) hours.||G||4|tablet^tablet|0|222^JONES^JON^E.|||||||||7^PC|^DECADRON\n" // RXO.7 is on this line ("^Take 1 tablet by mouth every 6 (six) hours.")
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        String patInstruct = medicationRequest.getDosageInstructionFirstRep().getPatientInstruction();

        // dosageInstruction.patientInstruction (RXO.7.2)
        assertThat(patInstruct).isEqualTo("Take 1 tablet by mouth every 6 (six) hours.");
    }

    @Test
    void dosageInstructionTestPatientInstructionRXE() {
        // Test dosageInstruction.patietInstruction (RXE.7)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47|333^Take 1 tablet by mouth every 6 (six) hours.||" // RXE.7 is on this line ("333^Take 1 tablet by mouth every 6 (six) hours.")
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER||||||5^PC||||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        String patInstruct = medicationRequest.getDosageInstructionFirstRep().getPatientInstruction();

        // dosageInstruction.patientInstruction (RXE.7.1 and 7.2 separated by a ':')
        assertThat(patInstruct).isEqualTo("333:Take 1 tablet by mouth every 6 (six) hours.");
    }

    @Test
    void dosageInstructionTestTextRXO() {
        // Test dosageInstruction.text (RXO.6)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||"
                // split and concatenate RXO for easier understanding
                + "|^Take 1 tablet by mouth every 6 (six) hours.|||G||4|tablet^tablet|0|222^JONES^JON^E.|||||||||7^PC|^DECADRON\n" // RXO.6 is on this line ("^Take 1 tablet by mouth every 6 (six) hours.")
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        String txt = medicationRequest.getDosageInstructionFirstRep().getText();

        // dosageInstruction.text (RXO.6.2)
        assertThat(txt).isEqualTo("Take 1 tablet by mouth every 6 (six) hours.");
    }

    @Test
    void dosageInstructionTestTextRXE() {
        // Test dosageInstruction.text (RXE.21)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|47|||"
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER||||||5^PC||333^Take 1 tablet by mouth every 6 (six) hours.||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n"; // RXE.21 is on this line ("333^Take 1 tablet by mouth every 6 (six) hours.")

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        String txt = medicationRequest.getDosageInstructionFirstRep().getText();

        // dosageInstruction.text (RXE.21.1 and 21.2 separated by a ':')
        assertThat(txt).isEqualTo("333:Take 1 tablet by mouth every 6 (six) hours.");
    }

    @Test
    void dosageInstructionTestRouteRXO() {
        // Test dosageInstruction.Route (RXO.5)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||6064005^Topical route^http://snomed.info/sct" // RXO.5 is on this line ("6064005^Topical route^http://snomed.info/sct")
                // split and concatenate RXO for easier understanding
                + "|^Take 1 tablet by mouth every 6 (six) hours.|||G||4|tablet^tablet|0|222^JONES^JON^E.|||||||||7^PC|^DECADRON\n"
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        CodeableConcept route = medicationRequest.getDosageInstructionFirstRep().getRoute();

        // dosageInstruction.route (RXO.5)
        assertThat(route.getCodingFirstRep().getCode()).isEqualTo("6064005"); //5.1
        assertThat(route.getCodingFirstRep().getDisplay()).isEqualTo("Topical route"); //5.2
        assertThat(route.getCodingFirstRep().getSystem()).isEqualTo("http://snomed.info/sct"); //5.3
        assertThat(route.getText()).isEqualTo("Topical route"); //5.2

    }

    @Test
    void dosageInstructionTestRouteRXE() {
        // Test dosageInstruction.Route (RXE.6)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL|6064005|||"// RXE.6 is on this line ("6064005")
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER||||||5^PC||333^Take 1 tablet by mouth every 6 (six) hours.||||||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        CodeableConcept route = medicationRequest.getDosageInstructionFirstRep().getRoute();

        // dosageInstruction.route (RXE.6)
        assertThat(route.getCodingFirstRep().getCode()).isEqualTo("6064005"); //6.1
        assertThat(route.getCodingFirstRep().getDisplay()).isNull(); //6.2
        assertThat(route.getCodingFirstRep().getSystem()).isNull(); //6.3
        assertThat(route.getText()).isNull(); //6.2

    }

    @Test
    void dosageInstructionTestRateRatioRXO() {
        // Test dosageInstruction.RateRatio (RXO.21)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS||||6064005^Topical route^http://snomed.info/sct"
                // split and concatenate RXO for easier understanding
                + "|^Take 1 tablet by mouth every 6 (six) hours.|||G||4|tablet^tablet|0|222^JONES^JON^E.|||day||||6|PC^^http://unitsofmeasure.org||^DECADRON\n" // RXO.21 and RXO.22 (6|PC^^http://unitsofmeasure.org)
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Ratio rateRatio = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getRateRatio();

        // dosageInstruction.doseAndRate.rateRatio.numerator(RXO.21)
        assertThat(rateRatio.getNumerator().getValue().toString()).isEqualTo("6.0"); //RXO.21
        assertThat(rateRatio.getNumerator().getUnit()).isEqualTo("PC"); //RXO.22.1
        assertThat(rateRatio.getNumerator().getSystem()).isEqualTo("http://unitsofmeasure.org"); //RXO.22.3

        // dosageInstruction.doseAndRate.rateRatio.denominator
        assertThat(rateRatio.getDenominator().getValue().toString()).isEqualTo("1.0");
        assertThat(rateRatio.getDenominator().getUnit()).isEqualTo("day");
        assertThat(rateRatio.getDenominator().getSystem()).isEqualTo("http://unitsofmeasure.org");

    }

    @Test
    void dosageInstructionTestRateRatioRXE() {
        // Test dosageInstruction.RateRatio (RXE.23)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL||||"
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER|||||day|||333^Take 1 tablet by mouth every 6 (six) hours.||7|PC|||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n"; // RXE.23 and 24 are on this line (7|PC)

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Ratio rateRatio = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getRateRatio();

        // dosageInstruction.doseAndRate.rateRatio.numerator(RXE.23)
        assertThat(rateRatio.getNumerator().getValue().toString()).isEqualTo("7.0"); //RXE.23
        assertThat(rateRatio.getNumerator().getUnit()).isEqualTo("PC"); //RXE.24
        assertThat(rateRatio.getNumerator().getSystem()).isEqualTo("http://unitsofmeasure.org"); //Defaulted

        // dosageInstruction.doseAndRate.rateRatio.denominator
        assertThat(rateRatio.getDenominator().getValue().toString()).isEqualTo("1.0");
        assertThat(rateRatio.getDenominator().getUnit()).isEqualTo("day"); //RXE.22
        assertThat(rateRatio.getDenominator().getSystem()).isEqualTo("http://unitsofmeasure.org");

    }

    @Test
    void dosageInstructionTestDoseQuantityRXO() {
        // Test dosageInstruction.DoseQuantity (RXO.2)
        String hl7message = "MSH|^~\\\\&|||||20210101000000||OMP^O09|MSGID|T|2.6\n"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||I|^^^Toronto^^5642 Hilly Av||||||770542^Doctor^Consulting^Jr||||||||||Visit_111|||||||||||||||||||||||||20210101000000\n"
                + "ORC|OP|1234|1234|0827|||||20210101000000||||||20210101000000||||||ORDERING FAC NAME||(9\n"
                + "RXO|00054418425^Dexamethasone 4 MG Oral Tablet^NDC^^^^^^dexamethasone (DECADRON) 4 MG TABS|100||CC|" // RXO.2 and 4 are on this line (100||CC)
                // split and concatenate RXO for easier understanding
                + "|^Take 1 tablet by mouth every 6 (six) hours.|||G||4|tablet^tablet|0|222^JONES^JON^E.|||day||||6|PC^^http://unitsofmeasure.org||^DECADRON\n"
                + "RXR|PO^Oral\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Quantity doseQuantity  = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getDoseQuantity();

        // dosageInstruction.doseAndRate.doseQuantity RXO.2
        assertThat(doseQuantity.getValue().toString()).isEqualTo("100.0"); //RXO.2
        assertThat(doseQuantity.getUnit()).isEqualTo("CC"); //RXO.4.1
        assertThat(doseQuantity.getSystem()).isEqualTo("http://unitsofmeasure.org"); //default

    }

    @Test
    void dosageInstructionTestDoseQuantityRXE() {
        // Test dosageInstruction.DoseQuantity (RXE.3)
        String hl7message = "MSH|^~\\&||||||S1|RDE^O11||T|2.6|||||||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "ORC|NW|||||E|10^BID^D4^^^R||20180622230000||||||||||||||||||||I\n"
                + "RXE|^Q24H&0600^^20210330144208^^ROU|DUONEB3INH^3 ML PLAS CONT : IPRATROPIUM-ALBUTEROL 0.5-2.5 (3) MG/3ML IN SOLN^ADS^^^^^^ipratropium-albuterol (DUONEB) nebulizer solution 3 mL|3||mL||||"// RXE.3 and 5 are on this line ("|3||mL|")
                // split and concatenate RXE for easier understanding
                + "|1|PC^^measureofunits||2213^ORDERING^PROVIDER|||||day|||333^Take 1 tablet by mouth every 6 (six) hours.||7|PC|||Wheezing^Wheezing^PRN||||^DUONEB|20180622230000||||||7|7|7\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> medicationRequestList = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        // Confirm that one medicationRequest was created.
        assertThat(medicationRequestList).hasSize(1);
        MedicationRequest medicationRequest = ResourceUtils.getResourceMedicationRequest(medicationRequestList.get(0),
                ResourceUtils.context);

        Quantity doseQuantity  = medicationRequest.getDosageInstructionFirstRep().getDoseAndRateFirstRep().getDoseQuantity();

        // dosageInstruction.doseAndRate.doseQuantity RXE.3
        assertThat(doseQuantity.getValue().toString()).isEqualTo("3.0"); //RXE.3
        assertThat(doseQuantity.getUnit()).isEqualTo("mL"); //RXE.5.1
        assertThat(doseQuantity.getSystem()).isEqualTo("http://unitsofmeasure.org"); //default

    }
}