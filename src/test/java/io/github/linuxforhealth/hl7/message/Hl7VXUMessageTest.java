/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;


import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7VXUMessageTest {
    private static FHIRContext context = new FHIRContext(true, false);
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7ORUMessageTest.class);
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
        .withBundleType(BundleType.COLLECTION)
        .withValidateResource()
        .withPrettyPrint()
        .build();

    @Test
    public void test_VXU_with_minimum_segments() throws IOException {
  	    String hl7message =
  		    "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
  		    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(1);
    }

    @Test
    public void test_VXU_with_patient_group_that_has_minimum_segments() throws IOException {
  	    String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
  		    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
  		    + "PV1|1|R||||||||||||||||||V01^20120901041038\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(2);
    }

    @Test
    public void test_VXU_with_patient_group_that_has_all_segments() throws IOException {
  	    String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
  		    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PD1|||||||||||01|N||||A\r"
  		    + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(2);
    }

    @Test
    public void test_VXU_with_full_patient_group_and_minimum_order_group() throws IOException {
  	    String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
  		    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PD1|||||||||||01|N||||A\r"
  		    + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> immunizationResource = e.stream()
            .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunizationResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
    }

    @Test
    public void test_VXU_with_minimum_patient_group_plus_order_group_without_OBX() throws IOException {
        String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "RXR|C28161^Intramuscular^NCIT||||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> immunizationResource = e.stream()
            .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunizationResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
  }

    @Test
    public void test_VXU_with_minimum_patient_group_plus_order_group_with_OBX_but_no_observations() throws IOException {
        String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "RXR|C28161^Intramuscular^NCIT||||\r"
            + "OBX|1|CE|30963-3^ VACCINE FUNDING SOURCE^LN|1|VXC2^STATE FUNDS^HL70396||||||F|||20120901041038\r"
            + "OBX|2|CE|64994-7^Vaccine funding program eligibility category^LN|1|V01^Not VFC^HL70064||||||F|||20140701041038\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> immunizationResource = e.stream()
            .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunizationResource).hasSize(1);

        // No Observations should be created because OBX3.1 is not 31044-1

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
}

    @Test
    public void test_VXU_with_minimum_patient_group_plus_order_group_with_OBX_with_observations() throws IOException {
        String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "RXR|C28161^Intramuscular^NCIT||||\r"
            + "OBX|1|CE|31044-1^Immunization reaction^LN|1|VXC12^fever of >40.5C within 48 hrs.^CDCPHINVS||||||F|||20120901041038\r"
            + "OBX|2|CE|31044-1^Immunization reaction^LN|1|VXC14^Rash within 14 days of dose^CDCPHINVS||||||F|||20140701041038\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> immunizationResource = e.stream()
            .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunizationResource).hasSize(1);

        // TODO: When implemented, expect 2 Observations for the OBX with OBX.3 of 31044-1
        //   List<Resource> observationResource = e.stream()
        //       .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
        //       .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //   assertThat(observationResource).hasSize(2);
        
        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);
    }

    @Test
    public void test_VXU_with_minimum_patient_group_plus_multiple_order_groups() throws IOException {
        String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "OBX|1|CE|31044-1^Immunization reaction^LN|1|VXC12^fever of >40.5C within 48 hrs.^CDCPHINVS||||||F|||20120901041038\r"
            + "OBX|2|CE|31044-1^Immunization reaction^LN|1|VXC14^Rash within 14 days of dose^CDCPHINVS||||||F|||20140701041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "RXR|C28161^Intramuscular^NCIT||||\r"
            + "OBX|1|CE|31044-1^Immunization reaction^LN|1|39579001^Anaphylaxis (disorder)^SCT||||||F|||20120901041038\r"
            + "OBX|2|TS|29768-9^DATE VACCINE INFORMATION STATEMENT PUBLISHED^LN|1|20010711||||||F|||20120720101321\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.info("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Validate that the correct resources are created
        List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> immunizationResource = e.stream()
            .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immunizationResource).hasSize(2);

        // TODO: When implemented, expect 3 Observations for the OBX with OBX.3 of 31044-1
        //   List<Resource> observationResource = e.stream()
        //       .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
        //       .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //   assertThat(observationResource).hasSize(3);

        // TODO: When implemented, verify that the correct Observations are associated with the correct Immunizations.

        
        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(4);
    }
}
