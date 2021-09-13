/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/*** Tests the MRG segment ***/

public class HL7MergeFHIRConversionTest {

    // Test ADT_A34 with one MRG segment (the most it supports).
    // TODO: When the internal bug #684 is fixed: convert this message into an ADT_A34 message.
    @Test
    public void validateHappyPathADT_A34WithMRG() {

        String hl7message = "MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|||ADT^A01||P|2.3||||\r"
        + "EVN|A40|20110613122406637||01\r"
        + "PID|1||123^^^^MR||||||||||||||||||||||||||||||||||||\r"
        + "MRG|456||||||\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // One for the PID segment and one for the MRG segment
        assertThat(patientResource).hasSize(2);

        // Get first patient and its id.
        Resource patientOne = patientResource.get(0);
        String patientOneId = patientOne.getId();
        
        // Get second patient and its id.
        Resource patientTwo = patientResource.get(1);
        String patientTwoId = patientTwo.getId();

        // Verify first patient has these fields
        //  "active": true,
        //  "link": [
        //    {
        //      "other": {
        //        "reference": "Patient/expiringID-MRG"
        //      },
        //      "type": "replaces"
        //    }
        //  ]

        // The first patient should be active.
        assertThat(ResourceUtils.getValueAsString(patientOne, "active")).isEqualTo("BooleanType[true]");

        // We should have link.other.reference to the MRG (2nd) patient
        Base linkOne = ResourceUtils.getValue(patientOne, "link");
        assertThat(ResourceUtils.getValueAsString(linkOne, "type")).isEqualTo("Enumeration[replaces]");
        Base patientOneRef = ResourceUtils.getValue(linkOne, "other");
        assertThat(ResourceUtils.getValueAsString(patientOneRef, "reference")).isEqualTo(patientTwoId);

        // Verify second patient has these fields.
        //  "active": false,
        //  "link": [
        //    {
        //      "other": {
        //        "reference": "Patient/survivingID"
        //      },
        //      "type": "replaced-by"
        //    }
        //  ]

        //The second patient should NOT be active.
        assertThat(ResourceUtils.getValueAsString(patientTwo, "active")).isEqualTo("BooleanType[false]");

        // We should have link.other.reference to the PID(1st) patient
        Base linkTwo = ResourceUtils.getValue(patientTwo, "link");
        assertThat(ResourceUtils.getValueAsString(linkTwo, "type")).isEqualTo("Enumeration[replaced-by]");
        Base patientTwoRef = ResourceUtils.getValue(linkTwo, "other");
        assertThat(ResourceUtils.getValueAsString(patientTwoRef, "reference")).isEqualTo(patientOneId);
    
    }

    // TESTs ADT_A40 with one MRG segment.
    // TODO: When the internal bug #684 is fixed: convert this message into an ADT_A40 message.
    @Test
    public void validateHappyPathADT_A40WithMRG() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A01|00000003|P|2.5\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR||MAIDENNAME^EVE\r"
        + "MRG|MR2^^^XYZ\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // One for the PID segment and one for the MRG segment
        assertThat(patientResource).hasSize(2);

        // Get first patient and its id.
        Resource patientOne = patientResource.get(0);
        String patientOneId = patientOne.getId();
        
        // Get second patient and its id.
        Resource patientTwo = patientResource.get(1);
        String patientTwoId = patientTwo.getId();

        // The first patient should be active.
        assertThat(ResourceUtils.getValueAsString(patientOne, "active")).isEqualTo("BooleanType[true]");

        // We should have link.other.reference to the MRG (2nd) patient
        Base linkOne = ResourceUtils.getValue(patientOne, "link");
        assertThat(ResourceUtils.getValueAsString(linkOne, "type")).isEqualTo("Enumeration[replaces]");
        Base patientOneRef = ResourceUtils.getValue(linkOne, "other");
        assertThat(ResourceUtils.getValueAsString(patientOneRef, "reference")).isEqualTo(patientTwoId);

        // Verify second patient has these fields.
        //  "active": false,
        //  "link": [
        //    {
        //      "other": {
        //        "reference": "Patient/survivingID"
        //      },
        //      "type": "replaced-by"
        //    }
        //  ]

        //The second patient should NOT be active.
        assertThat(ResourceUtils.getValueAsString(patientTwo, "active")).isEqualTo("BooleanType[false]");

        // We should have link.other.reference to the PID(1st) patient
        Base linkTwo = ResourceUtils.getValue(patientTwo, "link");
        assertThat(ResourceUtils.getValueAsString(linkTwo, "type")).isEqualTo("Enumeration[replaced-by]");
        Base patientTwoRef = ResourceUtils.getValue(linkTwo, "other");
        assertThat(ResourceUtils.getValueAsString(patientTwoRef, "reference")).isEqualTo(patientOneId);

    }

    // Test ADT_A40 message with 2 MRG segments.
    // TODO: When the internal bug #684 is fixed: convert this message into an ADT_A40 message.
    @Test
    public void validateTwoMRGs() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A01|00000003|P|2.5\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR||EVERYWOMAN^EVE|||||||||||||ACCT1\r"
        + "MRG|MR2^^^XYZ||ACCT1\r"
        + "PID|||MR3^^^XYZ||EVERYWOMAN^EVE|||||||||||||ACCT2\r"
        + "MRG|MR4^^^XYZ||ACCT2\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // One for each PID segment and one for each MRG segment
        assertThat(patientResource).hasSize(4);

        // Get first patient
        Resource patientOne = patientResource.get(0);

    }

  }
