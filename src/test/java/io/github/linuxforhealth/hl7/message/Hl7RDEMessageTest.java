/*
 * (C) Copyright IBM Corp. 2021
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
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class Hl7RDEMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder().withBundleType(BundleType.COLLECTION)
            .withValidateResource().withPrettyPrint().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7RDEMessageTest.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    // In the HL7 spec RXR is required for these messages, however, we can handle having no RXR.
    public void test_RDE_medRequest_patient_present_withoutRXR(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_encounter_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_encounter_withExtraSegments_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_encounter_allergy_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> allergyIntoleranceResource = e.stream()
                .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(allergyIntoleranceResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_observation_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> observationResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_encounter_observation_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> observationResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r",
            "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\r"
    })
    public void test_RDE_medRequest_patient_encounter_allergy_observation_present(String msh) throws IOException {
        String hl7message = msh
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|||||||||||||||||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r"
                + "ORC|RE|||3200|||||20210407191342||2799^BY^VERIFIED||||20210407191342||||||ORDERING FAC NAME||||||||I\r"
                + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||\r"
                + "RXR|IM\r"
                + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> medicationRequestResource = e.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medicationRequestResource).hasSize(1);

        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> allergyIntoleranceResource = e.stream()
                .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(allergyIntoleranceResource).hasSize(1);

        List<Resource> observationResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(observationResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(5);
    }

}