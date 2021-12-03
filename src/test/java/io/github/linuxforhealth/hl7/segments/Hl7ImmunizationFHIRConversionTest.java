/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7ImmunizationFHIRConversionTest {

    @Test
    public void testImmunization() throws IOException {

        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027||ER|||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r" //ORC.5 is here to prove RXA.20 is taking precedence
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A|20120901041038|||||^^^RI2056\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        List<Bundle.BundleEntryComponent> e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7VUXmessageRep);
        List<Resource> immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);
        Immunization resource = ResourceUtils.getResourceImmunization(immu.get(0), ResourceUtils.context);
        assertThat(resource).isNotNull();

        assertThat(resource.getStatus().getDisplay()).isEqualTo("completed"); // RXA.20
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
        assertThat(resource.getRecordedElement().toString().contains("2012-09-01")); //RXA.22
        String manufacturerRef = resource.getManufacturer().getReference();

        assertThat(resource.getLotNumber()).isEqualTo("33k2a"); // RXA.15
        assertThat(resource.getExpirationDate()).isEqualTo("2013-12-10"); // RXA.16

        //dose Quantity with an unknown system
        assertThat(resource.hasDoseQuantity()).isTrue();
        assertThat(resource.getDoseQuantity().getValue().toString()).isEqualTo("0.5");
        assertThat(resource.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(resource.getDoseQuantity().getCode()).isEqualTo("ML");
        assertThat(resource.getDoseQuantity().getSystem()).isEqualTo("urn:id:ISO+");

        String requesterRef1 = resource.getPerformer().get(0).getActor().getReference();
        Practitioner practBundle1 = ResourceUtils.getSpecificPractitionerFromBundleEntriesList(e, requesterRef1);
        assertThat(resource.getPerformer()).hasSize(3);
        assertThat(resource.getPerformer().get(0).getFunction().getCoding().get(0).getCode())
                .isEqualTo("OP"); // ORC.12
        assertThat(resource.getPerformer().get(0).getFunction().getText())
                .isEqualTo("Ordering Provider"); // ORC.12
        assertThat(resource.getPerformer().get(0).getActor().getReference().isEmpty()).isFalse(); // ORC.12
        assertThat(practBundle1.getNameFirstRep().getText()).isEqualTo("MARY Pediatric");
        assertThat(practBundle1.getNameFirstRep().getFamily()).isEqualTo("Pediatric");
        assertThat(practBundle1.getNameFirstRep().getGiven().get(0).toString()).isEqualTo("MARY");
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
        assertThat(practBundle2.getNameFirstRep().getGiven().get(0).toString()).isEqualTo("Nurse");

        String requesterRef3 = resource.getPerformer().get(2).getActor().getReference();
        Organization practBundle3 = ResourceUtils.getSpecificOrganizationFromBundleEntriesList(e, requesterRef3);
        assertThat(resource.getPerformer().get(2).getFunction().getCoding().get(0).getCode())
                .isEqualTo("AP"); // RXA.27.4 takes precedence over PV1.3.4 and RXA.11.4 (RXA.11.4 is present to prove RXA takes precedence)
        assertThat(resource.getPerformer().get(2).getFunction().getText())
                .isEqualTo("Administering Provider"); // RXA.27.4
        assertThat(resource.getPerformer().get(2).getActor().isEmpty()).isFalse(); // RXA.27.4
        assertThat(practBundle3.getName()).isEqualTo("RI2056");
        assertThat(practBundle3.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");
        assertThat(practBundle3.getIdentifierFirstRep().getValue()).isEqualTo("RI2056");

        // Looking for one Organization that matches the manufacturer reference
        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(2);
        Organization org = ResourceUtils.getResourceOrganization(organizations.get(1), ResourceUtils.context);
        assertThat(org.getName()).isEqualTo("sanofi");
        assertThat(org.getId()).isEqualTo(manufacturerRef);
        assertThat(org.hasContact()).isFalse();

        // Test that a ServiceRequest is not created for VXU_V04
        List<Resource> serviceRequestList = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        // Confirm that a serviceRequest was not created.
        assertThat(serviceRequestList).isEmpty();

        // Test should only return RXA.10, ORC.12  is empty
        hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^^||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7VUXmessageRep);
         immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);
        Immunization immunization = ResourceUtils.getResourceImmunization(immu.get(0), ResourceUtils.context);
        assertThat(immunization).isNotNull();
        Immunization.ImmunizationPerformerComponent performer1 = immunization.getPerformer().get(0);
        Immunization.ImmunizationPerformerComponent performer2 = immunization.getPerformer().get(1);

        assertThat(immunization.getPerformer()).hasSize(2);
        assertThat(performer1.getFunction().getCodingFirstRep().getCode()).isEqualTo("AP");// RXA.10
        assertThat(performer1.getFunction().getText()).isEqualTo("Administering Provider"); // RXA.10
        assertThat(performer2.getFunction().getCoding().get(0).getCode())
                .isEqualTo("AP"); // RXA.27.4 takes precedence over RXA.11.4
        assertThat(performer2.getFunction().getText())
                .isEqualTo("Administering Provider"); // RXA.27.4
        assertThat(performer2.getActor().isEmpty()).isFalse(); // RXA.27.4

        String requesterRef = immunization.getPerformer().get(1).getActor().getReference();
        Organization practBundle = ResourceUtils.getSpecificOrganizationFromBundleEntriesList(e, requesterRef);
        assertThat(practBundle.getName()).isEqualTo("RI2050");
        assertThat(practBundle.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");
        assertThat(practBundle.getIdentifierFirstRep().getValue()).isEqualTo("RI2050");

        assertThat(immunization.getStatus().getDisplay()).isEqualTo("completed"); // ORC.5 backup to rxa.20

        //dose Quantity without a system
        assertThat(immunization.hasDoseQuantity()).isTrue();
        assertThat(immunization.getDoseQuantity().getValue().toString()).isEqualTo("0.5");
        assertThat(immunization.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(immunization.getDoseQuantity().getSystem()).isNull();
        assertThat(immunization.getDoseQuantity().getCode()).isNull();

        // Test should only return RXA.10, ORC.12  is empty
        hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||+ "PV1|1|R|^^^RI2022|||||||||||||||||\r"
                + "ORC|RE||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal|CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        e = ResourceUtils
                .createFHIRBundleFromHL7MessageReturnEntryList(hl7VUXmessageRep);
        immu = ResourceUtils.getResourceList(e, ResourceType.Immunization);
        assertThat(immu).hasSize(1);
        immunization = ResourceUtils.getResourceImmunization(immu.get(0), ResourceUtils.context);

        assertThat(immunization).isNotNull();
        assertThat(immunization.hasReasonCode());
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getCode()).isEqualTo("00");
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getDisplay()).isEqualTo("refusal");
        assertThat(immunization.getReasonCodeFirstRep().getCodingFirstRep().getSystem()).isNull();
        assertThat(immunization.getReasonCodeFirstRep().getText()).isEqualTo("refusal");

        assertThat(immunization.getPerformer()).hasSize(2);
        assertThat(immunization.getPerformer().get(0).getFunction().getCodingFirstRep().getCode()).isEqualTo("AP");// RXA.10
        assertThat(immunization.getPerformer().get(0).getFunction().getText()).isEqualTo("Administering Provider"); // RXA.10

        requesterRef = immunization.getPerformer().get(1).getActor().getReference();
        practBundle = ResourceUtils.getSpecificOrganizationFromBundleEntriesList(e, requesterRef);
        assertThat(immunization.getPerformer().get(1).getFunction().getCoding().get(0).getCode())
                .isEqualTo("AP"); // PV1.3.4 takes precedence over RXA.11.4
        assertThat(immunization.getPerformer().get(1).getFunction().getText())
                .isEqualTo("Administering Provider"); // PV1.3
        assertThat(immunization.getPerformer().get(1).getActor().isEmpty()).isFalse(); // PV1.3.4
        assertThat(practBundle.getName()).isEqualTo("RI2050");
        assertThat(practBundle.getIdentifierFirstRep().getSystem()).isEqualTo("urn:id:extID");
        assertThat(practBundle.getIdentifierFirstRep().getValue()).isEqualTo("RI2050");

        //dose Quantity with a known system
        assertThat(immunization.hasDoseQuantity()).isTrue();
        assertThat(immunization.getDoseQuantity().getValue().toString()).isEqualTo("0.5");
        assertThat(immunization.getDoseQuantity().getUnit()).isEqualTo("ML");
        assertThat(immunization.getDoseQuantity().getSystem()).isEqualTo("http://unitsofmeasure.org");

        hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|999|ML^^UCUM||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX||00^refusal|CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        Immunization immunization3 = ResourceUtils.getImmunization(hl7VUXmessageRep);
        //dose Quantity with 999 as the value which should return null;
        assertThat(immunization3.hasDoseQuantity()).isFalse();
    }
    // TODO: 10/15/21 RXA-9 (also mapped to primarySource)
    //  RXA-18 statusReason
    //  RXA-20 (status, statusReason, isSubpotent)

    // The following checks for a situation where non-manufacturer RXA Immunizations interfered with the creation of manufacturer Immununization
    // The test will ensure the problem doesn't come back.
    @Test
    public void testMultipleImmunizationsNoInterference() throws IOException {

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
        assertThat(immunizations).hasSize(3);  // Exactly 3

        // This maps manufacturer to drug name from RXA records above
        Map<String, String> mapMfgNameToDrugName = new HashMap<>();
        mapMfgNameToDrugName.put("MERCK","MCV4-CRM");
        mapMfgNameToDrugName.put("sanofi","HIB PRP-T");
        // This maps Immunization drug name to manufacturer reference Id (the GUID)
        Map<String, String> mapDrugNameToMfgRefId = new HashMap<>();
        for (int immunizationIndex = 0; immunizationIndex < immunizations.size(); immunizationIndex++) { // condIndex is index for condition
            Immunization immunization = ResourceUtils.getResourceImmunization(immunizations.get(immunizationIndex), ResourceUtils.context);
            String mfgId = immunization.hasManufacturer() ? immunization.getManufacturer().getReference() : null;
            String codeText = immunization.getVaccineCode().getText();
            mapDrugNameToMfgRefId.put(codeText,mfgId);
        }

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(2); // Exactly 2
        // For each organization, we look up the name, get the known drug, get the manufacturer Id and see it is the same.
        for (int orgIndex = 0; orgIndex < organizations.size(); orgIndex++) { // orgIndex is index for organization
            Organization org = ResourceUtils.getResourceOrganization(organizations.get(orgIndex), ResourceUtils.context);
            assertThat( mapDrugNameToMfgRefId.get(mapMfgNameToDrugName.get(org.getName()))).contains(org.getId());
        }

    }
}