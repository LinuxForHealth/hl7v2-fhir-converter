/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7ImmunizationFHIRConversionTest {

    // Suppress warnings about too many assertions in a test.  Justification: creating a FHIR message is very costly; we need to check many asserts per creation for efficiency.
    @java.lang.SuppressWarnings("squid:S5961")
    @Test
    void testImmunizationRXA20Priority() throws IOException {

        // RXA.20 is "completed" this takes precedence over rxa.18 having a value and orc.5
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027||ER|||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r" //ORC.5 is here to prove RXA.20 is taking precedence
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|00^Patient refusal^NIP002||PA|A|20120901041038\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CWE|31044-1^Reaction^LN|1|VXC9^Persistent, inconsolable crying lasting > 3 hours within 48 hours of dose^CDCPHINVS||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7VUXmessageRep);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);
        Immunization resource = ResourceUtils.getResourceImmunization(immu.get(0), ResourceUtils.context);
        assertThat(resource).isNotNull();

        assertThat(resource.getStatus().getDisplay()).isEqualTo("completed"); // RXA.20 is "completed" this takes precedence over rxa.18 having a value and orc.5
        assertThat(resource.hasStatusReason()).isTrue();
        assertThat(resource.getStatusReason().getCodingFirstRep().getCode()).isEqualTo("00"); //RXA.18
        assertThat(resource.getStatusReason().getCodingFirstRep().getSystem()).isEqualTo("urn:id:NIP002"); //RXA.18
        assertThat(resource.getStatusReason().getCodingFirstRep().getDisplay()).isEqualTo("Patient refusal"); //RXA.18
        assertThat(resource.getStatusReason().getText()).isEqualTo("Patient refusal");
        assertThat(resource.getIsSubpotent()).isTrue();
        assertThat(resource.getIdentifier().get(0).getValue()).isEqualTo("48-CVX"); // RXA.5.1 + 5.3
        assertThat(resource.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:extID");
        assertThat(resource.getVaccineCode().getCoding().get(0).getSystem())
                .isEqualTo("http://hl7.org/fhir/sid/cvx"); // RXA.5.3
        assertThat(resource.getVaccineCode().getCoding().get(0).getCode()).isEqualTo("48"); // RXA.5.1
        assertThat(resource.getVaccineCode().getText()).isEqualTo("HIB PRP-T"); // RXA.5.2
        assertThat(resource.getOccurrence()).hasToString("DateTimeType[2013-05-31]"); // RXA.3

        assertThat(resource.getReportOrigin().getCoding().get(0).getSystem()).isEqualTo("urn:id:NIP001");// RXA.9.3
        assertThat(resource.getReportOrigin().getCoding().get(0).getCode()).isEqualTo("00");// RXA.9.
        assertThat(resource.getReportOrigin().getCoding().get(0).getDisplay()).isEqualTo("new immunization record"); // RXA.9.2
        assertThat(resource.getReportOrigin().getText()).isEqualTo("new immunization record");// RXA.9.2
        assertThat(resource.getManufacturer().isEmpty()).isFalse(); // RXA.17
        assertThat(resource.hasRecorded()).isTrue(); //RXA.22
        assertThat(resource.getRecordedElement().toString()).contains("2012-09-01"); //RXA.22
        String manufacturerRef = resource.getManufacturer().getReference();

        assertThat(resource.getLotNumber()).isEqualTo("33k2a"); // RXA.15
        assertThat(resource.getExpirationDate()).isEqualTo("2013-12-10"); // RXA.16

        //dose Quantity with an unknown system
        assertThat(resource.hasDoseQuantity()).isTrue();
        assertThat(resource.getDoseQuantity().getValue()).hasToString("0.5");
        assertThat(resource.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(resource.getDoseQuantity().getCode()).isEqualTo("ML");
        assertThat(resource.getDoseQuantity().getSystem()).isEqualTo("urn:id:ISO+");

        String requesterRef1 = resource.getPerformer().get(0).getActor().getReference();
        Practitioner practBundle1 = ResourceUtils.getSpecificPractitionerFromBundleEntriesList(e, requesterRef1);
        assertThat(resource.getPerformer()).hasSize(2);
        assertThat(resource.getPerformer().get(0).getFunction().getCoding().get(0).getCode())
                .isEqualTo("OP"); // ORC.12
        assertThat(resource.getPerformer().get(0).getFunction().getText())
                .isEqualTo("Ordering Provider"); // ORC.12
        assertThat(resource.getPerformer().get(0).getActor().getReference()).isNotEmpty(); // ORC.12
        assertThat(practBundle1.getNameFirstRep().getText()).isEqualTo("MARY Pediatric");
        assertThat(practBundle1.getNameFirstRep().getFamily()).isEqualTo("Pediatric");
        assertThat(practBundle1.getNameFirstRep().getGiven().get(0)).hasToString("MARY");
        assertThat(practBundle1.getIdentifierFirstRep().getValue()).isEqualTo("MD67895");

        String requesterRef2 = resource.getPerformer().get(1).getActor().getReference();
        Practitioner practBundle2 = ResourceUtils.getSpecificPractitionerFromBundleEntriesList(e, requesterRef2);
        assertThat(resource.getPerformer().get(1).getFunction().getCoding().get(0).getCode())
                .isEqualTo("AP"); // RXA.10
        assertThat(resource.getPerformer().get(1).getFunction().getText())
                .isEqualTo("Administering Provider"); // RXA.10
        assertThat(resource.getPerformer().get(1).getActor().isEmpty()).isFalse(); // RXA.10
        assertThat(practBundle2.getNameFirstRep().getText()).isEqualTo("Nurse Sticker");
        assertThat(practBundle2.getNameFirstRep().getFamily()).isEqualTo("Sticker");
        assertThat(practBundle2.getNameFirstRep().getGiven().get(0)).hasToString("Nurse");

        // Immunization.Reaction Date (OBX.14) and Detail (OBX.5 if OBX 3 is 31044-1)
        assertThat(resource.getReactionFirstRep().getDateElement().toString()).contains("2013-05-31"); //OBX.14
        assertThat(resource.getReactionFirstRep().getDetail().hasReference()).isTrue(); //OBX.5
        // Looking for one Observation that matches the Reaction.Detail reference
        String reactionDetail = resource.getReactionFirstRep().getDetail().getReference();
        List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observations).hasSize(1);
        Observation obs = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);
        assertThat(obs.getId()).isEqualTo(reactionDetail);
        assertThat(obs.getCode().getCodingFirstRep().getDisplay()).isEqualTo("Persistent, inconsolable crying lasting > 3 hours within 48 hours of dose");
        assertThat(obs.getCode().getCodingFirstRep().getCode()).isEqualTo("VXC9");
        assertThat(obs.getCode().getCodingFirstRep().getSystem()).isEqualTo("urn:id:CDCPHINVS");
        assertThat(obs.getCode().getText()).isEqualTo("Persistent, inconsolable crying lasting > 3 hours within 48 hours of dose");
        assertThat(obs.getIdentifierFirstRep().getValue()).isEqualTo("197027-VXC9-CDCPHINVS");
        assertThat(obs.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");

        // Looking for one Organization that matches the manufacturer reference
        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1);
        Organization org = ResourceUtils.getResourceOrganization(organizations.get(0), ResourceUtils.context);
        assertThat(org.getName()).isEqualTo("sanofi");
        assertThat(org.getId()).isEqualTo(manufacturerRef);
        assertThat(org.hasContact()).isFalse();

        // Test that a ServiceRequest is not created for VXU_V04
        List<Resource> serviceRequestList = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        // Confirm that a serviceRequest was not created.
        assertThat(serviceRequestList).isEmpty();
    }

    @Test
    void testImmunizationReturnOnlyRXA10() throws IOException {

        // Test should only return RXA.10, ORC.12  is empty
        // RXA.18 is not empty which signals that the status is not-done. ORC.5 is here to show precedence
        // Since status is "not-done" we show the Status reason (RXA.18)
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027||CP|||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^^||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|00^Patient refusal^NIP002|||A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CWE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|DT|69764-9^HIB Info Sheet^LN|1|19981216||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);

        assertThat(immunization.getPerformer()).hasSize(1);
        assertThat(immunization.getPerformer().get(0).getFunction().getCodingFirstRep().getCode()).isEqualTo("AP");// RXA.10
        assertThat(immunization.getPerformer().get(0).getFunction().getText()).isEqualTo("Administering Provider"); // RXA.10
        assertThat(immunization.getStatus().getDisplay()).isEqualTo("not-done"); // RXA.18 is not empty which signals that the status is not-done. ORC.5 is here to show precedence
        assertThat(immunization.hasStatusReason()).isTrue(); // if status is "not-done" we show the Status reason
        assertThat(immunization.getStatusReason().getCodingFirstRep().getCode()).isEqualTo("00");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getSystem()).isEqualTo("urn:id:NIP002");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getDisplay()).isEqualTo("Patient refusal");

        //dose Quantity without a system
        assertThat(immunization.hasDoseQuantity()).isTrue();
        assertThat(immunization.getDoseQuantity().getValue()).hasToString("0.5");
        assertThat(immunization.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(immunization.getDoseQuantity().getSystem()).isNull();
        assertThat(immunization.getDoseQuantity().getCode()).isNull();

        assertThat(immunization.getProgramEligibilityFirstRep().getCodingFirstRep().getCode()).isEqualTo("V02");
        assertThat(immunization.getProgramEligibilityFirstRep().getCodingFirstRep().getSystem())
                .isEqualTo("urn:id:v2-0064");
        assertThat(immunization.getProgramEligibilityFirstRep().getCodingFirstRep().getDisplay())
                .isEqualTo("VFC eligible Medicaid/MedicaidManaged Care");
        assertThat(immunization.getProgramEligibilityFirstRep().getText())
                .isEqualTo("VFC eligible Medicaid/MedicaidManaged Care");

        assertThat(immunization.hasFundingSource()).isFalse();
        assertThat(immunization.hasReaction()).isFalse();
        //Education.DocumentType (OBX.5) shows under the condition that OBX.3.1 has 69764-9 as the code
        assertThat(immunization.getEducationFirstRep().getDocumentType()).isEqualTo("19981216");

    }

    @Test
    void testImmunizationUsePATOBJ() throws IOException {



        // Test should only return RXA.10, ORC.12  is empty
        // If RXA.20 is RE but RXA.18 is blank then we use PATOBJ from v3ActReason
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027||PA|||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal|RE|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CWE|30963-3^ VACCINE FUNDING SOURCE^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|29768-9^VIS Publication Date^LN|1|19981216||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);

        assertThat(immunization.hasReasonCode()).isTrue();
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("00");
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("refusal");
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isNull();
        assertThat(immunization.getReasonCodeFirstRep().getText()).isEqualTo("refusal");

        assertThat(immunization.getStatus().getDisplay()).isEqualTo("not-done");
        assertThat(immunization.hasStatusReason()).isTrue();
        assertThat(immunization.getStatusReason().getCodingFirstRep().getCode()).isEqualTo("PATOBJ"); // If RXA.20 is RE but RXA.18 is blank then we use PATOBJ from v3ActReason
        assertThat(immunization.getStatusReason().getCodingFirstRep().getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v3-ActReason");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getDisplay()).isEqualTo("Patient Refusal");
        assertThat(immunization.getStatusReason().getText()).isEqualTo("Patient refusal");

        //dose Quantity with a known system
        assertThat(immunization.hasDoseQuantity()).isTrue();
        assertThat(immunization.getDoseQuantity().getValue()).hasToString("0.5");
        assertThat(immunization.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(immunization.getDoseQuantity().getSystem()).isEqualTo("http://unitsofmeasure.org");

        //Education.PublicationDate shows under the condition that OBX.3.1 has 29768-9 as the code
        assertThat(immunization.getEducationFirstRep().hasPublicationDate()).isTrue();

        // If OBX.3 is 30963-3 the OBX.5 is for funding source
        assertThat(immunization.getFundingSource().getCodingFirstRep().getCode()).isEqualTo("V02");
        assertThat(immunization.getFundingSource().getCodingFirstRep().getSystem()).isEqualTo("urn:id:v2-0064");
        assertThat(immunization.getFundingSource().getCodingFirstRep().getDisplay())
                .isEqualTo("VFC eligible Medicaid/MedicaidManaged Care");
        assertThat(immunization.getFundingSource().getText()).isEqualTo("VFC eligible Medicaid/MedicaidManaged Care");

        assertThat(immunization.hasProgramEligibility()).isFalse();
        assertThat(immunization.hasReaction()).isFalse();

    }

    @Test
    void testImmunizationTestORC5BackupForRXA20() throws IOException {

        //ORC.5 backs up RXA.20 and RXA.18
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|||197027||PA|||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|999|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal||A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-6^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);
        //dose Quantity with 999 as the value which should return null;
        assertThat(immunization.hasDoseQuantity()).isFalse();
        assertThat(immunization.getStatus().getDisplay()).isEqualTo("completed"); //ORC.5 backs up RXA.20 and RXA.18
        assertThat(immunization.hasStatusReason()).isFalse();

        assertThat(immunization.hasReaction()).isFalse();
        assertThat(immunization.hasProgramEligibility()).isFalse();
        assertThat(immunization.hasFundingSource()).isFalse();

    }

    @Test
    void testImmunizationDefaultCompleted() throws IOException {
        //Status defaults to completed RXA.20,RXA.18 and ORC.5 are empty
        //Status reason is MEDPREC when OBX.3 is 30945-0 RXA-18 and RXA-20 not provided
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|999|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal||A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|30945-0^contraindication^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);

        assertThat(immunization.getStatus().getDisplay()).isEqualTo("completed"); //Status defaults to completed
        assertThat(immunization.hasStatusReason()).isTrue();
        assertThat(immunization.getStatusReason().getCodingFirstRep().getCode()).isEqualTo("MEDPREC");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v3-ActReason");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getDisplay()).isEqualTo("medical precaution");

    }

    @Test
    void testImmunizationReasonImmune() throws IOException {
        //Status reason is IMMUNE when OBX.3 is 59784-9 and RXA-18 and RXA-20 not provided
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|999|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal||A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|59784-9^Disease with presumed immunity^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);

        assertThat(immunization.getStatus().getDisplay()).isEqualTo("completed"); //Status defaults to completed
        assertThat(immunization.hasStatusReason()).isTrue();
        assertThat(immunization.getStatusReason().getCodingFirstRep().getCode()).isEqualTo("IMMUNE");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v3-ActReason");
        assertThat(immunization.getStatusReason().getCodingFirstRep().getDisplay()).isEqualTo("immunity");

    }

    @Test
    void testImmunizationOBX3is309567() throws IOException {
        // When OBX.3 is 30956-7 and RXA.5 is not provided we get a vaccine code from OBX.5
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531||999|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal||A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CWE|30956-7^vaccine type^LN|1|107^DTAP^CVX||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization = ResourceUtils.getImmunization(hl7VUXmessageRep);

        assertThat(immunization.getStatus().getDisplay()).isEqualTo("completed"); //Status defaults to completed
        assertThat(immunization.hasStatusReason()).isFalse();
        assertThat(immunization.hasReaction()).isFalse();
        assertThat(immunization.hasProgramEligibility()).isFalse();
        assertThat(immunization.hasFundingSource()).isFalse();

        // When OBX.3 is 30956-7 we get a vaccine code
        assertThat(immunization.hasVaccineCode()).isTrue();
        assertThat(immunization.getVaccineCode().getCodingFirstRep().getCode()).isEqualTo("107");
        assertThat(immunization.getVaccineCode().getCodingFirstRep().getDisplay()).isEqualTo("DTAP");
        assertThat(immunization.getVaccineCode().getCodingFirstRep().getSystem())
                .isEqualTo("http://hl7.org/fhir/sid/cvx");
        assertThat(immunization.getVaccineCode().getText()).isEqualTo("DTAP");

    }
    // TODO: 10/15/21 RXA-9 (also mapped to primarySource)

    // The following checks for a situation where non-manufacturer RXA Immunizations interfered with the creation of manufacturer Immununization
    // The test will ensure the problem doesn't come back.
    @Test
    void testMultipleImmunizationsNoInterference() throws IOException {

        String hl7VUXmessageRep = "MSH|^~\\&|||||20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1||||||||||\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                // First Immunization, also create manufacturer reference to Organization sanofi 
                + "ORC|RE||197027||ER||||||||||||RI2050\r" //ORC.5 is here to prove RXA.20 is taking precedence
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||||||||33k2a|20131210|PMC^sanofi^MVX|||CP|A|\r"
                // Second Immunization.  No manufacturer or Organization created.
                + "ORC|RE||IZ-783278^NDA||||||||||||||\r"
                + "RXA|0|1|20170513|20170513|62^HPV quadrivalent^CVX|999|||||^^^MIICSHORTCODE|||||||00^Parental Refusal^NIP002||RE\r"
                // Third Immunization, also create manufacturer reference to Organization merck 
                + "ORC|RE||IZ-783279^NDA||||||||||||||\r"
                + "RXA|0|1|20170513|20170513|136^MCV4-CRM^CVX^90734^MCV4-CRM^CPT|1|mL||||||||MRK1234|20211201|MSD^MERCK^MVX||||CP|A\r";
        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7VUXmessageRep);
        List<Resource> immunizations = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immunizations).hasSize(3); // Exactly 3

        // This maps manufacturer to drug name from RXA records above
        Map<String, String> mapMfgNameToDrugName = new HashMap<>();
        mapMfgNameToDrugName.put("MERCK", "MCV4-CRM");
        mapMfgNameToDrugName.put("sanofi", "HIB PRP-T");
        // This maps Immunization drug name to manufacturer reference Id (the GUID)
        Map<String, String> mapDrugNameToMfgRefId = new HashMap<>();
        for (int immunizationIndex = 0; immunizationIndex < immunizations.size(); immunizationIndex++) { // condIndex is index for condition
            Immunization immunization = ResourceUtils.getResourceImmunization(immunizations.get(immunizationIndex),
                    ResourceUtils.context);
            String mfgId = immunization.hasManufacturer() ? immunization.getManufacturer().getReference() : null;
            String codeText = immunization.getVaccineCode().getText();
            mapDrugNameToMfgRefId.put(codeText, mfgId);
        }

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(2); // Exactly 2
        // For each organization, we look up the name, get the known drug, get the manufacturer Id and see it is the same.
        for (int orgIndex = 0; orgIndex < organizations.size(); orgIndex++) { // orgIndex is index for organization
            Organization org = ResourceUtils.getResourceOrganization(organizations.get(orgIndex),
                    ResourceUtils.context);
            assertThat(mapDrugNameToMfgRefId.get(mapMfgNameToDrugName.get(org.getName()))).contains(org.getId());
        }

    }
}