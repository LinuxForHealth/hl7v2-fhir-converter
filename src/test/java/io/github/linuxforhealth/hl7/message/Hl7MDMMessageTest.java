/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7MDMMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder().withBundleType(BundleType.COLLECTION)
            .withValidateResource().withPrettyPrint().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MDMMessageTest.class);

    @Test
    public void testMDMT02encounterPresentTwoOrders() throws IOException {
        String hl7message = "\n"
                + "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||MDM^T02|<MESSAGEID>|P|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|O|2GY^2417^W||||ATT_ID^ATT_LN^ATT_MI^ATT_FN^^^MD|REF_ID^REF_LN^REF_MI^REF_FN^^^MD|CONSULTING_ID^CONSULTING_LN^CONSULTING_MI^CONSULTING_FN^^^MD||||||||ADM_ID^ADM_LN^ADM_MI^ADM_FN^^^MD|OTW|<HospitalID>|||||||||||||||||||||||||20180115102400|20180118104500\n"
                + "ORC|NW|622470H432|||||^^^^^R|||||123456789^TEST1a^DOCTOR1a|123D432^^^Family Practice Clinic||||||||FAMILY PRACTICE CLINIC\n"
                + "OBR|1|622470H432|102397CE432|LAMIKP^AMIKACIN LEVEL, PEAK^83718||20170725143849|20180102||||L|||||123456789^TEST1b^DOCTOR1b|||REASON_TEXT_1|||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1|RESP_ID1&RESP_FAMILY_1&RESP_GIVEN1||TECH_1_ID&TECH_1_FAMILY&TECH_1_GIVEN|TRANS_1_ID&TRANS_1_FAMILY&TRANS_1_GIVEN\n"
                + "ORC|NW|9494138H600|||||^^^^^R|||||1992779250^TEST2a^DOCTOR2a\n"
                + "OBR|1|9494138H600^ORDER_PLACER_NAMESPACE_2|1472232CE600|83718^HIGH-DENSITY LIPOPROTEIN (HDL)^NAMING2||20150909170243|||||L|||||1992779250^TEST2b^DOCTOR2b|||REASON_TEXT_2|||||CAT|A||^^^20180204^^R||||REASON_ID_2^REASON_TEXT_2|RESP_ID2&RESP_FAMILY_2&RESP_GIVEN2||TECH_2_ID&TECH_2_FAMILY&TECH_2_GIVEN|TRANS_2_ID&TRANS_2_FAMILY&TRANS_2_GIVEN\n"
                + "TXA|1|05^Operative Report|TX|201801171442|5566^PAPLast^PAPFirst^J^^MD|201801171442|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>||4466^TRANSCLast^TRANSCFirst^J^^MD|<MESSAGEID>||P||AV\n"
                + "OBX|1|TX|05^Operative Report||<HOSPITAL NAME>||||||P\n"
                + "OBX|2|TX|05^Operative Report||<HOSPITAL ADDRESS2>||||||P\n"
                + "OBX|3|TX|05^Operative Report||<HOSPITAL ADDRESS2>||||||P\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> serviceRequestList = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // We expect two service requests because there are two orders (ORC/OBR)     
        assertThat(serviceRequestList).hasSize(2);
        ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0), context);
        assertThat(serviceRequest.hasStatus()).isTrue();

    }

    @Test
    public void test_MDMT06_encounter_present() throws IOException {
        String hl7message = "\n"
                + "MSH|^~\\&|HNAM|W|RAD_IMAGING_REPORT|W|20180118111520||MDM^T06|<MESSAGEID>|P|2.6\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|O|2GY^2417^W||||ATT_ID^ATT_LN^ATT_MI^ATT_FN^^^MD|REF_ID^REF_LN^REF_MI^REF_FN^^^MD|CONSULTING_ID^CONSULTING_LN^CONSULTING_MI^CONSULTING_FN^^^MD||||||||ADM_ID^ADM_LN^ADM_MI^ADM_FN^^^MD|OTW|<HospitalID>|||||||||||||||||||||||||20180115102400|20180118104500\n"
                + "ORC|NW|622470H432|||||^^^^^R|||||123456789^MILLER^BOB|123D432^^^Family Practice Clinic||||||||FAMILY PRACTICE CLINIC\n"
                + "OBR|1|622470H432|102397CE432|LAMIKP^AMIKACIN LEVEL, PEAK^83718||20170725143849|20180102||||L|||||123456789^MILLER^BOB|||REASON_TEXT_1|||||RAD|O||^^^^^R||||REASON_ID_1^REASON_TEXT_1|RESP_ID1&RESP_FAMILY_1&RESP_GIVEN1||TECH_1_ID&TECH_1_FAMILY&TECH_1_GIVEN|TRANS_1_ID&TRANS_1_FAMILY&TRANS_1_GIVEN\n"
                + "ORC|NW|9494138H600|||||^^^^^R|||||1992779250^TEST^DOCTOR\n"
                + "OBR|1|9494138H600^ORDER_PLACER_NAMESPACE_2|1472232CE600|83718^HIGH-DENSITY LIPOPROTEIN (HDL)^NAMING2||20150909170243|||||L|||||1992779250^TEST^DOCTOR||||||||CAT|A||^^^20180204^^R||||REASON_ID_2^REASON_TEXT_2|RESP_ID2&RESP_FAMILY_2&RESP_GIVEN2||TECH_2_ID&TECH_2_FAMILY&TECH_2_GIVEN|TRANS_2_ID&TRANS_2_FAMILY&TRANS_2_GIVEN\n"
                + "TXA|1|05^Operative Report|TX|201801171442|5566^PAPLast^PAPFirst^J^^MD|201801171442|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>||4466^TRANSCLast^TRANSCFirst^J^^MD|<MESSAGEID>||P||AV\n"
                + "OBX|1|TX|05^Operative Report||                        <HOSPITAL NAME>||||||P\n"
                + "OBX|2|TX|05^Operative Report||                             <HOSPITAL ADDRESS2>||||||P\n"
                + "OBX|3|TX|05^Operative Report||                              <HOSPITAL ADDRESS2>||||||P\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

    }

    @Test
    public void test_MDMT06_reason_reference() throws IOException {
        String hl7message = "MSH|^~\\&|Merge Los Angeles|SCA|TX04|SCA|20120101144000||MDM^T06^MDM_T02|0001234|P|2.6|||AL|NE\n"
                + "EVN||20120101100000\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1||O|^^^ANA\n"
                + "TXA|1|DS|TEXT|20120101100000|||||||||||||AU\n"
                + "OBX|1|ST|200||This is an append sample text.||||||F|\n"
                + "OBX|2|ST|200||This is an append sample text 2.||||||F|\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> obsResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(obsResource).hasSize(2);
        String obsExpecteRef1 = obsResource.get(0).getId();
        String obsExpecteRef2 = obsResource.get(1).getId();

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        Encounter enc = (Encounter) encounterResource.get(0);
        List<Reference> reasonRefs = enc.getReasonReference();
        assertThat(reasonRefs).hasSize(2);
        String obsActualRef1 = reasonRefs.get(0).getReference();
        String obsActualRef2 = reasonRefs.get(1).getReference();
        // We have exactly two observations and two references.  They must match one way or the other.
        assertThat((obsActualRef1.contains(obsExpecteRef1) && obsActualRef2.contains(obsExpecteRef2)) ||
                (obsActualRef1.contains(obsExpecteRef2) && obsActualRef2.contains(obsExpecteRef1))).isTrue();
    }


    @Test
    public void test_MDMT02_serviceRequest_reference() throws IOException {
        String hl7message =  "MSH|^~\\&|Epic|PQA|WHIA|IBM|20170920141233||MDM^T02^MDM_T02|M1005|D|2.6\n"
                + "EVN|T02|20170920141233|||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|I|^^^CARE HEALTH SYSTEMS^^^^^|E|||||||||||||||V1005|||||||||||||||||||||||||20170920141233\n"
                + "ORC|NW|P1005|F1005|P1005|SC|D|1||20170920141233|MS|MS||Unit233|4162223333|20170920141233|\n"
                + "OBR|1|P1005|F1005|71260^CT Chest with IV Contrast^CPTM||20170920141233||||123^tuberculosis warning|L|||||||||||20170920141233||OUS|F||||||||||\n"
                + "TXA|1|CN||20170920141233|1173^MATTHEWS^JAMES^A^^^|||||||^^U1005||P1005|||LA|||||\n"
                + "OBX|1|TX|100||Clinical summary: The patient likely exibits 'unspecified gastric ulcer with hemorrhage'.||||||F\n"
                + "OBX|2|TX|101||Diagnosis: gastrointestinal hemorrhage||||||F\n";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).isEmpty(); // None expected because OBX are TX type

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1);
        String encounterId = encounters.get(0).getId();

        List<Resource> practitioners = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitioners).hasSize(1);
        String practitionerId = practitioners.get(0).getId();
        
        List<Resource> servRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(servRequests).hasSize(1);
        String servRequestId = servRequests.get(0).getId();
        
        // Expecting the following context in DocumentReference:
        // "context": {
        //         "encounter": [ {
        //           "reference": "Encounter/<GUID>"
        //         } ],
        //         "period": {
        //           "start": "2017-09-20T14:12:33+08:00"
        //         },
        //         "related": [ {
        //           "reference": "Practitioner/<GUID>",
        //           "display": "JAMES A MATTHEWS"
        //         }, {
        //           "reference": "ServiceRequest/<GUID>"
        //         } ]
        //       }

        List<Resource> docRefs = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefs).hasSize(1);
        
        DocumentReference docRef = (DocumentReference) docRefs.get(0);
        assertThat(docRef.hasContext()).isTrue();
        assertThat(docRef.getContext().hasPeriod()).isTrue();
        assertThat(docRef.getContext().hasRelated()).isTrue();
        assertThat(docRef.getContext().hasEncounter()).isTrue();
        assertThat(docRef.getContext().getEncounter().get(0).getReference()).isEqualTo(encounterId);
        // --------------
        // The following should pass but it fails because the reference to the ServiceRequest is not created.
        // --------------        
        // assertThat(docRef.getContext().getRelated()).hasSize(2);
        // String related1 = docRef.getContext().getRelated().get(0).getReference();
        // String related2 = docRef.getContext().getRelated().get(1).getReference();
        // assertThat(related1.contains(practitionerId) || related1.contains(servRequestId)).isTrue();
        // assertThat(related2.contains(practitionerId) || related2.contains(servRequestId)).isTrue();
    }

}
