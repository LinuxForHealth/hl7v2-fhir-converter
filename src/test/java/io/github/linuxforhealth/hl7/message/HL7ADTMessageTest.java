/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7ADTMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(HL7ADTMessageTest.class);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
            .withBundleType(BundleType.COLLECTION)
            .withValidateResource()
            .withPrettyPrint()
            .build();


    @Test
    public void test_adta01_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A01|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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
    @Test@Disabled
    public void test_adta02_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A02|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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

    @Test@Disabled
    public void test_adta03_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A03|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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



    @Test@Disabled
    public void test_adta04_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A04|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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


    @Test@Disabled
    public void test_adta08_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A08|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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

    @Test@Disabled
    public void test_adta28_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A28|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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

    @Test@Disabled
    public void test_adta31_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|TestSystem||TestTransformationAgent||20150502090000||ADT^A31|controlID|P|2.6\n"
        		+ "EVN|A01|20150502090000|\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        		+ "NK1|1|Kennedy^Joe|FTH|||+44 201 12345678||\n"
        		+ "PV1||I|INT^0001^02^ACME||||0100^ANDERSON^CARL|0148^SMITH^JAMES||SUR|||||||0148^ANDERSON^CARL|S|VisitNumber^^^ACME|A|||||||||||||||||||ACME|||||20150502090000|\n"
        		+ "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

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

}