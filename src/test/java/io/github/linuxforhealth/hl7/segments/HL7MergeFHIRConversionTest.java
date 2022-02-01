/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/*** Tests the MRG segment ***/

class HL7MergeFHIRConversionTest {

    // Test ADT_A34 with one MRG segment (the most it supports).
    @Test
    void validateHappyPathADT_A34WithMRG() {

        String hl7message = "MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|||ADT^A34||P|2.3||||\r"
                + "EVN|A40|20110613122406637||01\r"
                // Identifier value 234 with no system and no identifier type
                + "PID|1||123^^^^MR||||||||||||||||||||||||||||||||||||\r"
                // Identifier value 456 with no system and no identifier type
                + "MRG|456||||||\r";

        // Convert hl7 message
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = ResourceUtils.getResourceList(e, ResourceType.Patient);

        // There should be 2 - One for the PID segment and one for the MRG segment
        assertThat(patientResources).hasSize(2);

        // Get first patient and associated identifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();

        // Get second patient and associated identifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify patient one's identifier is set correctly (this is the patient build
        // from the PID segment)
        //
        // "identifier":[
        //     {
        //        "type":{
        //           "coding":[
        //              {
        //                 "system":"http://terminology.hl7.org/CodeSystem/v2-0203",
        //                 "code":"MR",
        //                 "display":"Medical record number"
        //              }
        //           ]
        //        },
        //        "value":"123"
        //     },
        //     {
        //        "use":"old",
        //        "value":"456"
        //     }
        // ]
           

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values are correct.
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("123");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("456");

        // Verify the system values are not present
        assertThat(patientOneIdentifierList.get(0).getSystem()).isNull();
        assertThat(patientOneIdentifierList.get(1).getSystem()).isNull();

        // Verify the first identifier has no use field.
        assertThat(patientOneIdentifierList.get(0).getUse()).isNull();
        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is set correctly for the first identifier
        CodeableConcept patientOneIdentifierType = patientOneIdentifierList.get(0).getType();
        assertThat(patientOneIdentifierType.getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(patientOneIdentifierType.getCoding().get(0).getDisplay()).isEqualTo("Medical record number");
        assertThat(patientOneIdentifierType.getCoding().get(0).getCode()).isEqualTo("MR");

        // Verify the identifier type for the second identifier is not present.
        assertThat(patientOneIdentifierList.get(1).getType().getCoding()).isEmpty();

        // Verify first patient has these fields
        //
        // "active":true,
        // "link":[
        //    {
        //       "other":{
        //          "reference":"Patient/expiringID-MRG"
        //       },
        //       "type":"replaces"
        //    }
        // ]

        // The first patient should be active.
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

        // Verify patient two's identifier is set correctly (this is the patient build
        // from the MRG segment)
        //
        // "identifier":[
        //     {
        //        "use":"old",
        //        "value":"456"
        //     }
        //  ]

        // Verify has only 1 identifier
        assertThat(patientTwoIdentifierList).hasSize(1);

        // Verify the identifier value is correct.
        assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("456");

        // Verify the system is not present
        assertThat(patientTwoIdentifierList.get(0).getSystem()).isNull();

        // Verify the identifier is marked as old
        assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is not present
        assertThat(patientTwoIdentifierList.get(0).getType().getCoding()).isEmpty();

        // Verify second patient has these fields.
        // "active":false,
        // "link":[
        //    {
        //       "other":{
        //          "reference":"Patient/survivingID"
        //       },
        //       "type":"replaced-by"
        //    }
        // ]

        // The second patient should NOT be active.
        assertThat(patientTwo.getActive()).isFalse();

        // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
        PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
        assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
        assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);
        ;

    }

    // Test ADT_A40 with one MRG segment.
    @Test
    void validateHappyPathADT_A40WithMRG() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\r"  
                + "EVN|A40|200301051530\r"
                // Identifier value MR1 with XYZ system and MR identifier type
                + "PID|||MR1^^^XYZ^MR||\r"
                // Identifier value MR2 with XYZ system and no identifier type
                + "MRG|MR2^^^XYZ\r";

        // Convert hl7 message
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = ResourceUtils.getResourceList(e, ResourceType.Patient);

