/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class MedicationFHIRConverterTest {

    // Tests that we create a practitioner from the RXA segment in hl7 message.
    // This uses the Performer resource (specifically actor)
    @Test
    void practitonerCreatedForRXA() {

        String hl7message = "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "NK1|1|mother^patient|MTH^Mother^HL70063|5 elm st^^boston^MA^01234^^P|781-999-9999^PRN^PH^^1^781^9999999|||||||||||||||||01^No reminder/recall^HL70215\r"
                + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
                + "IN1|1||8|Aetna Inc\r"
                + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
                + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||00^New Immunization^NIP001|NPI001^LastName^ClinicianFirstName^^^^Title^^AssigningAuthority|14509||||L987||MSD^Merck^MVX|||CP||20120901041038\r"
                + "RXR|C28161^Intramuscular^NCIT|LA^Leftarm^HL70163\r"
                + "OBX|1|CE|30963-3^ VACCINE FUNDING SOURCE^LN|1|VXC2^STATE FUNDS^HL70396||||||F|||20120901041038\r"
                + "OBX|2|CE|64994-7^Vaccine funding program eligibility category^LN|1|V01^Not VFC^HL70064||||||F|||20140701041038\r"
                + "OBX|3|TS|29768-9^DATE VACCINE INFORMATION STATEMENT PUBLISHED^LN|1|20010711||||||F|||20120720101321\r"
                + "OBX|4|TS|29769-7^DATE VACCINE INFORMATION STATEMENT PRESENTED^LN|1|19901207||||||F|||20140701041038\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the practitioner from the FHIR bundle.
        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // Verify we have one practitioner
        assertThat(practitionerResource).hasSize(1);

        // Get practitioner Resource
        Resource practitioner = practitionerResource.get(0);

        // Verify name text, family, and given are set correctly.
        Base name = ResourceUtils.getValue(practitioner, "name");
        assertThat(ResourceUtils.getValueAsString(name, "text")).isEqualTo("ClinicianFirstName LastName");
        assertThat(ResourceUtils.getValueAsString(name, "family")).isEqualTo("LastName");
        assertThat(ResourceUtils.getValueAsString(name, "given")).isEqualTo("ClinicianFirstName");

        // Verify asserter identifier is set correctly.
        Base identifier = ResourceUtils.getValue(practitioner, "identifier");
        assertThat(ResourceUtils.getValueAsString(identifier, "value")).isEqualTo("NPI001");

    }

}
