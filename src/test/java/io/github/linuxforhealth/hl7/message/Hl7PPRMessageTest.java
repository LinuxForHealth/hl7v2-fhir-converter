/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class Hl7PPRMessageTest {
  private static FHIRContext context = new FHIRContext();

  @Test
  public void test_patient() throws IOException {
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, true, BundleType.COLLECTION);
    System.out.println(json);
    assertThat(json).isNotBlank();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(2);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);

    List<Resource> conditionresource =
        e.stream().filter(v -> ResourceType.Condition == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(conditionresource).hasSize(1);

  }



}
