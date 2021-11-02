/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7PPRMessageTest {

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_min_VISIT_and_PROBLEM_groups(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_min_VISIT_and_PROBLEM_with_multiple_PROBLEM_OBSERVATION_groups(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r"
            + "OBX|1|ST|100||First Problem Observation|||||||X\r"
            + "OBX|2|ST|101||Second Problem Observation|||||||X\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_min_VISIT_and_PROBLEM_with_multiple_PROBLEM_OBSERVATION_groups_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r"
            + "OBX|1|TX|||Report line 1|||||||X\r"
            + "OBX|2|TX|||Report line 2|||||||X\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(0); //TODO: Expect this to be 1 when card #855 is completed

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(3); //TODO: Expect this to be 4 when card #855 is completed
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_full_VISIT_and_PROBLEM_with_min_ORDER_groups(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
            + "PV2|||||||||||||||||||||||||AI|||||||||||||C|\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1);

        // Confirm that no extra resources are created
        // TODO: When card 849 is completed, then there will be no DocumentReference and we should have exactly 4 resources.
        assertThat(e.size()).isEqualTo(5); 
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_multiple_PROBLEM_with_ORDER_groups(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            // start 1st PROBLEM group, with 2 ORDER groups
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "ORC|NW|1001^OE|99999990^RX|||E|^Q6H^D10^^^R\r"
            + "ORC|NW|1002^OE|99999991^RX|||E|^Q6H^D10^^^R\r"
            // start 2nd PROBLEM group, with 1 ORDER group
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "ORC|NW|1003^OE|99999992^RX|||E|^Q6H^D10^^^R\r"
            // start 3rd PROBLEM group, with no ORDER group
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(3);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(3);

        // Confirm that no extra resources are created
        // TODO: When card 849 is completed, then there will be no DocumentReference and we should have exactly 7 resources.
        assertThat(e.size()).isEqualTo(8);  
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_VISIT_and_PROBLEM_with_ORDER_group_with_OBXnonTX(String message) throws IOException {
        String hl7message = 
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
            // start PROBLEM group
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            // start ORDER group - ServReq, 2 Observations
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "OBX|1|NM|111^TotalProtein||7.5|gm/dl|5.9-8.4||||F\r"
            + "OBX|2|ST|100||An order Observation|||||||X\n"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResources = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResources).hasSize(1);

        // TODO: This should work when card 849 is completed
        // List<Resource> documentReferences = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        // assertThat(documentReferences).isEmpty(); 

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2); 
        
        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(7); //TODO: This should be 6 when the DocRef is removed in card 849
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_VISIT_and_PROBLEM_with_ORDER_group_withOBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"

            // start PROBLEM group
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"

            // ORDER group - ServReq, MedReq, DocRef
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r" 
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r" //MedicationRequest
            + "OBX|1|TX|1234||First line||||||F||||||\r"
            + "OBX|2|TX|12345||Second line||||||F||||||\r"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1);

        List<Resource> medRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medRequestResource).hasSize(1);
        
        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_VISIT_and_PROBLEM_with_multiple_full_ORDER_groups_OBXtypeTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"

            // start PROBLEM group, contains 2 ORDER groups
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"

            // 1st ORDER group - ServReq, DocRef
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "OBX|1|TX|1234||1st group - First line||||||F||||||\r"
            + "OBX|2|TX|12345||1st group - Second line||||||F||||||\r"
        
            //2nd ORDER group - ServReq, MedReq, DocRef
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r" 
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r" //MedicationRequest
            + "OBX|1|TX|1234||2nd group - First line||||||F||||||\r"
            + "OBX|2|TX|12345||2nd group - Second line||||||F||||||\r"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(2);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1); //TODO: This should be 2 when card 859 is completed

        List<Resource> medReqResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medReqResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(7); //TODO: This should be 8 when card 859 is completed
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_VISIT_and_PROBLEM_with_multiple_full_ORDER_groups_OBXnotTX(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"

            // start PROBLEM group, contains 2 ORDER groups
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"

            // 1st ORDER group - ServReq, 2 Observations
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "OBX|1|ST|101||1st group - First Order Observation|||||||X\r"
            + "OBX|2|ST|102||1st group - Second Order Observation|||||||X\r"
        
            //2nd ORDER group - ServReq, MedReq, 2 Observations
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r" 
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
            + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r" //MedicationRequest
            + "OBX|1|ST|101||2nd group - First Order Observation|||||||X\r"
            + "OBX|2|ST|102||2nd group - Second Order Observation|||||||X\r"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(2);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(4); 
    
        List<Resource> medReqResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medReqResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(11);  //TODO: This should be 10 when card 849 is completed which will remove the unwanted docref 
    }

    @ParameterizedTest
    @ValueSource(strings = { "PPR^PC1", /* "PPR^PC2", "PPR^PC3" */ })
    public void test_ppr_pc1_with_VISIT_and_PROBLEM_with_multiple_PROBLEM_OBSERVATIONs_and_multiple_full_ORDER_groups(String message) throws IOException {
        String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|" + message + "|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson|||M||||||||||||||\r"
            + "PV1||I|||||||||||||||||1400|||||||||||||||||||||||||199501102300\r"
            // start PROBLEM group, contains 2 PROBLEM_OBSERVATION groups and 2 ORDER groups
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            // + "NTE|1|P|Problem Comments\r"
            // + "NTE|2|P|Problem Comments Two\r"

                // 1st PROBLEM_OBSERVATION group - Observation
                + "OBX|1|NM|111^TotalProtein||7.1|gm/dl|5.9-8.4||||F\r" 
                // + "NTE|1|P|First Observation Comments One\r"
                // + "NTE|2|P|First Observation Comments Two\r"
            
                // 2nd PROBLEM_OBSERVATION group - Observation
                + "OBX|2|ST|100||Observation content|||||||X\r"

                // 1st ORDER group - ServReq, 2 Observations
                + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
                + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
                //+ "NTE|1|P|Order Comments One\r"
                //+ "NTE|2|P|Order Comments Two\r"
                //+ "NTE|3|P|Order Comments Three\r"
                    + "OBX|1|NM|111^TotalProtein||7.2|gm/dl|5.9-8.4||||F\r"
                    //+ "NTE|1|P|Observation Comments\r"
                    + "OBX|2|NM|111^TotalProtein||7.3|gm/dl|5.9-8.4||||F\r"
                    //+ "NTE|1|P|Observation Comments\r"
            
                //2nd ORDER group - ServReq, MedReq, DocRef
                + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r" 
                + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\r"
                + "RXO|RX800006^Test15 SODIUM 100 MG CAPSULE|100||mg|||||G||10||5\r" //MedicationRequest
                    // ORDER_OBSERVATION group
                    + "OBX|1|TX|1234||First line||||||F||||||\r"
                    + "OBX|2|TX|12345||Second line||||||F||||||\r"
            ;

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(2);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(4);
        
        List<Resource> medReqResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medReqResource).hasSize(1);

        List<Resource> docRefResource = ResourceUtils.getResourceList(e, ResourceType.DocumentReference);
        assertThat(docRefResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(11); 
    }
}
