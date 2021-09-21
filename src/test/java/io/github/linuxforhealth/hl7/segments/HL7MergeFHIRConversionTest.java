/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
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

        // Convert hl7 message
        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // There should be 2 - One for the PID segment and one for the MRG segment
        assertThat(patientResources).hasSize(2);

        // Get first patient and associated indentifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();
        
        // Get second patient and associated indentifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();         
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

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
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("123");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("456");

        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

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
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

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
        assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("456");
        // Verify the patient two identifier value is marked as old
        assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

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
        assertThat(patientTwo.getActive()).isFalse();

        // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
        PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
        assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
        assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);;
    
    }

    // Test ADT_A40 with one MRG segment.
    @Test
    public void validateHappyPathADT_A40WithMRG() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR||\r"
        + "MRG|MR2^^^XYZ\r";

        // Convert hl7 message
        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // There should be 2 - One for the PID segment and one for the MRG segment
        assertThat(patientResources).hasSize(2);

        // Get first patient and associated indentifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();
        
        // Get second patient and associated indentifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();         
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("MR1");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("MR2");

        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // The first patient should be active.
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

       // Verify patient two has only 1 identifier
       assertThat(patientTwoIdentifierList).hasSize(1);

       // Verify the patient two identifier value
       assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("MR2");
       // Verify the patient two identifier value is marked as old
       assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

       //The second patient should NOT be active.
       assertThat(patientTwo.getActive()).isFalse();

       // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
       PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
       assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
       assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);

    }

    // Tests ADT_A40 message with 2 MRG segments.
    @Test
    public void validateTwoMRGs() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR|||||||||||||||\r"
        + "MRG|MR2^^^XYZ||\r"
        + "PID|||MR3^^^XYZ|||||||||||||||\r"
        + "MRG|MR4^^^XYZ||\r";

        // Convert hl7 message
        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // There should be 4 - One for each PID segment and one for each MRG segment
        assertThat(patientResources).hasSize(4);

        // Get first patient and associated indentifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();
        
        // Get second patient and associated indentifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        // Get third patient and associated indentifiers and id.
        Patient patientThree = PatientUtils.getPatientFromResource(patientResources.get(2));
        String patientThreeId = patientThree.getId();         
        List<Identifier> patientThreeIdentifierList = patientThree.getIdentifier();

        // Get fourth patient and associated indentifiers and id.
        Patient patientFour = PatientUtils.getPatientFromResource(patientResources.get(3));
        String patientFourId = patientFour.getId();         
        List<Identifier> patientFourIdentifierList = patientFour.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("MR1");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("MR2");

        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // The first patient should be active.
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

       // Verify patient two has only 1 identifier
       assertThat(patientTwoIdentifierList).hasSize(1);

       // Verify the patient two identifier value
       assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("MR2");
       // Verify the patient two identifier value is marked as old
       assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

       //The second patient should NOT be active.
       assertThat(patientTwo.getActive()).isFalse();

       // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
       PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
       assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
       assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);

        /*-----------Verify Patient Three-----------*/

        // Verify the patient has two identifiers
        assertThat(patientThreeIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(patientThreeIdentifierList.get(0).getValue()).isEqualTo("MR3");
        assertThat(patientThreeIdentifierList.get(1).getValue()).isEqualTo("MR4");

        // Verify the second identifier is marked as old
        assertThat(patientThreeIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // The first patient should be active.
        assertThat(patientThree.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkThree = patientThree.getLink().get(0);
        assertThat(linkThree.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkThree.getOther().getReference()).isEqualTo(patientFourId);

        /*-----------Verify Patient Four-----------*/

       // Verify patient two has only 1 identifier
       assertThat(patientFourIdentifierList).hasSize(1);

       // Verify the patient two identifier value
       assertThat(patientFourIdentifierList.get(0).getValue()).isEqualTo("MR4");
       // Verify the patient two identifier value is marked as old
       assertThat(patientFourIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

       //The second patient should NOT be active.
       assertThat(patientFour.getActive()).isFalse();

       // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
       PatientLinkComponent linkFour = patientFour.getLink().get(0);
       assertThat(linkFour.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
       assertThat(linkFour.getOther().getReference()).isEqualTo(patientThreeId);
       
    }

    // Tests MRG and PID segments that have a system / namespace value in the identifier.
    @Test
    public void validateMRGWithIdentifierSystems() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40|00000003|P|2.5\r"
        + "EVN|A40|200301051530\r"
        + "PID|||MR1^^^XYZ^MR||MAIDENNAME^EVE\r"
        + "MRG|MR2^^^XYZ\r";

        // Convert hl7 message
        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = e.stream()
        .filter(v -> ResourceType.Patient== v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // There should be 4 - One for each PID segment and one for each MRG segment
        assertThat(patientResources).hasSize(2);

        // Get first patient and associated indentifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();
        
        // Get second patient and associated indentifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();         
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("MR1");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("MR2");

        // Verify the system is set correctly for both identifiers
        assertThat(patientOneIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");
        assertThat(patientOneIdentifierList.get(1).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // The first patient should be active.
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

        // Verify patient two has only 1 identifier
        assertThat(patientTwoIdentifierList).hasSize(1);

        // Verify the patient two identifier value
        assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("MR2");

        // Verify the identifier system is set correctly.
        assertThat(patientTwoIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the patient two identifier value is marked as old
        assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        //The second patient should NOT be active.
        assertThat(patientTwo.getActive()).isFalse();

        // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
        PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
        assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
        assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);
    }

  }
