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

public class HL7MergeFHIRConversionTest {

    // TBD
    @Test
    public void validateMerge() {

        String hl7message = "MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|20110613122406637||ADT^A01|1965403220110613122406637|P|2.3||||\r"
        + "EVN|A40|20110613122406637||01\r"
        + "PID|1||1765431^^^^MR||McTavish^Henry^J||19700101|M|||117 W Main St^^Fort Wayne^IN^46808||(260)555-1234^^^^^|||M||1117112|999999999||||||||||||||||||||\r"
        + "MRG|1765475||||||\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResource = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(2);

        // Get first patient
        Resource patientOne = patientResource.get(0);

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

        assertThat(ResourceUtils.getValueAsString(patientOne, "active")).isEqualTo("true");

        Base linkOne = ResourceUtils.getValue(patientOne, "link");
        assertThat(ResourceUtils.getValueAsString(linkOne, "type")).isEqualTo("replaces");
        Base patientOneRef = ResourceUtils.getValue(patientOne, "other");
        assertThat(ResourceUtils.getValueAsString(patientOneRef, "reference").substring(0, 8)).isEqualTo("Patient/");

        // Get second patient
        Resource patientTwo = patientResource.get(1);

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
        assertThat(ResourceUtils.getValueAsString(patientTwo, "active")).isEqualTo("false");

        Base linkTwo = ResourceUtils.getValue(patientTwo, "link");
        assertThat(ResourceUtils.getValueAsString(linkTwo, "type")).isEqualTo("replaced-by");
        Base patientTwoRef = ResourceUtils.getValue(patientTwo, "other");
        assertThat(ResourceUtils.getValueAsString(patientTwoRef, "reference").substring(0, 8)).isEqualTo("Patient/");
    
    }

    // TODO: Add tests for this as well
    // String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A01|00000003|P|2.5\r"
    // + "EVN|A40|200301051530\r"
    // + "PID|||MR1^^^XYZ^MR||MAIDENNAME^EVE\r"
    // + "MRG|MR2^^^XYZ\r";

    // String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A01|00000003|P|2.5\r"
    // + "EVN|A40|200301051530\r"
    // + "PID|||MR1^^^XYZ^MR||EVERYWOMAN^EVE|||||||||||||ACCT3\r"
    // + "MRG|MR2^^^XYZ||ACCT1\r"
    // + "PID|||MR1^^^XYZ||EVERYWOMAN^EVE|||||||||||||ACCT4\r"
    // + "MRG|MR2^^^XYZ||ACCT2\r";

  }