        // There should be 2 - One for the PID segment and one for the MRG segment
        assertThat(patientResources).hasSize(2);

        // Get first patient and associated identifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();

        // Get second patient and associated identifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values are correct
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("MR1");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("MR2");

        // Verify the identifier systems are correct.
        assertThat(patientOneIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");
        assertThat(patientOneIdentifierList.get(1).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the first identifier has no use field.
        assertThat(patientOneIdentifierList.get(0).getUse()).isNull();
        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is set correctly for the first identifier
        CodeableConcept patientOneIdentifierType = patientOneIdentifierList.get(0).getType();
        assertThat(patientOneIdentifierType.getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(patientOneIdentifierType.getCoding().get(0).getDisplay()).isEqualTo("Medical record number");
        assertThat(patientOneIdentifierType.getCoding().get(0).getCode()).isEqualTo("MR");

        // Verify the identifier type for the second identifier is not present.
        assertThat(patientOneIdentifierList.get(1).getType().getCoding()).isEmpty();

        // The first patient should be active.
        assertThat(patientOne.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkOne = patientOne.getLink().get(0);
        assertThat(linkOne.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkOne.getOther().getReference()).isEqualTo(patientTwoId);

        /*-----------Verify Patient Two-----------*/

        // Verify patient two has only 1 identifier
        assertThat(patientTwoIdentifierList).hasSize(1);

        // Verify the identifier value is correct.
        assertThat(patientTwoIdentifierList.get(0).getValue()).isEqualTo("MR2");

        // Verify the system is not present
        assertThat(patientTwoIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the identifier value is marked as old
        assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);
        
        // Verify identifier type is not present
        assertThat(patientTwoIdentifierList.get(0).getType().getCoding()).isEmpty();

        // The second patient should NOT be active.
        assertThat(patientTwo.getActive()).isFalse();

        // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
        PatientLinkComponent linkTwo = patientTwo.getLink().get(0);
        assertThat(linkTwo.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
        assertThat(linkTwo.getOther().getReference()).isEqualTo(patientOneId);

    }

    // Tests ADT_A40 message with 2 MRG segments.
    // Suppress warnings about too many assertions in a test.  Justification: creating a FHIR message is very costly; we need to check many asserts per creation for efficiency.  
    @java.lang.SuppressWarnings("squid:S5961")
    @Test
    void validateTwoMRGs() {

        String hl7message = "MSH|^~\\&|REGADT|MCM|RSP1P8|MCM|200301051530|SEC|ADT^A40^ADT_A39|00000003|P|2.6\r"
                + "EVN|A40|200301051530\r"
                // Identifier value MR1 with XYZ system and MR identifier type
                + "PID|||MR1^^^XYZ^MR|||||||||||||||\r"
                // Identifier value MR1 with XYZ system and no identifier type
                + "MRG|MR2^^^XYZ||\r"
                // Identifier value MR1 with XYZ system and MR identifier type
                + "PID|||MR3^^^XYZ|||||||||||||||\r"
                // Identifier value MR1 with XYZ system and MR identifier type
                + "MRG|MR4^^^XYZ^MR||\r";

        // Convert hl7 message
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Find the patient resources in the FHIR bundle.
        List<Resource> patientResources = ResourceUtils.getResourceList(e, ResourceType.Patient);

        // There should be 4 - One for each PID segment and one for each MRG segment
        assertThat(patientResources).hasSize(4);

        // Get first patient and associated identifiers and id.
        Patient patientOne = PatientUtils.getPatientFromResource(patientResources.get(0));
        String patientOneId = patientOne.getId();
        List<Identifier> patientOneIdentifierList = patientOne.getIdentifier();

        // Get second patient and associated identifiers and id.
        Patient patientTwo = PatientUtils.getPatientFromResource(patientResources.get(1));
        String patientTwoId = patientTwo.getId();
        List<Identifier> patientTwoIdentifierList = patientTwo.getIdentifier();

        // Get third patient and associated identifiers and id.
        Patient patientThree = PatientUtils.getPatientFromResource(patientResources.get(2));
        String patientThreeId = patientThree.getId();
        List<Identifier> patientThreeIdentifierList = patientThree.getIdentifier();

        // Get fourth patient and associated identifiers and id.
        Patient patientFour = PatientUtils.getPatientFromResource(patientResources.get(3));
        String patientFourId = patientFour.getId();
        List<Identifier> patientFourIdentifierList = patientFour.getIdentifier();

        /*-----------Verify Patient One-----------*/

        // Verify the patient has two identifiers
        assertThat(patientOneIdentifierList).hasSize(2);

        // Verify the identifier values
        assertThat(patientOneIdentifierList.get(0).getValue()).isEqualTo("MR1");
        assertThat(patientOneIdentifierList.get(1).getValue()).isEqualTo("MR2");

        // Verify the identifier systems are correct.
        assertThat(patientOneIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");
        assertThat(patientOneIdentifierList.get(1).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the second identifier is marked as old
        assertThat(patientOneIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is set correctly for the first identifier
        CodeableConcept patientOneIdentifierType = patientOneIdentifierList.get(0).getType();
        assertThat(patientOneIdentifierType.getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(patientOneIdentifierType.getCoding().get(0).getDisplay()).isEqualTo("Medical record number");
        assertThat(patientOneIdentifierType.getCoding().get(0).getCode()).isEqualTo("MR");

        // Verify the identifier type for the second identifier is not present.
        assertThat(patientOneIdentifierList.get(1).getType().getCoding()).isEmpty();

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

        // Verify the system is not present
        assertThat(patientTwoIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the patient two identifier value is marked as old
        assertThat(patientTwoIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is not present
        assertThat(patientTwoIdentifierList.get(0).getType().getCoding()).isEmpty();

        // The second patient should NOT be active.
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

        // Verify the identifier systems are correct.
        assertThat(patientThreeIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");
        assertThat(patientThreeIdentifierList.get(1).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the second identifier is marked as old
        assertThat(patientThreeIdentifierList.get(1).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is not present for the first identifier
        assertThat(patientThreeIdentifierList.get(0).getType().getCoding()).isEmpty();

        // Verify identifier type is set correctly for the second identifier
        CodeableConcept patientThreeIdentifierTwoType = patientThreeIdentifierList.get(1).getType();
        assertThat(patientThreeIdentifierTwoType.getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(patientThreeIdentifierTwoType.getCoding().get(0).getDisplay()).isEqualTo("Medical record number");
        assertThat(patientThreeIdentifierTwoType.getCoding().get(0).getCode()).isEqualTo("MR");

        // The first patient should be active.
        assertThat(patientThree.getActive()).isTrue();

        // Verify link.other.reference references the MRG (2nd) patient with of a type of 'replaces'
        PatientLinkComponent linkThree = patientThree.getLink().get(0);
        assertThat(linkThree.getType()).isEqualTo(Patient.LinkType.REPLACES);
        assertThat(linkThree.getOther().getReference()).isEqualTo(patientFourId);

        /*-----------Verify Patient Four-----------*/

        // Verify patient has only 1 identifier
        assertThat(patientFourIdentifierList).hasSize(1);

        // Verify the identifier value
        assertThat(patientFourIdentifierList.get(0).getValue()).isEqualTo("MR4");

        // Verify the system is not present
        assertThat(patientFourIdentifierList.get(0).getSystem()).isEqualTo("urn:id:XYZ");

        // Verify the identifier is marked as old
        assertThat(patientFourIdentifierList.get(0).getUse()).isEqualTo(Identifier.IdentifierUse.OLD);

        // Verify identifier type is set correctly
        CodeableConcept patientFourIdentifierType = patientFourIdentifierList.get(0).getType();
        assertThat(patientFourIdentifierType.getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0203");
        assertThat(patientFourIdentifierType.getCoding().get(0).getDisplay()).isEqualTo("Medical record number");
        assertThat(patientFourIdentifierType.getCoding().get(0).getCode()).isEqualTo("MR");

        // The second patient should NOT be active.
        assertThat(patientFour.getActive()).isFalse();

        // We should have link.other.reference to the PID (1st) patient with a type of 'replaced-by'
        PatientLinkComponent linkFour = patientFour.getLink().get(0);
        assertThat(linkFour.getType()).isEqualTo(Patient.LinkType.REPLACEDBY);
        assertThat(linkFour.getOther().getReference()).isEqualTo(patientThreeId);

    }

}
