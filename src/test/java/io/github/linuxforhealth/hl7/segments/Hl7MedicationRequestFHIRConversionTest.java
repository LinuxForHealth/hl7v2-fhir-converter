/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

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

        //ORC.1 = SC -> Expected medication status = UNKNWON
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


}
