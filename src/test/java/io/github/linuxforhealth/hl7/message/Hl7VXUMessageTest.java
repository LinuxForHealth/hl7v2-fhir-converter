/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;


import java.io.IOException;
import java.util.List;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7VXUMessageTest.class);
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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);;

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(1);
    }

    @Test
    public void test_VXU_with_patient_group_that_has_minimum_segments() throws IOException {
  	    String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
  		    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
  		    + "PV1|1|R||||||||||||||||||V01^20120901041038\r";

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);

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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);

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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);

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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);

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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);

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

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);
        List<Resource> obs = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(obs).hasSize(2);
        // verify that the correct Observations are associated with the correct Immunizations.

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(5);
    }

    @Test
    public void test_VXU_with_minimum_patient_group_plus_multiple_order_groups() throws IOException {
        String hl7message = 
            "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
            + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "OBX|1|CWE|31044-1^Immunization reaction^LN|1|VXC12^fever of >40.5C within 48 hrs.^CDCPHINVS||||||F|||20120901041038\r"
            + "OBX|2|CWE|31044-1^Immunization reaction^LN|1|VXC14^Rash within 14 days of dose^CDCPHINVS||||||F|||20140701041038\r"
            + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
            + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||||14509|||||||||CP||\r"
            + "RXR|C28161^Intramuscular^NCIT||||\r"
            + "OBX|1|CWE|31044-1^Immunization reaction^LN|1|39579001^Anaphylaxis (disorder)^SCT||||||F|||20120901041038\r"
            + "OBX|2|TS|29768-9^DATE VACCINE INFORMATION STATEMENT PUBLISHED^LN|1|20010711||||||F|||20120720101321\r";

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
        // Validate that the correct resources are created
        List<Resource> patient = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patient).hasSize(1);
        List<Resource> enc = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(enc).hasSize(1);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(2);
        List<Resource> obs = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(obs).hasSize(3);
        // verify that the correct Observations are associated with the correct Immunizations.

        // The first two reaction details come from the first immunization resource.
        // Too confirm both observations match we compare the IDs of the reaction detail and the actual observation resource id
        // The first two sets of assertions are from the first RXA segment
        Immunization resource = ResourceUtils.getResourceImmunization(immu.get(0), ResourceUtils.context);
        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);

        String reactionDetail = resource.getReactionFirstRep().getDetail().getReference();
        Observation obsResource = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);

        assertThat(obsResource.getId()).isEqualTo(reactionDetail);
        assertThat(obsResource.getCode().getCodingFirstRep().getDisplay()).isEqualTo("fever of >40.5C within 48 hrs.");
        assertThat(obsResource.getCode().getCodingFirstRep().getCode()).isEqualTo("VXC12");
        assertThat(obsResource.getCode().getCodingFirstRep().getSystem()).isEqualTo("urn:id:CDCPHINVS");
        assertThat(obsResource.getCode().getText()).isEqualTo("fever of >40.5C within 48 hrs.");
        assertThat(obsResource.getIdentifierFirstRep().getValue()).isEqualTo("4242546-VXC12-CDCPHINVS");
        assertThat(obsResource.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");        // Expecting only the above resources, no extras!

        reactionDetail = resource.getReaction().get(1).getDetail().getReference();
        obsResource = ResourceUtils.getResourceObservation(observations.get(1), ResourceUtils.context);

        assertThat(obsResource.getId()).isEqualTo(reactionDetail);
        assertThat(obsResource.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Rash within 14 days of dose");
        assertThat(obsResource.getCode().getCodingFirstRep().getCode()).isEqualTo("VXC14");
        assertThat(obsResource.getCode().getCodingFirstRep().getSystem()).isEqualTo("urn:id:CDCPHINVS");
        assertThat(obsResource.getCode().getText()).isEqualTo("Rash within 14 days of dose");
        assertThat(obsResource.getIdentifierFirstRep().getValue()).isEqualTo("4242546-VXC14-CDCPHINVS");
        assertThat(obsResource.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");
        assertThat(e.size()).isEqualTo(7);

        // The second set of assertions are from the second RXA segment
        resource = ResourceUtils.getResourceImmunization(immu.get(1), ResourceUtils.context);
        reactionDetail = resource.getReaction().get(0).getDetail().getReference();
        obsResource = ResourceUtils.getResourceObservation(observations.get(2), ResourceUtils.context);

        assertThat(obsResource.getId()).isEqualTo(reactionDetail);
        assertThat(obsResource.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Anaphylaxis (disorder)");
        assertThat(obsResource.getCode().getCodingFirstRep().getCode()).isEqualTo("39579001");
        assertThat(obsResource.getCode().getCodingFirstRep().getSystem()).isEqualTo("http://snomed.info/sct");
        assertThat(obsResource.getCode().getText()).isEqualTo("Anaphylaxis (disorder)");
        assertThat(obsResource.getIdentifierFirstRep().getValue()).isEqualTo("4242546-39579001-SCT");
        assertThat(obsResource.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");
    }
}
