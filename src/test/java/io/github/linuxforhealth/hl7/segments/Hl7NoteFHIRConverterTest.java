/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7NoteFHIRConverterTest {

  // Tests NTE creation for OBX (Observations) and ORC/OBRs (ServiceRequests)
  @Test
  public void testNoteCreationMutiple() {
    String hl7ORU = "MSH|^~\\&|||||20180924152907||ORU^R01^ORU_R01|213|T|2.3.1|||||||||||\n"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        // TODO: Future work, handle NTE's on PID
        // "NTE|1|O|TEST NOTE DD line 1|\n" + "NTE|2|O|TEST NOTE DD line 2 |\n" + "NTE|3|O|TEST NOTE D line 3|\n"  
        + "PV1|1|I||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
        + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|||||||||||||||||||||||||||\n"
        + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||||||||||||||||||||||||||||||||||||\n"
        + "NTE|1|O|TEST ORC/OBR NOTE AA line 1|\n" + "NTE|2|O|TEST NOTE AA line 2|\n" + "NTE|3|O|TEST NOTE AA line 3|\n"
        + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F||||||||||||||\n"
        + "NTE|1|L|TEST OBXa NOTE BB line 1|\n" + "NTE|2|L|TEST NOTE BB line 2|\n" + "NTE|3|L|TEST NOTE BB line 3|\n"
        + "OBX|2|NM|17853^MEAN BLOOD GLUCOSE^LRR^^^^^^MEAN BLOOD GLUCOSE||114.02|mg/dL|||||F||||||||||||||\n"
        + "NTE|1|L|TEST OBXb NOTE CC line 1|\n" + "NTE|2|L|TEST NOTE CC line 2|\n" + "NTE|3|L|TEST NOTE CC line 3|\n"
        + "NTE|4|L|TEST NOTE CC line 4|\n"
        + "SPM|1|||^^^^^^^^Blood|||||||||||||20180924152700|20180924152755||||||";

    List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7ORU);
    List<Resource> diagnosticReports = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
    assertThat(diagnosticReports).hasSize(1);

    // Two observations.  One has GLYCOHEMOGLOBIN and notes BB, One has GLUCOSE and notes CC
    List<Resource> observations = ResourceUtils.getResourceList(e, ResourceType.Observation);
    assertThat(observations).hasSize(2);
    Observation ObsGlucose = ResourceUtils.getResourceObservation(observations.get(0), ResourceUtils.context);
    Observation ObsHemoglobin = ResourceUtils.getResourceObservation(observations.get(1), ResourceUtils.context);
    // Figure out which is first and reassign if needed for testing
    if (ObsGlucose.getCode().getText() != "MEAN BLOOD GLUCOSE") {
      Observation temp = ObsGlucose;
      ObsGlucose = ObsHemoglobin;
      ObsHemoglobin = temp;
    }
    assertThat(ObsGlucose.hasNote()).isTrue();
    assertThat(ObsGlucose.getNote()).hasSize(1);
    // Processing adds "  \n" two spaces and a line feed
    // Note on test strings. Must double back-slashes to create single backslash in string.
    assertThat(ObsGlucose.getNote().get(0).getText()).isEqualTo(
        "TEST OBXb NOTE CC line 1  \\nTEST NOTE CC line 2  \\nTEST NOTE CC line 3  \\nTEST NOTE CC line 4  \\n");
    assertThat(ObsHemoglobin.hasNote()).isTrue();
    assertThat(ObsHemoglobin.getNote()).hasSize(1);
    assertThat(ObsHemoglobin.getNote().get(0).getText())
        .isEqualTo("TEST OBXa NOTE BB line 1  \\nTEST NOTE BB line 2  \\nTEST NOTE BB line 3  \\n");

    // One ServiceRequest contains NTE for ORC/OBR
    List<Resource> serviceRequests = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
    assertThat(serviceRequests).hasSize(1);
    ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequests.get(0),
        ResourceUtils.context);
    assertThat(serviceRequest.hasNote()).isTrue();
    assertThat(serviceRequest.getNote()).hasSize(1);
    assertThat(serviceRequest.getNote().get(0).getText())
        .isEqualTo("TEST ORC/OBR NOTE AA line 1  \\nTEST NOTE AA line 2  \\nTEST NOTE AA line 3  \\n");
  }

}
