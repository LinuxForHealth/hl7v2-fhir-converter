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
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/*** Tests the MRG segment ***/

public class HL7MergeFHIRConversionTest {

    // Test ADT_A34 with one MRG segment (the most it supports).
    @Test
    public void validateHappyPathADT_A34WithMRG() {

        String hl7message = "MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|||ADT^A34||P|2.3||||\r"
        + "EVN|A40|20110613122406637||01\r"
        + "PID|1||123^^^^MR||||||||||||||||||||||||||||||||||||\r"
        + "MRG|456||||||\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // There should be 2 - One for the PID segment and one for the MRG segment
        assertThat(patientResource).hasSize(2);

        // Get first patient and associated indentifiers and id.
        Resource patientOne = patientResource.get(0);
        String patientOneId = patientOne.getId();
        Property patientOneIdentifierProperty = patientOne.getNamedProperty("identifier");            
        List<Base> patientOneIdentifierList = patientOneIdentifierProperty.getValues();;
        
        // Get second patient and associated indentifiers and id.
        Resource patientTwo = patientResource.get(1);
        String patientTwoId = patientTwo.getId();
        Property patientTwoIdentifierProperty = patientTwo.getNamedProperty("identifier");            
        List<Base> patientTwoIdentifierList = patientTwoIdentifierProperty.getValues();

        /*-----------Verify Patient One-----------*/

        // Verify patient one's identifier is set correctly (this is the patient build from the PID segment)
        //
        // "identifier": [ {
        //     "type": {
        //       "coding": [ {
        //         "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
        //         "code": "MR",
        //         "display": "Medical record number"
        //       } ]
        //     },
        //     "value": "123"
        //   }, {
        //     "use": "old",
        //     "type": {
        //       "coding": [ {
        //         "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
        //         "code": "MR",
        //         "display": "Medical Record"
        //       } ]
        //     },
        //     "value": "456"
        //   } ]

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(0), "value")).isEqualTo("123");
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(1), "value")).isEqualTo("456");

        // Verify the second identifier is marked as old
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(1), "use")).isEqualTo("Enumeration[old]");

        // Verify first patient has these fields
        //
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

        // Verify link.other.reference references the MRG (2nd) patient
        Base linkOne = ResourceUtils.getValue(patientOne, "link");
        assertThat(ResourceUtils.getValueAsString(linkOne, "type")).isEqualTo("Enumeration[replaces]");
        Base patientOneRef = ResourceUtils.getValue(linkOne, "other");
        assertThat(ResourceUtils.getValueAsString(patientOneRef, "reference")).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

        // Verify patient two's identifier is set correctly (this is the patient build from the MRG segment)
        //
        // "identifier": [ {
        //     "use": "old",
        //     "type": {
        //       "coding": [ {
        //         "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
        //         "code": "MR",
        //         "display": "Medical Record"
        //       } ]
        //     },
        //     "value": "456"
        //   } ]

        // Verify patient two has only 1 identifier
        assertThat(patientTwoIdentifierList).hasSize(1);

        // Verify the patient two identifier value
        assertThat(ResourceUtils.getValueAsString(patientTwoIdentifierList.get(0), "value")).isEqualTo("456");
        // Verify the patient two identifier value is marked as old
        assertThat(ResourceUtils.getValueAsString(patientTwoIdentifierList.get(0), "use")).isEqualTo("Enumeration[old]");

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

        // We should have link.other.reference to the PID (1st) patient
        Base linkTwo = ResourceUtils.getValue(patientTwo, "link");
        assertThat(ResourceUtils.getValueAsString(linkTwo, "type")).isEqualTo("Enumeration[replaced-by]");
        Base patientTwoRef = ResourceUtils.getValue(linkTwo, "other");
        assertThat(ResourceUtils.getValueAsString(patientTwoRef, "reference")).isEqualTo(patientOneId);
    
    }

    // Test ADT_A40 with one MRG segment.
    @Test
    @Disabled  // Disabled until bug 684 is fixed
    public void validateHappyPathADT_A40WithMRG() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.5\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR||\r"
        + "MRG|MR2^^^XYZ\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // One for the PID segment and one for the MRG segment
        assertThat(patientResource).hasSize(2);

        // Get first patient and associated indentifiers and id.
        Resource patientOne = patientResource.get(0);
        String patientOneId = patientOne.getId();
        Property patientOneIdentifierProperty = patientOne.getNamedProperty("identifier");            
        List<Base> patientOneIdentifierList = patientOneIdentifierProperty.getValues();;
        
        // Get second patient and associated indentifiers and id.
        Resource patientTwo = patientResource.get(1);
        String patientTwoId = patientTwo.getId();
        Property patientTwoIdentifierProperty = patientTwo.getNamedProperty("identifier");            
        List<Base> patientTwoIdentifierList = patientTwoIdentifierProperty.getValues();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(0), "value")).isEqualTo("MR1");
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(1), "value")).isEqualTo("MR2");

        // Verify the second identifier is marked as old
        assertThat(ResourceUtils.getValueAsString(patientOneIdentifierList.get(1), "use")).isEqualTo("Enumeration[old]");

        // The first patient should be active.
        assertThat(ResourceUtils.getValueAsString(patientOne, "active")).isEqualTo("BooleanType[true]");

        // We should have link.other.reference to the MRG (2nd) patient
        Base linkOne = ResourceUtils.getValue(patientOne, "link");
        assertThat(ResourceUtils.getValueAsString(linkOne, "type")).isEqualTo("Enumeration[replaces]");
        Base patientOneRef = ResourceUtils.getValue(linkOne, "other");
        assertThat(ResourceUtils.getValueAsString(patientOneRef, "reference")).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

       // Verify patient two has only 1 identifier
       assertThat(patientTwoIdentifierList).hasSize(1);

       // Verify the patient two identifier value
       assertThat(ResourceUtils.getValueAsString(patientTwoIdentifierList.get(0), "value")).isEqualTo("MR2");
       // Verify the patient two identifier value is marked as old
       assertThat(ResourceUtils.getValueAsString(patientTwoIdentifierList.get(0), "use")).isEqualTo("Enumeration[old]");

       //The second patient should NOT be active.
       assertThat(ResourceUtils.getValueAsString(patientTwo, "active")).isEqualTo("BooleanType[false]");

       // We should have link.other.reference to the PID (1st) patient
       Base linkTwo = ResourceUtils.getValue(patientTwo, "link");
       assertThat(ResourceUtils.getValueAsString(linkTwo, "type")).isEqualTo("Enumeration[replaced-by]");
       Base patientTwoRef = ResourceUtils.getValue(linkTwo, "other");
       assertThat(ResourceUtils.getValueAsString(patientTwoRef, "reference")).isEqualTo(patientOneId);

    }

    // Tests ADT_A40 message with 2 MRG segments.
    @Test
    @Disabled  // Disabled until bug 684 is fixed
    public void validateTwoMRGs() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.5\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR|||||||||||||||\r"
        + "MRG|MR2^^^XYZ||\r"
        + "PID|||MR3^^^XYZ|||||||||||||||\r"
        + "MRG|MR4^^^XYZ||\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // One for each PID segment and one for each MRG segment
        assertThat(patientResource).hasSize(4);

    }

  }
